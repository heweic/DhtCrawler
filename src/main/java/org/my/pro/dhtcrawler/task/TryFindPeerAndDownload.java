package org.my.pro.dhtcrawler.task;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.DownloadTorrent;
import org.my.pro.dhtcrawler.Future;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.btdownload.BEP09TorrentDownload;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.routingTable.PeerInfo;
import org.my.pro.dhtcrawler.util.DHTUtils;

import be.adaxisoft.bencode.BEncodedValue;

/**
 * 下载torrent任务
 * 
 */
public class TryFindPeerAndDownload implements DownloadTorrent, DHTTask {

	public static Log log = LogFactory.getLog(TryFindPeerAndDownload.class);

	// find_peer提交的下载任务执行线程执行器
	private ExecutorService tryFindPeerExe_findpeers1;
	// find_peer提交的下载任务执行线程执行器任务队列
	private LinkedBlockingQueue<Runnable> findPeersQueue;

	// announce_peer提交的下载任务执行线程执行器
	private ExecutorService tryFindPeerExe_announce_peer;
	// 下载任务执行器
	private ExecutorService downloadTorrentExe;

	// 运行状态
	private volatile boolean state = false;

	// 防止重复提交哈希执行find_peers
	private ConcurrentHashMap<String, Object> findPeersHash;
	// 防止重复提交IP加端口重复执行
	private ConcurrentHashMap<String, Object> downloadTorrentIPHash;

	private static final Object empty = new Object();

	// 单例实例
	private static volatile TryFindPeerAndDownload instance;

	// 节点总表
	private ConcurrentSkipListMap<BigInteger, Node> allNodes;
	// 节点key表
	private ConcurrentHashMap<BigInteger, Object> allnodesKeyMap;

	// 定时任务
	private ScheduledExecutorService scheduledExecutor;
	// 节点入表时，检查其是否是坏节点
	private ExecutorService addCheckIsBadNode;
	private LinkedBlockingQueue<Runnable> addCheckIsBadNodeQueue;

	// 节点总表最大节点数
	// 这个节点总数应该是和爬虫的发现速度产生关系的，这里就简单定死就行
	private static int MAX_NODESNUM = 88888 * 2;

	//
	private HashMap<Integer, LocalDHTNode> localDHTNodes = new HashMap<Integer, LocalDHTNode>();

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

	public synchronized void registerDHTNode(LocalDHTNode localDHTNode) {
		this.localDHTNodes.put(localDHTNode.noChangeId(), localDHTNode);
	}

	/**
	 * 只有哈希 findPerrs提交
	 * 
	 * findpeers 过来的包特别的多
	 * 
	 * @param hash
	 */
	public void subTask_findpeers(byte[] hash, LocalDHTNode localDHTNode) {
		if (!state) {
			return;
		}

		// 如果提交哈希正在执行下载,防止连续提交
		if (findPeersHash.containsKey(DHTUtils.byteArrayToHexString(hash))) {
			return;
		}
		// 队列任务太多咱就不提交了
		if (findPeersQueue.size() > 1024) {
			return;
		}
		//
		tryFindPeerExe_findpeers1.execute(new RunTask(hash, localDHTNode));
	}

	/**
	 * 只有哈希 announce_peer提交
	 * 
	 * announce_peer过来的包，较少
	 * 
	 * @param hash
	 */
	public void subTask_announce_peer(byte[] hash, LocalDHTNode localDHTNode) {
		if (!state) {
			return;
		}
		// 如果提交哈希正在执行查找peers,防止连续提交
		if (findPeersHash.containsKey(DHTUtils.byteArrayToHexString(hash))) {
			return;
		}
		//
		tryFindPeerExe_announce_peer.execute(new RunTask(hash, localDHTNode));
	}

	/**
	 * 提交IP端口下载torrent
	 * 
	 * @param ip
	 * @param port
	 * @param hash
	 */
	public void subTask(String ip, int port, byte[] hash) {
		if (!state) {
			return;
		}
		// 正在下载的任务防止重复提交
		if (downloadTorrentIPHash.containsKey(ip + port)) {
			return;
		}
		//
		downloadTorrentExe.execute(new Runnable() {
			@Override
			public void run() {

				//
				try {
					BEP09TorrentDownload.getInstance().tryDownload(ip, port, hash);
				} catch (Exception e) {

				} finally {
					downloadTorrentIPHash.remove(ip + port);
				}
				//

			}
		});
		//
		downloadTorrentIPHash.put(ip + port, empty);
	}

