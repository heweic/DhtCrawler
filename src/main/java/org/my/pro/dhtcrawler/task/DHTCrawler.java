package org.my.pro.dhtcrawler.task;

import java.util.List;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author hew 爬虫核心逻辑;不断的发送find_node 认识更多的节点 如果当前节点，长时间未获得哈希，修改当前节点ID
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

	class InitTask implements Runnable {

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				// 根据自身ID，查询自己距离较近的节点并建立联系
				try {

					byte[] targetNode = closetId();

					int nodeSize = dhtNode.targetSize(null);

					// 如果路由表中的节点数量小于8 执行初始化逻辑
					if (nodeSize < 8) {
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
					} else {
						//
						break;
					}

					//

				} catch (Exception e) {

				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//
			workThread.start();
			//
			checkHash.start();
		}
	}

	class workTask implements Runnable {
		
		@Override
		public void run() {
			
			log.info(Thread.currentThread().getName() + "开始运行！");
			
			while (!Thread.currentThread().isInterrupted()) {
				//
				byte[] targetNode = closetId();
				List<Node> nodes = TryFindPeerAndDownload.getInstance().findNearest(targetNode);
				if (nodes.size() > 0) {
					for (Node node : nodes) {
						findNode(node, targetNode , 0);
					}
				}
				try {
					Thread.sleep(1000 * 1);
				} catch (InterruptedException e) {
				}
			}

		}

		//
		private void findNode(Node node, byte[] targetNode , int findNum) {
			if(findNum > (8 *20)) { //控制一下递归结果数
				return;
			}
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				// TODO: handle exception
			}

			KrpcMessage findNodeMes = MessageFactory.createFindNode(node.ip(), node.port(), dhtNode.id(), targetNode);
			try {
				Future future = dhtNode.call(findNodeMes);
				KrpcMessage response = future.getValue();
				if (null != response) {

					DefaultResponse defaultResponse = (DefaultResponse) response;

					if (defaultResponse.r().getMap().containsKey(KeyWord.NODES)) {
						byte[] bs = defaultResponse.r().getMap().get(KeyWord.NODES).getBytes();

						ByteBuf byteBuf = Unpooled.wrappedBuffer(bs);
						int num = bs.length / 26;
						//
						for (int i = 0; i < num; i++) {
							Node findNode = DHTUtils.readNodeInfo(byteBuf);
							//log.info(Thread.currentThread().getName() + "找到新节点" + findNode.ip());
							//
							findNode(findNode, targetNode ,findNum ++);
						}

					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

	}

	class checkTask implements Runnable {

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {

				if (dhtNode.hasGetHash()) {
					dhtNode.resetId(DHTUtils.generateNodeId());

					//
					workThread.interrupt();
					try {
						workThread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					dhtNode.resetId(DHTUtils.generateNodeId());
					workThread = new Thread(new workTask(), dhtNode.port() + "-dhtcrawler-workThread");
					workThread.start();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
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
		// log.info("随机:" + DHTUtils.byteArrayToHexString(random));
		System.arraycopy(random, 0, target, tmp, random.length);
		//
		tmp--;
		if (tmp == -1) {
			tmp = 18;
		}
		//
		return target;
	}

	@Override
	public synchronized void start() {
		if (!state) {
			initThread = new Thread(new InitTask(),dhtNode.port() + "-dhtcrawler-initThread");
			initThread.start();

			workThread = new Thread(new workTask(), dhtNode.port() + "-dhtcrawler-workThread");

			checkHash = new Thread(new checkTask(),dhtNode.port() + "-dhtcrawler-checkHashThread");

			state = true;
			log.info(DHTUtils.byteArrayToHexString(dhtNode.id()) + "爬虫启动!");
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
		}
	}

}
