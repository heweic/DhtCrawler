package org.my.pro.dhtcrawler.task;

import org.my.pro.dhtcrawler.DHTTask;

/**
 * 文件保存
 */
public class WriteLineToFile implements DHTTask {

	private static volatile WriteLineToFile instance;
//
//	private ExecutorService executorService = Executors.newSingleThreadExecutor();
//
//	private static String canonicalPath;
//
//	static {
//		try {
//			canonicalPath = new File("").getCanonicalPath();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

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

		return;
		// 这玩意儿真没法记录，一天的文件都得上G
//		executorService.execute(() -> {
//			try {
//				File file = new File(canonicalPath + "/data/hash-"
//						+ FastDateFormat.getInstance("yyyy-MM-dd").format(new Date()) + ".txt");
//				String line = String + NEW_LINE;
//				//@TODO 后面再改吧
//				FileUtils.writeStringToFile(file, line, true);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
