package org.my.pro.dhtcrawler.handler;

import java.util.List;

import org.my.pro.dhtcrawler.DhtNode;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.util.StringUtil;

/**
 * 请求处理器
 */
public abstract class RequestMessageHandler extends AbstractMessageHandler {

	public RequestMessageHandler(RoutingTable routingTable, DhtNode dhtNode) {
		super(routingTable, dhtNode);
	}

	public static String magnetCode(String infoHash) {
		StringBuilder magnet = new StringBuilder();

		// infoHash的长度为20字节, 每个字节的值转为16进制后就是磁力链接
		for (char c : infoHash.toCharArray()) {
			String hs = Integer.toHexString(c);
			// 如果转为16进制后的长度不足2位则第一位补0
			if (hs.length() == 1) {
				magnet.append(0);
			}
			magnet.append(hs);
		}
		return magnet.toString();
	}
	
	public static String infoHash(String magnetCode) {
		char[] result = new char[20];
		List<String> tmp = StringUtil.getStrList(magnetCode, 2);
		
		for(int i = 0 ; i < tmp.size() ; i ++ ) {
			result[i] = (char) Integer.parseInt(tmp.get(i), 16);
		}
		return  new String(result);
	}

	public static void main(String[] args) {
		String s = "5e11667970c8bd2d6a9c7852c9ac75d3ef1383a3";
		String hash = infoHash(s);
		
		System.out.println(hash);
		System.out.println(hash.length());
		
		System.out.println(magnetCode(hash));
	}
}
