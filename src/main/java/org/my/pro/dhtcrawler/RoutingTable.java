package org.my.pro.dhtcrawler;

import java.util.List;
import java.util.Map;

/**
 * 路由表
 */
public interface RoutingTable {

	/**
	 * 添加节点
	 */
	public void add(NodeInfo info);
	
	public void remove(String id);

	/**
	 * 去size个随机元素
	 */
	public List<NodeInfo> random(int size);

	/**
	 * 刷新节点活跃信息
	 */
	public void nodeActive(String id);

	public boolean has(String id);
	
	public Map<String, NodeInfo> allNodes();

}
