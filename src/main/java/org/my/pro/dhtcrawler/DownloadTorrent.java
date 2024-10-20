package org.my.pro.dhtcrawler;

/**
 * 下载torrent接口
 */
public interface DownloadTorrent {
	
	
	/**
	 * 添加节点
	 * @param node
	 */
	public void addNode(Node node);
	
	/**
	 * 注册本地节点
	 * @param localDHTNode
	 */
	public void register(LocalDHTNode localDHTNode);

}
