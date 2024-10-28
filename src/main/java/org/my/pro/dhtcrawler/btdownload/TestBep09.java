package org.my.pro.dhtcrawler.btdownload;

import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestBep09 {

	public static void main(String[] args) {
		//   连接到/81.161.121.93:37715下载:f597d0a53b386b5b07b6c073146244c431797d8f---连接成功!
		//  连接到/91.199.227.106:19903下载:39690e16c328e245bd57b5969dc91ea694664468---下载完成！-channel数:2138
		String ip ="91.199.227.106";
		int port = 19903;
		byte[] hash = DHTUtils.hexStringToByteArray("39690e16c328e245bd57b5969dc91ea694664468");
		try {
			BEP09TorrentDownload.getInstance().tryDownload(ip, port, hash);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
