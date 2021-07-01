package org.my.pro.dhtcrawler.zdemo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 每个DHTServer对应多个本地DHT节点, 每个本地DHT节点监听一个端口, 每个DHTServer对象都有一个工作线程worker,
 * 这个线程负责当前DHTServer对象维护的所有节点的数据读写及逻辑处理, 这些操作都是非阻塞的,
 * 因此在多核处理器上创建和处理器数量相同的DHTServer对象就能最大限度利用CPU资源 每个DHTServer之间都是相互独立的,
 * 不能存在数据共享和争用, 由于每个DHTServer内部的所有操作都是单线程, 所以所有操作都不需要进行线程同步, 最大限度消除同步控制的开销
 * 
 * @author dgqjava
 *
 */
public class DHTServer {
	private static final NodeInfo[] ROOT_NODES; // DHT根节点, 这是几个长期稳定的公用节点,
												// 通过这几个节点查找其他节点来初始化路由表

	private final List<LocalDHTNode> localDHTNodes = new ArrayList<>(); // 这里面是当前DHTServer对象管理的本地的DHT节点
	private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(); // 这个线程负责处理所有的操作,
																									// 包括
																									// :
																									// NIO数据读写,
																									// 定时处理外部节点发送过来的请求数据并响应,
																									// 定时发起查找find_node请求来更新本地路由表及让更多的其他节点认识我们
	private final Map<String, LocalDHTNode> id2LocalDHTNode = new HashMap<>(); // 本地的nodeId和本地DHT节点的映射表
	private final NIOHelper nioHelper = new NIOHelper(worker); // NIOHelper类里封装了所有NIO操作,
																// 这样在DHTServer里只需要负责处理其他节点的请求数据和发送find_node等业务处理的逻辑而不需要关注NIO相关的所有代码

