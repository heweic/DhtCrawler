package org.my.pro.dhtcrawler;

public abstract class AbstractDhtNode implements LocalDHTNode {

	protected byte[] id;
	/** */
	protected int port;


	@Override
	public byte[] id() {
		return id;
	}

	@Override
	public int port() {
		return port;
	}

	
}
