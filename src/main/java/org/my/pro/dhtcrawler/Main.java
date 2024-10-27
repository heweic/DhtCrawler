package org.my.pro.dhtcrawler;

import org.my.pro.dhtcrawler.netty.DefaultDhtNode;

public class Main {

	public void start(int num) {

		//

		int tmp = 60000;
		for (int i = 0; i < num; i++) {

			int port = tmp + i;

			new DefaultDhtNode(port).start();
		}

	}

	public static void main(String[] args) {
		new Main().start(6);
	}
}
