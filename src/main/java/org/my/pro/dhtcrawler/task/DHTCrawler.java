package org.my.pro.dhtcrawler.task;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.util.DHTUtils;

/**
 * @author hew
 * 爬虫核心逻辑;不断的发送find_node 认识更多的节点 如果当前节点，长时间未获得哈希，修改当前节点ID
 */
public class DHTCrawler implements DHTTask {

	private static Log log = LogFactory.getLog(DHTCrawler.class);

	private LocalDHTNode dhtNode;

	private Thread thread;

	private volatile boolean state = false;

	public DHTCrawler(LocalDHTNode dhtNode) {
		super();
		this.dhtNode = dhtNode;
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				//
				while (!Thread.currentThread().isInterrupted()) {
					//判断是否需要更换节点ID
					if(!dhtNode.hasGetHash()) {
						
						dhtNode.resetId(DHTUtils.generateNodeId());
					}
					
					
					// 根据自身ID，查询自己距离较近的节点并建立联系
					try {
						// 尽可能均匀的生成剩下19个byte，空间的节点
						byte[] targetNode = closetId();

						int nodeSize = dhtNode.targetSize(targetNode);

						// 如果targetNode节点桶为空
						if (nodeSize == 0) {
							//
							KrpcMessage krpcMessage4 = MessageFactory.createFindNode("router.utorrent.com", 6881,
									dhtNode.id(), targetNode);
							KrpcMessage krpcMessage5 = MessageFactory.createFindNode("dht.transmissionbt.com", 6881,
									dhtNode.id(), targetNode);
							KrpcMessage krpcMessage6 = MessageFactory.createFindNode("router.bittorrent.com", 6881,
									dhtNode.id(), targetNode);

							dhtNode.sendMessage(krpcMessage4);
							dhtNode.sendMessage(krpcMessage5);
							dhtNode.sendMessage(krpcMessage6);
						}

						// 在自己节点表中发起find_node
						List<Node> nodes = dhtNode.findNearest(targetNode);
						if (nodes.size() > 0) {
							for (Node node : nodes) {
								KrpcMessage get_peers = MessageFactory.createFindNode(node.ip(), node.port(),
										dhtNode.id(), targetNode);
								// log.info(DHTUtils.byteArrayToHexString(targetNode) + "- find_node");
								dhtNode.sendMessage(get_peers);
							}
						}

						// 加入随机，概率重新随机find_node
						if (DHTUtils.rng.nextInt(0, 30) == 17) {
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
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}

	private volatile int tmp = 0;

	/**
	 * 生成下次find_node 目标ID
	 * 
	 * @return
	 */
	private synchronized byte[] closetId() {
		byte[] target = new byte[dhtNode.id().length];
		// 一致的字节
		for (int i = 0; i < tmp; i++) {
			target[i] = dhtNode.id()[i];
		}
		// 不一致的字节，随机填充
		byte[] random = new byte[target.length - tmp];
		DHTUtils.nextBytes(random);
		// log.info("随机:" + DHTUtils.byteArrayToHexString(random));
		System.arraycopy(random, 0, target, tmp, random.length);
		//
		tmp++;
		if (tmp == 19) {
			tmp = 0;
		}
		//
		return target;
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
