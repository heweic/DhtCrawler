package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;
import java.util.Map;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.ResponseMessage;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

public class DefaultResponse extends AbstractMessage implements ResponseMessage {

	private BEncodedValue r;

	public DefaultResponse( String t ,InetSocketAddress addr) {
		super( R, t , addr);
	}

	@Override
	public String y() {
		return R;
	}

	@Override
	public BEncodedValue r() {

		return r;
	}

	public void setR(BEncodedValue r) {
		this.r = r;
	}

	@Override
	public byte[] toByteArray() throws Exception {
		Map<String, BEncodedValue> map = toBencodedValue(); 
		//
		if(null != r){
			map.put(KeyWord.R, r);
		}
		//
		return BEncoder.encode(map).array();
	}

	@Override
	public String toString() {
		return "消息ID" + t()  + "消息类型:" + y() + "返回值;" + r.toString();
	}
	
	
	

}
