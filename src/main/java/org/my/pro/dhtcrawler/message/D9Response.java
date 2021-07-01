package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;

public class D9Response extends AbstractMessage {

	private int total_size;
	private byte[] data;

	public D9Response(InetSocketAddress addr, int piece, int total_size, byte[] data) {
		super(null, null, addr);
		//
		this.total_size = total_size;
		this.piece = piece;
		this.data = data;
		this.msg_type = 1;
	}

	@Override
	public byte[] toByteArray() throws Exception {
		return new byte[] {};
	}

	public int getTotal_size() {
		return total_size;
	}

	public byte[] getData() {
		return data;
	}

}
