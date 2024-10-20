package org.my.pro.dhtcrawler.task;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.DownloadTorrent;
import org.my.pro.dhtcrawler.Future;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.btdownload.Bep09MetadataFiles;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.routingTable.DefaultNodeInfo;
import org.my.pro.dhtcrawler.util.DHTUtils;

import be.adaxisoft.bencode.BEncodedValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 下载torrent任务
 */
public class TryFindPeerAndDownload implements DownloadTorrent, DHTTask {

	private ExecutorService tryFindPeerExe;
	private ExecutorService downloadTorrentExe;
	private static Log log = LogFactory.getLog(TryFindPeerAndDownload.class);

	private volatile boolean state = false;

	private ConcurrentHashMap<String, Object> downingHash;
	private Object empty = new Object();

	private HashSet<LocalDHTNode> localDHTNodes = new HashSet<LocalDHTNode>();

	private static volatile TryFindPeerAndDownload instance;

	private TryFindPeerAndDownload() {

	}

	/**
	 * 双锁懒汉单例
	 * 
	 * @return
	 */
	public static TryFindPeerAndDownload getInstance() {
		if (null == instance) {
			synchronized (TryFindPeerAndDownload.class) {
				if (null == instance) {
					instance = new TryFindPeerAndDownload();
				}
			}
		}

		return instance;
	}

	//
	private ConcurrentSkipListMap<BigInteger, Node> allNodes;
	//

	/**
	 * 只有哈希
	 * 
	 * @param hash
	 */
	public void subTask(byte[] hash) {
		if (!state) {
			return;
		}
		// 如果提交哈希正在执行下载,防止连续提交
		if (downingHash.containsKey(DHTUtils.byteArrayToHexString(hash))) {
			return;
		}
		//
		tryFindPeerExe.execute(new RunTask(hash));
	}

	/**
	 * 直接提交IP端口下载
	 * @param ip
	 * @param port
	 * @param hash
	 */
	public void subTask(String ip, int port, byte[] hash) {
		if (!state) {
			return;
		}

		downloadTorrentExe.execute(new Runnable() {

			@Override
			public void run() {
				// 给随机一个本地peer ID
				Bep09MetadataFiles bep09MetadataFiles = new Bep09MetadataFiles(hash, DHTUtils.generatePeerId(), ip,
						port);
				bep09MetadataFiles.tryDownload();
			}
		});
	}

	@Override
	public synchronized void register(LocalDHTNode localDHTNode) {
		localDHTNodes.add(localDHTNode);
	}

	private static int MAX_NODESNUM = 50000;

	@Override
	public void addNode(Node node) {
		if (!state) {
			return;
		}
		// 如果超过最大数量，删除一个节点再添加
		if (allNodes.size() > MAX_NODESNUM) {
			allNodes.remove(allNodes.firstKey());
		}
		// 按照最大ID值做距离计算确定在TreeMap中的位置
		byte[] distance = DHTUtils.xorDistance(DHTUtils.MAX_NODE_ID, node.nodeId().bsId());
		BigInteger key = DHTUtils.distanceAsBigInteger(distance);

		allNodes.put(key, node);

	}

	/**
	 * 随机抓一个节点用于执行find_peers
	 * 
	 * @return
	 */
	private LocalDHTNode randomGet() {

		int index = DHTUtils.rng.nextInt(0, localDHTNodes.size());

		int i = 0;
		for (LocalDHTNode node : localDHTNodes) {
			if (i == index) {
				return node;
			}
			i++;
		}
		return null;
	}

	private List<Node> findNearest(byte[] hash) {
		byte[] distance = DHTUtils.xorDistance(DHTUtils.MAX_NODE_ID, hash);
		BigInteger key = DHTUtils.distanceAsBigInteger(distance);

		// 从ke左右找出Node列表
		List<Node> rs = new ArrayList<Node>();

		fillInLocalNodes(rs, 0, false, key);

		//
		return rs;
	}