	@Override
	public void addNode(Node node) {
		if (!state) {
			return;
		}
		// 检查任务队列大小，限制任务数
		if (addCheckIsBadNodeQueue.size() > 1024) {
			return;
		}
		addCheckIsBadNode.execute(() -> {

			byte[] distance = DHTUtils.xorDistance(DHTUtils.MAX_NODE_ID, node.nodeId().bsId());
			BigInteger key = DHTUtils.distanceAsBigInteger(distance);
			// 如果已存在节点，不添加
			if (allnodesKeyMap.containsKey(key)) {
				return;
			}
			//
			if (nodeIsActive(localDHTNodes.get(node.localDHTID()), node)) {
				allnodesKeyMap.put(key, empty);
//				allNodesQueue.add(key);
				allNodes.put(key, node);
			}
		});

	}

	public List<Node> findNearest(byte[] hash) {

		byte[] distance = DHTUtils.xorDistance(DHTUtils.MAX_NODE_ID, hash);
		BigInteger key = DHTUtils.distanceAsBigInteger(distance);

		// 从ke左右找出Node列表
		List<Node> rs = new ArrayList<Node>();
		// 低添加
		lowerFillin(rs, key, 4);
		// 高添加
		higherFillIn(rs, key, 8);
		//
		return rs;
	}

	private void lowerFillin(List<Node> list, BigInteger key, int lowerSize) {
		// 如果填充满
		if (list.size() == lowerSize) {
			return;
		}
		Entry<BigInteger, Node> enty = allNodes.lowerEntry(key);
		if (null != enty) {
			list.add(enty.getValue());
			lowerFillin(list, enty.getKey(), lowerSize);
		}
	}

	private void higherFillIn(List<Node> list, BigInteger key, int highersize) {
		// 如果填充满
		if (list.size() == highersize) {
			return;
		}
		Entry<BigInteger, Node> enty = allNodes.higherEntry(key);
		if (null != enty) {
			list.add(enty.getValue());
			higherFillIn(list, enty.getKey(), highersize);
		}
	}

	private boolean nodeIsActive(LocalDHTNode localDHTNode, Node node) {
		KrpcMessage ping = MessageFactory.createPing(node, localDHTNode.id());
		return localDHTNode.call(ping).getValue() != null;
	}

	/**
	 * 定时清理缓存
	 */
	class clearCache implements Runnable {

		@Override
		public void run() {
			/**
			 * findPeersHash downloadTorrentIPHash allNodes allnodesKeyMap
			 */

			if (findPeersHash.size() > 1024) {
				findPeersHash.clear();
			}
			if (downloadTorrentIPHash.size() > 1024) {
				downloadTorrentIPHash.clear();
			}
			//
			int excess = allNodes.size() - MAX_NODESNUM;
			if (excess > 0) {
				// @TODO 就先删除头尾吧，后面可以改成先进先删除的方式
				for (int i = 0; i < excess; i++) {
					// 删除头尾尽量让节点集中
					BigInteger key = i % 2 == 0 ? allNodes.firstKey() : allNodes.lastKey();
					//
					allNodes.remove(key);
					allnodesKeyMap.remove(key);
					//

				}
			}
		}

	}

	/**
	 * 寻找peer列表任务
	 */
	class RunTask implements Runnable {

		private byte[] torrentHash;
		private LocalDHTNode dhtNode;

		public RunTask(byte[] torrentHash, LocalDHTNode dhtNode) {
			super();
			this.torrentHash = torrentHash;
			this.dhtNode = dhtNode;
		}

		@Override
		public void run() {

			//
			List<Node> nodes = findNearest(torrentHash);

			if (null == nodes) {
				return;
			}

			findPeersHash.put(DHTUtils.byteArrayToHexString(torrentHash), empty);
			try {
				//

				//
				List<Node> findNodes = new ArrayList<Node>();
				Stack<Node> findPeers = new Stack<Node>();

				//
				nodes.forEach(e -> {

					findNodes.add(e);
				});
				//
				int findLevel = 3;
				// 去重复node,IP端口
				HashSet<String> allFindNodes = new HashSet<String>();

				//
				while (findNodes.size() > 0) {

					List<KrpcMessage> taskList = new ArrayList<KrpcMessage>();
					// 取得任务
					for (Node node : findNodes) {
						if (!allFindNodes.contains(node.ip() + node.port())) {
							taskList.add(
									MessageFactory.createGet_peers(node.ip(), node.port(), dhtNode.id(), torrentHash));
							allFindNodes.add(node.ip() + node.port());
						}
						//

					}
					// 清空找到节点表
					findNodes.clear();
					// 执行任务
					getPerrs(taskList, torrentHash, findNodes, dhtNode, findPeers);
					taskList.clear();

					// 控制查询循环次数
					findLevel--;
					if (findLevel <= 0) {
						break;
					}
				}
				//
				int push = Math.min(findPeers.size(), 20);
				for (int i = 0; i < push; i++) {
					Node e = findPeers.pop();
					subTask(e.ip(), e.port(), torrentHash);

				}

			} catch (Exception e) {
				log.error(e.getMessage());
			}
			//
			findPeersHash.remove(DHTUtils.byteArrayToHexString(torrentHash));

		}

