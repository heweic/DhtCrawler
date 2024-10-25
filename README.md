# dhtCrawler

## 简介
* 一个使用JAVA实现的BT爬虫，它会记录收集到的种子文件哈希值，及下载torrent文件
* 磁力链格式：magnet:?xt=种子文件哈希值

```
	我在最新版本2.2在有公网IP启动每秒收集200-300个种子哈希（包含重复哈希值）,每分钟爬取torrent文件5-10不重复
```


## 功能

* 实现了DHT节点功能，包括节点路由表，及命令：ping，find_node，get_peers，announce_peer
* 在DHT节点功能的基础上加入爬虫逻辑，负责发现更多的附近节点
* 将find_node，get_peers请求过来的哈希值，储存到文件data/hash.txt
* 使用BEP09协议扩展协议实现torrent文件下载功能，将爬取到的torrent文件放在torrent/文件夹

## 爬虫逻辑

* 爬虫主要功能是：去不断认识当前节点附近的节点，从而才能被动收集到种子文件哈希值
* 爬虫持有三个线程，对应三个任务逻辑
* 初始化线程：加入DHT网络时执行，访问固定节点执行find_node认识初始节点
* 工作线程：根据初始节点，不断执行find_node,认识更多的节点
* 哈希检查线程:当一段时间未获得哈希值，它会修改节点ID，停止工作线程，重启初始化线程

## torrent下载逻辑

* 把所有爬虫节点收集到的其他DHT节点信息存入一个由跳表数据结构实现的按节点ID距离顺序存储的节点表中
* 需要下载torrent文件时，它会向节点表中查询其距离最近的节点，发起get_peers请求
* 向获取到的peer按照BEP09协议标准发起最终的torrent文件下载 

## 示例代码
```java

//启动一个DHT点
int port = 0; //随机可用端口，长期爬取建议固定端口及固定IP
LocalDHTNode node = new DefaultDhtNode(port);//实例化一个DHT节点
node.start(); //启动
			
```

## 运行方式

* 可以直接打包作为SpringBoot运行
* 也可以查看org.my.pro.dhtcrawler.Main手动启动
* SpringBoot配置说明
* dht.num=10 //DHT节点数量
* dht.port=60000 //起始端口

## 备注

* 参考协议: 
* http://www.bittorrent.org/beps/bep_0005.html
* http://www.bittorrent.org/beps/bep_0009.html

