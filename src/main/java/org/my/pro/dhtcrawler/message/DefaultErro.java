package org.my.pro.dhtcrawler.message;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.my.pro.dhtcrawler.ErroMessage;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.util.RequestIdGenerator;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import be.adaxisoft.bencode.InvalidBEncodingException;

public class DefaultErro extends AbstractMessage implements ErroMessage {

	/** 一般错误 */
	public static final int E_201 = 201;
	/** 服务错误 */
	public static final int E_202 = 202;
	/**
	 * 协议不规范
	 */
	public static final int E_203 = 203;
	/**
	 * 未知方法
	 */
	public static final int E_204 = 204;

	private BEncodedValue code;
	private BEncodedValue info;

	public DefaultErro(InetSocketAddress addr, BEncodedValue code, BEncodedValue info ,String localId) {
		//
		super(E, RequestIdGenerator.getRequestId(localId), addr);
		//
		this.code = code;
		this.info = info;
	}

	@Override
	public Object code() {
		try {
			return code.getString();
		} catch (InvalidBEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object info() {
		try {
			return info.getString();
		} catch (InvalidBEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public byte[] toByteArray() throws Exception {

		//
		Map<String, BEncodedValue> map = toBencodedValue();
		if (null != code && null != info) {

			List<BEncodedValue> list = new ArrayList<>();
			list.add(code);
			list.add(info);

			map.put(KeyWord.E, new BEncodedValue(list));
		}
		//
		return BEncoder.encode(map).array();

	}

}
