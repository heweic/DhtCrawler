package org.my.pro.dhtcrawler.handler;

import java.util.concurrent.ConcurrentHashMap;

import org.my.pro.dhtcrawler.LocalDHTNode;

public abstract class ResponseMessageHandler extends AbstractMessageHandler {

	public ResponseMessageHandler(LocalDHTNode dhtNode) {
		super(dhtNode);
	}

	/**
	 * 
	 */
	protected volatile boolean isFindingPeer = false;
	protected String taskId;
	protected byte[] hash;
	protected volatile ConcurrentHashMap<String, Integer> rs = new ConcurrentHashMap<String, Integer>();

	/**
	 * 
	 * @param taskId
	 * @param hash
	 */
	public void dofindPeer(String taskId, byte[] hash) {
		System.out.println("开始find_peer" + taskId);
		this.taskId = taskId;
		this.hash = hash;
		this.isFindingPeer = true;
		rs.clear();
	}

	public ConcurrentHashMap<String, Integer> getPeers() {
		return rs;
	}

	public void syn() {
		int time = 0;
		while (true && time < 8) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (rs.size() > 0) {
				break;
			}
			time++;
		}
		System.out.println("find peer 结束:" + rs.size() +"-" + taskId);

		isFindingPeer = false;
	}
}
