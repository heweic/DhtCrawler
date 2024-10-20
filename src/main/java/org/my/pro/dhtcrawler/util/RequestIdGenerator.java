package org.my.pro.dhtcrawler.util;

import java.util.concurrent.atomic.AtomicInteger;

/**

 */
public class RequestIdGenerator {

	private static AtomicInteger id = new AtomicInteger();

	/**
	 * 根据localId 生成 T;
	 * 
	 * 
	 * 
	 * @return
	 */
	public static String getRequestId() {
		return String.valueOf(id.incrementAndGet());
	}

}
