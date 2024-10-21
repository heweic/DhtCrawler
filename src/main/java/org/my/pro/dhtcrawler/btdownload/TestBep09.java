package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//   连接到94.180.57.185：51221下载:8cf40dc8eac30e39ae781a109b641ab0b7fa54bc
		
		String ip = "94.180.57.185"; // 目标IP
		int port = 51221; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("8cf40dc8eac30e39ae781a109b641ab0b7fa54bc");
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
