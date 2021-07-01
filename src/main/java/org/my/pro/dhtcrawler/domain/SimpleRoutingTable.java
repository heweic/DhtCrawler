package org.my.pro.dhtcrawler.domain;

import java.math.BigInteger;

import org.my.pro.dhtcrawler.DhtNode;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.message.MessageFactory;

public class SimpleRoutingTable extends AbstractRoutingTable {

	private String findId;

	public SimpleRoutingTable(DhtNode localNode, String findId) {
		super(localNode);
		this.findId = findId;
	}

	@Override
	public void add0(NodeInfo info, KrpcMessage krpcMessage) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				//System.out.println("do - find " + info.ip());
//				KrpcMessage Get_peers = MessageFactory.createFindNode(info.ip(), info.port(), findId);
//				localNode.exec(Get_peers);
			}
		});
	}

	@Override
	public void whenTableLess() {
		KrpcMessage krpcMessage1 = MessageFactory.createFindNode("router.utorrent.com", 6881, findId);
		KrpcMessage krpcMessage2 = MessageFactory.createFindNode("dht.transmissionbt.com", 6881, findId);
		KrpcMessage krpcMessage3 = MessageFactory.createFindNode("router.bittorrent.com", 6881, findId);

		localNode.exec(krpcMessage1);
		localNode.exec(krpcMessage2);
		localNode.exec(krpcMessage3);

		for (BigInteger id : ids) {
			if (nodes.containsKey(id)) {
				NodeInfo info = nodes.get(id);
				KrpcMessage mes = MessageFactory.createGet_peers(info.ip(), info.port(), localNode.id(), findId);
				localNode.exec(mes);
			}
		}
	}

}
