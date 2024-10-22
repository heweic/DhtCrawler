package org.my.pro.dhtcrawler.task;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * 文件保存
 */
public class WriteLineToFile {

	private static volatile WriteLineToFile instance;

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	private static String canonicalPath;

	static {
		try {
			canonicalPath = new File("").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String NEW_LINE = System.getProperty("line.separator");

	private WriteLineToFile() {

	}

	/**
	 * 双锁懒汉单例
	 * 
	 * @return
	 */
	public static WriteLineToFile getInstance() {
		if (null == instance) {
			synchronized (WriteLineToFile.class) {
				if (null == instance) {
					instance = new WriteLineToFile();
				}
			}
		}

		return instance;
	}

	/**
	 * 
	 * @param String
	 */
	public void writeLineToHashFile(String String) {

		executorService.execute(() -> {
			try {
				File file = new File(canonicalPath + "/data/hash-"
						+ FastDateFormat.getInstance("yyyy-MM-dd").format(new Date()) + ".txt");
				String line = String + NEW_LINE;
				FileUtils.writeStringToFile(file, line, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

}
