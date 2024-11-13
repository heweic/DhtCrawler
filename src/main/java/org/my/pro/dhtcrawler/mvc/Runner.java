package org.my.pro.dhtcrawler.mvc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.mvc.config.DHTConfig;
import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Runner implements ApplicationRunner {

	public static Log log = LogFactory.getLog(Runner.class);

	private Object lock = new Object();
	private volatile boolean isRun = false;
	//
	private List<LocalDHTNode> nodes = new ArrayList<LocalDHTNode>();
	//

	@Autowired
	private DHTConfig dhtConfig;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// 防止重复执行
		synchronized (lock) {
			if (isRun) {
				return;
			}
			isRun = true;
		}
		//
		if (dhtConfig.isEnabled()) {
			int port = dhtConfig.getPort();
			for (int i = 0; i < dhtConfig.getNum(); i++) {
				port = port + i;
				LocalDHTNode node = new DefaultDhtNode(port);
				node.start(); // 启动
				nodes.add(node);

			}
		}
		//

		//
	}

}
