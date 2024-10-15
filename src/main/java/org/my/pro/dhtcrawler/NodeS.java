package org.my.pro.dhtcrawler;

import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.my.pro.dhtcrawler.util.DHTUtils;

public class NodeS {

	public void start(int num) {
		System.setProperty("file.encoding", KeyWord.DHT_CHARSET_STR);

		int tmp = 60000;

		for (int i = 0; i < num; i++) {

			int port = tmp + i;

			String id = DHTUtils.byteArrayToHexString(DHTUtils.generateNodeId());
//			System.out.println(id + "----" + id.length());
			// System.out.println(ByteArrayHexUtils.byteArrayToHexString(id));

			new DefaultDhtNode(DHTUtils.hexStringToByteArray(id), port).start();
		}

	}

	public static void main(String[] args) {
		new NodeS().start(5);
	}
}
