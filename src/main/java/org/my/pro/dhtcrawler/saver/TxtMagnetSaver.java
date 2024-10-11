package org.my.pro.dhtcrawler.saver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.btdownload.BtInfo;

public class TxtMagnetSaver implements MagnetSaver {

	private File file = new File(canonicalPath + "/data/magnet.mg");
	private File btInfoFile = new File(
			"canonicalPath/data/" + FastDateFormat.getInstance("MM月dd日HH时mm分").format(new Date()) + ".mg");

	public static String NEW_LINE = System.getProperty("line.separator");
	private Executor executor = Executors.newSingleThreadExecutor();

	private static Log log = LogFactory.getLog(TxtMagnetSaver.class);

	private static String canonicalPath;

	static {
		try {
			canonicalPath = new File("").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public TxtMagnetSaver() {
		log.info(file.getAbsolutePath());
	}

	@Override
	public void saveMagnet(String hash) {
		executor.execute(new Runnable() {

			@Override
			public void run() {

				try {
					
					FileUtils.writeStringToFile(file, hash + NEW_LINE, true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void saveBtInfo(BtInfo btInfo) {
		if (null == btInfo) {
			return;
		}
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					FileUtils.writeStringToFile(btInfoFile, btInfo.toString() + NEW_LINE, true);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		});
	}

	public static void main(String[] args) {

		try {
			List<String> list = FileUtils.readLines(new File("E:\\magnet.mg"));

			HashMap<String, Integer> map = new HashMap<>();
			for (String s : list) {
				if (map.containsKey(s)) {
					map.replace(s, map.get(s) + 1);
				} else {
					map.put(s, 1);
				}
			}

			List<MagnetNum> result = new ArrayList<>();
			Iterator<Entry<String, Integer>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Integer> entry = it.next();

				MagnetNum magnetNum = new MagnetNum();

				magnetNum.setValue(entry.getKey());
				magnetNum.setNum((entry.getValue().intValue()));

				result.add(magnetNum);

			}
			result.sort((a, b) -> b.getNum() - a.getNum());

			FileUtils.writeLines(new File("E:\\magnet_result.mg"), result);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
