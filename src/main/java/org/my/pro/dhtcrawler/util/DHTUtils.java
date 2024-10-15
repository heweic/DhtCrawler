package org.my.pro.dhtcrawler.util;

import java.math.BigInteger;

import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.NodeId;
import org.my.pro.dhtcrawler.routingTable.DefaultNodeInfo;
import org.my.pro.dhtcrawler.routingTable.DhtNodeID;

import io.netty.buffer.ByteBuf;

/**
 * 
 */
public class DHTUtils {

	
	
	public static Node readNodeInfo(ByteBuf buffer) {

		byte[] id = new byte[20];
		byte[] ip = new byte[4];
		byte[] port = new byte[2];
		buffer.readBytes(id);
		buffer.readBytes(ip);
		buffer.readBytes(port);

		NodeId nodeId = new DhtNodeID(id);
		Node info = new DefaultNodeInfo(nodeId, ip, port);

		return info;
	}

	/**
	 * 随机生成DHT node ID
	 * 
	 * @return
	 */
	public static byte[] generateNodeId() {
		byte[] peerId = new byte[20];
		for (int i = 0; i < 20; i++) {
			peerId[i] = (byte) (Math.random() * 255);
		}
		return peerId;
	}

	/**
	 * 随机生成DHT peer ID
	 * 
	 * @return
	 */
	public static byte[] generatePeerId() {
		byte[] peerId = new byte[20];
		for (int i = 0; i < 20; i++) {
			peerId[i] = (byte) (Math.random() * 255);
		}
		return peerId;
	}

	/**
	 * 
	 * @param bytes
	 * @return
	 */
	public static String byteArrayToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param hex
	 * @return
	 */
	public static byte[] hexStringToByteArray(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}

	/**
	 * 计算两个长度一样的byte[]之间的异或距离
	 * 
	 * @param hash1
	 * @param hash2
	 * @return
	 */
	public static byte[] xorDistance(byte[] hash1, byte[] hash2) {
		if (hash1.length != hash2.length) {
			throw new IllegalAccessError();
		}
		//
		byte[] result = new byte[hash1.length];

		for (int i = 0; i < hash1.length; i++) {
			result[i] = (byte) (hash1[i] ^ hash2[i]);
		}
		//
		return result;
	}

	/**
	 * 将byte[]转换成一个大整数表示距离
	 * 
	 * @param xorResult
	 * @return
	 */
	public static BigInteger distanceAsBigInteger(byte[] xorResult) {
		return new BigInteger(1, xorResult);
	}
	
	public static void main(String[] args) {
		System.out.println(DHTUtils.hexStringToByteArray("c1b1743e0b67f322b735d0d7cf96a5710b19f6e3").length);
	}

}