	static {
		try {
			ROOT_NODES = new NodeInfo[] { // 初始化DHT根节点
					new NodeInfo(InetAddress.getByName("router.utorrent.com").getHostAddress(), 6881, null),
					new NodeInfo(InetAddress.getByName("dht.transmissionbt.com").getHostAddress(), 6881, null),
					new NodeInfo(InetAddress.getByName("router.bittorrent.com").getHostAddress(), 6881, null) };
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 创建一个DHTServer对象并监听到多个端口, 每个端口关联一个唯一的nodeId
	 * 
	 * @param ports
	 *            端口列表
	 * @param ids
	 *            nodeId列表
	 */
	public DHTServer(List<Integer> ports, List<String> ids) {
		// 监听指定的端口, 并初始化数据
		for (int i = 0; i < ids.size(); i++) {
			String id = ids.get(i);
			int port = ports.get(i);

			// 为当前节点创建一个路由表, 这里的192.168.0.1可以是任意一个合法的ip地址,
			// 只是为了NodeInfo的构造方法不报NPE
			RoutingList routingList = new RoutingList(new NodeInfo("192.168.0.1", port, id));

			// 所有未请求或者短时间内未重复请求的节点放到这个栈空间内,
			// 最新得到的节点信息放到栈顶, 优先向最新的节点发送find_node请求, 因为最新的节点是活动的概率最大,
			// 如果栈内节点数超过10000, 则将他减半防止内存占用过多, 如果栈内节点数为0, 则将根节点重新加入栈, 开始新的一轮查找,
			// 这样确保我们的节点一直在不停地 发送find_node来认识更多的其他节点
			Stack<NodeInfo> newNodes = new Stack<>();
			for (NodeInfo nodeInfo : ROOT_NODES) {
				newNodes.push(nodeInfo);
			}

			// 我们本地的DHT节点
			LocalDHTNode dhtNode = new LocalDHTNode(id, newNodes, routingList);

			// 添加一个端口监听
			nioHelper.bind(ports.get(i), id);

			// 添加id和本地节点映射关系
			id2LocalDHTNode.put(id, dhtNode);

			// 添加新的本地节点到本地节点列表
			localDHTNodes.add(dhtNode);
		}

		// 每隔10毫秒通过nioHelper对象从队列中读取要处理的数据, 这些数据是外部节点请求或者回复我们的数据
		worker.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					// 每隔10毫秒都会把队列里要处理的数据耗尽, 由于所有操作都是非阻塞且单线程的, 所以这些操作都会非常快速的完成,
					// 不会影响其他功能
					while (!nioHelper.getReadDataQueue().isEmpty()) {
						// 获取待处理的数据
						NIOHelper.ReadData readData = nioHelper.getReadDataQueue().poll();
						InetSocketAddress remoteAddress = (InetSocketAddress) readData.getRemoteAddress();
						LocalDHTNode localDHTNode = id2LocalDHTNode.get(readData.getId());
						Stack<NodeInfo> newNodes = localDHTNode.getNewNodes();
						Set<String> oldNodes = localDHTNode.getOldNodes();
						RoutingList routingList = localDHTNode.getRoutingList();

						// 将收到的数据转化为B编码字典对象
						BencodeMap response = BencodeMap.getMap(new String(readData.getData(), "iso-8859-1"), 0);

						// 当y为r时为其他节点对我们请求的回复, y为q时为其他节点对我们的请求, y为e时表示错误,
						// 我们这里只处理请求和回复两种数据
						String y = new String(response.get(new BencodeString("y")).getData(), "iso-8859-1");
						if (y.equals("r")) {
							// 获取数据来源方的nodeId
							String remoteId = new String(((BencodeMap) (response.get(new BencodeString("r"))))
									.get(new BencodeString("id")).getData(), "iso-8859-1");

							// 将请求发送方的信息保存到路由表, 因为所有对我们发送请求或者响应请求的节点肯定都是活跃节点,
							// 路由表中的桶使用LUR算法实现, 直接替换掉最旧的节点
							routingList.addNode(new NodeInfo(remoteAddress.getAddress().getHostAddress(),
									remoteAddress.getPort(), remoteId));

							// 获取响应的的节点列表并解析节点信息, 每个节点信息长度为26, 包括20字节的节点id,
							// 4字节的ip, 2字节的端口,
							// 由于我们只发送find_node请求, 因此所有响应类型的数据都是对find_node的响应
							byte[] nodes = ((BencodeMap) (response.get(new BencodeString("r"))))
									.get(new BencodeString("nodes")).getData();
							for (int i = 0; i < nodes.length; i += 26) {
								// 获取节点id, 端口和ip
								String nodeId = new String(Arrays.copyOfRange(nodes, i, i + 20), "iso-8859-1");
								byte[] ipPort = Arrays.copyOfRange(nodes, i + 20, i + 26);
								String ip = (ipPort[0] & 0xFF) + "." + (ipPort[1] & 0xFF) + "." + (ipPort[2] & 0xFF)
										+ "." + (ipPort[3] & 0xFF);
								int port = ByteBuffer.wrap(new byte[] { 0, 0, ipPort[4], ipPort[5] }).getInt();

								// 判断得到的节点信息是否短期内已经请求过, 这里简单的用ip+端口来判断,
								// 如果没有请求过则加入到新节点栈中
								boolean isContain = oldNodes.contains(new String(ipPort, "iso-8859-1"));
								if (!isContain) {
									newNodes.push(new NodeInfo(ip, port, nodeId));
								}
							}
							continue;
						}

						// y为q时的数据是其他节点对我们的请求, 需要对其他节点的请求进行响应
						if (y.equals("q")) {
							// 获取数据来源方的nodeId
							String remoteId = new String(((BencodeMap) (response.get(new BencodeString("a"))))
									.get(new BencodeString("id")).getData(), "iso-8859-1");

							// 将请求发送方的信息保存到路由表, 因为所有对我们发送请求或者响应请求的节点肯定都是活跃节点,
							// 路由表中的桶使用LUR算法实现, 直接替换掉最旧的节点
							routingList.addNode(new NodeInfo(remoteAddress.getAddress().getHostAddress(),
									remoteAddress.getPort(), remoteId));

							// 获取数据来源方的transactionId,
							// 对请求进行响应时必须带上请求中的transactionId,
							// 这样对方才知道这个响应针对的是哪个请求
							String t = new String(response.get(new BencodeString("t")).getData(), "iso-8859-1");

							// 获取请求类型, 请求类型一共四种 : ping, find_node, get_peers,
							// announce_peer
							String q = new String(response.get(new BencodeString("q")).getData(), "iso-8859-1");

							// 处理ping请求, 这个是对方检查我们节点是否存活
							if (q.equals("ping")) {
								responsePing(readData.getId(), remoteAddress, t);
								continue;
							}

							// 处理find_node请求, 这个是对方向我们节点查找距离目标节点最近的8个节点
							if (q.equals("find_node")) {
								// 获取要查找的目标节点id
								String target = new String(((BencodeMap) (response.get(new BencodeString("a"))))
										.get(new BencodeString("target")).getData(), "iso-8859-1");

								// 获取距离目标节点id最近的八个节点信息并响应
								responseFindNode(readData.getId(), remoteAddress, t,
										routingList.getNearestNodes(new NodeId(target.getBytes("iso-8859-1"))));
								continue;
							}

							// 处理get_peers请求, 这个是对方向我们查询可以下载到指定磁力链接数据的节点信息,
							// 如果我们没有可下载的节点则提供最近的8个节点返回, 因为我们不保存peer的信息,
							// 所以我们这里每次都只返回最近的八个节点
							if (q.equals("get_peers")) {
								// 获取infoHash, 这个就能转化为磁力链接,
								// get_peers中得到的infoHash有可能是无效的
								String infoHash = new String(((BencodeMap) (response.get(new BencodeString("a"))))
										.get(new BencodeString("info_hash")).getData(), "iso-8859-1");

								// 转化为磁力链接并打印
								printMagnet(infoHash, "get_peers");

								// 获取距离infoHash最近的八个节点信息并响应
								responseGetPeers(readData.getId(), remoteAddress, t,
										routingList.getNearestNodes(new NodeId(infoHash.getBytes("iso-8859-1"))));
								continue;
							}

							// 处理announce_peer请求, 这个是对方告诉我们他已经找到了文件的下载地址,
							// 我们这里只获取他正在下载的磁力链接, 这里获取的磁力链接基本上是有效的
							if (q.equals("announce_peer")) {
								// 获取infoHash
								String infoHash = new String(((BencodeMap) (response.get(new BencodeString("a"))))
										.get(new BencodeString("info_hash")).getData(), "iso-8859-1");

								// 转化为磁力链接并打印
								printMagnet(infoHash, "announce_peer");

								// 响应对方的announce_peer请求
								responseAnnouncePeer(readData.getId(), remoteAddress, t);
								continue;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 0, 10, TimeUnit.MILLISECONDS);

		// 每秒对当前DHTServer维护的所有本地节点执行一次find_node操作, 让更多的节点认识我们,
		// 这样才能有更多的节点发送磁力链接到我们节点
		worker.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					// 遍历本地节点
					for (LocalDHTNode localDHTNode : localDHTNodes) {
						Stack<NodeInfo> newNodes = localDHTNode.getNewNodes();
						Set<String> oldNodes = localDHTNode.getOldNodes();
						String id = localDHTNode.getId();

						// 如果新节点列表为空, 则重新加入根节点, 重新开始查找
						if (newNodes.isEmpty()) {
							newNodes.push(ROOT_NODES[0]);
							newNodes.push(ROOT_NODES[1]);
							newNodes.push(ROOT_NODES[2]);
						}

						// 如果新节点数量过多, 防止占用过多内存, 移除一半的最旧节点, 这里用栈实现, 每次移除栈底的元素
						if (newNodes.size() > 10000) {
							int count = newNodes.size() / 2;
							for (int i = 0; i < count; i++) {
								newNodes.remove(0);
							}
						}

						// 获取最新的节点, 用来发送find_node请求
						NodeInfo latestNode = newNodes.pop();

						// 获取最新节点的ip和端口
						InetSocketAddress target = new InetSocketAddress(latestNode.getIp(), latestNode.getPort());

						// 发送find_node请求
						findNode(localDHTNode.getId(), target, randomTargetId(id));

						// 将请求过的节点加入到旧节点列表中
						byte[] ipData = latestNode.getIpData();
						byte[] portData = latestNode.getPortData();
						oldNodes.add(new String(
								new byte[] { ipData[0], ipData[1], ipData[2], ipData[3], portData[0], portData[1] },
								"iso-8859-1"));

						// 如果旧节点数量过多, 防止占用过多内存, 移除一半最旧的旧节点,
						// 这里的oldNodes是用LinkedHashSet实现, 因此按顺序移除将先移除最旧的
						if (oldNodes.size() > 10000) {
							int count = oldNodes.size() / 2;
							for (Iterator<String> it = oldNodes.iterator(); it.hasNext() && count-- > 0; it.remove()) {
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	/**
	 * 发送findNode请求
	 * 
	 * @param id
	 *            本地节点的id
	 * @param target
	 *            目标节点的地址
	 * @param targetId
	 *            目标节点的id
	 */
	public void findNode(String id, InetSocketAddress target, String targetId) {
		try {
			// 发送请求时候需要带一个transactionId, 这里生成两个字节的随机字符
			ByteBuffer bb = ByteBuffer.allocate(2).putShort((short) new Random().nextInt(Short.MAX_VALUE));
			bb.flip();
			String transactionId = new String(bb.array(), "iso-8859-1");

			// 请求数据放入到队列中等待异步发送
			nioHelper.write(id,
					new NIOHelper.WriteData(DHTHelper.getFindNodeData(transactionId, id, targetId), target));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 响应ping请求
	 * 
	 * @param id
	 *            本地节点的id
	 * @param target
	 *            目标节点的地址
	 * @param t
	 *            目标节点请求时发送过来的transactionId
	 */
	public void responsePing(String id, InetSocketAddress target, String t) {
		// 响应数据放入到队列中等待异步发送
		nioHelper.write(id, new NIOHelper.WriteData(DHTHelper.getPingResponseData(t, id), target));
	}

	/**
	 * 响应find_node请求
	 * 
	 * @param id
	 *            本地节点的id
	 * @param target
	 *            目标节点的地址
	 * @param t
	 *            目标节点请求时发送过来的transactionId
	 * @param nodes
	 *            回复给目标节点的最近的八个节点信息
	 */
	public void responseFindNode(String id, InetSocketAddress target, String t, List<NodeInfo> nodes) {
		// 响应数据放入到队列中等待异步发送
		nioHelper.write(id, new NIOHelper.WriteData(DHTHelper.getFindNodeResponseData(t, id, nodes), target));
	}

	/**
	 * 响应get_peers请求
	 * 
	 * @param id
	 *            本地节点的id
	 * @param target
	 *            目标节点的地址
	 * @param t
	 *            目标节点请求时发送过来的transactionId
	 * @param nodes
	 *            回复给目标节点的最近的八个节点信息
	 */
	public void responseGetPeers(String id, InetSocketAddress target, String t, List<NodeInfo> nodes) {
		// 响应数据放入到队列中等待异步发送
		nioHelper.write(id, new NIOHelper.WriteData(DHTHelper.getGetPeersResponseData(t, id, nodes), target));
	}

	/**
	 * 响应announce_peer请求
	 * 
	 * @param id
	 *            本地节点的id
	 * @param target
	 *            目标节点的地址
	 * @param t
	 *            目标节点请求时发送过来的transactionId
	 */
	public void responseAnnouncePeer(String id, InetSocketAddress target, String t) {
		// 响应数据放入到队列中等待异步发送
		nioHelper.write(id, new NIOHelper.WriteData(DHTHelper.getAnnouncePeerResponseData(t, id), target));
	}

	/**
	 * 打印得到的磁力链接
	 * 
	 * @param infoHash
	 * @param source
	 *            获得磁力链接的来源 : 其他节点的get_peers和announce_peer请求
	 */
	@SuppressWarnings("deprecation")
	private static void printMagnet(String infoHash, String source) {
		StringBuilder magnet = new StringBuilder(new Date().toLocaleString()).append(" magnet:?xt=urn:btih:");

		// infoHash的长度为20字节, 每个字节的值转为16进制后就是磁力链接
		for (char c : infoHash.toCharArray()) {
			String hs = Integer.toHexString(c);
			// 如果转为16进制后的长度不足2位则第一位补0
			if (hs.length() == 1) {
				magnet.append(0);
			}
			magnet.append(hs);
		}
		System.out.println(source + " " + magnet);
	}

	/**
	 * 根据一个节点id随机获取一个目标节点id, 如果每次都只根据自身的节点id进行find_node操作很容易就会遍历完所有的节点导致无节点可遍历,
	 * 因此这里根据节点的距离按照一定概率生成一个随机的目标节点id, 距离越远的目标节点id生成的概率越小
	 * 
	 * @param id
	 *            本地节点的id
	 * @return
	 */
	private static String randomTargetId(String id) {
		int i = ThreadLocalRandom.current().nextInt();
		// 有百分之一的概率产生一个基本完全随机的节点id
		if (i % 100 == 0) {
			return (char) (i % 256) + UUID.randomUUID().toString().substring(0, 19);
		}

		int i2 = ThreadLocalRandom.current().nextInt();
		// 十六分之一的概率修改原本节点id的最后一个字节, 修改的字节越靠后, 被其他节点保存的概率越大
		if ((i & 0B1111) == (i2 & 0B1111)) {
			char[] cs = id.toCharArray();
			cs[cs.length - 1] = (char) (i & 0B011111111);
			return new String(cs);
		}
		return id;
	}
}
