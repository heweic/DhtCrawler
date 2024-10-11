package org.my.pro.dhtcrawler.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.my.pro.dhtcrawler.KeyWord;

public class NodeIdRandom {
	
	
	public static byte[] generatePeerId() {
		// 生成随机的peer_id
		byte[] peerId = new byte[20];
		String prefix = "-NETTY001-";
		System.arraycopy(prefix.getBytes(StandardCharsets.US_ASCII), 0, peerId, 0, prefix.length());
		for (int i = prefix.length(); i < 20; i++) {
			peerId[i] = (byte) (Math.random() * 255);
		}
		return peerId;
	}

	/**
	 * 
	 * 思路： 一个节点生成多个ID 大范围认识节点
	 * 
	 */

	public static String random(byte tmp) {

		byte[] bs = new byte[20];
		bs[0] = tmp;
		byte[] randoms = RandomUtils.nextBytes(19);
		System.arraycopy(randoms, 0, bs, 1, 19);
		return new String(bs, KeyWord.DHT_CHARSET);
	}

	public static String random() {
		byte[] randoms = RandomUtils.nextBytes(20);
		return new String(randoms, KeyWord.DHT_CHARSET);
		// return RandomStringUtils.random(20);
	}

	public static List<String> randoms(byte num) {

		List<String> ids = new ArrayList<>();
		//
		for (byte i = 0; i <= num; i++) {

			ids.add(random(i));
		}

		//
		return ids;

	}

}
