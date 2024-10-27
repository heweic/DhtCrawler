package org.my.pro.dhtcrawler.task;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;

public class SaveTorrent {

	private static volatile SaveTorrent instance;

	private static String saveDirectory;
	static {
		try {
			saveDirectory = new File("").getCanonicalPath() + "/torrent/";
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<String, Object>();

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
		Object lock = lockMap.computeIfAbsent(hash, key -> new Object());
		synchronized (lock) {
			try {
				File file = new File(saveDirectory + FastDateFormat.getInstance("yyyyMMdd").format(new Date()) + "/" + hash);
				if(!file.exists()) {
					FileUtils.writeByteArrayToFile(file, bs, false);
				}
			} catch (Exception e) {

			} finally {
				lockMap.remove(hash);
			}
		}
	}
}
