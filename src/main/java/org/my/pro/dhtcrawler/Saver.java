package org.my.pro.dhtcrawler;

import org.my.pro.dhtcrawler.btdownload.BtInfo;

public interface Saver {


	public void saveTorrent(String hash);
	
	public void saveBtInfo(BtInfo btInfo);
	
}
