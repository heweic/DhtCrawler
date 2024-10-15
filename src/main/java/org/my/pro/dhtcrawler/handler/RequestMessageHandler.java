package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.LocalDHTNode;

/**
 * 请求处理器
 */
public abstract class RequestMessageHandler extends AbstractMessageHandler {

	public RequestMessageHandler(LocalDHTNode dhtNode) {
		super(dhtNode);
	}

}
