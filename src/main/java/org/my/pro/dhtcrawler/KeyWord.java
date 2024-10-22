package org.my.pro.dhtcrawler;

import java.nio.charset.Charset;

public interface KeyWord {

	public static final Charset DHT_CHARSET  = Charset.forName("iso-8859-1");
	public static final String DHT_CHARSET_STR  = "iso-8859-1";
	
	/** 消息类型字段 */
	public static final String Y = "y";
	/** 消息ID字段 */
	public static final String T = "t";

	/** 请求返回值 字典 */
	public static final String R = "r";

	/** 请求参数 字典类型 */
	public static final String A = "a";

	/** 请求方法名 */
	public static final String Q = "q";

	/**
	 * 错误码 及描述 列表类型
	 */
	public static final String E = "e";

	/**
	 * 节点ID 所有的请求都包含一个关键字id，它包含了请求节点的nodeID。所有的回复也包含关键字id，它包含了回复节点的nodeID。
	 */
	public static final String ID = "id";

	/**
	 * 
	 * 最基础的请求就是ping。 这时KPRC协议中的“q”=“ping”。 Ping请求包含一个参数id，
	 * 它是一个20字节的字符串包含了发送者网络字节序的nodeID。 对应的ping回复也包含一个参数id，包含了回复者的nodeID。
	 */
	public static final String PING = "ping";

	/**
	 * Findnode被用来查找给定ID的node的列表
	 */
	public static final String FIND_NODE = "find_node";

	/**
	 * 用与查询持有文件哈希的peer列表
	 */
	public static final String GET_PEERS = "get_peers";

	/** 
	 * 当一个节点想分享某个torrent文件时他会通过这个命令发送至DHT网络
	 */
	public static final String ANNOUNCE_PEER = "announce_peer";

	public static final String TARGET = "target";
	public static final String INFO_HASH = "info_hash";
	public static final String TOKEN = "token";

	public static final String NODES = "nodes";

	
	public static final String VALUES = "values";
	
	//
	public static final String msg_type = "msg_type";
	public static final String piece = "piece";
	public static final String total_size = "total_size";
}
