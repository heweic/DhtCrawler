package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.RoutingTable;

public abstract class ResponseMessageHandler extends AbstractMessageHandler {

	public ResponseMessageHandler(RoutingTable routingTable, LocalDHTNode dhtNode) {
		super(routingTable, dhtNode);
	}

	
}
