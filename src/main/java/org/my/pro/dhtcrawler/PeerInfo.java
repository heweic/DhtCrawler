package org.my.pro.dhtcrawler;

public interface PeerInfo {

	
	/** 
	 * Ip
	 * */
	public String ip();
	
	/**
	 * 端口
	 *  */
	public int port();
	
	/**
	 * 转字符
	 * 20字节ID  +  4字节IP + 2字节端口
	 *  */
	public byte[] toBs();
	
	/** 
	 * 
	 * */
	public void set(byte[] bs);
	
	
	
}
