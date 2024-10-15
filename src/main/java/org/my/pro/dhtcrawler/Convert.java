package org.my.pro.dhtcrawler;

import java.io.File;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.my.pro.dhtcrawler.btdownload.HttpMagnetToTorrent;
import org.my.pro.dhtcrawler.btdownload.ItorrentsMagnetTo;
import org.my.pro.dhtcrawler.saver.MagnetSaver;
import org.my.pro.dhtcrawler.saver.TxtMagnetSaver;

public class Convert {

	private volatile static Convert convert = new Convert();
	private volatile static AtomicBoolean isRun = new AtomicBoolean(false);

	private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 20, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(10000 * 100), new RejectedExecutionHandler() {

				// 向线程池提交任务出现异常时
				@Override
				public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadpoolexecutor) {
					try {
						// 取得线程池队列并使用阻塞入队方法
						threadpoolexecutor.getQueue().put(runnable);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			});

	private File file = new File("/opt/magnet/data_result.mg");

	private Convert() {

	}

	public static Convert getInstance() {
		return convert;
	}

	private void work() {
		// 加载
		if (isRun.get()) {
			return;
		}
		isRun.set(true);

		try {

			//
			List<String> lines = FileUtils.readLines(file);

			MagnetSaver magnetSaver = new TxtMagnetSaver();
			HttpMagnetToTorrent httpMagnetToTorrent = new ItorrentsMagnetTo(magnetSaver);

			for (String s : lines) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							httpMagnetToTorrent.convert(s);
						} catch (Exception e) {
							// e.printStackTrace();
						}
					}
				});
			}
			//
		} catch (Exception e) {

		}
		System.out.println("所有任务提交完毕.");
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
