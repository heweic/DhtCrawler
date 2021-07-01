package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.my.pro.dhtcrawler.KeyWord;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

public class D9Request extends AbstractMessage {

	
	
	public D9Request( InetSocketAddress addr , int piece) {
		super(null, null, addr);
		// TODO Auto-generated constructor stub
		this.piece = piece;
		this.msg_type = 0;
		
	}

	@Override
	public byte[] toByteArray() throws Exception {
		Map<String, BEncodedValue> map = new HashMap<>();
		//
		map.put(KeyWord.msg_type, new BEncodedValue(msg_type));
		map.put(KeyWord.piece, new BEncodedValue(piece));
		
		return BEncoder.encode(map).array();
	}

}
