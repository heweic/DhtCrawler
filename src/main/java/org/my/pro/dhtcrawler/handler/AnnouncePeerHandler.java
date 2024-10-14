package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.WorkHandler;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.util.BenCodeUtils;
import org.my.pro.dhtcrawler.util.ByteArrayHexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 这个请求用来表明发出announce_peer请求的node， 正在某个端口下载torrent文件。announce_peer包含4个参数。
 * 第一个参数是id，包含了请求node的nodeID； 第二个参数是info_hash，包含了torrent文件的infohash；
 * 第三个参数是port包含了整型的端口号，表明peer在哪个端口下载； 第四个参数数是token，这是在之前的get_peers请求中收到的回复中包含的。
 * 收到announce_peer请求的node必须检查这个token与之前我们回复给这个节点get_peers的token是否相同。
 * 如果相同，那么被请求的节点将记录发送announce_peer节点的 IP和请求中包含的port端口号在peer联系信息中对应的infohash下。
 * 
 */
public class AnnouncePeerHandler extends RequestMessageHandler {

	private WorkHandler handler;

	private static Logger logger = LoggerFactory.getLogger(AnnouncePeerHandler.class);

	public AnnouncePeerHandler(RoutingTable routingTable, LocalDHTNode dhtNode, WorkHandler handler) {
		super(routingTable, dhtNode);
		this.handler = handler;
	}

	@Override
	public KrpcMessage handler0(KrpcMessage message) throws Exception {

		if (message instanceof DefaultRequest) {
			DefaultRequest defaultRequest = (DefaultRequest) message;

			String code = ByteArrayHexUtils
					.byteArrayToHexString(defaultRequest.a().getMap().get(KeyWord.INFO_HASH).getBytes());

			String mes = message.addr().getAddress().getHostAddress() + "下载端口:"
					+ defaultRequest.a().getMap().get("port").getInt() + "种子hash:" + code;
			logger.error(mes);

			if (null != handler) {
				handler.handler(mes, message);
			}
		}

		DefaultResponse defaultResponse = new DefaultResponse(message.t(), message.addr());
		defaultResponse.setR(BenCodeUtils.to(KeyWord.ID, localNode.id()));
		//
		return defaultResponse;
	}

}
