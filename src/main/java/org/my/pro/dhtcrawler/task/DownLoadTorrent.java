package org.my.pro.dhtcrawler.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.btdownload.Bep09MetadataFiles;
import org.my.pro.dhtcrawler.util.DHTUtils;

/**
 * 处理DHT网络中 announce_peer广播消息
 */
public class DownLoadTorrent implements DHTTask {

	private ExecutorService executor = Executors.newFixedThreadPool(10);

	private static Log log = LogFactory.getLog(DownLoadTorrent.class);
	private LocalDHTNode dhtNode;
	private volatile boolean state = false;

	
	
	public DownLoadTorrent(LocalDHTNode dhtNode) {
		this.dhtNode = dhtNode;
	}

	/**
	 * 提交种子下载任务
	 * 
	 * @param ip
	 * @param port
	 * @param hash
	 */
	public void tryDownLoad(String ip, int port, byte[] hash) {

		if (state) {
			executor.submit(new Runnable() {

				@Override
				public void run() {
					Bep09MetadataFiles bep09MetadataFiles = new Bep09MetadataFiles(hash, dhtNode.id(), ip, port);
					try {
						bep09MetadataFiles.tryDownload();
					} catch (Exception e) {
						e.printStackTrace();
						log.info(String.format("下载异常,在%s:%s下载%s", ip, port, DHTUtils.byteArrayToHexString(hash)));
					}
				}
			});
		}
	}

	@Override
	public synchronized void start() {
		state = true;
		log.info("下载Torrent任务启动");
	}

	@Override
	public void stop() {
		state = false;
		executor.shutdown();

	}
}
