package org.my.pro.dhtcrawler.handler;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.WorkHandler;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.util.BenCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.adaxisoft.bencode.BEncodedValue;

/**
 * Getpeers与torrent文件的info_hash有关。 这时KPRC协议中的”q”=”get_peers”。 get_peers请求包含2个参数。
 * 第一个参数是id，包含了请求node的nodeID。 第二个参数是info_hash，它代表torrent文件的infohash。
 * 如果被请求的节点有对应info_hash的peers，他将返回一个关键字values,这是一个列表类型的字符串。
 * 每一个字符串包含了"CompactIP-address/portinfo"格式的peers信息。
 * 如果被请求的节点没有这个infohash的peers，那么他将返回关键字nodes，
 * 这个关键字包含了被请求节点的路由表中离info_hash最近的K个nodes，
 * 使用"Compactnodeinfo"格式回复。在这两种情况下，关键字token都将被返回。
 * token关键字在今后的annouce_peer请求中必须要携带。Token是一个短的二进制字符串。
 * 
 */
public class GetPeersHandler extends RequestMessageHandler {

	private WorkHandler handler;

	public Logger logger = LoggerFactory.getLogger(GetPeersHandler.class);

	public GetPeersHandler(LocalDHTNode dhtNode, WorkHandler handler) {
		super(dhtNode);
		this.handler = handler;
	}

	@Override
	public KrpcMessage handler0(KrpcMessage message) throws Exception {
		if (message instanceof DefaultRequest) {
			//
			DefaultRequest defaultRequest = (DefaultRequest) message;

			byte[] bs = defaultRequest.a().getMap().get(KeyWord.INFO_HASH).getBytes();

			//String hashcode = DHTUtils.byteArrayToHexString(bs);

		//	logger.info("{  " + localNode.port() + "  }  " + hashcode + "{ "
		//			+ message.addr().getAddress().getHostAddress() + ":" + message.addr().getPort() + " }");

			if (null != handler) {
				handler.handler(bs, message);
			}

			// printMagnet(new String(bs, KeyWord.DHT_CHARSET), "GetPeersHandler");
			//
			List<Node> result = localNode.findNearest(bs);

			ByteBuffer buffer = ByteBuffer.allocate(26 * result.size());

			for (Node info : result) {
				buffer.put(info.toBuf().array());
			}

			DefaultResponse defaultResponse = new DefaultResponse(message.t(), message.addr());
			try {
				BEncodedValue r = BenCodeUtils.to(KeyWord.ID, localNode.id(), KeyWord.NODES, buffer.array());
				r.getMap().put(KeyWord.TOKEN, new BEncodedValue(RandomStringUtils.random(6)));
				defaultResponse.setR(r);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//
			return defaultResponse;
		}

		return null;
	}

}
