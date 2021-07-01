package org.my.pro.dhtcrawler;

import java.nio.ByteBuffer;

/**
 * 节点信息
 */
public interface NodeInfo {

	/**
	 * 最后活跃时间
	 */
	public long activeTime();

	/**
	 * 刷新活跃时间
	 */
	public void refActiveTime();

	/** 节点ID */
	public NodeId nodeId();

	/**
	 * IP
	 */
	public String ip();

	/**
	 * 端口
	 */
	public int port();

	/**
	 * 转字符 20字节ID + 4字节IP + 2字节端口
	 */
	public ByteBuffer toBuf();

}
