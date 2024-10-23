package org.my.pro.dhtcrawler.routingTable;

import java.nio.ByteBuffer;

import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.NodeId;

/**
 * Peer节点信息
 */
public class PeerInfo implements Node {

	private byte[] ipData; // ip的字节数组
	private byte[] portData; // 端口的字节数组

	public PeerInfo(byte[] ipData, byte[] portData) {

		this.ipData = ipData;
		this.portData = portData;
	}

	@Override
	public NodeId nodeId() {
		return null;
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
	public ByteBuffer toBuf() {
		return null;
	}

	@Override
	public int localDHTID() {
		return 0;
	}

}
