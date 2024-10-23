package org.my.pro.dhtcrawler.saver;

import org.my.pro.dhtcrawler.Saver;
import org.my.pro.dhtcrawler.btdownload.BtInfo;

@Deprecated
public class TxtMagnetSaver implements Saver {
//
//	public static String NEW_LINE = System.getProperty("line.separator");
//	private Executor executor = Executors.newSingleThreadExecutor();
//
//	public static Log log = LogFactory.getLog(TxtMagnetSaver.class);
//
//	private static String canonicalPath;

//	static {
//		try {
//			canonicalPath = new File("").getCanonicalPath();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public TxtMagnetSaver() {

	}

	@Override
	public void saveTorrent(String hash) {
		// TODO Auto-generated method stub

	}

//
//
//	@Override
//	public void saveMagnet(String hash) {
//		executor.execute(new Runnable() {
//
//			@Override
//			public void run() {
//
//				try {
//					File btInfoFile = new File(
//							canonicalPath + "/data/" + hash + ".mg");
//					FileUtils.writeStringToFile(btInfoFile, hash + NEW_LINE, true);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	@Override
	public void saveBtInfo(BtInfo btInfo) {
//		if (null == btInfo) {
//			return;
//		}
//		executor.execute(new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					FileUtils.writeStringToFile(btInfoFile, btInfo.toString() + NEW_LINE, true);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//			}
//
//		});
	}

}
