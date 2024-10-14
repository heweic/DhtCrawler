package org.my.pro.dhtcrawler.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.btdownload.Bep09MetadataFiles;
import org.my.pro.dhtcrawler.util.ByteArrayHexUtils;
import org.my.pro.dhtcrawler.util.NodeIdRandom;

public class DownLoadBtTorrent {

	private Executor executor = Executors.newFixedThreadPool(1);

	private LocalDHTNode node;



	public DownLoadBtTorrent(LocalDHTNode node) {
		super();
		this.node = node;
	}

	/**
	 * 提交下载torrent任务
	 * 
	 * @param hash
	 */
	public void subTask(String hash) {

		executor.execute(new RunTask(hash));
	}

	class RunTask implements Runnable {

		private byte[] hash;

		public RunTask(String hash) {

			this.hash = ByteArrayHexUtils.hexStringToByteArray(hash);
		}

		@Override
		public void run() {
			// 通过hash找到peerlist

			HashMap<String, Integer> allPeers = new HashMap<String, Integer>();

			try {
				List<NodeInfo> peerList = node.find_peer(hash);
				//

				for (NodeInfo info : peerList) {
					allPeers.put(info.ip(), info.port());
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

			// 通过peer尝试下载torrent

			Iterator<Entry<String, Integer>> it = allPeers.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Integer> next = it.next();
				System.out.println("开始尝试下载:" + next.getKey() + ":" + next.getValue() + "-" + ByteArrayHexUtils.byteArrayToHexString(hash));
				Bep09MetadataFiles bep09MetadataFiles = new Bep09MetadataFiles(hash, NodeIdRandom.generatePeerId(),
						next.getKey(), next.getValue());

				try {
					bep09MetadataFiles.tryDownload();
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				if (bep09MetadataFiles.get()) {
					// 下载成功，
					break;
				}
			}

		}
	}
}
