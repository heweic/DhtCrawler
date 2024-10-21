package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		// 连接到178.167.120.111：44247下载:4e3186e52d9406891a50c81b90d29707fe4ea63e---连接成功!
		
		String ip = "178.167.120.111"; // 目标IP
		int port = 44247; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("4e3186e52d9406891a50c81b90d29707fe4ea63e");
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
