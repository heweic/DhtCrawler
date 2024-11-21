package org.my.pro.dhtcrawler;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

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

	public static void main(String[] args) {
		HmacUtils hmacUtils = 	new HmacUtils(HmacAlgorithms.HMAC_SHA_256, "123333");
		System.out.println(hmacUtils.hmacHex("12564156151"));
		System.out.println(hmacUtils.hmacHex("12564156151"));
	}

}
