package org.my.pro.dhtcrawler;

import java.net.InetSocketAddress;

public interface KrpcMessage {

	/** 请求 */
	public static final String Q = "q";

	/**
	 * 响应
	 */
	public static final String R = "r";

	/**
	 * 错误
	 */
	public static final String E = "e";

	public InetSocketAddress addr();

	/**
	 * 消息类型
	 */
	public String y();

	/**
	 * transactionID
	 */
	public String t();

	public byte[] toByteArray() throws Exception;

	// tmp

	public int msg_type();

	public int piece();

}
