# dhtCrawler

## 免责申明
* 本项目仅供学习和研究用途，任何人使用本项目源码进行违反当地法律法规的行为，均需自行承担相应的法律责任。
* 作者对任何直接或间接使用本项目源码导致的后果不承担任何责任。

## 简介
* 一个使用JAVA实现的BT爬虫，它会记录收集到的种子文件哈希值，及下载torrent文件
* 一个BT检索引擎，它会将爬取的torrent文件，解析出需要的内容，保存到data目录下
* BT搜索地址: http://localhost:/index
* 磁力链格式：magnet:?xt=种子文件哈希值
* 检索页面截图

![检索实例图](search1.png)

```
	我在最新版本2.2在有公网IP启动28个节点，大概每分钟能爬取60+的torrent文件
```

## 功能

* 实现了DHT节点功能，包括节点路由表，及命令：ping，find_node，get_peers，announce_peer
* 在DHT节点功能的基础上加入爬虫逻辑，负责发现更多的附近节点
* 使用BEP09协议扩展协议实现torrent文件下载功能，将爬取到的torrent文件放在torrent/文件夹
* 使用Apache-Lucene实现全文检索

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

## 检索逻辑

* 使用Apache-Lucene实现全文档搜索
* 使用ik-analyzer，对待检索内容做中文分词

## 示例代码

* 建议在服务器级别的网络环境中运行当前程序
* 爬虫在工作的时候会发起大量的UDP和TCP链接，一般的家用路由器维护不了庞大的NAT路由表
* 这将可能使你的家用网络无法访问网络，爬虫程序无法正常工作

```java

//启动一个DHT点并启动一个torrent下载
//启动多个DHT节点，会共用一个torrent下载
int port = 0; //随机可用端口，长期爬取建议固定端口及固定IP
LocalDHTNode node = new DefaultDhtNode(port);//实例化一个DHT节点
node.start(); //启动
			
```

## 运行方式

* 可以直接打包作为SpringBoot运行
* 也可以查看org.my.pro.dhtcrawler.Main
* SpringBoot配置说明
```
dht.pass=hedaye   //BT检索功能访问密码配置
dht.enabled=true  //是否启动BT爬虫功能,不启动，仅有检索功能
dht.num=1         //DHT节点数量
dht.port=60000    //DHT节点起始端口

```

## 备注

* 参考协议: 
* http://www.bittorrent.org/beps/bep_0005.html
* http://www.bittorrent.org/beps/bep_0009.html

