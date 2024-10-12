package org.my.pro.dhtcrawler;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.my.pro.dhtcrawler.handler.AnnouncePeerHandler;
import org.my.pro.dhtcrawler.handler.DefaultResponseHandler;
import org.my.pro.dhtcrawler.handler.FindNodeHandler;
import org.my.pro.dhtcrawler.handler.GetPeersHandler;
import org.my.pro.dhtcrawler.handler.PingHandler;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.handler.ResponseMessageHandler;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.netty.DHTRequestChannelInboundHandler;
import org.my.pro.dhtcrawler.netty.DHTResponseChannelInboundHandler;
import org.my.pro.dhtcrawler.netty.DataServerCodec;
import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.my.pro.dhtcrawler.routingTable.SimpleRoutingTable;
import org.my.pro.dhtcrawler.util.ByteArrayHexUtils;
import org.my.pro.dhtcrawler.util.NodeIdRandom;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class TryFindPeer {

	private boolean isFound;
	private String hash;

	private DefaultDhtNode localNode;
	
	/**
	 * 搜索目标节点
	 */
	private ConcurrentHashMap<String, NodeInfo> map = new ConcurrentHashMap<String, NodeInfo>();

	public TryFindPeer(String hash) {
		super();
		this.hash = hash;
	}

	/**
	 * 
	 * 试图找到一个哈希的种子文件
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		String hash = "fe398bcb9f127804ba9afcbee934303496487428";

		new TryFindPeer(hash).doFound();

	}

	public void doFound() {
		localNode = new DefaultDhtNode(NodeIdRandom.generatePeerId(), 0);

		initNode(localNode);

		// 启动节点
		localNode.start();

		KrpcMessage getPeer = MessageFactory.createGet_peers("localhost", 26701, localNode.id(),
				ByteArrayHexUtils.hexStringToByteArray(hash));

		localNode.sendMessage(getPeer);

		// 寻找node直到寻找到peer

		/// 寻找到peer

		// 执行种子下载任务

	}

	public void initNode(DefaultDhtNode localNode) {
		RoutingTable routingTable = new SimpleRoutingTable(localNode);
		// 以下ID 用于回复
		HashMap<String, RequestMessageHandler> map = new HashMap<>();
		map.put(KeyWord.PING, new PingHandler(routingTable, localNode));
		map.put(KeyWord.FIND_NODE, new FindNodeHandler(routingTable, localNode));
		map.put(KeyWord.GET_PEERS, new GetPeersHandler(routingTable, localNode, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				// magnetSaver.saveMagnet("ANNOUNCE_PEER:" + hash);
			}
		}));
		map.put(KeyWord.ANNOUNCE_PEER, new AnnouncePeerHandler(routingTable, localNode, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				// magnetSaver.saveMagnet("ANNOUNCE_PEER:" + hash);
			}
		}));
		//
		ResponseMessageHandler responseMessageHandler = new DefaultResponseHandler(routingTable, localNode);

		ChannelHandler channelHandler = new ChannelInitializer<NioDatagramChannel>() {
			@Override
			protected void initChannel(NioDatagramChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				p.addLast(new DataServerCodec());
				p.addLast(new DHTRequestChannelInboundHandler(map, localNode.id()));
				p.addLast(new DHTResponseChannelInboundHandler(responseMessageHandler));

			}
		};

		localNode.setChannelHandler(channelHandler);
	}
}
