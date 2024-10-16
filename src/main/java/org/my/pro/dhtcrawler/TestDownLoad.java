package org.my.pro.dhtcrawler;

import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.my.pro.dhtcrawler.util.DHTUtils;

public class TestDownLoad {
	
	
	public static void main(String[] args) {
		DefaultDhtNode defaultDhtNode = new DefaultDhtNode(DHTUtils.generateNodeId(), 0);
		
		
		defaultDhtNode.start();
		
		
		while(true) {
			try {
				Thread.sleep(1000 * 10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			defaultDhtNode.tryDownLoad(DHTUtils.hexStringToByteArray("68EC1FDE0D70D6662B1F9B8F80A4952751F5C36D"));
		}
	}

}
