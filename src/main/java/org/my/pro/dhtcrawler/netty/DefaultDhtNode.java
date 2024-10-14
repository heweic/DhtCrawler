package org.my.pro.dhtcrawler.netty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.AbstractDhtNode;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.WorkHandler;
import org.my.pro.dhtcrawler.handler.AnnouncePeerHandler;
import org.my.pro.dhtcrawler.handler.DefaultResponseHandler;
import org.my.pro.dhtcrawler.handler.FindNodeHandler;
import org.my.pro.dhtcrawler.handler.GetPeersHandler;
import org.my.pro.dhtcrawler.handler.PingHandler;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.handler.ResponseMessageHandler;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.routingTable.DefaultNodeInfo;
import org.my.pro.dhtcrawler.routingTable.SimpleRoutingTable;
import org.my.pro.dhtcrawler.saver.MagnetSaver;
import org.my.pro.dhtcrawler.saver.TxtMagnetSaver;
import org.my.pro.dhtcrawler.task.DownLoadBtTorrent;
import org.my.pro.dhtcrawler.util.ByteArrayHexUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;
import org.my.pro.dhtcrawler.util.NodeIdRandom;
import org.my.pro.dhtcrawler.util.RequestIdGenerator;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class DefaultDhtNode extends AbstractDhtNode {

	/**
	 * 节点表默认最小数量
	 */
	public static final int NODE_INIT_SIZE = 1000;

	/**
	 * 节点表默认最大值
	 */
	public static final int MAX_NODE_SIZE = NODE_INIT_SIZE * 2;

	public static final long timeOut = 60 * 15 * 1000;

	private EventLoopGroup group;
	private Bootstrap bootstrap;
	private ChannelFuture channelFuture;

	private static Log log = LogFactory.getLog(DefaultDhtNode.class);

	// private ChannelHandler channelHandler;

	private Thread nodeThread;

	private RoutingTable routingTable;

	protected ScheduledExecutorService worker = Executors.newScheduledThreadPool(1);
	private Executor executor = Executors.newFixedThreadPool(3);

	private ResponseMessageHandler responseMessageHandler;

	
	private DownLoadBtTorrent btTorrent;
//	private MagnetSaver magnetSaver;

	public DefaultDhtNode(byte[] id, int port  ) {
		super(id, port);
		//
		
		//
		this.btTorrent = new DownLoadBtTorrent(this);

		worker.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				// log.info("当前节点表数量:" + nodes.size());
				// 当缓存表的数量过大随机抓取清理
				if (routingTable.allNodes().size() > MAX_NODE_SIZE) {
					List<NodeInfo> list = routingTable.random(NODE_INIT_SIZE);
					for (NodeInfo i : list) {
						routingTable.remove(i.nodeId().id());
					}
				}
			}

		}, 2, 2, TimeUnit.SECONDS);

		// 如果列表小于 K
		worker.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

				// 节点小于1000执行填充任务
				if (routingTable.allNodes().size() < NODE_INIT_SIZE) {
					KrpcMessage krpcMessage1 = MessageFactory.createFindNode("router.utorrent.com", 6881, id(),
							NodeIdRandom.generatePeerId());
					KrpcMessage krpcMessage2 = MessageFactory.createFindNode("dht.transmissionbt.com", 6881, id(),
							NodeIdRandom.generatePeerId());
					KrpcMessage krpcMessage3 = MessageFactory.createFindNode("router.bittorrent.com", 6881, id(),
							NodeIdRandom.generatePeerId());
					KrpcMessage krpcMessage4 = MessageFactory.createGet_peers("router.utorrent.com", 6881, id(),
							NodeIdRandom.generatePeerId());
					KrpcMessage krpcMessage5 = MessageFactory.createGet_peers("dht.transmissionbt.com", 6881, id(),
							NodeIdRandom.generatePeerId());
					KrpcMessage krpcMessage6 = MessageFactory.createGet_peers("router.bittorrent.com", 6881, id,
							NodeIdRandom.generatePeerId());

					sendMessage(krpcMessage1);
					sendMessage(krpcMessage2);
					sendMessage(krpcMessage3);
					sendMessage(krpcMessage4);
					sendMessage(krpcMessage5);
					sendMessage(krpcMessage6);

				}
				//
				// 随机抓节点发送findNode扩充自己节点表
				List<NodeInfo> list = routingTable.random(3);
				for (int i = 0; i < list.size(); i++) {
					NodeInfo info = list.get(i);
					KrpcMessage krpcMessage;
					if (i % 2 == 0) {
						krpcMessage = MessageFactory.createFindNode(info.ip(), info.port(), id(), id());
					} else {
						krpcMessage = MessageFactory.createPing(info, id());
					}

					sendMessage(krpcMessage);
				}
			}
		}, 1, 1, TimeUnit.SECONDS);

	}

	private Object lock = new Object();

	@Override
	public List<NodeInfo> findNearest() {
		return routingTable.random(8);
	}

	@Override
	public void add(NodeInfo info) {
		if (null == info) {
			return;
		}
		if (info.nodeId() == null) {
			return;
		}
		// log.info("添加节点:" + "[" + info.ip() + ":" + info.port() + "]" + "--------" +
		// "{" + info.nodeId().intId() + "}");

		if (routingTable.allNodes().size() < NODE_INIT_SIZE) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					KrpcMessage getPeer = MessageFactory.createFindNode(info.ip(), info.port(), id(),
							NodeIdRandom.generatePeerId());
					sendMessage(getPeer);
				}
			});
			routingTable.add(info);
		} else if (RandomUtils.nextInt(0, 10) == 5) { // 新增加的节点概率添加且发送getPeer请求
			executor.execute(() -> {
				sendMessage(MessageFactory.createFindNode(info.ip(), info.port(), id(), NodeIdRandom.generatePeerId()));
			});
			routingTable.add(info);
		}
	}

	@Override
	public List<NodeInfo> find_peer(byte[] hash) {

		synchronized (lock) {
			// 遍历node 发送find_peer
			// 为当前任务生成一个ID
			String taskId = RequestIdGenerator.getRequestId();
			// 向节点列表发送查找请求

			responseMessageHandler.dofindPeer(taskId, hash);
			Iterator<Entry<String, NodeInfo>> it = routingTable.allNodes().entrySet().iterator();
			while (it.hasNext()) {
				NodeInfo info = it.next().getValue();
				KrpcMessage getPeer = MessageFactory.createGet_peers(info.ip(), taskId, info.port(), id(), hash);
				sendMessage(getPeer);
			}
			// 等待结果
			responseMessageHandler.syn();
			// 返回结果
			if (responseMessageHandler.getPeers().size() > 0) {
				
				List<NodeInfo> infos = new ArrayList<NodeInfo>();
				Iterator<Entry<String, Integer>> rs = responseMessageHandler.getPeers().entrySet().iterator();
				while (rs.hasNext()) {
					Entry<String, Integer> en = rs.next();
					NodeInfo info = new DefaultNodeInfo(null, en.getKey(), en.getValue());
					infos.add(info);
				}
				return infos;
			} else {
				return new ArrayList<NodeInfo>();
			}
		}

	}

	@Override
	public void sendMessage(KrpcMessage krpcMessage) {
		channelFuture.channel().writeAndFlush(krpcMessage);
	}

	@Override
	public Channel channel() {
		return channelFuture.channel();
	}

	private CountDownLatch startCountDownLatch = new CountDownLatch(1);

	@Override
	public void start() {

		routingTable = new SimpleRoutingTable();
		// DefaultDhtNode localNode = new DefaultDhtNode(id(), port(),routingTable);

		// 以下ID 用于回复
		HashMap<String, RequestMessageHandler> map = new HashMap<>();
		map.put(KeyWord.PING, new PingHandler(routingTable, this));
		map.put(KeyWord.FIND_NODE, new FindNodeHandler(routingTable, this));
		map.put(KeyWord.GET_PEERS, new GetPeersHandler(routingTable, this, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				//magnetSaver.saveMagnet("GET_PEERS:" + hash);
				btTorrent.subTask(hash);
			}
		}));
		map.put(KeyWord.ANNOUNCE_PEER, new AnnouncePeerHandler(routingTable, this, new WorkHandler() {

			@Override
			public void handler(String hash, KrpcMessage message) {
				//magnetSaver.saveMagnet("ANNOUNCE_PEER:" + hash);
			}
		}));
		//
		responseMessageHandler = new DefaultResponseHandler(routingTable, this);

		ChannelHandler channelHandler = new ChannelInitializer<NioDatagramChannel>() {
			@Override
			protected void initChannel(NioDatagramChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				p.addLast(new DataServerCodec());
				p.addLast(new DHTRequestChannelInboundHandler(map));
				p.addLast(new DHTResponseChannelInboundHandler(responseMessageHandler));

			}
		};

		nodeThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					group = new NioEventLoopGroup();

					bootstrap = new Bootstrap();
					bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, 1048576)
					.option(ChannelOption.SO_SNDBUF, 1048576)
					.option(ChannelOption.SO_REUSEADDR, true)
					
					;
					bootstrap.handler(channelHandler);

					channelFuture = bootstrap.bind(port()).sync();

					channelFuture.addListener(future -> {
						if (future.isSuccess()) {
							log.info(
									"启动节点:" + GsonUtils.GSON.toJson(id()) + "-" + channelFuture.channel().localAddress()
											+ "-" + ByteArrayHexUtils.byteArrayToHexString(id()));
						} else {
							log.info("启动节点:" + GsonUtils.GSON.toJson(id()) + "-" + port() + "失败!");
						}
						startCountDownLatch.countDown();
					});

					channelFuture.channel().closeFuture().await();

				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("节点启动失败..." + port());
				} finally {
					group.shutdownGracefully();
				}

			}
		});

		nodeThread.start();

		//
		try {
			startCountDownLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void stop() {
		if (null != channelFuture.channel()) {
			channelFuture.channel().close();
		}
	}

	@Override
	public boolean isRun() {
		return channelFuture != null && channelFuture.channel().isActive();
	}

}
