package org.my.pro.dhtcrawler.task;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

	private ExecutorService tryFindPeerExe_findpeers1;
	private LinkedBlockingQueue<Runnable> findPeersQueue;

	private ExecutorService tryFindPeerExe_announce_peer;

	private ExecutorService downloadTorrentExe;
	private static Log log = LogFactory.getLog(TryFindPeerAndDownload.class);

	private volatile boolean state = false;

	// 防止重复提交哈希执行find_peers
	private static ConcurrentHashMap<String, Object> findPeersHash;
	// 防止重复提交IP加端口重复执行
	private static ConcurrentHashMap<String, Object> downloadTorrentIPHash;
	private static final Object empty = new Object();


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
	 * 只有哈希 findPerrs提交
	 * 
	 * @param hash
	 */
	public void subTask_findpeers(byte[] hash, LocalDHTNode localDHTNode) {
		if (!state) {
			return;
		}
		if(findPeersHash.size() > 1024) {
			findPeersHash.clear();
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
	 * announce_peer提交
	 * 
	 * @param hash
	 */
	public void subTask_announce_peer(byte[] hash, LocalDHTNode localDHTNode) {
		if (!state) {
			return;
		}
		//
		if(findPeersHash.size() > 1024) {
			findPeersHash.clear();
		}
		// 如果提交哈希正在执行查找peers,防止连续提交
		if (findPeersHash.containsKey(DHTUtils.byteArrayToHexString(hash))) {
			return;
		}
		//
		tryFindPeerExe_announce_peer.execute(new RunTask(hash, localDHTNode));
	}

	/**
	 * 直接提交IP端口下载
	 * 
	 * @param ip
	 * @param port
	 * @param hash
	 */
	public void subTask(String ip, int port, byte[] hash) {
		if (!state) {
			return;
		}
		//
		if(downloadTorrentIPHash.size() > 100) {
			downloadTorrentIPHash.clear();
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
					Bep09MetadataFiles bep09MetadataFiles = new Bep09MetadataFiles(hash, DHTUtils.generatePeerId(), ip,
							port);
					bep09MetadataFiles.tryDownload();
					bep09MetadataFiles.get();
				} finally {
					downloadTorrentIPHash.remove(ip + port);
				}
				//

			}
		});
		//
		downloadTorrentIPHash.put(ip + port, empty);
	}

	

	private static int MAX_NODESNUM = 60000;

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

	public List<Node> findNearest(byte[] hash) {
		byte[] distance = DHTUtils.xorDistance(DHTUtils.MAX_NODE_ID, hash);

		BigInteger key = DHTUtils.distanceAsBigInteger(distance);

		// 从ke左右找出Node列表
		List<Node> rs = new ArrayList<Node>();
		// 低添加
		lowerFillin(rs, key);
		// 高添加
		higherFillIn(rs, key);

		//
		return rs;
	}

	private void lowerFillin(List<Node> list, BigInteger key) {
		// 如果填充满
		if (list.size() == 4) {
			return;
		}
		Entry<BigInteger, Node> enty = allNodes.lowerEntry(key);
		if (null != enty) {
			list.add(enty.getValue());
			lowerFillin(list, enty.getKey());
		}
	}

	private void higherFillIn(List<Node> list, BigInteger key) {
		// 如果填充满
		if (list.size() == 8) {
			return;
		}
		Entry<BigInteger, Node> enty = allNodes.higherEntry(key);
		if (null != enty) {
			list.add(enty.getValue());
			higherFillIn(list, enty.getKey());
		}
	}

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

			if(null == nodes) {
				return;
			}
			findPeersHash.put(DHTUtils.byteArrayToHexString(torrentHash), empty);
			try {

				HashSet<String> findNodeIpPort = new HashSet<String>();
				HashMap<String, Node> findNodes = new HashMap<String, Node>();
				nodes.forEach(e -> {
					findNodes.put(e.ip() + e.port(), e);
				});
				HashMap<String, Node> findPeers = new HashMap<String, Node>();
				int findLevel = 3;
				while (findNodes.size() > 0 && findPeers.size() < 8) {

					List<Node> taskList = new ArrayList<Node>();
					// 取得任务
					for (Entry<String, Node> node : findNodes.entrySet()) {

						//
						if (!findNodeIpPort.contains(node.getKey())) {
							findNodeIpPort.add(node.getValue().ip() + node.getValue().port());
							taskList.add(node.getValue());
						}
						findNodes.remove(node.getKey());

					}
					// 执行任务
					taskList.forEach(task -> {
						getPerrs(task.ip(), task.port(), torrentHash, dhtNode, findNodes, findPeers);
					});

					//
					findLevel--;
					if (findLevel == 0) {
						break;
					}
				}
				//
				findPeers.entrySet().forEach(e -> {
					subTask(e.getValue().ip(), e.getValue().port(), torrentHash);
				});

			} catch (Exception e) {
				log.error(e.getMessage());
			}
			//
			findPeersHash.remove(DHTUtils.byteArrayToHexString(torrentHash));

		}

		public void getPerrs(String ip, int port, byte[] hash, LocalDHTNode localDHTNode,
				HashMap<String, Node> findNodes, HashMap<String, Node> findPeers) {

			//

			KrpcMessage get_peers = MessageFactory.createGet_peers(ip, port, localDHTNode.id(), hash);
			// 可能返回nodeList或peerList
			Future future = localDHTNode.call(get_peers);
			KrpcMessage response = future.getValue();

			if (null == response) {
				return;
			}
			//
			try {

				DefaultResponse defaultResponse = (DefaultResponse) response;

				//
				if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
					byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

					List<Node> nodes = DHTUtils.readNodeInfo(bs);
					//
					nodes.forEach(info -> {
						findNodes.put(info.ip() + info.port(), info);
					});

				}
				// 响应包含VALUE，找到peers
				if (defaultResponse.r().getMap().containsKey(KeyWord.VALUES)) {
					try {
						List<BEncodedValue> list = defaultResponse.r().getMap().get(KeyWord.VALUES).getList();

						for (BEncodedValue bv : list) {
							ByteBuf byteBuf = Unpooled.wrappedBuffer(bv.getBytes());
							Node peer = readIpPort(byteBuf);
							findPeers.put(peer.ip(), peer);
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
			return new DefaultNodeInfo(null, ip, port);
		}

	}

	@Override
	public synchronized void start() {
		if (state) {
			return;
		}
		state = true;

		findPeersQueue = new LinkedBlockingQueue<Runnable>();
		tryFindPeerExe_findpeers1 = new ThreadPoolExecutor(16, 16, 0L, TimeUnit.MILLISECONDS, findPeersQueue);

		tryFindPeerExe_announce_peer = Executors.newCachedThreadPool();
		downloadTorrentExe = Executors.newCachedThreadPool();
		findPeersHash = new ConcurrentHashMap<String, Object>();
		downloadTorrentIPHash = new ConcurrentHashMap<String, Object>();
		allNodes = new ConcurrentSkipListMap<BigInteger, Node>(new NodeComParator());
		log.info("Bt下载模块启动");
	}

	@Override
	public void stop() {
		if (!state) {
			return;
		}
		state = false;

		tryFindPeerExe_findpeers1.shutdown();
		tryFindPeerExe_announce_peer.shutdown();
		downloadTorrentExe.shutdown();
		findPeersHash.clear();
		allNodes.clear();
		downloadTorrentIPHash.clear();
		log.info("Bt下载模块关闭");
	}

	class NodeComParator implements Comparator<BigInteger> {

		@Override
		public int compare(BigInteger o1, BigInteger o2) {
			return o1.compareTo(o2);
		}

	}
}
