package org.my.pro.dhtcrawler.util;

import java.util.concurrent.atomic.AtomicInteger;

public class NoChangeID {

	private static AtomicInteger atomicInteger = new AtomicInteger(100);
	
	
	public static int nextNum() {
		return atomicInteger.incrementAndGet();
	}
}
