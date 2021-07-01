package org.my.pro.dhtcrawler.domain;

import org.my.pro.dhtcrawler.DhtNode;

public abstract class AbstractDhtNode implements DhtNode {

	private String id;
	/** */
	private int port;

	public AbstractDhtNode(String id, int port) {

		this.port = port;
		this.id = id;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public int port() {
		return port;
	}

}
