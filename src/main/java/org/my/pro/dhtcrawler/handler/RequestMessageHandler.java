package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.RoutingTable;

/**
 * 请求处理器
 */
public abstract class RequestMessageHandler extends AbstractMessageHandler {

	public RequestMessageHandler(RoutingTable routingTable, LocalDHTNode dhtNode) {
		super(routingTable, dhtNode);
	}

	

}
