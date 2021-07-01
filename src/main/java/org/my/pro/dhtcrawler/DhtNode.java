package org.my.pro.dhtcrawler;

import io.netty.channel.ChannelHandler;

/**
 * DHT 节点
 */
public interface DhtNode {

	/** */
	public void start(ChannelHandler channelHandler);

	/** */
	public void stop();

	/** */
	public boolean isRun();

	/**
	 * 执行命令
	 */
	public void exec(KrpcMessage krpcMessage);

	/**
	 * 节点ID
	 */
	public String id();

	/**
	 * 节点端口
	 */
	public int port();
	
	

}
