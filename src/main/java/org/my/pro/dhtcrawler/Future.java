package org.my.pro.dhtcrawler;

/**
 * 
 * @author hew
 *
 */
public interface Future {

	/**
	 * if task is success, return the result.
	 * 
	 * @throws Exception when timeout, cancel, onFailure
	 * @return
	 */
	KrpcMessage getValue();

	
	void back(KrpcMessage krpcMessage);
}
