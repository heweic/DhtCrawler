package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		// 211.248.221.219：40852下载:5db092779521f8be061936366cbac35bcc96ad0c---连接成功!
		
		String ip = "211.248.221.219"; // 目标IP
		int port = 40852; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("5db092779521f8be061936366cbac35bcc96ad0c");
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
