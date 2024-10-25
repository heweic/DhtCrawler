package org.my.pro.dhtcrawler.handler;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.util.DHTUtils;

/**
 * 处理远端节点的响应消息
 */
public class DefaultResponseHandler extends ResponseMessageHandler {

	public static Log log = LogFactory.getLog(DefaultResponseHandler.class);

	public DefaultResponseHandler(LocalDHTNode dhtNode) {
		super(dhtNode);
	}

	@Override
	public KrpcMessage handler0(KrpcMessage message) throws Exception {

		if (message instanceof DefaultResponse) {
			// 需要处理callBack的是find_peer，返回的节点不添加到自身的节点表
			localNode.back(message);
			//

			try {
				DefaultResponse defaultResponse = (DefaultResponse) message;
				//
				if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
					byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

					List<Node> nodes = DHTUtils.readNodeInfo(bs , localNode);
					for(int i = 0 ; i < nodes.size() ; i ++) {
						localNode.add(nodes.get(i));
					}

				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		return null;
	}

}
