package org.my.pro.dhtcrawler.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**

 */
public class RequestIdGenerator {

	private static ConcurrentHashMap<String, AtomicLong> concurrentHashMap = new ConcurrentHashMap<>();

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
