package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//   连接到/223.109.90.79:6890下载:5bd5475a411d0f7f631b5d442b83a7bc089d7165---连接成功!
		String ip ="223.109.90.79";
		int port = 6890;
		byte[] hash = DHTUtils.hexStringToByteArray("5bd5475a411d0f7f631b5d442b83a7bc089d7165");
		try {
			BEP09TorrentDownload.getInstance().tryDownload(ip, port, hash);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
