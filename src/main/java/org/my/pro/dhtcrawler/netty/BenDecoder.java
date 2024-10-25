package org.my.pro.dhtcrawler.netty;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.message.DefaultResponse;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

public class BenDecoder extends MessageToMessageDecoder<DatagramPacket> {

	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {

		try {
			ByteBuf buf = msg.content();
			int read = buf.readableBytes();
			byte[] bytes = new byte[read];
			buf.readBytes(bytes);
			
			
			
			//bencode
			BEncodedValue bv = BDecoder.decode(new AutoCloseInputStream(new ByteArrayInputStream(bytes)));
			String messageType = bv.getMap().get(KeyWord.Y).getString();
			String transactionID = bv.getMap().get(KeyWord.T).getString();
			InetSocketAddress addr = msg.sender();

			// KrpcMessage message;

			switch (messageType) {
			case KrpcMessage.E:
				break;
			case KrpcMessage.R:
				DefaultResponse defaultResponse = new DefaultResponse(transactionID, addr);
				//
				defaultResponse.setR(bv.getMap().get(KeyWord.R));
				//
				out.add( defaultResponse);
				break;
			case KrpcMessage.Q:
				DefaultRequest defaultRequest = new DefaultRequest(transactionID, addr);
				//
				defaultRequest.setA(bv.getMap().get(KeyWord.A));
				defaultRequest.setQ(bv.getMap().get(KeyWord.Q).getString());
				//
				out.add(defaultRequest);
				break;
			default:
				break;
			}

		} catch (Exception e) {
			return;
		}
	}

}
