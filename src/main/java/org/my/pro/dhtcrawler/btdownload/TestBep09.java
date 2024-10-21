package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		// 218.110.172.96：61018下载:2a034cbfc5a15e4db7ccb0b7813cbda7b866d1bf---连接成功!
		
		String ip = "218.110.172.96"; // 目标IP
		int port = 61018; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("2a034cbfc5a15e4db7ccb0b7813cbda7b866d1bf");
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
