package org.my.pro.dhtcrawler.message;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.NodeId;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.routingTable.DefaultNodeInfo;
import org.my.pro.dhtcrawler.routingTable.DhtNodeID;

public class NodeInfoFactory {

	
	
	public static Node fromMessage(DefaultResponse krpcMessage) {
		try {
			String ip = krpcMessage.addr().getAddress().getHostAddress();
			byte[] id = krpcMessage.r().getMap().get(KeyWord.ID).getBytes();
			int port = krpcMessage.addr().getPort();

			NodeId nodeId = new DhtNodeID(id);
			Node info = new DefaultNodeInfo(nodeId, ip, port);

			return info;
		} catch (Exception e) {
			return null;
		}
	}

	
}
