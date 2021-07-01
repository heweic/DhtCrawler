package org.my.pro.dhtcrawler.zdemo;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动类
 * java技术交流群 : 326337220, 欢迎各位技术爱好者共同交流学习
 * 由于之前的代码没写注释并且结构混乱, 因此重构一版方便有需要的朋友参考, 
 * 具体关于dht协议细节可以参考网上的这篇翻译 : http://blog.csdn.net/xxxxxx91116/article/details/7970815
 * @author dgqjava
 *
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // 获取当前计算机的逻辑处理器数量, 由于使用的是NIO, 因此线程数保持和处理器数量一致能最大限度利用CPU资源
        final int processorCount = Runtime.getRuntime().availableProcessors();
        
        // 每个dht节点都需要一个唯一的id, 该id必须为20字节, 这里为17个字节, 下面的代码会随机加上另外3个字节
        final String id = "123456789125cedrf12";
        
        // 这里表示从60000到60255这256个端口将会被监听, 也就是一共会创建256个dht节点, 修改节点数量时确保下面生成id的逻辑产生的id长度为20字节
        final int port = 60000;
        final int nodeCount = 5;
        
        List<Integer> ports = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for(int i = 0; i < nodeCount; i++) {
        	// 根据处理器数量平均分配节点
            if(i % (nodeCount / processorCount) == 0 && !ids.isEmpty()) {
                //new DHTServer(ports, ids);
                ports = new ArrayList<>();
                ids = new ArrayList<>();
            }
            ports.add(port + i);
            String idtp = (char)i + id;
            ids.add(idtp); // 生成256个id, id的第一个字节为0-255, 这样让256个节点平均分布在整个哈希表上
            
        }
        if(ids.size() != 0) {
            new DHTServer(ports, ids);
        }
    }
}

