package org.my.pro.dhtcrawler.handler;

import java.math.BigInteger;

import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.MessageHandler;
import org.my.pro.dhtcrawler.RoutingTable;

public abstract class AbstractMessageHandler implements MessageHandler {

	protected RoutingTable routingTable;
	protected LocalDHTNode dhtNode;
	
	


	public AbstractMessageHandler(RoutingTable routingTable, LocalDHTNode dhtNode) {
		this.routingTable = routingTable;
		this.dhtNode = dhtNode;
	}

	@Override
	public KrpcMessage handler(BigInteger id, KrpcMessage message) throws Exception {

		// 刷新节点活跃时间
		routingTable.nodeActive(id);
		
		//
		return handler0(id, message);
	}

	public abstract KrpcMessage handler0(BigInteger id, KrpcMessage message) throws Exception;

}
