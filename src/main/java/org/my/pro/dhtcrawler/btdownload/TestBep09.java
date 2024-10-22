package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//  连接到218.91.199.3：6885下载:b16550fe59b5d7f942ffbc5c506f5ca9101fe814---下載完成!
		
		String ip = "218.91.199.3"; // 目标IP
		int port = 6885; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("b16550fe59b5d7f942ffbc5c506f5ca9101fe814");
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
