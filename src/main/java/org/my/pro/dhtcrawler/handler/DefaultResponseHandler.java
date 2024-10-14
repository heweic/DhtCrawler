package org.my.pro.dhtcrawler.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.NodeId;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.routingTable.DefaultNodeInfo;
import org.my.pro.dhtcrawler.routingTable.DhtNodeID;

import be.adaxisoft.bencode.BEncodedValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 处理远端节点的响应消息
 */
public class DefaultResponseHandler extends ResponseMessageHandler {

	public static Log log = LogFactory.getLog(DefaultResponseHandler.class);

	public DefaultResponseHandler(RoutingTable routingTable, LocalDHTNode dhtNode) {
		super(dhtNode);
	}

	@Override
	public KrpcMessage handler0(KrpcMessage message) throws Exception {

		if (message instanceof DefaultResponse) {
			DefaultResponse defaultResponse = (DefaultResponse) message;

			// 添加活跃节点到 路由表中
			NodeInfo newNode = readFromResponse(defaultResponse);
			localNode.add(newNode);

			// 发现新节点,针对find_node响应
			if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
				byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

				ByteBuf byteBuf = Unpooled.wrappedBuffer(bs);
				int num = bs.length / 26;
				// 添加新节点
				for (int i = 0; i < num; i++) {
					NodeInfo info = readNodeInfo(byteBuf);
					localNode.add(info);

					// 如果是在处理查找peer
					if (isFindingPeer) {
						KrpcMessage find_peer = MessageFactory.createGet_peers(info.ip(), taskId, info.port(),
								localNode.id(), hash);
						localNode.sendMessage(find_peer);
					}
				}

			}
			// 响应包含VALUE，针对get_peer的响应
			if (defaultResponse.r().getMap().containsKey(KeyWord.VALUES)) {
				try {
					List<BEncodedValue> list = defaultResponse.r().getMap().get(KeyWord.VALUES).getList();

					for (BEncodedValue bv : list) {
						ByteBuf byteBuf = Unpooled.wrappedBuffer(bv.getBytes());
						NodeInfo info = readIpPort(byteBuf);
						log.info(defaultResponse.t() + "获得peer:" + info.ip() + "-" + info.port());
						if (isFindingPeer) {
							rs.put(info.ip(), info.port());
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

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
