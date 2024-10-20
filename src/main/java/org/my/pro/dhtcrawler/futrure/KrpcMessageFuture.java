package org.my.pro.dhtcrawler.futrure;

import org.my.pro.dhtcrawler.Future;
import org.my.pro.dhtcrawler.KrpcMessage;

public class KrpcMessageFuture implements Future {

	private KrpcMessage response;
	private long createTime = System.currentTimeMillis();
	// 默认超时时间
	public final static long TIME_OUT = 1000 * 2;
	
	public final static long LIVE_TIME = TIME_OUT * 2;
	private final Object lock = new Object();

	@Override
	public KrpcMessage getValue() {
		synchronized (lock) {
			long waitTime = TIME_OUT - (System.currentTimeMillis() - createTime);
			while (null == response) {
				try {
					lock.wait(waitTime);
				} catch (InterruptedException e) {
				}

				waitTime = TIME_OUT - (System.currentTimeMillis() - createTime);
				if (waitTime <= 0) {
					break;
				}
			}
		}
		return response;
	}

	@Override
	public void back(KrpcMessage krpcMessage) {
		this.response = krpcMessage;
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	public long getCreateTime() {
		return createTime;
	}
	
	
}
