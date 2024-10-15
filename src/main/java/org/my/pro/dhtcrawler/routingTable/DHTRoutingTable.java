package org.my.pro.dhtcrawler.routingTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.RoutingTable;

/**
 * DHT路由表实现
 */
public class DHTRoutingTable implements RoutingTable {

	/**
	 * 当前节点ID
	 */
	private final byte[] localNodeId;
	/**
	 * 每个桶的大小
	 */
	private final int bucketSize = 8;
	/**
	 * 桶集合
	 */
	private volatile List<List<Node>> buckets = new ArrayList<List<Node>>();

	private volatile boolean hasValue = false;

	private Object lock = new Object();

	public DHTRoutingTable(byte[] localNodeId) {
		this.localNodeId = localNodeId;
		int bucketCount = localNodeId.length * 8; // 例如 160 位对应 160 个桶
		for (int i = 0; i < bucketCount; i++) {
			buckets.add(new ArrayList<>());
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
				if (bucket.size() < bucketSize) {
					bucket.add(node); // 如果桶未满，则添加节点
				} else {
					// 桶已满，可能需要实现替换或移除策略
					handleFullBucket(bucket, node);
				}
			}
			if(!hasValue) {
				hasValue = true;
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
		
		if(closestNodes.size() == 0) {
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
		int distance = xorDistance(localNodeId, nodeId);
		return Integer.numberOfLeadingZeros(distance) - (Integer.SIZE - buckets.size());
	}

	// XOR 计算距离
	private int xorDistance(byte[] id1, byte[] id2) {
		int distance = 0;
		for (int i = 0; i < id1.length; i++) {
			distance = (distance << 8) | (id1[i] ^ id2[i]);
		}
		return distance;
	}

	// 处理满桶的替换策略 (占位实现)
	private void handleFullBucket(List<Node> bucket, Node node) {
		// 在 Kademlia 协议中，可以实现类似替换不可用节点的策略
		// 这里只是示例，占位处理逻辑
		bucket.remove(0); // 简单移除最旧的节点
		bucket.add(node); // 添加新的节点
	}

	@Override
	public boolean hasNode() {
		return hasValue;
	}

	@Override
	public List<Node> randomNodes(int num) {
		return getRandomEntries(num);
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
}
