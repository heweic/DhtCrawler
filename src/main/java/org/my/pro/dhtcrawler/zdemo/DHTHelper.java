
package org.my.pro.dhtcrawler.zdemo;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * DHT工具类, 用来生成DHT请求和响应的数据
 * @author dgqjava
 *
 */
public class DHTHelper {

    /**
     * 生成ping请求的数据
     * @param t 本次请求的事务id
     * @param id 请求发送方的nodeId
     * @return
     */
    public static ByteBuffer getPingData(String t, String id) {
        BencodeMap params = new BencodeMap();
        params.put(new BencodeString("id"), new BencodeString(id));
        
        BencodeMap request = new BencodeMap();
        request.put(new BencodeString("t"), new BencodeString(t));
        request.put(new BencodeString("y"), new BencodeString("q"));
        request.put(new BencodeString("q"), new BencodeString("ping"));
        request.put(new BencodeString("a"), params);
        
        return ByteBuffer.wrap(request.getTotalData());
    }
    
    /**
     * 生成find_node请求的数据
     * @param t 本次请求的事务id
     * @param id 请求发送方的nodeId
     * @param target 请求目标id
     * @return
     */
    public static ByteBuffer getFindNodeData(String t, String id, String target) {
        BencodeMap params = new BencodeMap();
        params.put(new BencodeString("id"), new BencodeString(id));
        params.put(new BencodeString("target"), new BencodeString(target));
        
        BencodeMap request = new BencodeMap();
        request.put(new BencodeString("t"), new BencodeString(t));
        request.put(new BencodeString("y"), new BencodeString("q"));
        request.put(new BencodeString("q"), new BencodeString("find_node"));
        request.put(new BencodeString("a"), params);

        return ByteBuffer.wrap(request.getTotalData());
    }
    
    /**
     * 生成响应ping请求的数据
     * @param t 本次响应对应的请求的事务id
     * @param id 响应方的nodeId
     * @return
     */
    public static ByteBuffer getPingResponseData(String t, String id) {
        BencodeMap params = new BencodeMap();
        params.put(new BencodeString("id"), new BencodeString(id));
        
        BencodeMap response = new BencodeMap();
        response.put(new BencodeString("t"), new BencodeString(t));
        response.put(new BencodeString("y"), new BencodeString("r"));
        response.put(new BencodeString("r"), params);
        
        return ByteBuffer.wrap(response.getTotalData());
    }
    
    /**
     * 生成响应find_node请求的数据
     * @param t 本次响应对应的请求的事务id
     * @param id 响应方的nodeId
     * @param nodes 返回的节点
     * @return
     */
    public static ByteBuffer getFindNodeResponseData(String t, String id, List<NodeInfo> nodes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(NodeInfo node : nodes) {
        	try {
	            baos.write(node.getId().getData());
	            baos.write(node.getIpData());
	            baos.write(node.getPortData());
        	} catch (Exception e) {
        		throw new RuntimeException(e);
        	}
        }
        
        BencodeMap params = new BencodeMap();
        params.put(new BencodeString("id"), new BencodeString(id));
        params.put(new BencodeString("nodes"), new BencodeString(baos.toByteArray()));
        
        BencodeMap response = new BencodeMap();
        response.put(new BencodeString("t"), new BencodeString(t));
        response.put(new BencodeString("y"), new BencodeString("r"));
        response.put(new BencodeString("r"), params);
        
        return ByteBuffer.wrap(response.getTotalData());
    }
    
    /**
     * 生成响应get_peers请求的数据
     * @param t 本次响应对应的请求的事务id
     * @param id 响应方的nodeId
     * @param nodes 返回的节点
     * @return
     */
    public static ByteBuffer getGetPeersResponseData(String t, String id, List<NodeInfo> nodes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(NodeInfo node : nodes) {
        	try {
	            baos.write(node.getId().getData());
	            baos.write(node.getIpData());
	            baos.write(node.getPortData());
        	} catch (Exception e) {
        		throw new RuntimeException(e);
        	}
        }
        
        BencodeMap params = new BencodeMap();
        params.put(new BencodeString("id"), new BencodeString(id));
        params.put(new BencodeString("token"), new BencodeString("dg"));
        params.put(new BencodeString("nodes"), new BencodeString(baos.toByteArray()));
        
        BencodeMap response = new BencodeMap();
        response.put(new BencodeString("t"), new BencodeString(t));
        response.put(new BencodeString("y"), new BencodeString("r"));
        response.put(new BencodeString("r"), params);

        return ByteBuffer.wrap(response.getTotalData());
    }
    
    /**
     * 生成响应announce_peer请求的数据
     * @param t 本次响应对应的请求的事务id
     * @param id 响应方的nodeId
     * @return
     */
    public static ByteBuffer getAnnouncePeerResponseData(String t, String id) {
        BencodeMap params = new BencodeMap();
        params.put(new BencodeString("id"), new BencodeString(id));
        
        BencodeMap response = new BencodeMap();
        response.put(new BencodeString("t"), new BencodeString(t));
        response.put(new BencodeString("y"), new BencodeString("r"));
        response.put(new BencodeString("r"), params);
        
        return ByteBuffer.wrap(response.getTotalData());
    }
}
