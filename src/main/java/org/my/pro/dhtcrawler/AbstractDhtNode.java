package org.my.pro.dhtcrawler;

public abstract class AbstractDhtNode implements LocalDHTNode {

	private byte[] id;
	/** */
	private int port;

	public AbstractDhtNode(byte[] localId, int port) {
		super();
		this.id = localId;
		this.port = port;
	}

	@Override
	public byte[] id() {
		return id;
	}

	@Override
	public int port() {
		return port;
	}

	
}
