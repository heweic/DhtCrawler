package org.my.pro.dhtcrawler;

import java.util.List;

/**
 * 路由表
 */
public interface RoutingTable {

	/**
	 * 添加节点
	 */
	public void add(Node node);

	/**
	 * 删除节点
	 * 
	 * @param id
	 */
	public void removeNode(Node node);

	/**
	 * 查询目标节点附近的节点
	 * 
	 * @param targetId
	 * @param count
	 * @return
	 */
	public List<Node> getClosestNodes(byte[] targetId, int count);
	
	/**
	 * 随机抓node
	 * @param num
	 * @return
	 */
	public List<Node> randomNodes(int num);
	
	/**
	 *  是否有节点
	 * @return
	 */
	public boolean hasNode();
	
	/**
	 * 目标节点与当前节点距离Index桶中是否有节点表数量
	 * @param target
	 * @return
	 */
	public int targetSize(byte[] target);

}
