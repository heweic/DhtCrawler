# dhtCrawler

## 简介
* 使用Netty实现的一个DHT爬虫
* 实现了加入DHT，发现附近节点，通过收到的get_peers,announce_peer获得到网络中正在寻找种子哈希值
* 实现了将获得到的种子哈希，去遍历节点主动get_peers,并根据返回的peer列表使用BEP09协议下载种子文件

## 示例代码

```java

//使用demo
int port = 0;
byte[] nodeId = DHTUtils.generateNodeId();//生成随机ID
LocalDHTNode node = new DefaultDhtNode(nodeId, port);
node.start(); //启动
			
```

## 备注

* 参考协议: 
* http://www.bittorrent.org/beps/bep_0005.html
* http://www.bittorrent.org/beps/bep_0009.html
* 默认将种子hash保存到 data/hash.txt文件
* 默认将使用BEP09协议获得的种子文件放在 /torrent文件夹下

