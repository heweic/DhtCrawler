package org.my.pro.dhtcrawler;

import java.util.List;

import io.netty.channel.Channel;

/**
 * DHT 节点
 */
public interface LocalDHTNode {

	/** 启动 */
	public void start();

	/** 停止 */
	public void stop();

	/** 是否在运行 */
	public boolean isRun();

	/**
	 * 
	 * 发送消息
	 * 
	 * @param krpcMessage
	 */
	public void sendMessage(KrpcMessage krpcMessage);
	/**
	 * 
	 * 发送消息
	 * 
	 * @param krpcMessage
	 */
	public Future call(KrpcMessage krpcMessage);
	/**
	 * 
	 * @param krpcMessage
	 */
	public boolean back(KrpcMessage krpcMessage);
	
	public void clearTimeOutFutrue();

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

	/**
	 * 添加节点
	 */
	public void add(Node info);

	/**
	 * 查询hash附近的节点
	 */
	public List<Node> findNearest(byte[] hash);
	/**
	 * 
	 * @param hash
	 */
	public void tryDownLoad(byte[] hash);
	
	/**
	 * 目标节点与当前节点距离Index桶中是否有节点表数量
	 * @param target
	 * @return
	 */
	public int targetSize(byte[] target);
	
	/**
	 * 重置节点ID
	 */
	public void resetId(byte[] id);
	/**
	 * 时间段内是否获得哈希
	 * @return
	 */
	public boolean hasGetHash();
}
