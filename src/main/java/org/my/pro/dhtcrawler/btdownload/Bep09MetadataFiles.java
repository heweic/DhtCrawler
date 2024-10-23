package org.my.pro.dhtcrawler.btdownload;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.task.SaveTorrent;
import org.my.pro.dhtcrawler.util.BenCodeUtils;
import org.my.pro.dhtcrawler.util.DHTUtils;

import be.adaxisoft.bencode.BDecoder;
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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author hew 使用BEP09扩展协议，创建与peer TCP连接 根据协议内容，发送握手包，协商，分块请求种子元数据
 */
public class Bep09MetadataFiles {

	private static Log log = LogFactory.getLog(Bep09MetadataFiles.class);

	/**
	 * 握手消息长度
	 */
	public static int HANDSHAKE_LENGTH = 68;

	/**
	 * 握手消息，固定前20字节
	 */
	public static final byte[] HANDSHAKE_BYTES = { 19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
			111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1 };

	/**
	 * 种子哈希值
	 */
	private byte[] hash;
	/**
	 * 本机peer节点ID
	 */
	private byte[] localPeerID;
	/**
	 * peer 网络地址
	 */
	private String peerIp;
	/**
	 * peer 端口
	 */
	private int peerPort;

	public Bep09MetadataFiles(byte[] hash, byte[] localPeerID, String peerIp, int peerPort) {
		super();
		this.hash = hash;
		this.localPeerID = localPeerID;
		this.peerIp = peerIp;
		this.peerPort = peerPort;
	}

	/**
	 * 默认标记握手请求
	 */
	private volatile AtomicBoolean isHandShack = new AtomicBoolean(true);
	private CountDownLatch isDownload = new CountDownLatch(1);
	private NioEventLoopGroup group;
	private Channel clientChannel;

	private static int CONNECT_TIMEOUT_MILLIS = 2000;
	private static int HANDSHACK_TIME_OUT = CONNECT_TIMEOUT_MILLIS + 1000;
	private static int DOWNLOAD_TIME_OUT = CONNECT_TIMEOUT_MILLIS + 7000;
	private static int SO_RCVBUF = 1024 * 500;

	private void logMes(String mes) {
		log.info("连接到" + peerIp + "：" + peerPort + "下载:" + DHTUtils.byteArrayToHexString(hash) + "---"
				+ (null == mes ? "" : mes));
	}

	public void get() {
		
		try {
			isDownload.await(HANDSHACK_TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {

		}
		// 如果等待3秒后还在等待握手完成
		if (isHandShack.get()) {
			close();
		}
		//
		try {
			isDownload.await(DOWNLOAD_TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {

		}
		//
	}

	private void close() {
		//
		isDownload.countDown();
		//
		if (null != clientChannel) {
			clientChannel.close();
		}
		if (null != group) {
			group.shutdownGracefully();
		}
	}

	public void tryDownload() {

		// 如果文件已下载，不执行
		if (SaveTorrent.getInstance().exists(DHTUtils.byteArrayToHexString(hash))) {
			return;
		}

		// 使用netty发起tcp连接到peer
		group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();

			bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
					.option(ChannelOption.SO_RCVBUF, SO_RCVBUF).handler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(new HandShackerHandler());
							ch.pipeline().addLast(new TorrentMetadataHandler());
						}
					});
			//

			ChannelFuture channelFuture = bootstrap.connect(peerIp, peerPort);
			channelFuture.addListener(future -> {
				if (future.isSuccess()) {
					logMes("连接成功!");
				} else {
					close();
				}
			});

			clientChannel = channelFuture.sync().channel();
			//

			channelFuture.channel().closeFuture().await();
		} catch (Exception e) {
			// e.printStackTrace();
			isDownload.countDown();
		} finally {
			group.shutdownGracefully();
		}

	}

	private volatile int metadata_size = 0;
	private int ut_metadata = 0;;
	/**
	 * 元数据有多少块
	 */
	private int blockSize;

	/**
	 * 处理握手handler
	 */
	class HandShackerHandler extends SimpleChannelInboundHandler<ByteBuf> {

		private boolean waitNext = false;

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			close();
		}

		private void doHandShack(ByteBuf bf, ChannelHandlerContext ctx) throws Exception {

			// 递归读信息
			readInfo(bf);
			// 如果获取到metadata_size
			if (metadata_size > 0) {
				isHandShack.set(false);
				// 握手完成后替换解码器
				ctx.pipeline().replace(this, "encoder", new LengthFieldBasedFrameDecoder(1024 * 64, 0, 4, 0, 0));
				// 发送扩展握手协议
				sendHandshakeMsg(ctx);

				// 请求第一块数据
				sendMetadataRequest(ctx, 0);
			} else {
				throw new Exception("获取metadata_size失败");
			}
		}

		private void readInfo(ByteBuf bf) throws Exception {

			if (bf.readableBytes() < 4) {
				throw new Exception("可读ByteBuf不足");
			}

			try {
				BitTorrent bt = new BitTorrent();
				bf.markReaderIndex();
				bt.setLength(bf.readInt());
				if (bt.getLength() > bf.readableBytes()) {
					//
					bf.resetReaderIndex();
					return;
				}
				// 仅仅解析BEP09协议中 种子元数据部分
				bt.setType(bf.readByte());
				//
				boolean needJump = true;
				//
				if (bt.getType() == 20) {
					bt.setId(bf.readByte());

					byte[] dictionary = new byte[bt.getLength() - 2];
					bf.readBytes(dictionary);
					BEncodedValue bv = BDecoder.decode(new AutoCloseInputStream(new ByteArrayInputStream(dictionary)));

					// 关注ut_metadata及metadata_size
					ut_metadata = bv.getMap().get("m").getMap().get("ut_metadata").getInt();
					metadata_size = bv.getMap().get("metadata_size").getInt();
					blockSize = (int) Math.ceil((double) metadata_size / (16 << 10));
					//
					needJump = false;
				}
				// 跳过
				if (needJump) {
					bf.readBytes(bt.getLength() - 1);
				}
				//
				if (bf.readableBytes() > 0) {
					readInfo(bf);
				}
			} catch (Exception e) {
				throw e;
			}

		}

