package org.my.pro.dhtcrawler.btdownload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.routingTable.DefaultNodeInfo;
import org.my.pro.dhtcrawler.util.BenCodeUtils;
import org.my.pro.dhtcrawler.util.ByteArrayHexUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

/**
 * 磁力链接转种子
 * 
 */
public class TryDownloadBt {

	public static final byte[] HANDSHAKE_BYTES = { 19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
			111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1 };

	/**
	 * 种子hash
	 */
	private byte[] infoHash;

	/**
	 * peer
	 */
	private NodeInfo peer;

	private byte[] localId;

	private static Log log = LogFactory.getLog(TryDownloadBt.class);

	public TryDownloadBt(byte[] infoHash, NodeInfo peer, byte[] localId) {
		super();
		this.infoHash = infoHash;
		this.peer = peer;
		this.localId = localId;
	}

	public void downloadTorrentFile() throws InterruptedException {

		NioEventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();

			bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
					.option(ChannelOption.SO_RCVBUF, 1024 * 500).handler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							// ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 500, 0,
							// 4,0,4));
							ch.pipeline().addLast(new TorrentMetadataHanler(infoHash, localId));
						}
					});
			//
			System.out.println(peer.ip() + "-" + peer.port());
			ChannelFuture channelFuture = bootstrap.connect(peer.ip(), peer.port()).sync();
			//

			channelFuture.channel().closeFuture().await();
		} catch (Exception e) {
			// TODO: handle exception

		} finally {
			group.shutdownGracefully();
		}

	}

	private class TorrentMetadataHanler extends SimpleChannelInboundHandler<ByteBuf> {

		private final byte[] infoHash;
		private final byte[] peerId;

		public TorrentMetadataHanler(byte[] infoHash, byte[] peerId) {
			this.infoHash = infoHash;
			this.peerId = peerId;
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {

			byte[] sendBytes = new byte[68];
			System.arraycopy(HANDSHAKE_BYTES, 0, sendBytes, 0, 28);
			System.arraycopy(infoHash, 0, sendBytes, 28, 20);
			System.arraycopy(peerId, 0, sendBytes, 48, 20);
			System.out.println(
					"发送握手消息" + ByteArrayHexUtils.byteArrayToHexString(sendBytes) + "-------" + sendBytes.length);
			ctx.channel().writeAndFlush(Unpooled.copiedBuffer(sendBytes));

		}

		private int metadata_size = 0;

		private int ut_metadata = 0;;

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf bf) throws Exception {

			if (bf.readableBytes() < 68) {
				return;
			}

			// 读取4个字节做消息类型判断
			bf.markReaderIndex();
			byte tag = bf.readByte();
			bf.resetReaderIndex();

//			byte[] bytes = new byte[bf.readableBytes()];
//			bf.readBytes(bytes);
//			String messageStr = new String(bytes, CharsetUtil.ISO_8859_1);

			// System.out.println("收到消息：" + messageStr);

			if (tag == 19) {
				System.out.println("tag 199999999999999999");
				// 重置读index
				bf.readByte();
				bf.readBytes(19);// 协议
				bf.readBytes(8);// 8字节占位
				// 请求hash
				byte[] hashBs = new byte[20];
				bf.readBytes(hashBs);
				// @TODO 判断与请求哈希是否一致

				bf.readBytes(20);// 对方peerID

				if (bf.readableBytes() > 68) {
					bf.readBytes(6);// 未知内容

					//
					byte[] value = new byte[bf.readableBytes() - 68 - 6];

					bf.readBytes(value);

					BEncodedValue bv = BDecoder.decode(new AutoCloseInputStream(new ByteArrayInputStream(value)));

					System.out.println(bv.getMap().get("m").getMap().get("ut_metadata").getInt());
					ut_metadata = bv.getMap().get("m").getMap().get("ut_metadata").getInt();
					metadata_size = bv.getMap().get("metadata_size").getInt();
					System.out.println(bv.getMap().get("metadata_size").getInt());
					// System.out.println("-------" + bf.readableBytes());
					//
					sendHandshakeMsg(ctx);
//					// 分片请求种子
					if (ut_metadata != 0 && metadata_size != 0) {
						sendMetadataRequest(ctx);
					}

				}
			} else {
				if(null == bt) {
					bt = Unpooled.buffer();
					bf.readInt();
					byte pid = bf.readByte();
					byte dataType = bf.readByte();
					System.out.println("metadata_size:" + metadata_size);
					//System.out.println("tmp:" + );
					//System.out.println(tmp);
				}

				
				byte[] data = new byte[bf.readableBytes()];
				bf.readBytes(data);
				//System.out.println(new String(data));
				bt.writeBytes(data);
				if(bt.readableBytes() >= metadata_size) {
					
					
					//
					int cha = bt.readableBytes() - metadata_size;
					System.out.println("满了" + bt.readableBytes() + "差:" + cha);
					//
					byte[] fullData = new byte[bt.readableBytes()];
					bt.readBytes(fullData);
					BEncodedValue value = BDecoder.bdecode(ByteBuffer.wrap(fullData));
					System.out.println(GsonUtils.toJsonString(value));
					System.out.println(GsonUtils.toJsonString(new BtInfo(value, ByteArrayHexUtils.byteArrayToHexString(infoHash))));
					FileUtils.writeByteArrayToFile(new File("D:\\out.txt"), fullData, false);
				}
				
				
				//
				//System.out.println();

			}
		}
	//	private int tmp;
		private ByteBuf bt = null;
		
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

			System.out.println("发送扩展握手消息:" + new String(extendedHandshake.array()));
			ctx.writeAndFlush(extendedHandshake);

		}

//
		private void sendMetadataRequest(ChannelHandlerContext ctx) throws Exception {

			int blockSize = (int) Math.ceil((double) metadata_size / (16 << 10));

			for (int i = 0; i < blockSize; i++) {
				Map<String, BEncodedValue> map = new HashMap<>();
				map.put("msg_type", new BEncodedValue(0));
				map.put("piece", new BEncodedValue(i));
				BEncodedValue metadata = new BEncodedValue(map);

				ByteBuffer sendMetadataRequest = BEncoder.encode(metadata.getMap());

				ByteBuf extendedHandshake = Unpooled.buffer(2 + sendMetadataRequest.array().length + 4);
				extendedHandshake.writeInt(2 + sendMetadataRequest.array().length);
				extendedHandshake.writeByte(20); // BT_MSG_ID = 20（扩展协议消息）
				extendedHandshake.writeByte(ut_metadata); // EXT_MSG_ID = 0（扩展握手消息）
				extendedHandshake.writeBytes(sendMetadataRequest);

				System.out.println("发送分片消息:" + new String(extendedHandshake.array()));
				ctx.writeAndFlush(extendedHandshake);

			}

		}

	}
	
	
	public static void main(String[] args) throws InterruptedException {
		String ip = "176.9.137.195"; // 目标IP
		int port = 37040; // 目标端口
		byte[] infoHash = ByteArrayHexUtils.hexStringToByteArray("fe398bcb9f127804ba9afcbee934303496487428");

		// 假设已经填充infoHash
		// Arrays.fill(infoHash, (byte) 1);

		TryDownloadBt client;
		try {
			NodeInfo info = new DefaultNodeInfo(null, ip, port);
			client = new TryDownloadBt(infoHash, info,
					ByteArrayHexUtils.hexStringToByteArray("2d4e455454593030312d8fd20652dd2588277467"));
			client.downloadTorrentFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
