package org.my.pro.dhtcrawler.zdemo;

/**
 * B编码类型, 
 * DHT的报文必须是B编码格式(参见http://blog.csdn.net/xxxxxx91116/article/details/7970815中KRPC协议部分), 
 * B编码有四种类型的数据：
 * srings(字符串), integers(整数), lists(列表), dictionaries(字典) 
 * 编码规则如下： 
 * strings(字符串)编码为：<字符串长度>：<字符串> 例如： 4:test 表示为字符串"test" 4:例子 表示为字符串“例子” 字符串长度单位为字节 没开始或结束标记
 * integers(整数)编码为：i<整数>e 开始标记i，结束标记为e 例如： i1234e 表示为整数1234 i-1234e 表示为整数-1234 整数没有大小限制 i0e 表示为整数0 i-0e 为非法 以0开头的为非法如： i01234e 为非法
 * lists(列表)编码为：l<bencoding编码类型>e 开始标记为l,结束标记为e 列表里可以包含任何bencoding编码类型，包括整数，字符串，列表，字典。 例如： l4:test5abcdee 表示为二个字符串["test","abcde"]
 * dictionaries(字典)编码为 : d<bencoding字符串><bencoding编码类型>e 开始标记为d,结束标记为e 关键字必须为bencoding字符串 值可以为任何bencoding编码类型 例如： d3:agei20ee 表示为{"age"=20} d4:path3:C:/8:filename8:test.txte 表示为{"path"="C:/","filename"="test.txt"}
 * @author dgqjava
 *
 */
public interface BencodeType {
	
	/**
	 * 获取数据长度, 例如字符串abc的B编码为4:abc, 数据长度为"abc".length=3
	 * @return
	 */
    int getLength();
    
    /**
     * 获取总数据长度, 例如字符串abc的B编码为4:abc, 数据总长度为"4:abc".length=5
     * @return
     */
    int getTotalLength();
    
    /**
     * 获取数据, 例如字符串abc的B编码为4:abc, 数据为"abc".getBytes("iso-8859-1")
     * @return
     */
    byte[] getData();
    
    /**
     * 获取总数据, 例如字符串abc的B编码为4:abc, 总数据为"4:abc".getBytes("iso-8859-1")
     * @return
     */
    byte[] getTotalData();
}