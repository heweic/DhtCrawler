package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		// 89.212.159.131：21140下载:1ae9f70f3c7e13c52c8c924aa0039c34445dff32---连接成功!
		
		String ip = "89.212.159.131"; // 目标IP
		int port = 21140; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("1ae9f70f3c7e13c52c8c924aa0039c34445dff32");
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
