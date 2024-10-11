package org.my.pro.dhtcrawler.btdownload;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.my.pro.dhtcrawler.util.ByteArrayHexUtils;
import org.my.pro.dhtcrawler.util.NodeIdRandom;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class TorrentClient {
	private byte[] infoHash;
	private final byte[] peerId = NodeIdRandom.generatePeerId();
	private String filePath = "D:\\out.bt";
	private final int pieceLength = 1024 * 256;
	private final int totalLength = 1024 * 1024;
	private FileChannel fileChannel;
	private int currentPieceIndex = 0;

	private String ip;
	private int port;

	public TorrentClient(byte[] infoHash, String ip, int prot) throws Exception {
		super();
		this.infoHash = infoHash;
		this.ip = ip;
		this.port = prot;
		// 打开文件准备写入
		RandomAccessFile file = new RandomAccessFile(filePath, "rw");
		file.setLength(totalLength);
		this.fileChannel = file.getChannel();

	}

	public void downloadTorrentFile() throws InterruptedException {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) {
					ch.pipeline().addLast(new ByteArrayDecoder());
					ch.pipeline().addLast(new ByteArrayEncoder());
					ch.pipeline().addLast(new TorrentClientHandler());
				}
			});

			ChannelFuture future = bootstrap.connect(ip, port).sync();
			future.channel().closeFuture().sync(); // 等待连接关闭
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully();
		}

	}

	private class TorrentClientHandler extends SimpleChannelInboundHandler<byte[]> {

		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			// 当连接建立时，发送握手消息
			ByteBuf handshakeMessage = createHandshakeMessage();
			System.out.println("发送握手消息");
			ctx.writeAndFlush(handshakeMessage);
		}

		private int step = 0;

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {

			System.out.println("收到消息");
			// 解析接收到的数据
			if (isHandshakeResponse(msg)) {
				// 握手成功后，发送 `interested` 消息
				ByteBuf interestedMessage = createInterestedMessage();
				System.out.println("发送interested");
				ctx.writeAndFlush(interestedMessage);
			} else if (isUnchokeMessage(msg)) {
				// Peer 已解除限制，发送块请求
				ByteBuf requestMessage = createRequestMessage(currentPieceIndex, 0, pieceLength);
				System.out.println("发送块请求");
				ctx.writeAndFlush(requestMessage);
			} else if (isPieceMessage(msg)) {
				// 接收到文件块，保存到文件
				System.out.println("接收到文件块，保存到文件");
				savePiece(msg);

				// 请求下一个块
				currentPieceIndex++;
				if (currentPieceIndex * pieceLength < totalLength) {
					ByteBuf requestMessage = createRequestMessage(currentPieceIndex, 0, pieceLength);
					ctx.writeAndFlush(requestMessage);
				} else {
					System.out.println("下载完成");
					ctx.close();
				}
			}

		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}
	}

	private boolean isHandshakeResponse(byte[] msg) {
		return msg.length >= 68 && Arrays.equals(Arrays.copyOfRange(msg, 28, 48), infoHash);
	}

	private boolean isUnchokeMessage(byte[] msg) {
		return msg.length == 5 && msg[4] == 1; // unchoke 消息的 message ID 是 1
	}

	private boolean isPieceMessage(byte[] msg) {
		return msg.length > 9 && msg[4] == 7; // piece 消息的 message ID 是 7
	}

	private ByteBuf createHandshakeMessage() {
		ByteBuf buffer = Unpooled.buffer(68);
		buffer.writeByte(19); // 固定协议字符串长度
		buffer.writeBytes("BitTorrent protocol".getBytes()); // 协议字符串
		buffer.writeZero(8); // 保留位
		buffer.writeBytes(infoHash); // info_hash
		buffer.writeBytes(peerId); // peer_id
		return buffer;
	}

	private ByteBuf createInterestedMessage() {
		ByteBuf buffer = Unpooled.buffer(5);
		buffer.writeInt(1); // 消息长度
		buffer.writeByte(2); // interested 消息ID
		return buffer;
	}

	private ByteBuf createRequestMessage(int index, int begin, int length) {
		ByteBuf buffer = Unpooled.buffer(17);
		buffer.writeInt(13); // 消息长度
		buffer.writeByte(6); // request 消息ID
		buffer.writeInt(index); // 块索引
		buffer.writeInt(begin); // 块的起始位置
		buffer.writeInt(length); // 请求的字节长度
		return buffer;
	}

	private void savePiece(byte[] msg) throws Exception {
		// 从消息中提取块数据，并写入文件
		int index = ByteBuffer.wrap(Arrays.copyOfRange(msg, 5, 9)).getInt();
		int begin = ByteBuffer.wrap(Arrays.copyOfRange(msg, 9, 13)).getInt();
		byte[] block = Arrays.copyOfRange(msg, 13, msg.length);

		fileChannel.position((long) index * pieceLength + begin);
		fileChannel.write(ByteBuffer.wrap(block));
	}

	public static void main(String[] args) throws InterruptedException {
		String ip = "223.109.147.180"; // 目标IP
		int port = 6882; // 目标端口
		byte[] infoHash = ByteArrayHexUtils.hexStringToByteArray("950e7fcf8d5269f412e2f5cf71b28f8b83c377b1");

		// 假设已经填充infoHash
		// Arrays.fill(infoHash, (byte) 1);

		TorrentClient client;
		try {
			client = new TorrentClient(infoHash, ip, port);
			client.downloadTorrentFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
