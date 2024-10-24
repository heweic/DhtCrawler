package org.my.pro.dhtcrawler.routingTable;

import java.nio.ByteBuffer;

import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.NodeId;

public class DefaultNodeInfo implements Node {

	
	private NodeId id; // 节点id
	private byte[] ipData; // ip的字节数组
	private byte[] portData; // 端口的字节数组
	
	//本地DHT节点端口
	private int localDHTID;

	public DefaultNodeInfo(NodeId id, byte[] ipData, byte[] portData , int localDHTID) {
		this.id = id;
		this.ipData = ipData;
		this.portData = portData;
		this.localDHTID = localDHTID;
		//
	}


	@Override
	public ByteBuffer toBuf() {
		ByteBuffer buffer = ByteBuffer.allocate(26);
		buffer.put(id.bsId()); // ID
		buffer.put(ipData); // IP
		buffer.put(portData);// port
		return buffer;
	}

	@Override
	public String ip() {
		return (0xFF & ipData[0]) + "." + (0xFF & ipData[1]) + "." + (0xFF & ipData[2]) + "." + (0xFF & ipData[3]);
	}

	@Override
	public int port() {
		return ((0xFF & portData[0]) << 8) | (0xFF & portData[1]);
	}

	@Override
	public NodeId nodeId() {
		return id;
	}

	@Override
	public int localDHTID() {
		// TODO Auto-generated method stub
		return localDHTID;
	}

}
