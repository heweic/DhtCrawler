package org.my.pro.dhtcrawler.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.my.pro.dhtcrawler.KeyWord;

public class NodeIdRandom {

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
	//	return RandomStringUtils.random(20);
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

	public static void main(String[] args) {
		List<String> ids = randoms((byte) 126);
		for (int i = 0; i < ids.size(); i++) {

			try {
				System.out.println(ids.get(i).getBytes("iso-8859-1").length);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//

	}

}
