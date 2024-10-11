package org.my.pro.dhtcrawler.handler;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;

import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.util.BenCodeUtils;

/**
 * 
 * Findnode被用来查找给定ID的node的联系信息。这时KPRC协议中的q=“find_node”。 find_node请求包含2个参数，
 * 第一个参数是id，包含了请求node的nodeID。 第二个参数是target，包含了请求者正在查找的node的nodeID。
 * 当一个node接收到了find_node的请求， 他应该给出对应的回复，回复中包含2个关键字id和nodes，nodes是一个字符串类型，
 * 包含了被请求节点的路由表中最接近目标node的K(8)个最接近的nodes的联系信息。
 * 
 */
public class FindNodeHandler extends RequestMessageHandler {

	public FindNodeHandler(RoutingTable routingTable, LocalDHTNode dhtNode) {
		super(routingTable, dhtNode);
	}

	@Override
	public KrpcMessage handler0(BigInteger id, KrpcMessage message) throws Exception {

		if (message instanceof DefaultRequest) {
			//
			DefaultRequest defaultRequest = (DefaultRequest) message;

			byte[] bs = defaultRequest.a().getMap().get(KeyWord.TARGET).getBytes();
			BigInteger bigInteger = new BigInteger(bs);
			//
			List<NodeInfo> result = routingTable.findNearest(bigInteger);

			ByteBuffer buffer = ByteBuffer.allocate(26 * result.size());

			for (NodeInfo info : result) {
				buffer.put(info.toBuf().array());
			}

			DefaultResponse defaultResponse = new DefaultResponse(message.t(), message.addr());
			defaultResponse.setR(BenCodeUtils.to(KeyWord.ID, dhtNode.id(), KeyWord.NODES, buffer.array()));

			//
			return defaultResponse;
		}

		return null;
	}

}
