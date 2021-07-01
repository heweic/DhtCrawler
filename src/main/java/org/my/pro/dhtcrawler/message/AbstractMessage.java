package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;

import be.adaxisoft.bencode.BEncodedValue;

public abstract class AbstractMessage implements KrpcMessage {

	private String y;
	private String t;

	private InetSocketAddress addr;
	
	protected int msg_type;
	protected int piece;

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
