package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;
import java.util.Map;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.RequestMessage;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

public class DefaultRequest extends AbstractMessage implements RequestMessage {

	public DefaultRequest( String t , InetSocketAddress addr) {
		super(Q, t , addr);
	}

	private String q;
	private BEncodedValue a;


	
	@Override
	public String y() {
		return Q;
	}

	@Override
	public String q() {
		return q;
	}

	@Override
	public BEncodedValue a() {
		return a;
	}

	public void setQ(String q) {
		this.q = q;
	}

	public void setA(BEncodedValue a) {
		this.a = a;
	}



	@Override
	public byte[] toByteArray() throws Exception {
		Map<String, BEncodedValue> map = toBencodedValue();
		//
		if(null != q){
			map.put(KeyWord.Q, new BEncodedValue(q , KeyWord.DHT_CHARSET_STR));
		}
		if(null != a){
			map.put(KeyWord.A, a);
		}
		//
		return BEncoder.encode(map).array();
		
	}
	
	
	
	
	

}
