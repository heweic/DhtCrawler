package org.my.pro.dhtcrawler.util;

import java.util.UUID;

/**

 */
public class RequestIdGenerator {

	/**
	 * 根据localId 生成 T;
	 * 
	 * 
	 * 
	 * @return
	 */
	public static String getRequestId() {

//		if (!concurrentHashMap.containsKey(localId)) {
//			concurrentHashMap.putIfAbsent(localId, new AtomicLong(0L));
//		}
//
//		return localId + "_" + concurrentHashMap.get(localId).incrementAndGet();

		return UUID.randomUUID().toString().replaceAll("-", "");

	}

	public static String toLocalId(String tid) {
		return tid.split("_")[0];
	}

}
