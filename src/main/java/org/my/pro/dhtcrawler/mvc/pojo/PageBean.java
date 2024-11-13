package org.my.pro.dhtcrawler.mvc.pojo;

import java.util.List;

import org.my.pro.dhtcrawler.btdownload.BtInfo;

public class PageBean {

	/**
	 * 当前页
	 */
	private int page;
	/**
	 * 分页数量
	 */
	private int pageSize;
	/**
	 * 总数量
	 */
	private int count;

	private long timeUse;
	
	/**
	 * 
	 */
	private List<BtInfo> btInfos;

	public PageBean() {
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<BtInfo> getBtInfos() {
		return btInfos;
	}

	public void setBtInfos(List<BtInfo> btInfos) {
		this.btInfos = btInfos;
	}

	public long getTimeUse() {
		return timeUse;
	}

	public void setTimeUse(long timeUse) {
		this.timeUse = timeUse;
	}
	
	

}
