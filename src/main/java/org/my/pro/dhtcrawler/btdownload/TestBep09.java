package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//  连接到18.234.247.232：58090下载:05512678327c59d14eb17b88914b9d390d13b684
		
		String ip = "12.234.247.232"; // 目标IP
		int port = 58090; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("05512678327c59d14eb17b88914b9d390d13b684");
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
