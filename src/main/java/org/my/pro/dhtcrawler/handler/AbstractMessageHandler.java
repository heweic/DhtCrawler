package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.MessageHandler;

public abstract class AbstractMessageHandler implements MessageHandler {

	protected LocalDHTNode localNode;

	public AbstractMessageHandler(LocalDHTNode dhtNode) {
		super();
		this.localNode = dhtNode;
	}

	@Override
	public KrpcMessage handler(KrpcMessage message) throws Exception {

		//
		return handler0(message);
	}

	public abstract KrpcMessage handler0(KrpcMessage message) throws Exception;

}
