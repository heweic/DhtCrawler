package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//   连接到62.171.169.198：6666下载:62f550165041e583de99844a3c4c5b6a19b481d3-
		String ip = "62.171.169.198"; // 目标IP
		int port = 6666; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("62f550165041e583de99844a3c4c5b6a19b481d3");
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
