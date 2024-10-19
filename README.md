# dhtCrawler

## 简介
* 使用Netty实现的一个DHT爬虫
* 实现了加入DHT，发现附近节点，通过收到的get_peers,announce_peer请求取出网络中正在寻找文件的哈希值
* 实现了种子文件下载功能，使用DHT协议中get_peers搜索哈希的peer列表，并通过BEP09扩展协议下载torrent文件
* 默认将种子hash保存到data/hash.txt文件
* 默认将使用BEP09协议获得的种子文件放在/torrent文件夹下

## 示例代码
```java

//使用demo
int port = 0; //随机可用端口
byte[] nodeId = DHTUtils.generateNodeId();//生成随机ID
LocalDHTNode node = new DefaultDhtNode(nodeId, port);
node.start(); //启动
			
```

## 运行方式

* 可以直接打包作为SpringBoot运行
* 也可以查看org.my.pro.dhtcrawler.Main手动启动
* 如果需要拿到哈希，做后续操作，在DefaultDhtNode中get_peers和announce_peer编写自定义Handler
* SpringBoot 配置说明如下

```java

dht.num=10 //DHT节点数量
dht.port=60000 //起始端口
dht.runbep09=false //是否启用BEP09扩展协议下载torrent，TCP连接能成功获取概率不大
	
```

## 爬虫逻辑

* 初始填充路由表
* 循环执行根据自己id计算出距离较近的节点id,然后从路由表中取得node列表发送find_node
* 循环执行当一定时间内未获得种子哈希值，需要重生成节点id，并设置为自己节点id


## 备注

* 参考协议: 
* http://www.bittorrent.org/beps/bep_0005.html
* http://www.bittorrent.org/beps/bep_0009.html

