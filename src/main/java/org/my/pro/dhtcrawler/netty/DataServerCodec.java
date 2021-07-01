package org.my.pro.dhtcrawler.netty;

import io.netty.channel.CombinedChannelDuplexHandler;

public class DataServerCodec extends CombinedChannelDuplexHandler<BenDecoder, BenEncoder> {

	public DataServerCodec() {
		super(new BenDecoder(), new BenEncoder());
	}
}
