package org.my.pro.dhtcrawler.task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DHTTask;
import org.my.pro.dhtcrawler.Future;
import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.Node;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.message.MessageFactory;
import org.my.pro.dhtcrawler.util.DHTUtils;

/**
 * @author hew
 * 
 *         <pre>
 *  	1.随机一个节点，尝试发现其周围节点
 *  	2.如果节点发现完毕，更换ID
 *  	3.如果节点长时间没有获得hash,更换ID
 *         </pre>
 */
public class DHTCrawler implements DHTTask {

	private static Log log = LogFactory.getLog(DHTCrawler.class);

	private LocalDHTNode dhtNode;

	/**
	 * 加入DHT网络线程
	 */
	private Thread initThread;
	/**
	 * 爬虫线程
	 */
	private Thread workThread;

	private Thread checkHash;

	private volatile boolean state = false;

	public DHTCrawler(LocalDHTNode dhtNode) {
		super();
		this.dhtNode = dhtNode;

	}

	/**
	 * 初始化任务，加入DHT网络
	 */
	class InitTask implements Runnable {

		@Override
		public void run() {
			log.info("InitTask启动!");
			while (!Thread.currentThread().isInterrupted()) {

				// 根据自身ID，查询自己距离较近的节点并建立联系
				try {

					byte[] targetNode = closetId();

					// 如果路由表中的节点数量小于8 执行初始化逻辑
					if (dhtNode.targetSize(null) < 8) {
						//

						boolean find1 = initfindNode("router.utorrent.com", 6881, targetNode);
						boolean find2 = initfindNode("dht.transmissionbt.com", 6881, targetNode);
						boolean find3 = initfindNode("router.bittorrent.com", 6881, targetNode);

						if (!find1 && !find2 && !find3 && dhtNode.targetSize(null) == 0) {
							dhtNode.resetId(DHTUtils.generateNodeId());
						}

					} else {
						//
						break;
					}

					//

				} catch (Exception e) {

				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			log.info("InitTask结束!");
			//
			if (workThread != null && !workThread.isAlive()) {
				workThread.start();
			}
			if (checkHash != null && !checkHash.isAlive()) {
				checkHash.start();
			}
		}

		private boolean initfindNode(String ip, int port, byte[] nodeId) {
			KrpcMessage messge = MessageFactory.createFindNode(ip, port, dhtNode.id(), nodeId);
			KrpcMessage response = dhtNode.call(messge).getValue();

			try {
				if (null != response) {

					DefaultResponse defaultResponse = (DefaultResponse) response;

					if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
						return defaultResponse.r().getMap().containsKey(KeyWord.NODES);

					}
				}
			} catch (Exception e) {
				return false;
			}
			return false;
		}
	}

	class findPeerTask implements Runnable {
		private Node node;
		private byte[] targetNode;
		private ConcurrentHashMap<String, Node> rs;

		public findPeerTask(Node node, byte[] targetNode, ConcurrentHashMap<String, Node> rs) {
			this.node = node;
			this.targetNode = targetNode;
			this.rs = rs;
		}

		@Override
		public void run() {
			//
			KrpcMessage findNodeMes = MessageFactory.createFindNode(node.ip(), node.port(), dhtNode.id(), targetNode);
			try {
				Future future = dhtNode.call(findNodeMes);
				KrpcMessage response = future.getValue();
				if (null != response) {

					DefaultResponse defaultResponse = (DefaultResponse) response;

					if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
						byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

						List<Node> nodes = DHTUtils.readNodeInfo(bs);
						//

						nodes.forEach(findNode -> {
							rs.put(findNode.ip() + findNode.port(), findNode);

						});

					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

	}

	class workTask implements Runnable {

		private ExecutorService executorService = Executors.newFixedThreadPool(32);

		@Override
		public void run() {

			log.info(Thread.currentThread().getName() + "开始运行！");

			while (!Thread.currentThread().isInterrupted()) {
				//
				byte[] targetNode = closetId();
				List<Node> nodes = dhtNode.findNearest(targetNode);
				if (nodes.size() > 0) {

					// 每一层待执行查找的node
					ConcurrentHashMap<String, Node> rs = new ConcurrentHashMap<String, Node>();
					nodes.forEach(e -> rs.put(e.ip() + e.port(), e));
					HashSet<String> findNodesIpPort = new HashSet<String>();

					try {

						while (rs.size() > 0 || !Thread.currentThread().isInterrupted()) {
							List<java.util.concurrent.Future<?>> futures = new ArrayList<java.util.concurrent.Future<?>>(
									rs.size());
							//
							Iterator<Entry<String, Node>> it = rs.entrySet().iterator();
							List<Node> taskList = new ArrayList<Node>();
							//遍历待执行任务
							while (it.hasNext()) {
								Entry<String, Node> entry = it.next();
								it.remove();
								//
								if (findNodesIpPort.contains(entry.getKey())) {
									continue;
								}
								//防止缓存过多，1024判断一轮
								if(findNodesIpPort.size() > 1024) {
									findNodesIpPort.clear();
								}
								findNodesIpPort.add(entry.getKey());
								//
								taskList.add(entry.getValue());
							}
							//提交执行
							taskList.forEach( e->{
								futures.add(executorService.submit(new findPeerTask(e, targetNode, rs)));
							});
							//等待所有任务执行完成
							for (int i = 0; i < futures.size(); i++) {
								futures.get(i).get();
							}
	
						//	log.info(DHTUtils.byteArrayToHexString(targetNode) + "发现node数量:" + rs.size() + "遍历节点:"
						// + findNodesIpPort.size());
							Thread.sleep(50);
							
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (Exception e) {
						// TODO: handle exception
					}

				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			//
			executorService.shutdownNow();
			log.info(Thread.currentThread().getName() + "结束!");
			//
		}

	}

	class checkTask implements Runnable {

		@Override
		public void run() {
			log.info("checkTask启动!");
			while (!Thread.currentThread().isInterrupted()) {
				//
				if (!dhtNode.hasGetHash() && !initThread.isAlive()) {
					byte[] id = DHTUtils.generateNodeId();
				//	log.info("重置" + dhtNode.port() + "ID:" + DHTUtils.byteArrayToHexString(id));
					dhtNode.resetId(id);

					//
					workThread.interrupt();
					try {
						workThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					workThread = new Thread(new workTask(), dhtNode.port() + "-dhtcrawler-workThread");
					initThread = new Thread(new InitTask(), dhtNode.port() + "-dhtcrawler-initThread");
					initThread.start();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private volatile int tmp = 18;

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
		System.arraycopy(random, 0, target, tmp, random.length);
		//
		tmp--;
		if (tmp == 14) {
			tmp = 18;
		}
		//
		return target;
	}

	@Override
	public synchronized void start() {
		if (!state) {
			log.info(dhtNode.port() + "爬虫启动!");
			initThread = new Thread(new InitTask(), dhtNode.port() + "-dhtcrawler-initThread");
			initThread.start();

			workThread = new Thread(new workTask(), dhtNode.port() + "-dhtcrawler-workThread");

			checkHash = new Thread(new checkTask(), dhtNode.port() + "-dhtcrawler-checkHashThread");

			state = true;
		}
	}

	@Override
	public void stop() {
		if (state) {
			//
			if (workThread.isAlive()) {
				workThread.interrupt();
			}
			//
			if (initThread.isAlive()) {
				initThread.interrupt();
			}
			if (checkHash.isAlive()) {
				checkHash.interrupt();
			}
			state = false;
		}
	}

}
