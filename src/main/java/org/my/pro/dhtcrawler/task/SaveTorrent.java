package org.my.pro.dhtcrawler.task;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

	private ExecutorService executorService = Executors.newCachedThreadPool();

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

	/**
	 * 异步执行，使netty EventLoop专注于IO操作
	 * 
	 * @param hash
	 * @param bs
	 * @throws Exception
	 */
	public void synWriteBytesToFile(String hash, byte[] bs) throws Exception {
		executorService.execute(() -> {
			// 校验数据完整性
			if (!DigestUtils.sha1Hex(bs).equals(hash)) {
				return;
			}

			try {
				BEncodedValue bEncodedValue = BDeCoderProxy.bdecode(ByteBuffer.wrap(bs));
				BtInfo btInfo = new BtInfo(bEncodedValue, hash);
				LuceneTorrentRespository.getInstance().saveBtInfo(btInfo);
			} catch (Exception e) {
				// TODO: handle exception
			}
		});
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
