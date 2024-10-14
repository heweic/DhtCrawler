package org.my.pro.dhtcrawler.routingTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.RoutingTable;

/**
 * 节点表实现
 * 
 * 
 * 
 * router.utorrent.com 6881 dht.transmissionbt.com 6881 router.bittorrent.com
 * 6881
 */
public class SimpleRoutingTable implements RoutingTable {

	private Map<String, NodeInfo> nodes = new ConcurrentHashMap<String, NodeInfo>();

	private static Log log = LogFactory.getLog(SimpleRoutingTable.class);

	public SimpleRoutingTable() {

	}
	
	

	@Override
	public void remove(String id) {
		nodes.remove(id);
	}



	@Override
	public Map<String, NodeInfo> allNodes() {
		return nodes;
	}

	@Override
	public boolean has(String id) {
		return nodes.containsKey(id);
	}

	@Override
	public void add(NodeInfo info) {
		nodes.put(info.nodeId().id(), info);

	}

	@Override
	public List<NodeInfo> random(int size) {
		// 协议要求返回八个可能认识节点
		if (nodes.size() == 0) {
			return new ArrayList<NodeInfo>();
		}
		return getRandomEntries(8);

	}

	/**
	 * 水桶抽样算法实现
	 * 
	 * @param map
	 * @param count
	 */
	private List<NodeInfo> getRandomEntries(int count) {

		List<NodeInfo> reservoir = new ArrayList<NodeInfo>();
		//
		Random random = new Random();
		int i = 0;
		for (Map.Entry<String, NodeInfo> entry : nodes.entrySet()) {
			i++;
			if (reservoir.size() < count) {
				reservoir.add(entry.getValue());
			} else {
				int j = random.nextInt(i);
				if (j < count) {
					reservoir.set(j, entry.getValue());
				}
			}
		}
		//
		return reservoir;
	}

	@Override
	public void nodeActive(String id) {
		NodeInfo cacheNodeInfo = nodes.get(id);

		if (cacheNodeInfo != null) {
			cacheNodeInfo.refActiveTime();
		}
	}

}
