package org.my.pro.dhtcrawler.zdemo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 路由表, 路由表中的桶用来保存其他DHT节点, 参考http://blog.csdn.net/xxxxxx91116/article/details/7970815中的路由表解释
 * @author dgqjava
 *
 */
public class RoutingList {
    private final List<Bucket> buckets = new ArrayList<Bucket>(); // 路由表中的所有桶, 一共157个(20 * 8 - 3)
    
    /**
     * 创建一个路由表
     * @param nodeInfo 我们自身的节点信息
     */
    public RoutingList(NodeInfo nodeInfo) {
        // 创建初始化的桶, 范围为0到2的160次方, 添加我们自身的节点到桶中
        Bucket b = new Bucket(new BigInteger("0"), Bucket.MAX_VALUE);
        b.addNode(nodeInfo);
        
        // 添加桶到桶列表中
        buckets.add(b);
        
        // 将桶完成分裂, 如果返回null则无法继续分裂, 返回的数组为分裂出的两个新桶, 数组的第一个元素为我们自身的节点所在的桶, 因此可以继续分裂, 直到桶容量为8
        Bucket[] newBs;
        while(null != (newBs = b.split())) {
        	buckets.remove(b); // 移除掉被分裂的桶
            buckets.add(newBs[0]); // 添加新桶1
            buckets.add(newBs[1]); // 添加新桶2
            b = newBs[0]; // 新桶0为我们自身的节点所在的桶, 因此需要再次分裂
        }
        
        // 清空所有桶内的节点, 也就是不把我们自身的节点信息保留到桶内
        b.clear();
        
        // 分裂完毕后将所有桶排序, 方便接下来查找一个id所在的桶旁边的两个桶, 
        // 因为当其他节点发送find_node请求时我们需要返回我们的路由表中距离他查找的id最近的8个节点, 
        // 而他查找的id所在的桶可能不足8个节点, 因此需要从所在的桶的两边的桶继续获取节点来得到8个距离目标id最近的节点
        Collections.sort(buckets);
    }
    
    /**
     * 获取最近的8个节点
     * @param id 目标id
     * @return
     */
    public List<NodeInfo> getNearestNodes(NodeId id) {
    	// 定位目标id所在的桶
        int index = 0;
        for(int i = 0; i < buckets.size(); i++) {
            if(buckets.get(i).contain(id.getValue())) {
                index = i;
            }
        }
        
        // 目标id所在的桶中的节点
        List<NodeInfo> nodes = new ArrayList<NodeInfo>(buckets.get(index).getNodes());
        // 目标id所在的桶左边的桶中的节点
        List<NodeInfo> leftNodes = new ArrayList<NodeInfo>();
        // 目标id所在的桶右边的桶中的节点
        List<NodeInfo> rightNodes = new ArrayList<NodeInfo>();
        
        // 获取目标id所在的桶的左边的桶的节点直到获取够8个以上
        for(int i = index - 1; i >= 0; i--) {
            leftNodes.addAll(buckets.get(i).getNodes());
            if(leftNodes.size() >= 8) {
                break;
            }
        }
        
        // 获取目标id所在的桶的右边的桶的节点直到获取够8个以上
        for(int i = index + 1; i < buckets.size(); i++) {
            rightNodes.addAll(buckets.get(i).getNodes());
            if(rightNodes.size() >= 8) {
                break;
            }
        }
        
        // 在最终的16个以上节点中筛选出距离目标id最近的8个节点, 目前得到的数据为: 左边8个或以上节点, 左边若干节点, 目标id, 右边若干节点, 右边8个或以上节点
        // 目标id左右两边都有至少8个节点, 因此可以确保获取到当前路由表中距离目标id最近的8个节点
        nodes.addAll(leftNodes);
        nodes.addAll(rightNodes);
        
        // 所有的距离保存在TreeSet中, 利用TreeSet维护自然顺序的特性, 获取的第一个就是最近的距离, 最后一个就是最远的距离
        Set<BigInteger> distances = new TreeSet<BigInteger>();
        // 距离对应的节点列表, 因为同样的距离可能有左右两个节点
        Map<BigInteger, List<NodeInfo>> distance2Node = new HashMap<BigInteger, List<NodeInfo>>();
        for(NodeInfo n : nodes) {
        	// 两个id的距离为他们id转化为无符号数后的差值的绝对值
            BigInteger distance = n.getId().getValue().subtract(id.getValue()).abs();
            distances.add(distance);
            
            // 设置距离对应的节点
            List<NodeInfo> value;
            if(null == (value = distance2Node.get(distance))) {
                value = new ArrayList<NodeInfo>();
                distance2Node.put(distance, value);
            }
            value.add(n);
        }
        
        // 将要返回的节点, 从TreeSet中依次取出最近距离的节点, 满8个后结束
        List<NodeInfo> result = new ArrayList<NodeInfo>();
        for(BigInteger distance : distances) {
            result.addAll(distance2Node.get(distance));
            if(result.size() >= 8) {
                break;
            }
        }
        
        // 只返回8个节点
        return result.size() > 8 ? result.subList(0, 8) : result;
    }

    /**
     * 添加节点到路由表
     * @param node 需要添加的节点
     * @return
     */
    public void addNode(NodeInfo node) {
    	BigInteger id = node.getId().getValue();
        for(Bucket bucket : buckets) {
        	// 如果id在当前桶范围内, 则添加到当前桶
        	if(bucket.contain(id)) {
        		bucket.addNode(node);
        		break;
        	}
        }
    }
}