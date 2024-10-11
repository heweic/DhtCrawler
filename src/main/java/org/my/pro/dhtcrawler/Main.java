package org.my.pro.dhtcrawler;

import java.util.HashMap;

import org.my.pro.dhtcrawler.handler.AnnouncePeerHandler;
import org.my.pro.dhtcrawler.handler.DefaultResponseHandler;
import org.my.pro.dhtcrawler.handler.FindNodeHandler;
import org.my.pro.dhtcrawler.handler.GetPeersHandler;
import org.my.pro.dhtcrawler.handler.PingHandler;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.handler.ResponseMessageHandler;
import org.my.pro.dhtcrawler.netty.DHTRequestChannelInboundHandler;
import org.my.pro.dhtcrawler.netty.DHTResponseChannelInboundHandler;
import org.my.pro.dhtcrawler.netty.DataServerCodec;
import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.my.pro.dhtcrawler.routingTable.SimpleRoutingTable;
import org.my.pro.dhtcrawler.saver.MagnetSaver;
import org.my.pro.dhtcrawler.saver.TxtMagnetSaver;
import org.my.pro.dhtcrawler.util.NodeIdRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class Main {

	private static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {

		System.setProperty("file.encoding", KeyWord.DHT_CHARSET_STR);

		int tmp = 60000;

		MagnetSaver magnetSaver = new TxtMagnetSaver();

		for (int i = 0; i < 1; i++) {

			try {
				Thread.sleep(1000 * 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int port = tmp + i;
			byte[] id = NodeIdRandom.generatePeerId();

			new Thread(new Runnable() {
				@Override
				public void run() {
					Main nodeTest = new Main();
					nodeTest.work(port, id, magnetSaver);
				}
			}).start();

		}

	}

	public void work(int port, byte[] localID, MagnetSaver magnetSaver) {

		DefaultDhtNode localNode = new DefaultDhtNode(localID, port);
		RoutingTable routingTable = new SimpleRoutingTable(localNode);

		// 以下ID 用于回复
		HashMap<String, RequestMessageHandler> map = new HashMap<>();
		map.put(KeyWord.PING, new PingHandler(routingTable, localNode));
		map.put(KeyWord.FIND_NODE, new FindNodeHandler(routingTable, localNode));
		map.put(KeyWord.GET_PEERS, new GetPeersHandler(routingTable, localNode, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				magnetSaver.saveMagnet("GET_PEERS:" + hash);
			}
		}));
		map.put(KeyWord.ANNOUNCE_PEER, new AnnouncePeerHandler(routingTable, localNode, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				magnetSaver.saveMagnet("ANNOUNCE_PEER:" + hash);
			}
		}));
		//
		ResponseMessageHandler responseMessageHandler = new DefaultResponseHandler(routingTable, localNode);

		ChannelHandler channelHandler = new ChannelInitializer<NioDatagramChannel>() {
			@Override
			protected void initChannel(NioDatagramChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				p.addLast(new DataServerCodec());
				p.addLast(new DHTRequestChannelInboundHandler(map, localID));
				p.addLast(new DHTResponseChannelInboundHandler(responseMessageHandler));

			}
		};

		localNode.setChannelHandler(channelHandler);

		// localNode.setServerStarted(serverStarted);

		// 启动节点
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				localNode.start();
			}
		});
		t.start();
	}
}
