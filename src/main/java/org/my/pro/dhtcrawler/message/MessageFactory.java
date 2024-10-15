package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.util.BenCodeUtils;
import org.my.pro.dhtcrawler.util.RequestIdGenerator;

import be.adaxisoft.bencode.BEncodedValue;

public class MessageFactory {

	public static DefaultErro createErr(int code, String info, InetSocketAddress addr, String localId)
			throws Exception {

		BEncodedValue a = new BEncodedValue(code);
		BEncodedValue b = new BEncodedValue(info, KeyWord.DHT_CHARSET_STR);

		DefaultErro defaultErro = new DefaultErro(addr, a, b, localId);

		return defaultErro;
	}

	public static KrpcMessage createPing(Node info, byte[] id) {

		try {
			DefaultRequest defaultRequest = new DefaultRequest(RequestIdGenerator.getRequestId(),
					new InetSocketAddress(info.ip(), info.port()));
			//
			defaultRequest.setQ(KeyWord.PING);
			defaultRequest.setA(BenCodeUtils.to(KeyWord.ID, id));
			//
			return defaultRequest;

		} catch (Exception e) {
			return null;
		}
	}

	public static KrpcMessage createFindNode(String ip, int port, byte[] localID, byte[] targetId) {
		try {
			DefaultRequest defaultRequest = new DefaultRequest(RequestIdGenerator.getRequestId(),
					new InetSocketAddress(ip, port));
			//
			defaultRequest.setQ(KeyWord.FIND_NODE);
			defaultRequest.setA(BenCodeUtils.to(KeyWord.ID, localID, KeyWord.TARGET, targetId));
			//
			return defaultRequest;

		} catch (Exception e) {
			return null;
		}
	}
	public static KrpcMessage createGet_peers(String ip, int port, byte[] localId , byte[] hash) {
		try {
			DefaultRequest defaultRequest = new DefaultRequest(RequestIdGenerator.getRequestId(),
					new InetSocketAddress(ip, port));
			//
			defaultRequest.setQ(KeyWord.GET_PEERS);
			defaultRequest.setA(BenCodeUtils.to(KeyWord.ID, localId, KeyWord.INFO_HASH,hash));
			//
			return defaultRequest;

		} catch (Exception e) {
			return null;
		}
	}
	public static KrpcMessage createGet_peers(String ip,String id , int port, byte[] localId , byte[] hash) {
		try {
			DefaultRequest defaultRequest = new DefaultRequest(id,
					new InetSocketAddress(ip, port));
			//
			defaultRequest.setQ(KeyWord.GET_PEERS);
			defaultRequest.setA(BenCodeUtils.to(KeyWord.ID, localId, KeyWord.INFO_HASH,hash));
			//
			return defaultRequest;

		} catch (Exception e) {
			return null;
		}
	}



	/**
	 * 根据一个节点id随机获取一个目标节点id, 如果每次都只根据自身的节点id进行find_node操作很容易就会遍历完所有的节点导致无节点可遍历,
	 * 因此这里根据节点的距离按照一定概率生成一个随机的目标节点id, 距离越远的目标节点id生成的概率越小
	 * 
	 * @param id 本地节点的id
	 * @return
	 */
	public static String randomTargetId(String id) {
		int i = ThreadLocalRandom.current().nextInt();
		// 有百分之一的概率产生一个基本完全随机的节点id
		if (i % 100 == 0) {
			return (char) (i % 256) + UUID.randomUUID().toString().substring(0, 19);
		}

		int i2 = ThreadLocalRandom.current().nextInt();
		// 十六分之一的概率修改原本节点id的最后一个字节, 修改的字节越靠后, 被其他节点保存的概率越大
		if ((i & 0B1111) == (i2 & 0B1111)) {
			char[] cs = id.toCharArray();
			cs[cs.length - 1] = (char) (i & 0B011111111);
			return new String(cs);
		}
		return id;
	}
}
