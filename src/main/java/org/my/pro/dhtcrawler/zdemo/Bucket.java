package org.my.pro.dhtcrawler.zdemo;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 桶, 用来保存DHT节点, 参考http://blog.csdn.net/xxxxxx91116/article/details/7970815中的路由表解释
 * @author dgqjava
 *
 */
public class Bucket implements Comparable<Bucket> {
    public static final BigInteger MAX_VALUE = new BigInteger("2").pow(160); // 桶的覆盖范围最大为0到2的160次方, 因为节点id为20字节
    
    private final BigInteger start; // 当前桶的开始位置, 其他节点的id值在开始位置(包含)和结束位置(不包含)之间则会被放入到当前桶中
    private final BigInteger end; // 当前桶的结束位置, 其他节点的id值在开始位置(包含)和结束位置(不包含)之间则会被放入到当前桶中
    private final Map<BigInteger, NodeInfo> nodes = new LinkedHashMap<BigInteger, NodeInfo>() { // 当前桶中保存的节点, 每个桶最多保存8个节点, 超过8个节点后自动移除最旧节点, 同时同一节点多次重复添加不会影响其他节点, 这里使用LinkedHashMap作为LRU算法的简单实现
		private static final long serialVersionUID = 1L;
		protected boolean removeEldestEntry(Entry<BigInteger, NodeInfo> eldest) {
			return size() > 8;
		}
    }; 
    
    public Bucket(BigInteger start, BigInteger end) {
        this.start = start;
        this.end = end;
    }
    
    /**
     * 判断该id是否可以放入到当前桶中
     * @param bi
     * @return
     */
    public boolean contain(BigInteger bi) {
        return start.compareTo(bi) <= 0 && end.compareTo(bi) > 0;
    }
    
    /**
     * 将一个节点放入到当前桶中
     * @param node
     * @return
     */
    public void addNode(NodeInfo node) {
        nodes.put(node.getId().getValue(), node);
    }
    
    /**
     * 获取桶内所有节点
     * @return
     */
    public Collection<NodeInfo> getNodes() {
        return nodes.values();
    }
    
    public void clear() {
    	nodes.clear();
    }
    
    /**
     * 当前桶分裂为两个桶
     * @return 分裂出来的两个新桶, 第一个为当前节点id所在的桶
     */
    Bucket[] split() {
    	// 计算当前桶长度, 如果小于等于8则无法进行分裂
        BigInteger len = end.subtract(start);
        if(len.compareTo(new BigInteger("8")) <= 0) {
            return null;
        }
        
        // 桶2的开始位置为旧的开始位置加上当前旧桶容量的一半, 例如旧桶范围为0-16, 则分裂为0-8和8-16
        BigInteger newStart = start.add(len.divide(new BigInteger("2")));
        // 桶1的结束位置为桶2开始位置
        BigInteger newEnd = newStart;
        
        Bucket b1 = new Bucket(start, newEnd);
        Bucket b2 = new Bucket(newStart, end);
        
        // 获取我们自身的节点, 我们自身的节点id所在的桶可以再次分裂为两个, 直到大小为8后不再分裂
        NodeInfo currentNode = (NodeInfo) nodes.values().toArray()[0];
        if(b1.contain(currentNode.getId().getValue())) {
        	b1.addNode(currentNode);
        	return new Bucket[]{b1, b2};
        }
        
        b2.addNode(currentNode);
        return new Bucket[]{b2, b1};
    }

    public int compareTo(Bucket o) {
        return start.compareTo(o.start);
    }
}