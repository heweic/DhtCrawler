package org.my.pro.dhtcrawler;

public interface WorkHandler {

	/**
	 * 处理请求
	 * 
	 * @param hash    种子hash
	 * @param message 消息
	 */
	public void handler(byte[] hash, KrpcMessage message );
}
