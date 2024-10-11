package org.my.pro.dhtcrawler;

import java.math.BigInteger;
import java.util.List;

/**
 * 路由表
 */
public interface RoutingTable {

	/**
	 * 添加节点
	 */
	public void add(NodeInfo info );

	/**
	 * 查找节点最近个K个节点
	 */
	public List<NodeInfo> findNearest(BigInteger id);

	/**
	 * 刷新节点活跃信息
	 */
	public void nodeActive(BigInteger id);
	

	public boolean has(BigInteger id);
	
}
