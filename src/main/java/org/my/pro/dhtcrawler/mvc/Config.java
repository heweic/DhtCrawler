package org.my.pro.dhtcrawler.mvc;

import java.util.ArrayList;
import java.util.List;

import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.netty.DefaultDhtNode;
import org.my.pro.dhtcrawler.util.DHTUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class Config implements ApplicationListener<ApplicationEvent> {

	private Object lock = new Object();
	private volatile boolean isRun = false;

	private List<LocalDHTNode> nodes = new ArrayList<LocalDHTNode>();

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		// 防止重复执行
		synchronized (lock) {
			if (isRun) {
				return;
			}
			isRun = true;
		}

		int tmp = 60000;

		for (int i = 0; i < 25; i++) {

			int port = tmp + i;
			byte[] nodeId = DHTUtils.generateNodeId();//生成随机ID
			LocalDHTNode node = new DefaultDhtNode(nodeId, port);
			node.start(); //启动
			
			nodes.add(node);
			
		}
	}

}
