package org.my.pro.dhtcrawler.netty;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.AbstractDhtNode;
import org.my.pro.dhtcrawler.Future;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.WorkHandler;
import org.my.pro.dhtcrawler.futrure.KrpcMessageFuture;
import org.my.pro.dhtcrawler.handler.AnnouncePeerHandler;
import org.my.pro.dhtcrawler.handler.DefaultResponseHandler;
import org.my.pro.dhtcrawler.handler.FindNodeHandler;
import org.my.pro.dhtcrawler.handler.GetPeersHandler;
import org.my.pro.dhtcrawler.handler.PingHandler;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.handler.ResponseMessageHandler;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.routingTable.DHTRoutingTable;
import org.my.pro.dhtcrawler.task.CleanTimeOutFuture;
import org.my.pro.dhtcrawler.task.DHTCrawler;
import org.my.pro.dhtcrawler.task.TryFindPeerAndDownload;
import org.my.pro.dhtcrawler.task.WriteLineToFile;
import org.my.pro.dhtcrawler.util.DHTUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;
import org.my.pro.dhtcrawler.util.NoChangeID;

import be.adaxisoft.bencode.InvalidBEncodingException;
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

	private static Log log = LogFactory.getLog(DefaultDhtNode.class);

	// netty相关
	private EventLoopGroup group;
	private Bootstrap bootstrap;
	private Channel channel;

	// netty运行线程
	private Thread nodeThread;

	// DHT路由表
	private RoutingTable routingTable;

	// response handler
	private ResponseMessageHandler responseMessageHandler;

	// 清理过期同步请求
	private CleanTimeOutFuture cleanTimeOutFuture;

	// 同步请求缓存
	private ConcurrentHashMap<String, Future> futures;

	// DHT爬虫
	private DHTCrawler dhtCrawler;

	// BT下载

	private TryFindPeerAndDownload downloadTorrent;

	// 最近获得哈希时间
	private long hashTime = System.currentTimeMillis();

	// 本地唯一标识ID
	private int noChangeId;

	public DefaultDhtNode(int port) {
		this(port, true);
	}

	public DefaultDhtNode(int port, boolean runBep09) {
		noChangeId = NoChangeID.nextNum();
		//
		this.id = DHTUtils.generateNodeId();
		this.port = port;
		downloadTorrent = TryFindPeerAndDownload.getInstance();
		downloadTorrent.registerDHTNode(this);
		this.cleanTimeOutFuture = new CleanTimeOutFuture(this);
		this.dhtCrawler = new DHTCrawler(this);
		//
	}

	@Override
	public int noChangeId() {
		return noChangeId;
	}

	private static long HASH_TIME_OUT = 1000 * 60 * 5;
	private static long LIVE_TIME_OUT = 1000 * 60 * 60;

	private volatile long startTime = System.currentTimeMillis();

	public void writeHashToFile(byte[] hash, String code) {
		String mes = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss").format(new Date()) + ":"
				+ DHTUtils.byteArrayToHexString(hash) + ":" + code;
		WriteLineToFile.getInstance().writeLineToHashFile(mes);
	}

	@Override
	public boolean hasGetHash() {
		//如果获取哈希未超时且存货时间未超时，返回true
		return System.currentTimeMillis() - hashTime < HASH_TIME_OUT
				&& (System.currentTimeMillis() - startTime) < LIVE_TIME_OUT;
	}


	@Override
	public void resetId(byte[] id) {
		this.id = id;
		//
		routingTable.resetNodeId(id);
		//
		hashTime = System.currentTimeMillis();
		//
		startTime = System.currentTimeMillis();

	}

	@Override
	public List<Node> findNearest(byte[] hash) {
		return routingTable.getClosestNodes(hash, 8);
	}

	@Override
	public void add(Node info) {
		if (null == info) {
			return;
		}
		if (info.nodeId() == null) {
			return;
		}
		routingTable.add(info);
	}

	@Override
	public void sendMessage(KrpcMessage krpcMessage) {

		channel.eventLoop().execute(() -> channel.writeAndFlush(krpcMessage));
	}

	@Override
	public Future call(KrpcMessage krpcMessage) {
		//
		channel.eventLoop().execute(() -> channel.writeAndFlush(krpcMessage));

		Future future = new KrpcMessageFuture();
		futures.put(krpcMessage.t(), future);

		return future;

	}

	@Override
	public List<Future> invokeAll(List<KrpcMessage> krpcMessages) {
		List<Future> futures = new ArrayList<Future>();

		//
		for (KrpcMessage krpcMessage : krpcMessages) {
			futures.add(call(krpcMessage));
		}

		// 阻塞
		for (Future future : futures) {
			future.getValue();
		}

		return futures;
	}

	@Override
	public void tryDownLoad(byte[] hash) {
		this.downloadTorrent.subTask_announce_peer(hash, this);
	}

	@Override
	public boolean back(KrpcMessage krpcMessage) {
		// 处理是否需要callBack
		Future future = futures.get(krpcMessage.t());
		if (null != future) {
			future.back(krpcMessage);
		}
		return future != null;
	}

	@Override
	public Channel channel() {
		return channel;
	}

	@Override
	public void clearTimeOutFutrue() {

		long currentTime = System.currentTimeMillis();

		Iterator<Entry<String, Future>> it = futures.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Future> entry = it.next();
			KrpcMessageFuture future = (KrpcMessageFuture) entry.getValue();
			if (future.getCreateTime() + KrpcMessageFuture.LIVE_TIME < currentTime) {
				it.remove();
			}
		}

	}

	private CountDownLatch initNettyCountDownLatch = new CountDownLatch(1);

	class getPeerHandler implements WorkHandler {

		private LocalDHTNode localDHTNode;

		public getPeerHandler(LocalDHTNode localDHTNode) {

			this.localDHTNode = localDHTNode;
		}

		@Override
		public void handler(byte[] hash, KrpcMessage message) {
			hashTime = System.currentTimeMillis();
			writeHashToFile(hash, KeyWord.GET_PEERS);
			// 提交下载任务
			downloadTorrent.subTask_findpeers(hash, localDHTNode);
		}

	}

	class announcePeerHandler implements WorkHandler {

		private LocalDHTNode dhtNode;

		public announcePeerHandler(LocalDHTNode dhtNode) {
			super();
			this.dhtNode = dhtNode;
		}

		@Override
		public void handler(byte[] hash, KrpcMessage message) {
			hashTime = System.currentTimeMillis();
			writeHashToFile(hash, KeyWord.ANNOUNCE_PEER);
			//
			DefaultRequest defaultRequest = (DefaultRequest) message;
			try {
				// 直接提交下载任务
				downloadTorrent.subTask(message.addr().getAddress().getHostAddress(),
						defaultRequest.a().getMap().get("port").getInt(), hash);
				// 重新查找perrs再下载
				downloadTorrent.subTask_announce_peer(hash, dhtNode);
			} catch (InvalidBEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public void start() {

		routingTable = new DHTRoutingTable(id());
		futures = new ConcurrentHashMap<>();
		// DefaultDhtNode localNode = new DefaultDhtNode(id(), port(),routingTable);

		// 以下ID 用于回复
		HashMap<String, RequestMessageHandler> map = new HashMap<>();
		map.put(KeyWord.PING, new PingHandler(this));
		map.put(KeyWord.FIND_NODE, new FindNodeHandler(this));
		map.put(KeyWord.GET_PEERS, new GetPeersHandler(this, new getPeerHandler(this)));
		map.put(KeyWord.ANNOUNCE_PEER, new AnnouncePeerHandler(this, new announcePeerHandler(this)));
		//
		responseMessageHandler = new DefaultResponseHandler(this);

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
							.option(ChannelOption.SO_RCVBUF, 1048576).option(ChannelOption.SO_SNDBUF, 1048576)
							.option(ChannelOption.SO_REUSEADDR, true)

					;
					bootstrap.handler(channelHandler);

					ChannelFuture channelFuture = bootstrap.bind(port()).sync();

					channelFuture.addListener(future -> {
						if (future.isSuccess()) {

							log.info(
									"启动节点:" + GsonUtils.GSON.toJson(id()) + "-" + channelFuture.channel().localAddress()
											+ "-" + DHTUtils.byteArrayToHexString(id()));

						} else {
							log.info("启动节点:" + GsonUtils.GSON.toJson(id()) + "-" + port() + "失败!");

						}
						initNettyCountDownLatch.countDown();
					});

					channel = channelFuture.channel();

					channel.closeFuture().await();

				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("节点启动失败..." + port());
				} finally {
					group.shutdownGracefully();
				}

			}
		});

		nodeThread.start();
		try {
			initNettyCountDownLatch.await();
		} catch (Exception e) {
			// TODO: handle exception
		}

		//
		if (null != downloadTorrent) {
			downloadTorrent.start();
		}
		cleanTimeOutFuture.start();
		dhtCrawler.start();
	}

	@Override
	public int targetSize(byte[] target) {
		return routingTable.targetSize(target);
	}

	@Override
	public void stop() {
		if (null != channel) {
			channel.close();
		}
		if (null != group) {
			group.shutdownGracefully();
		}
		if (downloadTorrent != null) {
			downloadTorrent.stop();
		}
		if (cleanTimeOutFuture != null) {
			cleanTimeOutFuture.stop();
		}
		if (null != dhtCrawler) {
			dhtCrawler.stop();
		}

		if (null != futures) {
			futures.clear();
		}

	}

	@Override
	public boolean isRun() {
		return channel != null && channel.isActive();
	}

}
