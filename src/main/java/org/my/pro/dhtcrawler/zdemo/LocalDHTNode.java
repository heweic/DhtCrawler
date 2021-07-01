package org.my.pro.dhtcrawler.zdemo;


import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

/**
 * 一个本地的DHT节点对象
 * @author dgqjava
 *
 */
public class LocalDHTNode {
	private final String id; // 节点id
	private final Stack<NodeInfo> newNodes; // 未请求或短时间内未请求的节点信息
	private final Set<String> oldNodes = new LinkedHashSet<String>(); // 已经请求过的节点信息, 共6个字符, 4个ip字节加2个端口字节进行iso-8859-1编码, 使用LinkedHashSet自动去重并保持顺序
	private final RoutingList routingList; // 节点维护的路由列表

	public LocalDHTNode(String id, Stack<NodeInfo> newNodes, RoutingList routingList) {
		this.id = id;
		this.newNodes = newNodes;
		this.routingList = routingList;
	}

	public String getId() {
		return id;
	}

	public Stack<NodeInfo> getNewNodes() {
		return newNodes;
	}

	public Set<String> getOldNodes() {
		return oldNodes;
	}

	public RoutingList getRoutingList() {
		return routingList;
	}
}
