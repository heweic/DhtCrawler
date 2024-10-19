package org.my.pro.dhtcrawler.routingTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.my.pro.dhtcrawler.Node;
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

	private Map<String, Node> nodes = new ConcurrentHashMap<String, Node>();

	// private static Log log = LogFactory.getLog(SimpleRoutingTable.class);

	public SimpleRoutingTable() {

	}

	@Override
	public void removeNode(Node node) {
		nodes.remove(node.nodeId().toString());
	}

	@Override
	public void add(Node info) {
		nodes.put(info.nodeId().id(), info);

	}

	
	
	@Override
	public void resetNodeId(byte[] id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int targetSize(byte[] target) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * 水桶抽样算法实现
	 * 
	 * @param map
	 * @param count
	 */
	private List<Node> getRandomEntries(int count) {

		List<Node> reservoir = new ArrayList<Node>();
		//
		Random random = new Random();
		int i = 0;
		for (Map.Entry<String, Node> entry : nodes.entrySet()) {
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
	public List<Node> getClosestNodes(byte[] targetId, int count) {
		return getRandomEntries(count);
	}

	@Override
	public List<Node> randomNodes(int num) {
		// TODO Auto-generated method stub
		return null;
	}



	

}
