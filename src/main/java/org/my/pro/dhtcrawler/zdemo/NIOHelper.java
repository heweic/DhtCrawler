package org.my.pro.dhtcrawler.zdemo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 封装UDP的NIO操作, 将业务逻辑代码和NIO的API进行解耦, 不关注NIO代码方便更加专注业务编程,
 * 每个NIOHelper对象都通过单线程管理多个NIO通道, 不存在数据竞争
 * @author dgqjava
 *
 */
public class NIOHelper {
	private static final Selector SELECTOR; // 选择器, 全局共享, 线程安全
	private static final ExecutorService EVENT_SELECTOR = Executors.newSingleThreadExecutor(); // 一个进行事件选择和派发的单线程池
	
	private final Map<String, SelectionKey> id2SelectionKey = new HashMap<>(); // 本地nodeId和选择键的映射
	private final ExecutorService worker; // 用来从NIO通道中读写数据的工作线程
	private final Queue<ReadData> readDataQueue = new LinkedList<>(); // 用来存放从通道中读取的数据对象
	private final ByteBuffer buffer = ByteBuffer.allocateDirect(2048); // 2kb的缓冲区用来读取数据
	static {
		try {
			// 创建一个默认的选择器
			SELECTOR = Selector.open();
			
			// 开始事件的监听, 并将监听到的读写事件派发给读写线程去完成异步读写
			EVENT_SELECTOR.execute(new Runnable() {
				public void run() {
					startSelect();
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public NIOHelper(ExecutorService worker) {
		this.worker = worker;
	}
    
	/**
	 * 为本地DHT节点监听到指定端口
	 * @param port 端口
	 * @param id 本地DHT节点id
	 */
    public void bind(final int port, final String id) {
    	try {
	    	worker.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					try {
						// 打开一个UDP协议的NIO通道, 配置为非阻塞模式, 并监听到指定端口
						DatagramChannel channel = DatagramChannel.open();
						channel.configureBlocking(false);
						channel.socket().bind(new InetSocketAddress(port));
						
						// 注册该通道到选择器, 并关注读事件, 这个注册可能会阻塞, 因为选择器已经开始阻塞在select方法上, 但是选择器每隔10毫秒会中断一次, 所以这个阻塞时间很短
						SelectionKey key = channel.register(SELECTOR, SelectionKey.OP_READ);
						key.attach(new Attachment(id));
						
						// 添加节点id到选择键的映射关系, 对id2SelectionKey的读写操作都必须放到worker这个线程中进行, 这样就可以消除线程同步的工作
						id2SelectionKey.put(id, key);
						return true;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}).get();
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * 异步写出数据到指定的通道, 这个方法必须在worker线程中被调用, 这样就可以消除线程同步的工作
     * @param id 本地DHT节点的id
     * @param writeData 要被写出的数据
     */
    public void write(final String id, final WriteData writeData) {
		// 根据本地节点id获取对应的选择键
		SelectionKey key = id2SelectionKey.get(id);
		
		// 添加要写入的数据到队列, 等待NIO的写事件变成可用后, worker线程会将数据写出到指定通道
		((Attachment) key.attachment()).writeDataQueue.add(writeData);
		
		// 开始关注读写事件, 并唤醒选择器
		key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		key.selector().wakeup();
    }
    
    /**
     * 开始选择事件并派发给对应的读写线程完成读写
     */
    private static void startSelect() {
        for(;;) {
            try {
            	// 阻塞等待事件, 每隔10毫秒中断一次
                if (SELECTOR.select(10) == 0) {
                    continue;
                }

                // 循环事件
                Iterator<SelectionKey> it = SELECTOR.selectedKeys().iterator();
                while (it.hasNext()) {
                    final SelectionKey key = it.next(); // 获取选择键
                    final NIOHelper helper = ((Attachment) key.attachment()).helper;
                    
                    // 如果有数据可读, 则派发给对应的线程去异步读取
                    if (key.isReadable()) {
                    	helper.worker.execute(new Runnable() {
							public void run() {
								helper.read(key);
							}
						});
                    }
                    
                    // 如果有数据可写, 则派发给对应的线程去异步写入
                    if (key.isWritable()) {
                    	helper.worker.execute(new Runnable() {
                    		public void run() {
                    			helper.write(key);
                    		}
                    	});
                    }

                    it.remove();
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
    }
    
    /**
     * 读取数据并将读取的数据放入到队列中, 等待数据处理逻辑来获取并处理, 这个方法必须在worker线程中被调用, 这样就可以消除线程同步的工作
     * @param key
     */
    private void read(SelectionKey key) {
    	try {
	        DatagramChannel channel = (DatagramChannel) key.channel(); // 获取通道
	
	        // 获取来源地址
	        SocketAddress address = channel.receive(buffer);
	        if(null == address) {
	        	return;
	        }
	        
	        // 读取数据并重置缓冲区
	        buffer.flip();
	        byte[] data = new byte[buffer.limit()];
	        buffer.get(data).clear();
	        if(data.length <= 0) {
	        	return;
	        }
	        
	        // 数据放入到队列中, 等待其他线程读取
	        readDataQueue.add(new ReadData(data, address, ((Attachment) key.attachment()).id));
    	} catch (Exception e) {
    		//e.printStackTrace();
    	}
    }
    
    /**
     * 从队列中获取数据并写入指定通道, 这个方法必须在worker线程中被调用, 这样就可以消除线程同步的工作
     * @param key
     */
    private void write(SelectionKey key) {
    	try {
	    	Queue<WriteData> writeDataQueue = ((Attachment) key.attachment()).writeDataQueue; // 获取附件中绑定的写入数据队列
	    	DatagramChannel channel = (DatagramChannel) key.channel(); // 获取通道
	    	
	    	if(writeDataQueue.isEmpty()) {
    			// 如果待写入数据队列为空则取消关注写入事件, 对该队列的读取和写入为同一个线程, 因此不需要同步控制
				key.interestOps(SelectionKey.OP_READ);
				key.selector().wakeup();
				return;
	    	}
	
	    	// 从请求队列中获取要写入的数据的封装对象
	    	WriteData writeData = writeDataQueue.poll();
	        ByteBuffer data = writeData.data;
	        
	        // 发送数据
	        channel.send(data, writeData.remoteAddress);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
	public Queue<ReadData> getReadDataQueue() {
		return readDataQueue;
	}

    /**
     * 封装了选择键的附件, 将一些和通道绑定的数据封装在一起
     * @author dgqjava
     *
     */
    class Attachment {
    	private final String id; // 本地DHT节点的id
    	private final NIOHelper helper = NIOHelper.this;
    	private final Queue<WriteData> writeDataQueue = new LinkedList<>(); // 用来存放附件所在通道需要写出的数据对象, 对于这个队列的读取都是单线程的, 所以不用做同步
    	
    	Attachment(String id) {
    		this.id = id;
		}
    	
    	public NIOHelper getHelper() {
    		return helper;
    	}
    }
    
    /**
     * 封装将要写入到通道的数据
     * @author dgqjava
     *
     */
    static class WriteData {
    	private final ByteBuffer data; // 将要写出的数据
    	private final SocketAddress remoteAddress; // 写出的目标地址

    	WriteData(ByteBuffer data, SocketAddress remoteAddress) {
			this.data = data;
			this.remoteAddress = remoteAddress;
		}
    }
    
    /**
     * 封装从通道读取到的数据
     * @author dgqjava
     *
     */
    static class ReadData {
        private final byte[] data; // 读取到的数据
        private final SocketAddress remoteAddress; // 数据来源地址
        private final String id; // 本地DHT节点的id

		public ReadData(byte[] data, SocketAddress remoteAddress, String id) {
			this.data = data;
			this.remoteAddress = remoteAddress;
			this.id = id;
		}

		public byte[] getData() {
			return data;
		}

		public SocketAddress getRemoteAddress() {
			return remoteAddress;
		}

		public String getId() {
			return id;
		}
    }
}
