package org.my.pro.dhtcrawler.domain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.DhtNode;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.NodeInfo;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.message.MessageFactory;

public abstract class AbstractRoutingTable implements RoutingTable {

	public static final long timeOut = 60 * 15 * 1000;

	protected Executor executor = Executors.newCachedThreadPool();
	protected ScheduledExecutorService worker = Executors.newScheduledThreadPool(3);

	protected ConcurrentHashMap<BigInteger, NodeInfo> nodes = new ConcurrentHashMap<>();
	protected List<BigInteger> ids = Collections.synchronizedList(new ArrayList<>());
	//
	protected DhtNode localNode;

	private static Log log = LogFactory.getLog(AbstractRoutingTable.class);
	
	public AbstractRoutingTable(DhtNode localNode) {
		this.localNode = localNode;

		// 15分钟清理一次 清理超时/无响应节点
		worker.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

				long nowTime = System.currentTimeMillis();
				Iterator<BigInteger> iterator = ids.iterator();
				while (iterator.hasNext()) {
					BigInteger id = iterator.next();

					NodeInfo info = nodes.get(id);
					if (null != info && (nowTime - info.activeTime() >= timeOut)) {
						nodes.remove(id);
						iterator.remove();
					}
				}

			}
		}, 60, 60 * 5, TimeUnit.SECONDS);

		// 5分钟 定时ping(节点静默超过十分钟)
		worker.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

				Iterator<BigInteger> iterator = ids.iterator();
				long nowTime = System.currentTimeMillis();

				while (iterator.hasNext()) {
					BigInteger id = iterator.next();
					NodeInfo info = nodes.get(id);
					if (null != info && (info.activeTime() + (1000 * 60 * 10) < nowTime)) {
						localNode.exec(MessageFactory.createPing(info, localNode.id()));
					}
				}

			}
		}, 60, 60 * 5, TimeUnit.SECONDS);

		// 如果列表小于 K
		worker.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

				if (ids.size() < 1000) {

					whenTableLess();
				}

			}
		}, 60, 60, TimeUnit.SECONDS);
	}

	public abstract void whenTableLess();

	
	
	@Override
	public boolean has(BigInteger id) {
		return ids.contains(id);
	}

	@Override
	public void add(NodeInfo info, KrpcMessage krpcMessage) {
		if (null == info) {
			return;
		}
		if (nodes.containsKey(info.nodeId().intId())) {
			nodes.replace(info.nodeId().intId(), info);
			return;
		}
		log.info("添加节点:" + "[" + info.ip() + ":" + info.port() + "]" +"--------" + "{" + info.nodeId().intId() + "}");
		ids.add(info.nodeId().intId());
		nodes.put(info.nodeId().intId(), info);
		//
		add0(info, krpcMessage);
	}

	public abstract void add0(NodeInfo info, KrpcMessage krpcMessage);

	@Override
	public List<NodeInfo> findNearest(BigInteger id) {
		List<Integer> indexs = new ArrayList<>();
		//
		int size = 8;
		if (ids.size() < size) {
			size = ids.size();
		}
		//
		for (int i = 0; i < size; i++) {
			randomIndex(indexs, ids.size());
		}
		//
		List<NodeInfo> infos = new ArrayList<>();
		for (int i = 0; i < indexs.size(); i++) {
			NodeInfo tmp = nodes.get(ids.get(indexs.get(i)));
			if (null != tmp) {
				infos.add(tmp);
			}
		}
		//
		return infos;
	}

	public static void randomIndex(List<Integer> index, int size) {

		Integer random = RandomUtils.nextInt(0, size);
		boolean find = false;
		for (Integer it : index) {
			if (it.intValue() == random.intValue()) {
				find = true;
			}
		}

		if (find) {
			randomIndex(index, size);
		} else {
			index.add(random);
		}

	}

	@Override
	public void nodeActive(BigInteger id) {
		NodeInfo cacheNodeInfo = nodes.get(id);

		if (cacheNodeInfo != null) {
			cacheNodeInfo.refActiveTime();
		}
	}

}
