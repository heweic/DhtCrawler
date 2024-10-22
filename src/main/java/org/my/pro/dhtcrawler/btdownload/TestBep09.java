package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//   连接到77.164.214.169：8102下载:e7a13fce58017b981b3aacf078aa19fd208ab84b---连接成功!
		
		String ip = "77.164.214.169"; // 目标IP
		int port = 8102; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("e7a13fce58017b981b3aacf078aa19fd208ab84b");
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
