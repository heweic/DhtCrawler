package org.my.pro.dhtcrawler.zdemo;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * DHT节点信息, 26字节, 包括了节点id(20字节), ip(4字节), 端口(2字节)
 * @author dgqjava
 *
 */
public class NodeInfo {
    private final NodeId id; // 节点id
    private final byte[] ipData; // ip的字节数组
    private final byte[] portData; // 端口的字节数组

	public NodeInfo(String ip, int port, String nodeId) {
        String[] ss = ip.split("\\.");
        this.ipData = new byte[] {(byte) Integer.parseInt(ss[0]), (byte) Integer.parseInt(ss[1]), (byte) Integer.parseInt(ss[2]), (byte) Integer.parseInt(ss[3])};
        ByteBuffer bb = ByteBuffer.allocate(4).putInt(port);
        bb.flip();
        this.portData = Arrays.copyOfRange(bb.array(), 2, 4);
        try {
			id = null == nodeId ? null : new NodeId(nodeId.getBytes("iso-8859-1"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
    }
    
    public NodeId getId() {
        return id;
    }
    
    public byte[] getIpData() {
        return ipData;
    }
    
    public byte[] getPortData() {
        return portData;
    }
    
    public String getIp() {        
    	return (0xFF & ipData[0]) + "." + (0xFF & ipData[1]) + "." + (0xFF & ipData[2]) + "." + (0xFF & ipData[3]);
	}

	public int getPort() {
		return ((0xFF & portData[0]) << 8) | (0xFF & portData[1]);
	}

	/**
	 * 需要保存到路由表的桶中, 而桶是用LinkedHashMap实现, 因此需要重写equals和hashCode, 这里认为id一致就为同一个节点
	 */
    public boolean equals(Object obj) {
        if(obj instanceof NodeInfo) {
            NodeInfo n = (NodeInfo) obj;
            return id.getValue().equals(n.getId().getValue());
        }
        return false;
    }

	/**
	 * 需要保存到路由表的桶中, 而桶是用LinkedHashMap实现, 因此需要重写equals和hashCode, 这里认为id一致就为同一个节点
	 */
	public int hashCode() {
		return id.getValue().hashCode();
	}
}