package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.LocalDHTNode;

public abstract class ResponseMessageHandler extends AbstractMessageHandler {

	public ResponseMessageHandler(LocalDHTNode dhtNode) {
		super(dhtNode);
	}

	/**
	 * 
	 */
	protected volatile boolean isFindingPeer = false;
	protected String taskId;
	protected byte[] hash;

}
