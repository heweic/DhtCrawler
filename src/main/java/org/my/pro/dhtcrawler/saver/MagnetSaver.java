package org.my.pro.dhtcrawler.saver;

import org.my.pro.dhtcrawler.btdownload.BtInfo;

public interface MagnetSaver {


	public void saveMagnet(String hash);
	
	public void saveBtInfo(BtInfo btInfo);
	
}
