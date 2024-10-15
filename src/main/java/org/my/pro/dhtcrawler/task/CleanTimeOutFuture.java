package org.my.pro.dhtcrawler.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.util.DHTUtils;

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
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//
					localDHTNode.clearTimeOutFutrue();

				}
			}
		},DHTUtils.byteArrayToHexString(localDHTNode.id()) +":CleanTimeOutThread");
		thread.start();
		
		log.info(DHTUtils.byteArrayToHexString(localDHTNode.id()) +"清理过期Futrue线程启动");
	}

	@Override
	public void stop() {
		thread.interrupt();
		
	}

}
