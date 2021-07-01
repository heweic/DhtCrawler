package org.my.pro.dhtcrawler;

/**
 * 错误消息
 *  */
public interface ErroMessage  extends KrpcMessage{

	
	/**
	 * 消息内容
	 *  */
	public Object code();

	public Object info();
	
}
