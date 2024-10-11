package org.my.pro.dhtcrawler.message;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.NodeId;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.routingTable.DefaultNodeInfo;
import org.my.pro.dhtcrawler.routingTable.DhtNodeID;

public class NodeInfoFactory {

	
	
	public static NodeInfo fromMessage(DefaultResponse krpcMessage) {
		try {
			String ip = krpcMessage.addr().getAddress().getHostAddress();
			byte[] id = krpcMessage.r().getMap().get(KeyWord.ID).getBytes();
			int port = krpcMessage.addr().getPort();

			NodeId nodeId = new DhtNodeID(id);
			NodeInfo info = new DefaultNodeInfo(nodeId, ip, port);

			return info;
		} catch (Exception e) {
			return null;
		}
	}

	
}
