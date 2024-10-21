package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		// 连接到220.118.160.236：7784下载:9f3cd19c5b94eaf26db004993e9b53ad4e776e7d---连接成功!
		
		String ip = "220.118.160.236"; // 目标IP
		int port = 7784; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("9f3cd19c5b94eaf26db004993e9b53ad4e776e7d");
		Bep09MetadataFiles bep09MetadataFiles = new Bep09MetadataFiles(infoHash, DHTUtils.generatePeerId(), ip, port);

		try {
			bep09MetadataFiles.tryDownload();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		bep09MetadataFiles.get();
	}
}
