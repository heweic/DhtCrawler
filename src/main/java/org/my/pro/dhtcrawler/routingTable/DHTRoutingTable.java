package org.my.pro.dhtcrawler.routingTable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.task.TryFindPeerAndDownload;
import org.my.pro.dhtcrawler.util.DHTUtils;

/**
 * DHT路由表实现
 */
public class DHTRoutingTable implements RoutingTable {

	public static Log LOG = LogFactory.getLog(DHTRoutingTable.class);

	/**
	 * 当前节点ID
	 */
	private byte[] localNodeId;
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
	private volatile List<ConcurrentSkipListMap<BigInteger, Node>> buckets = new ArrayList<ConcurrentSkipListMap<BigInteger, Node>>(
			bucketSize);

	public DHTRoutingTable(byte[] localNodeId) {
		this.localNodeId = localNodeId;

		// 初始化容器
		for (int i = 0; i < bucketSize; i++) {
			buckets.add(new ConcurrentSkipListMap<BigInteger, Node>());
		}
	}

	@Override
	public void resetNodeId(byte[] id) {
		// 更新ID
		this.localNodeId = id;
		//
		// 初始化容器
		buckets.clear();
		buckets = new ArrayList<ConcurrentSkipListMap<BigInteger, Node>>(bucketSize);

		for (int i = 0; i < bucketSize; i++) {
			buckets.add(new ConcurrentSkipListMap<BigInteger, Node>());
		}
	}

	@Override
	public void add(Node node) {

		//
		int bucketIndex = calculateBucketIndex(node.nodeId().bsId());
		ConcurrentSkipListMap<BigInteger, Node> bucket = buckets.get(bucketIndex);
		bucket.put(node.nodeId().intId(), node);
		//
		if (bucket.size() > bucketValueSize) {
			bucket.remove(bucket.firstKey());
		}
		//
		TryFindPeerAndDownload.getInstance().addNode(node);
		//

	}

	@Override
	public void removeNode(Node node) {

		int bucketIndex = calculateBucketIndex(node.nodeId().bsId());
		buckets.get(bucketIndex).remove(node.nodeId().intId());

	}

	@Override
	public List<Node> getClosestNodes(byte[] targetId, int count) {
		//
		int bucketIndex = calculateBucketIndex(targetId);

		ConcurrentSkipListMap<BigInteger, Node> closestBucket = buckets.get(bucketIndex);

		List<Node> list = new ArrayList<Node>();

		closestBucket.entrySet().forEach( e ->{
			list.add(e.getValue());
		});
		if (list.size() == count) {
			return list;
		}

		// 在其他桶中寻找节点，直到找到所需数量的节点
		int left = bucketIndex - 1;
		int right = bucketIndex + 1;
		while (list.size() < count && (left >= 0 || right < buckets.size())) {
			if (left >= 0) {
				buckets.get(left).entrySet().forEach( e->{
					list.add(e.getValue());
				});
				
				left--;
			}
			if (right < buckets.size()) {
				buckets.get(right).entrySet().forEach( e->{
					list.add(e.getValue());
				});
				right++;
			}
		}

		// 返回最多 count 个节点
		return list.subList(0, Math.min(count, list.size()));
	}

	// 获取桶的索引，基于XOR距离
	private int calculateBucketIndex(byte[] nodeId) {
		//
		byte[] distance = DHTUtils.xorDistance(localNodeId, nodeId);
		int index = bucketSize - DHTUtils.distanceAsBigInteger(distance).bitCount();

		// log.info(DHTUtils.byteArrayToHexString(nodeId) + "-桶索引:" + index);
		return index;
	}

	@Override
	public int targetSize(byte[] target) {
		if (null == target) {
			int count = 0;
			for (ConcurrentSkipListMap<BigInteger, Node> bucket : buckets) {
				count += bucket.size();
			}
			return count;
		}
		int index = calculateBucketIndex(target);
		return buckets.get(index).size();
	}

	class NodeComParator implements Comparator<BigInteger> {

		@Override
		public int compare(BigInteger o1, BigInteger o2) {
			return o1.compareTo(o2);
		}

	}

}
