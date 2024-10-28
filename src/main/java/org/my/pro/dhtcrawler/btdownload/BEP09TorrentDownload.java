package org.my.pro.dhtcrawler.btdownload;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.task.SaveTorrent;
import org.my.pro.dhtcrawler.util.BDeCoderProxy;
import org.my.pro.dhtcrawler.util.BenCodeUtils;
import org.my.pro.dhtcrawler.util.DHTUtils;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * BEP09 torrent下载,单例
 * <p>
 * 重用NioEventLoopGroup实现
 */
public class BEP09TorrentDownload {

	//

	private static Log log = LogFactory.getLog(BEP09TorrentDownload.class);

	private static String PROTO_TYPE = "BitTorrent protocol";
	private static byte[] PROTO_TYPE_BS = PROTO_TYPE.getBytes();
	private static byte[] RESERVE = new byte[] { 0, 0, 0, 0, 0, 16, 0, 1 };
	//
	private volatile static BEP09TorrentDownload instance;
	//
	private static NioEventLoopGroup group;
	private Bootstrap bootstrap;
	//
	private static int SO_RCVBUF = 1024 * 500;
	private static int CONNECT_TIMEOUT_MILLIS = 2000;

	// 所有TCP通道
	private static ConcurrentHashMap<SocketAddress, Channel> allChannels;

	// 待处理任务
	private static ConcurrentHashMap<SocketAddress, TaskInfo> tasks;

	// 握手时间
	private static int HANDSHAKE_TIME = 1000 * 3;
	private static int HANDSHAKE_TIME_OUT = HANDSHAKE_TIME * 2;
	// 下载超时时间
	private static int DOWNLOAD_TIME_OUT = 1000 * 20;
	// 定时任务
	private ScheduledExecutorService scheduledExecutor;

