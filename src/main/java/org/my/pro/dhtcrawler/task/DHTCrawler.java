package org.my.pro.dhtcrawler.task;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.util.DHTUtils;

/**
 * 不断的发送find_node 认识更多的节点
 */
public class DHTCrawler implements DHTTask {

	private static Log log = LogFactory.getLog(DHTCrawler.class);

	private LocalDHTNode dhtNode;

	private Thread thread;

	private volatile boolean state = false;
	
	private static byte[] closestID(byte[] targetID) {
		byte[] peerId = new byte[20];
		System.arraycopy(targetID, 0, peerId, 0, 18);
		peerId[18] = (byte) (Math.random() * 255);
		peerId[19] = (byte) (Math.random() * 255);
		return peerId;
	}

	public DHTCrawler(LocalDHTNode dhtNode) {
		super();
		this.dhtNode = dhtNode;
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				// 初始化连接表

				//
				while (!Thread.currentThread().isInterrupted()) {
					// 根据自身ID，查询自己距离较近的节点并建立联系
					long sleepTime = 100;
					try {
						byte[] targetNode = closestID(dhtNode.id());
						List<Node> nodes = dhtNode.findNearest(targetNode);

						if (nodes.size() < 8) {

							KrpcMessage krpcMessage4 = MessageFactory.createFindNode("router.utorrent.com", 6881,
									dhtNode.id(), DHTUtils.generateNodeId());
							KrpcMessage krpcMessage5 = MessageFactory.createFindNode("dht.transmissionbt.com", 6881,
									dhtNode.id(), DHTUtils.generateNodeId());
							KrpcMessage krpcMessage6 = MessageFactory.createFindNode("router.bittorrent.com", 6881,
									dhtNode.id(), DHTUtils.generateNodeId());

							dhtNode.sendMessage(krpcMessage4);
							dhtNode.sendMessage(krpcMessage5);
							dhtNode.sendMessage(krpcMessage6);

						}
						//
						if(nodes.size() > 0) {
							sleepTime = 5000;
							for (Node node : nodes) {
								KrpcMessage get_peers = MessageFactory.createFindNode(node.ip(), node.port(),
										dhtNode.id(), targetNode);
								dhtNode.sendMessage(get_peers);
							}
						}
						//加入随机，防止附近节点完全遍历,概率重新随机find_node
						if(RandomUtils.nextInt(0, 30) == 17) {
							KrpcMessage krpcMessage4 = MessageFactory.createFindNode("router.utorrent.com", 6881,
									dhtNode.id(), DHTUtils.generateNodeId());
							KrpcMessage krpcMessage5 = MessageFactory.createFindNode("dht.transmissionbt.com", 6881,
									dhtNode.id(), DHTUtils.generateNodeId());
							KrpcMessage krpcMessage6 = MessageFactory.createFindNode("router.bittorrent.com", 6881,
									dhtNode.id(), DHTUtils.generateNodeId());

							dhtNode.sendMessage(krpcMessage4);
							dhtNode.sendMessage(krpcMessage5);
							dhtNode.sendMessage(krpcMessage6);
						}
					} catch (Exception e) {

					}
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public synchronized void start() {
		if (!state) {
			thread.start();
			state = true;
			log.info(DHTUtils.byteArrayToHexString(dhtNode.id()) + "爬虫任务启动!");
		}
	}

	@Override
	public void stop() {
		thread.interrupt();
	}

}
