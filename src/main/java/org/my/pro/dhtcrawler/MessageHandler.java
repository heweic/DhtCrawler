package org.my.pro.dhtcrawler;

import java.math.BigInteger;

public interface MessageHandler {

	
	
	/** 
	 * 处理请求
	 * 
	 * @param id 节点ID
	 * @param message 消息
	 * */
	public KrpcMessage handler( BigInteger id ,KrpcMessage message) throws Exception;
	
}
