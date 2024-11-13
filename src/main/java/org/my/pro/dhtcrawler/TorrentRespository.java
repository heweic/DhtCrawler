package org.my.pro.dhtcrawler;

import org.my.pro.dhtcrawler.btdownload.BtInfo;
import org.my.pro.dhtcrawler.mvc.pojo.PageBean;

/**
 * 
 * @author hew
 *
 *         <pre>
 * 
 *         </pre>
 */
public interface TorrentRespository {

	/**
	 * 
	 * @param hash
	 * @param bs
	 */
	public long saveBtInfo(BtInfo btInfo);
	
	public long count();

	/**
	 * 
	 * @param hash
	 * @return
	 */
	public BtInfo findByHash(String hash);

	/**
	 * 
	 * @param hash
	 * @return
	 */
	public long del(String hash);
	
	/**
	 * 检索
	 * 
	 * @param ss
	 * @return
	 */
	public PageBean search(String ss, int page, int pageSize);

}
