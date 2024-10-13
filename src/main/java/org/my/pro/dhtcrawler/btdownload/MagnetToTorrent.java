package org.my.pro.dhtcrawler.btdownload;


/**
 * 种子哈希获得Torrent文件
 */
public interface MagnetToTorrent {


	public void convert(String magnet) throws Exception;
	
}
