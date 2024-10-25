package org.my.pro.dhtcrawler.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.LocalDHTNode;

public class CleanTimeOutFuture implements DHTTask {

	private LocalDHTNode localDHTNode;

	private Thread thread;
	private static Log log = LogFactory.getLog(CleanTimeOutFuture.class);

	public CleanTimeOutFuture(LocalDHTNode localDHTNode) {
		super();
		this.localDHTNode = localDHTNode;
	}

	@Override
	public synchronized void start() {
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					//
					try {
						localDHTNode.clearTimeOutFutrue();
					}catch (Exception e) {
						// TODO: handle exception
					}

				}
			}
		}, localDHTNode.port() + ":CleanTimeOutThread");
		thread.start();

		log.info(localDHTNode.port() + "清理过期Futrue线程启动");
	}

	@Override
	public void stop() {
		thread.interrupt();

	}

}
