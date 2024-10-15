package org.my.pro.dhtcrawler.netty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.AbstractDhtNode;
import org.my.pro.dhtcrawler.Future;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
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
import org.my.pro.dhtcrawler.routingTable.SimpleRoutingTable;
import org.my.pro.dhtcrawler.task.CleanTimeOutFuture;
import org.my.pro.dhtcrawler.task.DHTCrawler;
import org.my.pro.dhtcrawler.task.DownLoadTorrent;
import org.my.pro.dhtcrawler.task.TryFindPeerAndDownload;
import org.my.pro.dhtcrawler.util.DHTUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;

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

	private EventLoopGroup group;
	private Bootstrap bootstrap;
	private ChannelFuture channelFuture;
	private Thread nodeThread;

	private static Log log = LogFactory.getLog(DefaultDhtNode.class);

	private RoutingTable routingTable;

	private ResponseMessageHandler responseMessageHandler;

	private DownLoadTorrent downLoadTorrent;
	private TryFindPeerAndDownload tryCrawlingTorrent;
	private CleanTimeOutFuture cleanTimeOutFuture;
	private DHTCrawler dhtCrawler;

	private ConcurrentHashMap<String, Future> futures = new ConcurrentHashMap<>();

	public DefaultDhtNode(byte[] id, int port) {
		super(id, port);
		//
		if(id.length != 20) {
			throw new IllegalArgumentException();
		}
		//
		this.downLoadTorrent = new DownLoadTorrent(this);
		this.tryCrawlingTorrent = new TryFindPeerAndDownload(this, downLoadTorrent);
		this.cleanTimeOutFuture = new CleanTimeOutFuture(this);
		this.dhtCrawler = new DHTCrawler(this);

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
		channelFuture.channel().writeAndFlush(krpcMessage);
	}

	@Override
	public Future call(KrpcMessage krpcMessage) {
		channelFuture.channel().writeAndFlush(krpcMessage);
		Future future = new KrpcMessageFuture();

		futures.put(krpcMessage.t(), future);

		return future;

	}

	@Override
	public List<Node> randomNodes(int num) {
		return routingTable.randomNodes(num);
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
		return channelFuture.channel();
	}

	@Override
	public void clearTimeOutFutrue() {
		Iterator<Entry<String, Future>> it = futures.entrySet().iterator();
		long nowTime = System.currentTimeMillis();
		while (it.hasNext()) {
			Entry<String, Future> en = it.next();
			KrpcMessageFuture future = (KrpcMessageFuture) en.getValue();
			if (future.getCreateTime() + KrpcMessageFuture.LIVE_TIME >= nowTime) {
				it.remove();
			}
		}
	}

	private CountDownLatch initNettyCountDownLatch = new CountDownLatch(1);

	@Override
	public void start() {

		routingTable = new SimpleRoutingTable();
		// DefaultDhtNode localNode = new DefaultDhtNode(id(), port(),routingTable);

		// 以下ID 用于回复
		HashMap<String, RequestMessageHandler> map = new HashMap<>();
		map.put(KeyWord.PING, new PingHandler(this));
		map.put(KeyWord.FIND_NODE, new FindNodeHandler(this));
		map.put(KeyWord.GET_PEERS, new GetPeersHandler(this, new WorkHandler() {

			@Override
			public void handler(byte[] hash, KrpcMessage message) {
				tryCrawlingTorrent.subTask(hash);
			}
		}));
		map.put(KeyWord.ANNOUNCE_PEER, new AnnouncePeerHandler(this, downLoadTorrent));
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
							.option(ChannelOption.SO_RCVBUF, 1048576).option(ChannelOption.SO_SNDBUF, 1048576)
							.option(ChannelOption.SO_REUSEADDR, true)

					;
					bootstrap.handler(channelHandler);

					channelFuture = bootstrap.bind(port()).sync();

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
		try {
			initNettyCountDownLatch.await();
		} catch (Exception e) {
			// TODO: handle exception
		}

		//
		downLoadTorrent.start();
		tryCrawlingTorrent.start();
		cleanTimeOutFuture.start();
		dhtCrawler.start();
	}

	@Override
	public void stop() {
		if (null != channelFuture.channel()) {
			channelFuture.channel().close();
		}
		if (null != group) {
			group.close();
		}
		tryCrawlingTorrent.stop();
		downLoadTorrent.stop();
		cleanTimeOutFuture.stop();
		dhtCrawler.stop();
	}

	@Override
	public boolean isRun() {
		return channelFuture != null && channelFuture.channel().isActive();
	}

}
