package org.my.pro.dhtcrawler.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
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
public class TryFindPeerAndDownload implements DHTTask {

	private LocalDHTNode node;
	private ExecutorService tryFindPeerExe;
	private ExecutorService downloadTorrentExe;
	private static Log log = LogFactory.getLog(TryFindPeerAndDownload.class);

	private volatile boolean state = false;

	private ConcurrentHashMap<String, Object> downingHash;
	private Object empty = new Object();

	public TryFindPeerAndDownload(LocalDHTNode node) {
		super();
		this.node = node;
	}

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

	public void subTask(String ip, int port, byte[] hash) {
		if (!state) {
			return;
		}

		downloadTorrentExe.execute(new Runnable() {

			@Override
			public void run() {
				Bep09MetadataFiles bep09MetadataFiles = new Bep09MetadataFiles(hash, node.id(), ip, port);
				bep09MetadataFiles.tryDownload();
			}
		});
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
			List<Node> nodes = node.findNearest(targetHash);

			downingHash.put(DHTUtils.byteArrayToHexString(targetHash), empty);

			log.info("开始尝试下载:" + DHTUtils.byteArrayToHexString(targetHash));
			List<Node> peers = new ArrayList<Node>();
			for (Node n : nodes) {
				getPerrs(peers, n.ip(), n.port(), targetHash);
			}
			log.info(DHTUtils.byteArrayToHexString(targetHash) + "找到peer数:" + peers.size());
			for (Node peer : peers) {
				subTask(peer.ip(), peer.port(), targetHash);
			}
			//
			downingHash.remove(DHTUtils.byteArrayToHexString(targetHash));

		}

		public void getPerrs(List<Node> peers, String ip, int port, byte[] hash) {
			KrpcMessage get_peers = MessageFactory.createGet_peers(ip, port, node.id(), hash);
			// 可能返回nodeList或peerList
			Future future = node.call(get_peers);
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
		tryFindPeerExe = Executors.newFixedThreadPool(8);
		downloadTorrentExe = Executors.newCachedThreadPool();
		state = true;
		downingHash = new ConcurrentHashMap<String, Object>();
		log.info(DHTUtils.byteArrayToHexString(node.id()) + "Bt下载模块启动");
	}

	@Override
	public void stop() {
		if (!state) {
			return;
		}
		tryFindPeerExe.shutdown();
		downloadTorrentExe.shutdown();
		state = false;
		downingHash.clear();
		log.info(DHTUtils.byteArrayToHexString(node.id()) + "Bt下载模块关闭");
	}
}
