package org.my.pro.dhtcrawler;

import java.util.HashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.my.pro.dhtcrawler.domain.SimpleRoutingTable;
import org.my.pro.dhtcrawler.handler.AnnouncePeerHandler;
import org.my.pro.dhtcrawler.handler.DefaultResponseHandler;
import org.my.pro.dhtcrawler.handler.FindNodeHandler;
import org.my.pro.dhtcrawler.handler.GetPeersHandler;
import org.my.pro.dhtcrawler.handler.PingHandler;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.handler.ResponseMessageHandler;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.my.pro.dhtcrawler.netty.DhtSimpleChannelInboundHandler;
import org.my.pro.dhtcrawler.saver.MagnetSaver;
import org.my.pro.dhtcrawler.saver.TxtMagnetSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class Main {

	private static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {

		System.setProperty("file.encoding", KeyWord.DHT_CHARSET_STR);

		int tmp = 60000;

		MagnetSaver magnetSaver = new TxtMagnetSaver();

		for (int i = 1; i < 5; i++) {

			try {
				Thread.sleep(1000 * 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int port = tmp + i;
			String sha1Id = DigestUtils.sha1Hex(RandomUtils.nextBytes(20));
			String id = RequestMessageHandler.infoHash(sha1Id);

			new Thread(new Runnable() {
				@Override
				public void run() {
					Main nodeTest = new Main();
					nodeTest.work(port, id, magnetSaver);
				}
			}).start();

		}

	}

	public void work(int port, String nodeId, MagnetSaver magnetSaver) {

		DefaultDhtNode localNode = new DefaultDhtNode(nodeId, port);
		RoutingTable routingTable = new SimpleRoutingTable(localNode, localNode.id());

		// 以下ID 用于回复
		HashMap<String, RequestMessageHandler> map = new HashMap<>();
		map.put(KeyWord.PING, new PingHandler(routingTable, localNode));
		map.put(KeyWord.FIND_NODE, new FindNodeHandler(routingTable, localNode));
		map.put(KeyWord.GET_PEERS, new GetPeersHandler(routingTable, localNode, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				magnetSaver.saveMagnet("maybe:" + hash);
			}
		}));
		map.put(KeyWord.ANNOUNCE_PEER, new AnnouncePeerHandler(routingTable, localNode, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				magnetSaver.saveMagnet(hash);
			}
		}));
		//
		ResponseMessageHandler responseMessageHandler = new DefaultResponseHandler(routingTable, localNode);
		DhtSimpleChannelInboundHandler channelHandler = new DhtSimpleChannelInboundHandler(map, responseMessageHandler);

		ServerStarted serverStarted = new ServerStarted() {

			@Override
			public void work(Channel channel) {
				String s = nodeId;
				KrpcMessage krpcMessage1 = MessageFactory.createFindNode("router.utorrent.com", 6881, s);
				KrpcMessage krpcMessage2 = MessageFactory.createFindNode("dht.transmissionbt.com", 6881, s);
				KrpcMessage krpcMessage3 = MessageFactory.createFindNode("router.bittorrent.com", 6881, s);

				channel.writeAndFlush(krpcMessage1);
				channel.writeAndFlush(krpcMessage2);
				channel.writeAndFlush(krpcMessage3);

				logger.info(port + "启动完成，发送初始find_node 结束.");
			}
		};

		localNode.setServerStarted(serverStarted);

		// 启动节点
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				localNode.start(channelHandler);
			}
		});
		t.start();
	}
}
