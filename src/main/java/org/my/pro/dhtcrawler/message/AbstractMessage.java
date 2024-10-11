package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;

import be.adaxisoft.bencode.BEncodedValue;

public abstract class AbstractMessage implements KrpcMessage {

	/**
	 * 它由一个字节组成，表明这个消息的类型。y 对应的值有三种情况：q 表示请求，r 表示回复，e 表示错误。
	 */
	private String y;
	/**
	 * 它是一个代表了 transaction ID 的字符串类型。transaction ID
	 * 由请求节点产生，并且回复中要包含回显该字段，所以回复可能对应一个节点的多个请求。简单理解transaction ID就是一个请求的唯一标识
	 */
	private String t;
	
	
	protected int msg_type;
	protected int piece;

	private InetSocketAddress addr;

	public AbstractMessage(String y, String t, InetSocketAddress addr) {
		this.y = y;
		this.t = t;
		this.addr = addr;
	}

	public Map<String, BEncodedValue> toBencodedValue() throws Exception {
		Map<String, BEncodedValue> map = new HashMap<>();
		//
		map.put(KeyWord.T, new BEncodedValue(t(), KeyWord.DHT_CHARSET_STR));
		map.put(KeyWord.Y, new BEncodedValue(y(), KeyWord.DHT_CHARSET_STR));
		//
		return map;
	}

	@Override
	public String y() {
		return y;
	}

	@Override
	public String t() {
		return t;
	}

	public void setY(String y) {
		this.y = y;
	}

	public void setT(String t) {
		this.t = t;
	}

	@Override
	public InetSocketAddress addr() {
		return addr;
	}

	@Override
	public int msg_type() {
		return msg_type;
	}

	@Override
	public int piece() {
		return piece;
	}

}
