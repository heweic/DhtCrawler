package org.my.pro.dhtcrawler;

public interface RequestMessage extends KrpcMessage {

	/**
	 * 请求方法
	 *  */
	public String q();
	
	/** 
	 * 请求参数
	 * */
	public Object a();
	
	
}
