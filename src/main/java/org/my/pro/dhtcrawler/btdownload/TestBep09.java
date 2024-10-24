package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//  连接到124.90.154.187：30502下载:04e2c2674506a3da872f8f3ebacb5a3a9bf12cf5---连接成功!
		String ip ="124.90.154.187";
		int port = 30502;
		byte[] hash = DHTUtils.hexStringToByteArray("04e2c2674506a3da872f8f3ebacb5a3a9bf12cf5");
		try {
			BEP09TorrentDownload.getInstance().tryDownload(ip, port, hash);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