		public void getPerrs(List<KrpcMessage> krpcMessages, byte[] hash, List<Node> findNodes,
				LocalDHTNode localDHTNode, Stack<Node> findPeers) {

			//

			List<Future> futures = localDHTNode.invokeAll(krpcMessages);

			if (null == futures) {
				return;
			}
			//
			// 去重复peer,IP端口
			HashSet<String> allFindPeers = new HashSet<String>();
			//
			for (Future future : futures) {
				if (future.getValue() == null) {
					continue;
				}
				try {

					DefaultResponse defaultResponse = (DefaultResponse) future.getValue();

					//
					if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
						byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

						List<Node> nodes = DHTUtils.readNodeInfo(bs, localDHTNode);
						//
						findNodes.addAll(nodes);

					}
					// 响应包含VALUE，找到peers
					if (defaultResponse.r().getMap().containsKey(KeyWord.VALUES)) {
						try {
							List<BEncodedValue> list = defaultResponse.r().getMap().get(KeyWord.VALUES).getList();

							for (BEncodedValue bv : list) {

								Node peer = readIpPort(bv.getBytes());
								if (!allFindPeers.contains(peer.ip() + peer.port())) {
									allFindPeers.add(peer.ip() + peer.port());
									//
									findPeers.push(peer); // 入栈
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			//

		}

		public Node readIpPort(byte[] bs) {
			byte[] ip = new byte[4];
			byte[] port = new byte[2];

			System.arraycopy(bs, 0, ip, 0, 4);
			System.arraycopy(bs, 4, port, 0, 2);
			return new PeerInfo(ip, port);
		}

	}

	@Override
	public synchronized void start() {
		if (state) {
			return;
		}
		state = true;

		findPeersQueue = new LinkedBlockingQueue<Runnable>();
		tryFindPeerExe_findpeers1 = new ThreadPoolExecutor(12, 12, 0L, TimeUnit.MILLISECONDS, findPeersQueue);

		tryFindPeerExe_announce_peer = Executors.newFixedThreadPool(6);
		downloadTorrentExe = Executors.newFixedThreadPool(32);
		findPeersHash = new ConcurrentHashMap<String, Object>();
		downloadTorrentIPHash = new ConcurrentHashMap<String, Object>();
		allNodes = new ConcurrentSkipListMap<BigInteger, Node>(new NodeComParator());
		allnodesKeyMap = new ConcurrentHashMap<BigInteger, Object>();

		//
		scheduledExecutor = Executors.newScheduledThreadPool(1);

		scheduledExecutor.scheduleAtFixedRate(new clearCache(), 100, 100, TimeUnit.MILLISECONDS);
		addCheckIsBadNodeQueue = new LinkedBlockingQueue<Runnable>();
		addCheckIsBadNode = new ThreadPoolExecutor(16, 16, 0L, TimeUnit.MILLISECONDS, addCheckIsBadNodeQueue);
		//

		log.info("Bt下载模块启动");
	}

	@Override
	public void stop() {
		if (!state) {
			return;
		}
		state = false;

		tryFindPeerExe_findpeers1.shutdownNow();
		findPeersQueue.clear();

		tryFindPeerExe_announce_peer.shutdownNow();
		downloadTorrentExe.shutdownNow();
		findPeersHash.clear();
		downloadTorrentIPHash.clear();
		allNodes.clear();
		allnodesKeyMap.clear();

		scheduledExecutor.shutdownNow();

		addCheckIsBadNodeQueue.clear();
		addCheckIsBadNode.shutdownNow();
		//
		log.info("Bt下载模块关闭");
	}

	class NodeComParator implements Comparator<BigInteger> {

		@Override
		public int compare(BigInteger o1, BigInteger o2) {
			return o1.compareTo(o2);
		}

	}
}