	private void fillInLocalNodes(List<Node> list, int count, boolean change, BigInteger key) {
		if (list.size() == 8) {
			return;
		}
		// 填充
		if (count < 4 && !change) {
			Entry<BigInteger, Node> enty = allNodes.lowerEntry(key);
			if (null == enty) { // 换方向
				fillInLocalNodes(list, count, true, key);
			}
			list.add(enty.getValue());
			fillInLocalNodes(list, count++, false, enty.getKey());
			if (count == 4) { // 换方向
				fillInLocalNodes(list, count, true, key);
			}
		}
		// 换方向填充
		if (change) {
			Entry<BigInteger, Node> enty = allNodes.higherEntry(key);
			if (null == enty) {
				return;
			}
			list.add(enty.getValue());
			fillInLocalNodes(list, count++, true, enty.getKey());
		}
	}

	class RunTask implements Runnable {

		private byte[] hash;

		public RunTask(byte[] hash) {
			super();
			this.hash = hash;
		}

		@Override
		public void run() {

			//
			byte[] targetHash = hash;
			//
			List<Node> nodes = findNearest(targetHash);

			downingHash.put(DHTUtils.byteArrayToHexString(targetHash), empty);
			try {

				log.info("开始尝试下载:" + DHTUtils.byteArrayToHexString(targetHash) + "node节点数:" + nodes.size());
				List<Node> peers = new ArrayList<Node>();
				for (Node n : nodes) {
					getPerrs(peers, n.ip(), n.port(), targetHash);
				}
				log.info(DHTUtils.byteArrayToHexString(targetHash) + "找到peer数:" + peers.size());
				for (Node peer : peers) {
					subTask(peer.ip(), peer.port(), targetHash);
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			//
			downingHash.remove(DHTUtils.byteArrayToHexString(targetHash));

		}

		public void getPerrs(List<Node> peers, String ip, int port, byte[] hash) {
			// 给随机一个nodeId
			KrpcMessage get_peers = MessageFactory.createGet_peers(ip, port, DHTUtils.generateNodeId(), hash);
			// 可能返回nodeList或peerList
			Future future = randomGet().call(get_peers);
			KrpcMessage response = future.getValue();
			try {

				DefaultResponse defaultResponse = (DefaultResponse) response;

				// 发现新节点,针对find_node响应
				if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
					byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

					ByteBuf byteBuf = Unpooled.wrappedBuffer(bs);
					int num = bs.length / 26;
					// 添加新节点
					for (int i = 0; i < num; i++) {
						Node info = DHTUtils.readNodeInfo(byteBuf);
						if (peers.size() < 8) {
							getPerrs(peers, info.ip(), info.port(), hash);
						}
					}

				}
				// 响应包含VALUE，针对get_peer的响应
				if (defaultResponse.r().getMap().containsKey(KeyWord.VALUES)) {
					try {
						List<BEncodedValue> list = defaultResponse.r().getMap().get(KeyWord.VALUES).getList();

						for (BEncodedValue bv : list) {
							ByteBuf byteBuf = Unpooled.wrappedBuffer(bv.getBytes());
							Node peer = readIpPort(byteBuf);
							log.info("找到Peer:" + peer.ip() + ":" + peer.port());
							peers.add(peer);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		public Node readIpPort(ByteBuf buffer) {
			byte[] ip = new byte[4];
			byte[] port = new byte[2];
			buffer.readBytes(ip);
			buffer.readBytes(port);
			Node info = new DefaultNodeInfo(null, ip, port);
			return info;
		}

	}

	@Override
	public synchronized void start() {
		if (state) {
			return;
		}
		state = true;

		tryFindPeerExe = Executors.newFixedThreadPool(8);
		downloadTorrentExe = Executors.newCachedThreadPool();
		downingHash = new ConcurrentHashMap<String, Object>();
		allNodes = new ConcurrentSkipListMap<BigInteger, Node>(new NodeComParator());
		log.info("Bt下载模块启动");
	}

	@Override
	public void stop() {
		if (!state) {
			return;
		}
		state = false;

		tryFindPeerExe.shutdown();
		downloadTorrentExe.shutdown();
		downingHash.clear();
		allNodes.clear();
		log.info("Bt下载模块关闭");
	}

	class NodeComParator implements Comparator<BigInteger> {

		@Override
		public int compare(BigInteger o1, BigInteger o2) {
			return o1.compareTo(o2);
		}

	}
}
