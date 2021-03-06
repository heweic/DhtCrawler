package org.my.pro.dhtcrawler.handler;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DhtNode;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.NodeId;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.domain.DefaultNodeInfo;
import org.my.pro.dhtcrawler.domain.DhtNodeID;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.message.MessageFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DefaultResponseHandler extends ResponseMessageHandler {

	public static Log log = LogFactory.getLog(DefaultResponseHandler.class);

	public DefaultResponseHandler(RoutingTable routingTable, DhtNode dhtNode) {
		super(routingTable, dhtNode);
	}

	@Override
	public KrpcMessage handler0(BigInteger id, KrpcMessage message) throws Exception {

		if (message instanceof DefaultResponse) {
			DefaultResponse defaultResponse = (DefaultResponse) message;

			// 添加活跃节点到 路由表中
			NodeInfo newNode = readFromResponse(defaultResponse);
			routingTable.add(newNode, message);
			
			// System.out.println("收到响应包" + newNode.ip() + ":" + newNode.port());

			if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
				byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

				ByteBuf byteBuf = Unpooled.wrappedBuffer(bs);
				int num = bs.length / 26;
				// 新节点 findNode
				for (int i = 0; i < num; i++) {
					NodeInfo info = readNodeInfo(byteBuf);
					if (!routingTable.has(info.nodeId().intId())) {
						KrpcMessage Get_peers = MessageFactory.createFindNode(info.ip(), info.port(), dhtNode.id());
						dhtNode.exec(Get_peers);
					}

				}

			}
//			if (defaultResponse.r().getMap().containsKey(KeyWord.VALUES)) {
//				try {
//					List<BEncodedValue> list = defaultResponse.r().getMap().get(KeyWord.VALUES).getList();
//
//					for (BEncodedValue bv : list) {
//						ByteBuf byteBuf = Unpooled.wrappedBuffer(bv.getBytes());
//						NodeInfo info = readIpPort(byteBuf);
//						executor.execute(() -> canConnect(info.ip(), info.port()));
//					}
//
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}

			// System.out.println(new String(defaultResponse.toByteArray() ,
			// Charset.forName(KeyWord.DHT_CHARSET_STR)));

		}

		return null;
	}

	/** */
	public void canConnect(String host, int port) {
		// 向目标发送 {'msg_type': 0, 'piece': 0}
//		dhtNode.exec(MessageFactory.createD9Request(0, "192.168.150.167", 14303));
//		System.out.println(" do   get matedata  "   + "192.168.150.167" + " : " + port);

		Socket socket = new Socket();
		boolean can = false;
		try {
			socket.connect(new InetSocketAddress(host, port), 1000 * 3);
			can = true;
		} catch (Exception e) {

		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (can) {
			System.out.println("--------------------  find  " + host + " : " + port + "  ok !!!");
		} else {
			// System.out.println("find " + host + " : " + port + " faild !!!");
		}
	}

	public NodeInfo readFromResponse(DefaultResponse krpcMessage) {
		try {
			String ip = krpcMessage.addr().getAddress().getHostAddress();
			byte[] id = krpcMessage.r().getMap().get(KeyWord.ID).getBytes();
			int port = krpcMessage.addr().getPort();

			NodeId nodeId = new DhtNodeID(id);
			NodeInfo info = new DefaultNodeInfo(nodeId, ip, port);

			return info;
		} catch (Exception e) {
			return null;
		}
	}

	public NodeInfo readIpPort(ByteBuf buffer) {
		byte[] ip = new byte[4];
		byte[] port = new byte[2];
		buffer.readBytes(ip);
		buffer.readBytes(port);
		NodeInfo info = new DefaultNodeInfo(null, ip, port);
		return info;
	}

	private NodeInfo readNodeInfo(ByteBuf buffer) {

		byte[] id = new byte[20];
		byte[] ip = new byte[4];
		byte[] port = new byte[2];
		buffer.readBytes(id);
		buffer.readBytes(ip);
		buffer.readBytes(port);

		NodeId nodeId = new DhtNodeID(id);
		NodeInfo info = new DefaultNodeInfo(nodeId, ip, port);

		return info;
	}

}
