package org.my.pro.dhtcrawler.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.my.pro.dhtcrawler.exception.IdErroException;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

public class BenCodeUtils {

	public static BEncodedValue to(String key, byte[] bs) throws Exception {

		Map<String, BEncodedValue> map = new HashMap<>();

		map.put(key, new BEncodedValue(bs));

		return new BEncodedValue(map);
	}

	public static BEncodedValue to(String key, int value) throws Exception {

		Map<String, BEncodedValue> map = new HashMap<>();

		map.put(key, new BEncodedValue(value));

		return new BEncodedValue(map);
	}

	public static BEncodedValue to(String key, BEncodedValue bEncodedValue) throws Exception {

		Map<String, BEncodedValue> map = new HashMap<>();

		map.put(key, bEncodedValue);

		return new BEncodedValue(map);
	}

	public static BEncodedValue to(String key1, byte[] value1, String key2, byte[] value2) throws Exception {

		Map<String, BEncodedValue> map = new HashMap<>();

		map.put(key1, new BEncodedValue(value1));
		map.put(key2, new BEncodedValue(value2));

		return new BEncodedValue(map);
	}

	public static BigInteger id(byte[] bs) throws Exception {
		if (bs.length != 20) {
			throw new IdErroException();
		}

		byte[] dest = new byte[bs.length + 1];
		System.arraycopy(bs, 0, dest, 1, bs.length);
		return new BigInteger(dest);
	}

	public static String binaryArray2Ipv4Address(byte[] addr) {
		String ip = "";
		for (int i = 0; i < addr.length; i++) {
			ip += (addr[i] & 0xFF) + ".";
		}
		return ip.substring(0, ip.length() - 1);
	}

	public static byte[] ipv4Address2BinaryArray(String ipAdd) {
		byte[] binIP = new byte[4];
		String[] strs = ipAdd.split("\\.");
		for (int i = 0; i < strs.length; i++) {
			binIP[i] = (byte) Integer.parseInt(strs[i]);
		}
		return binIP;
	}

	public static void main(String[] args) throws Exception {
	byte[] HANDSHAKE_BYTES = { 19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
				111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1 };
	System.out.println(new String(HANDSHAKE_BYTES));
		
	}

}
