package org.my.pro.dhtcrawler.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class DHTNodeIDPartitionGenerator {
	// 生成指定数量的 DHT 节点 ID，分为 numPartitions 个区间
	public static List<BigInteger> generateDHTNodeIDs(int nodeCount, int numPartitions) {
		List<BigInteger> nodeIDs = new ArrayList<>();
		SecureRandom random = new SecureRandom();
		int bitLength = 160; // DHT 通常使用 160 位的节点 ID (SHA-1 哈希值)

		// 计算每个分区的范围大小 (2^160 / numPartitions)
		BigInteger maxID = BigInteger.ONE.shiftLeft(bitLength); // 2^160
		BigInteger partitionSize = maxID.divide(BigInteger.valueOf(numPartitions));

		for (int i = 0; i < numPartitions; i++) {
			// 计算当前分区的起始值和结束值
			BigInteger startRange = partitionSize.multiply(BigInteger.valueOf(i));
			BigInteger endRange = startRange.add(partitionSize).subtract(BigInteger.ONE);

			// 生成每个分区中的节点 ID，确保均匀分布
			int nodesPerPartition = nodeCount / numPartitions;
			if (i < nodeCount % numPartitions) {
				nodesPerPartition++; // 平衡节点分布，处理除不尽的情况
			}

			for (int j = 0; j < nodesPerPartition; j++) {
				BigInteger nodeId = generateRandomInRange(startRange, endRange, random);
				nodeIDs.add(nodeId);
			}
		}

		return nodeIDs;
	}

	// 在给定的区间 [min, max] 内随机生成一个节点 ID
	private static BigInteger generateRandomInRange(BigInteger min, BigInteger max, SecureRandom random) {
		BigInteger range = max.subtract(min).add(BigInteger.ONE); // max - min + 1
		BigInteger randomID;
		do {
			randomID = new BigInteger(max.bitLength(), random);
		} while (randomID.compareTo(range) >= 0); // 确保生成的 ID 在范围内
		return randomID.add(min); // 加上最小值，保证在区间内
	}

	// 测试方法
	public static void main(String[] args) {
		int nodeCount = 10; // 要生成的节点数量
		int numPartitions = 10; // 将 ID 空间分成5个区间

		List<BigInteger> nodeIDs = generateDHTNodeIDs(nodeCount, numPartitions);

		// 输出生成的 DHT 节点 ID
		for (BigInteger nodeId : nodeIDs) {
			System.out.println("Node ID: " + nodeId.toString(16)); // 十六进制输出
		}
	}
}
