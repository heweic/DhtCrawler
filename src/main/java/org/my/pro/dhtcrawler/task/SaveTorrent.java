package org.my.pro.dhtcrawler.task;

import java.nio.ByteBuffer;

import org.apache.commons.codec.digest.DigestUtils;
import org.my.pro.dhtcrawler.btdownload.BtInfo;
import org.my.pro.dhtcrawler.mvc.respository.LuceneTorrentRespository;
import org.my.pro.dhtcrawler.util.BDeCoderProxy;

import be.adaxisoft.bencode.BEncodedValue;

public class SaveTorrent {

	private static volatile SaveTorrent instance;

//	private static String saveDirectory;
//	static {
//		try {
//			saveDirectory = new File("").getCanonicalPath() + "/torrent/";
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<String, Object>();

	private SaveTorrent() {
	}

	/**
	 * 双锁懒汉单例
	 * 
	 * @return
	 */
	public static SaveTorrent getInstance() {
		if (null == instance) {
			synchronized (SaveTorrent.class) {
				if (null == instance) {
					instance = new SaveTorrent();
				}
			}
		}

		return instance;
	}



	
	public void synWriteBytesToFile(String hash, byte[] bs) throws Exception {
		// 校验数据完整性
		if (!DigestUtils.sha1Hex(bs).equals(hash)) {
			throw new Exception(hash + "数据SHA1校验失败!");
		}
		
		BEncodedValue bEncodedValue = BDeCoderProxy.bdecode(ByteBuffer.wrap(bs));
		BtInfo btInfo = new BtInfo(bEncodedValue, hash);
		LuceneTorrentRespository.getInstance().saveBtInfo(btInfo);
//		Object lock = lockMap.computeIfAbsent(hash, key -> new Object());
//		synchronized (lock) {
//			try {
//				File file = new File(saveDirectory + FastDateFormat.getInstance("yyyyMMdd").format(new Date()) + "/" + hash);
//				if(!file.exists()) {
//					FileUtils.writeByteArrayToFile(file, bs, false);
//				}
//			} catch (Exception e) {
//
//			} finally {
//				lockMap.remove(hash);
//			}
//		}
		//
		
	}
}
