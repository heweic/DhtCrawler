package org.my.pro.dhtcrawler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

/**
 * DHT 节点
 */
public interface LocalDHTNode {

	/**
	 * 
	 */
	public void setChannelHandler(ChannelHandler channelHandler);

	/** 启动 */
	public void start();

	/** 停止 */
	public void stop();

	/** 是否在运行 */
	public boolean isRun();
	
	/**
	 * 搜寻持有hash的peer
	 * 
	 */
	 
	

	/**
	 * 
	 * 发送消息
	 * 
	 * @param krpcMessage
	 */
	public void sendMessage(KrpcMessage krpcMessage);

	/**
	 * 返回netty Channel
	 * 
	 * @return
	 */
	public Channel channel();

	/**
	 * 节点ID
	 */
	public byte[] id();

	/**
	 * 节点端口
	 */
	public int port();

}
