package org.my.pro.dhtcrawler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.my.pro.dhtcrawler.task.DownLoadBtTorrent;
import org.my.pro.dhtcrawler.util.DHTNodeIDPartitionGenerator;

public class NodeS {

	public void start(int num) {
		System.setProperty("file.encoding", KeyWord.DHT_CHARSET_STR);

		int tmp = 70000;

		List<BigInteger> nodeIDs = DHTNodeIDPartitionGenerator.generateDHTNodeIDs(num, num);


		
		for (int i = 0; i < num; i++) {

			int port = tmp + i;

			byte[] id = nodeIDs.get(i).toByteArray();
			// System.out.println(ByteArrayHexUtils.byteArrayToHexString(id));

			new DefaultDhtNode(id , 0).start();
		}
	
	}

	public static void main(String[] args) {
		new NodeS().start(10);
	}
}
