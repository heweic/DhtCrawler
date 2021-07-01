package org.my.pro.dhtcrawler.domain;

import java.math.BigInteger;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.NodeId;

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
		return new String(bs, KeyWord.DHT_CHARSET);
	}

	@Override
	public byte[] bsId() {
		return bs;
	}

	public static void main(String[] args) {
		// DhtNodeID dhtNodeID1 = new DhtNodeID(NodeIdRandom.random("a").getBytes());
		// DhtNodeID dhtNodeID2 = new DhtNodeID(NodeIdRandom.random("B").getBytes());
		// DhtNodeID dhtNodeID3 = new DhtNodeID(NodeIdRandom.random("c").getBytes());
		//
		// System.out.println(dhtNodeID1.intId().toString());
		// System.out.println(dhtNodeID1.intId().intValue());
		// System.out.println("//");
		// System.out.println(dhtNodeID2.intId().toString());
		// System.out.println(dhtNodeID2.intId().intValue());

	}

}
