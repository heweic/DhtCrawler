package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		// 连接到121.136.148.153：8032下载:d3dbb182d56fa994d9f41bca616ca12cff19ba18---连接成功!
		
		String ip = "31.215.157.114"; // 目标IP
		int port = 6881; // 目标端口
		byte[] infoHash = DHTUtils.hexStringToByteArray("142bb0ed65b3cfd4ffab3a7c0d9309f2522cf97e");
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