		@Override
		public void channelRead0(ChannelHandlerContext ctx, ByteBuf bf) throws Exception {

			if (isHandShack.get()) {
				if (waitNext) {
					doHandShack(bf, ctx);
				}

				if (bf.readableBytes() < 68) {
					return;
				}
				bf.readByte();
				bf.readBytes(19);// 协议
				bf.readBytes(8);// 8字节占位
				// 请求hash
				byte[] hashBs = new byte[20];
				bf.readBytes(hashBs);
				// @TODO 判断与请求哈希是否一致
				bf.readBytes(20);// 对方peerID

				if (bf.readableBytes() > 5) {
					doHandShack(bf, ctx);
				} else {
					// 如果未获取到metadata_size,可能会在下一个包中发送
					waitNext = true;
					return;
				}

			}
		}

	}

	/**
	 * 当前获得块数
	 */
	private int nowPiece;

	/**
	 * 完整种子元数据信息
	 */
	private ByteBuf fullData = Unpooled.buffer();

	/***
	 * netty handler
	 */
	class TorrentMetadataHandler extends SimpleChannelInboundHandler<ByteBuf> {

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			close();
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			// step1 发送握手包
			// 握手信息固定68字节
			ByteBuf bf = Unpooled.buffer(68);
			bf.writeBytes(HANDSHAKE_BYTES);
			bf.writeBytes(hash); // 种子哈希值
			bf.writeBytes(localPeerID);// 本机peerID
			//
			ctx.channel().writeAndFlush(bf);
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf bf) throws Exception {

			if (!isHandShack.get()) {
				// 接收分块响应数据
				bf.readInt();
				bf.readByte();// messageType
				bf.readByte();// messageID

				byte[] data = new byte[bf.readableBytes()];
				bf.readBytes(data);

				BEncodedValue benData = BDecoder.bdecode(ByteBuffer.wrap(data));
				// int nowPiece = benData.getMap().get("piece").getInt();
				// 将row_metadata_piece 加入bt
				int dataLength = BEncoder.encode(benData.getMap()).array().length;

				byte[] rowMetaData = new byte[data.length - dataLength];
				System.arraycopy(data, dataLength, rowMetaData, 0, rowMetaData.length);

				fullData.writeBytes(rowMetaData);

				// 判断是否请求块
				nowPiece++;
				if (nowPiece < blockSize) {
					sendMetadataRequest(ctx, nowPiece);
				} else {
					// 下载完成
					byte[] fulbs = new byte[fullData.readableBytes()];
					fullData.readBytes(fulbs);

					//
					SaveTorrent.getInstance().synWriteBytesToFile(DHTUtils.byteArrayToHexString(hash), fulbs);
					//
					logMes("下載完成!");
					//
					close();
				}
			}
		}
	}

	private void sendMetadataRequest(ChannelHandlerContext ctx, int piece) throws Exception {

		Map<String, BEncodedValue> map = new HashMap<>();
		map.put("msg_type", new BEncodedValue(0));
		map.put("piece", new BEncodedValue(piece));
		BEncodedValue metadata = new BEncodedValue(map);

		ByteBuffer sendMetadataRequest = BEncoder.encode(metadata.getMap());

		ByteBuf extendedHandshake = Unpooled.buffer(2 + sendMetadataRequest.array().length + 4);
		extendedHandshake.writeInt(2 + sendMetadataRequest.array().length);
		extendedHandshake.writeByte(20); // BT_MSG_ID = 20（扩展协议消息）
		extendedHandshake.writeByte(ut_metadata); // EXT_MSG_ID = 0（扩展握手消息）
		extendedHandshake.writeBytes(sendMetadataRequest);

		ctx.writeAndFlush(extendedHandshake);

	}

	/**
	 * 发送扩展握手协议
	 * 
	 * @param ctx
	 * @throws Exception
	 */
	public void sendHandshakeMsg(ChannelHandlerContext ctx) throws Exception {

		BEncodedValue ut_metadata = BenCodeUtils.to("ut_metadata", 1);
		BEncodedValue handshakeData = BenCodeUtils.to("m", ut_metadata);

		if (metadata_size != 0) {
			handshakeData.getMap().put("metadata_size", new BEncodedValue(metadata_size));
		}

		ByteBuffer bencodedHandshake = BEncoder.encode(handshakeData.getMap());

		ByteBuf extendedHandshake = Unpooled.buffer(2 + bencodedHandshake.array().length + 4);
		extendedHandshake.writeInt(2 + bencodedHandshake.array().length);
		extendedHandshake.writeByte(20); // BT_MSG_ID = 20（扩展协议消息）
		extendedHandshake.writeByte(0); // EXT_MSG_ID = 0（扩展握手消息）
		extendedHandshake.writeBytes(bencodedHandshake);

		//
		ctx.writeAndFlush(extendedHandshake);

	}

	class BitTorrent {

		private int length;
		private byte type;
		private byte id;
		private byte[] dictionary;

		public BitTorrent() {
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public byte getType() {
			return type;
		}

		public void setType(byte type) {
			this.type = type;
		}

		public byte getId() {
			return id;
		}

		public void setId(byte id) {
			this.id = id;
		}

		public byte[] getDictionary() {
			return dictionary;
		}

		public void setDictionary(byte[] dictionary) {
			this.dictionary = dictionary;
		}

	}

}
