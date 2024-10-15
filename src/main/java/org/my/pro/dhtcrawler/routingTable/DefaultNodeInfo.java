package org.my.pro.dhtcrawler.routingTable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.my.pro.dhtcrawler.NodeId;
import org.my.pro.dhtcrawler.Node;

public class DefaultNodeInfo implements Node {

	private long activeTime;

	private NodeId id; // 节点id
	private byte[] ipData; // ip的字节数组
	private byte[] portData; // 端口的字节数组

	public DefaultNodeInfo(NodeId id, byte[] ipData, byte[] portData) {
		this.id = id;
		this.ipData = ipData;
		this.portData = portData;
		//
		activeTime = System.currentTimeMillis();
	}

	public DefaultNodeInfo(NodeId id, String ip, int port) {
		this.id = id;

		String[] ss = ip.split("\\.");
		this.ipData = new byte[] { (byte) Integer.parseInt(ss[0]), (byte) Integer.parseInt(ss[1]),
				(byte) Integer.parseInt(ss[2]), (byte) Integer.parseInt(ss[3]) };

		ByteBuffer bb = ByteBuffer.allocate(4).putInt(port);
		bb.flip();
		this.portData = Arrays.copyOfRange(bb.array(), 2, 4);

		//
		activeTime = System.currentTimeMillis();
	}

	@Override
	public long activeTime() {
		return activeTime;
	}

	@Override
	public void refActiveTime() {
		this.activeTime = System.currentTimeMillis();
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

}
