package org.my.pro.dhtcrawler.routingTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.util.DHTUtils;

/**
 * DHT路由表实现
 */
public class DHTRoutingTable implements RoutingTable {

	public static Log LOG = LogFactory.getLog(DHTRoutingTable.class);

	/**
	 * 当前节点ID
	 */
	private  byte[] localNodeId;
	/**
	 * 每个桶的大小
	 */
	private final int bucketValueSize = 8;
	/**
	 * 桶的数量， 20字节的ID，对应160个位
	 */
	private final int bucketSize = 20 * 8;
	/**
	 * 桶集合
	 */
	private volatile List<List<Node>> buckets = new ArrayList<List<Node>>(bucketSize);



	private Object lock = new Object();

	public DHTRoutingTable(byte[] localNodeId) {
		this.localNodeId = localNodeId;

		//初始化容器
		for (int i = 0; i < bucketSize; i++) {
			buckets.add(new ArrayList<>());
		}
	}
	
	

	@Override
	public void resetNodeId(byte[] id) {
		synchronized (lock) {
			//更新nodeID
			this.localNodeId = id;
			
			//重置桶中节点所在位置
			List<List<Node>> old = buckets;
			buckets = new ArrayList<List<Node>>(bucketSize);
			for (int i = 0; i < bucketSize; i++) {
				buckets.add(new ArrayList<>());
			}
			
			for(List<Node> bucket : old) {
				for(Node node : bucket) {
					add(node);
				}	
			}
			
		}
		
	}



	@Override
	public void add(Node node) {
		synchronized (lock) {
			// 判断节点是否是已经存在
			for (List<Node> bucket : buckets) {
				for (Node n : bucket) {
					if (n.nodeId().id().equals(node.nodeId().id())) {
						// 已经存在
						return;
					}
				}
			}
			//
			int bucketIndex = calculateBucketIndex(node.nodeId().bsId());
			List<Node> bucket = buckets.get(bucketIndex);
			if (!bucket.contains(node)) {
				if (bucket.size() < bucketValueSize) {
					//log.info(node.nodeId().id() + "添加到" + bucketIndex);
					bucket.add(node); // 如果桶未满，则添加节点
				} else {
					// 桶已满，可能需要实现替换或移除策略
					handleFullBucket(bucket, node);
				}
			}
			
		}

	}

	@Override
	public void removeNode(Node node) {
		synchronized (lock) {
			int bucketIndex = calculateBucketIndex(node.nodeId().bsId());
			buckets.get(bucketIndex).remove(node);
		}
	}

	@Override
	public List<Node> getClosestNodes(byte[] targetId, int count) {
		//
		int bucketIndex = calculateBucketIndex(targetId);
		List<Node> closestNodes = new ArrayList<>(buckets.get(bucketIndex));

		if (closestNodes.size() == 0) {
			return closestNodes;
		}

		// 在其他桶中寻找节点，直到找到所需数量的节点
		int left = bucketIndex - 1;
		int right = bucketIndex + 1;
		while (closestNodes.size() < count && (left >= 0 || right < buckets.size())) {
			if (left >= 0) {
				closestNodes.addAll(buckets.get(left));
				left--;
			}
			if (right < buckets.size()) {
				closestNodes.addAll(buckets.get(right));
				right++;
			}
		}

		// 返回最多 count 个节点
		return closestNodes.subList(0, Math.min(count, closestNodes.size()));
	}

	// 获取桶的索引，基于XOR距离
	private int calculateBucketIndex(byte[] nodeId) {
		//
		byte[] distance = DHTUtils.xorDistance(localNodeId, nodeId);
		int index = bucketSize - DHTUtils.distanceAsBigInteger(distance).bitCount();

		// log.info(DHTUtils.byteArrayToHexString(nodeId) + "-桶索引:" + index);
		return index;
	}

	// 处理满桶的替换策略 (占位实现)
	private void handleFullBucket(List<Node> bucket, Node node) {
		// 在 Kademlia 协议中，可以实现类似替换不可用节点的策略
		// 这里只是示例，占位处理逻辑
		bucket.remove(0); // 简单移除最旧的节点
		bucket.add(node); // 添加新的节点
	}

	@Override
	public List<Node> randomNodes(int num) {
		return getRandomEntries(num);
	}

	@Override
	public int targetSize(byte[] target) {
		int index = calculateBucketIndex(target);
		return buckets.get(index).size();
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
		for (List<Node> list : buckets) {

			for (Node n : list) {
				i++;
				if (reservoir.size() < count) {
					reservoir.add(n);
				} else {
					int j = random.nextInt(i);
					if (j < count) {
						reservoir.set(j, n);
					}
				}
			}
		}

		//
		return reservoir;
	}
	
	public static void main(String[] args) {
		String a = new String("123");
		String b = a;
		a = null;
		System.out.println(a);
		System.out.println(b);
		
		//
		List<String> list = new ArrayList<String>();
		list.add("1");
		List<String> list2 = list;
		list = null;
		
		System.out.println(list2.size());
		
		
		
		
		
		
		
		
	}
}
