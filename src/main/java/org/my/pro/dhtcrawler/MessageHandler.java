package org.my.pro.dhtcrawler;

public interface MessageHandler {

	/**
	 * 处理请求
	 * 
	 * @param id      节点ID
	 * @param message 消息
	 */
	public KrpcMessage handler(KrpcMessage message) throws Exception;

}