	private BEP09TorrentDownload() {

		// @TODO 待调整
		group = new NioEventLoopGroup();

		bootstrap = new Bootstrap();

		bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
				.option(ChannelOption.SO_RCVBUF, SO_RCVBUF).handler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast("decoder", new HandShakeDecoder());
						ch.pipeline().addLast(new TorrentPieceHandler());
						ch.pipeline().addLast(new TorrentFullHandler());

					}
				});
		allChannels = new ConcurrentHashMap<SocketAddress, Channel>();
		tasks = new ConcurrentHashMap<SocketAddress, TaskInfo>();
		//
		scheduledExecutor = Executors.newScheduledThreadPool(2);
		
		scheduledExecutor.scheduleAtFixedRate(new ClearTimeOutTask(), 100, 100, TimeUnit.MILLISECONDS);
		scheduledExecutor.scheduleAtFixedRate(new readDataAndStart(), 100, 100, TimeUnit.MILLISECONDS);
	}

	public static BEP09TorrentDownload getInstance() {
		if (null == instance) {
			synchronized (BEP09TorrentDownload.class) {
				if (null == instance) {
					instance = new BEP09TorrentDownload();
				}
			}
		}
		return instance;
	}

	class readDataAndStart implements Runnable {

		@Override
		public void run() {

			// 判断是否可以执行下载请求 , 如果获得到20类型且不包含4,即可以开始下载
			Iterator<Entry<SocketAddress, TaskInfo>> it = tasks.entrySet().iterator();
			while (it.hasNext()) {
				Entry<SocketAddress, TaskInfo> entry = it.next();
				TaskInfo taskInfo = entry.getValue();
				//
				try {
					if (!taskInfo.isReadMetadata()
							&& System.currentTimeMillis() - taskInfo.getCreateTime() >= HANDSHAKE_TIME) {
						boolean canDownload = false;
						for (Body body : taskInfo.getBodies()) {
							//
							if (body.getType() == 20) {

								BEncodedValue bv = BDeCoderProxy.bdecode(body.getDictionary());
								taskInfo.setUt_metadata(bv.getMap().get("m").getMap().get("ut_metadata").getInt());
								taskInfo.setMetadata_size(bv.getMap().get("metadata_size").getInt());
								taskInfo.setBuf(Unpooled.buffer(taskInfo.getMetadata_size()));
								canDownload = true;

							}
							if (body.getType() == 4) {
								canDownload = false;
								break;
							}
						}
						if (canDownload) {
							Channel channel = allChannels.get(entry.getKey());
							if (null != channel) {
								channel.eventLoop().execute(new Runnable() {

									@Override
									public void run() {
										try {
											// 扩展握手
											sendHandshakeMsg(channel, taskInfo.getMetadata_size());
											// 请求第一块数据
											sendMetadataRequest(channel, 0, entry.getValue().getUt_metadata());
										} catch (Exception e) {
										}
									}
								});
							}
						}

						//
						taskInfo.setReadMetadata(true);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

		}

	}

	/**
	 * 遍历所有任务 该关闭的关闭 该触发下载的触发下载
	 */
	class ClearTimeOutTask implements Runnable {

		@Override
		public void run() {

			Iterator<Entry<SocketAddress, TaskInfo>> it = tasks.entrySet().iterator();
			while (it.hasNext()) {
				Entry<SocketAddress, TaskInfo> entry = it.next();
				TaskInfo taskInfo = entry.getValue();
				try {

					// 如果任务已经完成
					if (taskInfo.isDown()) {
						closeChannel(entry.getKey());
						break;
					}
					long tmpTime = System.currentTimeMillis() - taskInfo.getCreateTime();
					// 下载限时过后无论超时与否，关闭连接
					if (tmpTime >= DOWNLOAD_TIME_OUT) {
						closeChannel(entry.getKey());
					}

					// 如果在握手超时时间过后，还未开始下载，关闭
					if (!taskInfo.isReadMetadata() && tmpTime >= HANDSHAKE_TIME_OUT) {
						closeChannel(entry.getKey());
						break;
					}

				} catch (Exception e) {
					// e.printStackTrace();
				}
			}

		}

	}

	/**
	 * 尝试下载torrent
	 * 
	 * @param ip
	 * @param port
	 * @param hash
	 * @throws Exception 需要对异常处理
	 */
	public void tryDownload(String ip, int port, byte[] hash) throws Exception {
		// 如果当前连接已存在
		SocketAddress socketAddress = new InetSocketAddress(ip, port);
		if (allChannels.containsKey(socketAddress)) {
			return;
		}
		// 如果当前channel数大于

		// 创建连接并发送握手包
		ChannelFuture channelFuture = bootstrap.connect(socketAddress);
		channelFuture.addListener(future -> {
			if (future.isSuccess()) {

				Channel channel = channelFuture.sync().channel();
				// log.info("连接到" + channel.remoteAddress() + "下载:" +
				// DHTUtils.byteArrayToHexString(hash) + "---连接成功!");
				// 发送握手包
				ByteBuf bf = Unpooled.buffer(68);
				bf.writeByte(19);
				bf.writeBytes(PROTO_TYPE_BS);
				bf.writeBytes(RESERVE);
				bf.writeBytes(hash); // 种子哈希值
				bf.writeBytes(DHTUtils.generatePeerId());// 随机一个本地peerID
				channel.writeAndFlush(bf);
				// 缓存channel及任务
				allChannels.put(socketAddress, channel);
				tasks.put(socketAddress, new TaskInfo(hash));
			}
		});

	}

	/**
	 * 握手协议解码器
	 * <p>
	 * 处理握手响应协议 头固定长度68
	 * <P>
	 * 处理torrent传输时协议 头固定长度6
	 */
	class HandShakeDecoder extends ByteToMessageDecoder {

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
			if (in.readableBytes() < 5) {
				return;
			}
			TaskInfo taskInfo = tasks.get(ctx.channel().remoteAddress());
			if (null == taskInfo) {
				return;
			}
			// 是否已握手
			if (!taskInfo.isReadHandShake()) {
				if (in.readableBytes() < 68) {
					return;
				}
				in.skipBytes(1);
				in.skipBytes(19);
				in.skipBytes(8);
				in.skipBytes(20); // hash
				in.skipBytes(20); // peer ID
				//
				taskInfo.setReadHandShake(true);
			}
			//
			if (in.readableBytes() < 4) {
				return;
			}
			// 读取TLV数据
			in.markReaderIndex();
			int length = in.readInt(); // type + body

			if (in.readableBytes() < length) {
				in.resetReaderIndex();
				return;
			}
			//
			byte type = in.readByte();// messageType

			// read body
			switch (type) {
			case 20:
				in.readByte(); // messageID
				byte[] dictionary = new byte[length - 2];
				in.readBytes(dictionary);
				try {
					if (!taskInfo.isReadMetadata()) {
						//
						taskInfo.getBodies().add(new Body(type, dictionary));
					} else {
						BEncodedValue bv = BDeCoderProxy.bdecode(dictionary);
						int dataLength = BEncoder.encode(bv.getMap()).array().length;
						byte[] rowMetaData = new byte[dictionary.length - dataLength];

						System.arraycopy(dictionary, dataLength, rowMetaData, 0, rowMetaData.length);
						taskInfo.setPiece(rowMetaData);
						// 当解析到torrent数据时叫给下一个handler处理
						out.add(taskInfo);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case 1:
				taskInfo.getBodies().add(new Body(type, null));
				break;
			default:
				byte[] other = new byte[length - 1];

				in.readBytes(other);
				taskInfo.getBodies().add(new Body(type, other));
				break;
			}
			//
		}

	}

	/**
	 * 处理收到的piece拼装到
	 */
	class TorrentPieceHandler extends SimpleChannelInboundHandler<TaskInfo> {

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			//
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, TaskInfo msg) throws Exception {

			// 接收torrent中
			msg.getBuf().writeBytes(msg.getPiece());

			// 接收未完成，发送下一块
			if (msg.getBuf().readableBytes() < msg.getMetadata_size()) {
				msg.setNowPiece(msg.getNowPiece() + 1);
				sendMetadataRequest(ctx.channel(), msg.getNowPiece(), msg.getUt_metadata());
			}
			//
			if (msg.getBuf().readableBytes() >= msg.getMetadata_size()) {
				// 接收完成
				msg.setPiece(null);
				ctx.fireChannelRead(new Full(msg));
			}
		}
	}

	class TorrentFullHandler extends SimpleChannelInboundHandler<Full> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Full msg) throws Exception {

			if (null != msg) {
				//
				TaskInfo info = msg.getTaskInfo();
				log.info("连接到" + ctx.channel().remoteAddress() + "下载:" + DHTUtils.byteArrayToHexString(info.getHash())
						+ "---下载完成！-channel数:" + allChannels.size());
				SaveTorrent.getInstance().synWriteBytesToFile(DHTUtils.byteArrayToHexString(info.getHash()),
						info.getBuf().array());
				info.setDown(true);
			}

		}

	}

	private void closeChannel(SocketAddress remote) {

		Channel channel = allChannels.get(remote);

		if (channel != null) {
			channel.eventLoop().execute(new Runnable() {

				@Override
				public void run() {

					channel.closeFuture().addListener(futrue -> {
						if (futrue.isSuccess()) {
							// 移除缓存
							TaskInfo taskInfo = tasks.get(remote);
							if (null != taskInfo && taskInfo.getBuf() != null) {
								taskInfo.getBuf().release();
							}
							if (null != taskInfo && taskInfo.getBodies() != null) {
								taskInfo.getBodies().clear();
							}
							tasks.remove(channel.remoteAddress());
							allChannels.remove(channel.remoteAddress());
							// 关闭channel
						}
					});
				}
			});
		}
	}

	private void sendMetadataRequest(Channel channel, int piece, int ut_metadata) throws Exception {

		Map<String, BEncodedValue> map = new HashMap<>();
		map.put("msg_type", new BEncodedValue(0));
		map.put("piece", new BEncodedValue(piece));
		BEncodedValue metadata = new BEncodedValue(map);

		ByteBuffer sendMetadataRequest = BEncoder.encode(metadata.getMap());

		ByteBuf extendedHandshake = Unpooled.buffer(2 + sendMetadataRequest.array().length + 4);
		extendedHandshake.writeInt(2 + sendMetadataRequest.array().length);
		extendedHandshake.writeByte(20); // BT_MSG_ID = 20
		extendedHandshake.writeByte(ut_metadata); // EXT_MSG_ID = 0
		extendedHandshake.writeBytes(sendMetadataRequest);
		channel.writeAndFlush(extendedHandshake);

	}

	/**
	 * 发送扩展握手协议
	 * 
	 * 
	 * @throws Exception
	 */
	public void sendHandshakeMsg(Channel channel, int metadata_size) throws Exception {

		BEncodedValue ut_metadata = BenCodeUtils.to("ut_metadata", 1);

		BEncodedValue handshakeData = BenCodeUtils.to("m", ut_metadata);
		handshakeData.getMap().put("metadata_size", new BEncodedValue(metadata_size));

		ByteBuffer bencodedHandshake = BEncoder.encode(handshakeData.getMap());

		ByteBuf extendedHandshake = Unpooled.buffer(2 + bencodedHandshake.array().length + 4);
		extendedHandshake.writeInt(2 + bencodedHandshake.array().length);
		extendedHandshake.writeByte(20); // BT_MSG_ID = 20（扩展协议消息）
		extendedHandshake.writeByte(0); // EXT_MSG_ID = 0（扩展握手消息）
		extendedHandshake.writeBytes(bencodedHandshake);
		//
		channel.writeAndFlush(extendedHandshake);

	}

	class Full {
		private TaskInfo taskInfo;

		public Full(TaskInfo taskInfo) {

			this.taskInfo = taskInfo;
		}

		public TaskInfo getTaskInfo() {
			return taskInfo;
		}

	}

	class TaskInfo {
		// 创建时间
		private long createTime;
		// 种子哈希
		private byte[] hash;
		// 已接收torrent文件缓存
		private ByteBuf buf;
		// 收到的piece数据
		private byte[] piece;
		// 当前拥有
		private int nowPiece;
		// 是否已读取握手响应头信息
		private boolean readHandShake = false;
		// 是否已完成握手判单
		private boolean readMetadata = false;
		private int ut_metadata;
		private int metadata_size;

		private boolean isDown = false;

		private int id;

		private List<Body> bodies = new ArrayList<BEP09TorrentDownload.Body>();

		public TaskInfo(byte[] hash) {
			createTime = System.currentTimeMillis();
			this.hash = hash;
		}

		public int getIdAdd() {
			return id += 1;
		}

		public boolean isDown() {
			return isDown;
		}

		public void setDown(boolean isDown) {
			this.isDown = isDown;
		}

		public void setBuf(ByteBuf buf) {
			this.buf = buf;
		}

		public int getUt_metadata() {
			return ut_metadata;
		}

		public void setUt_metadata(int ut_metadata) {
			this.ut_metadata = ut_metadata;
		}

		public int getMetadata_size() {
			return metadata_size;
		}

		public void setMetadata_size(int metadata_size) {
			log.info(DHTUtils.byteArrayToHexString(hash) + "获得metadata_size：" + metadata_size);
			this.metadata_size = metadata_size;
		}

		public byte[] getPiece() {
			return piece;
		}

		public void setPiece(byte[] piece) {
			this.piece = piece;
		}

		public int getNowPiece() {
			return nowPiece;
		}

		public void setNowPiece(int nowPiece) {
			this.nowPiece = nowPiece;
		}

		public boolean isReadHandShake() {
			return readHandShake;
		}

		public void setReadHandShake(boolean readHandShake) {
			this.readHandShake = readHandShake;
		}

		public List<Body> getBodies() {
			return bodies;
		}

		public void setBodies(List<Body> bodies) {
			this.bodies = bodies;
		}

		public long getCreateTime() {
			return createTime;
		}

		public byte[] getHash() {
			return hash;
		}

		public ByteBuf getBuf() {
			return buf;
		}

		public boolean isReadMetadata() {
			return readMetadata;
		}

		public void setReadMetadata(boolean readMetadata) {
			this.readMetadata = readMetadata;
		}

	}

	class Body {
		private byte type;
		private byte[] dictionary;

		public Body(byte type, byte[] dictionary) {
			this.type = type;
			this.dictionary = dictionary;
		}

		public byte getType() {
			return type;
		}

		public byte[] getDictionary() {
			return dictionary;
		}
	}

}
