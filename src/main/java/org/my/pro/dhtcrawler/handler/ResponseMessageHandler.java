package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.DhtNode;
import org.my.pro.dhtcrawler.RoutingTable;

public abstract class ResponseMessageHandler extends AbstractMessageHandler {

	public ResponseMessageHandler(RoutingTable routingTable, DhtNode dhtNode) {
		super(routingTable, dhtNode);
	}

	
}
