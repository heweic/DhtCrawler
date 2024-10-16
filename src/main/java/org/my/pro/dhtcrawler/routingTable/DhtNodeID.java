package org.my.pro.dhtcrawler.routingTable;

import java.math.BigInteger;

import org.my.pro.dhtcrawler.NodeId;
import org.my.pro.dhtcrawler.util.DHTUtils;

public class DhtNodeID implements NodeId {

	private BigInteger value;
	private byte[] bs;

	public DhtNodeID(byte[] bs) {
		//
		this.bs = bs;

		if (bs.length != 20) {
			throw new RuntimeException();
		}

		value = new BigInteger(bs);
	}

	@Override
	public BigInteger intId() {
		return value;
	}

	@Override
	public String id() {
		return DHTUtils.byteArrayToHexString(bs);
	}

	@Override
	public byte[] bsId() {
		return bs;
	}

}
