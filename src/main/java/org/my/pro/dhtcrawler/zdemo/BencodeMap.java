package org.my.pro.dhtcrawler.zdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * B编码的字典类型, B编码的字典必须根据主键预排序, 因此这里继承TreeMap, 利用TreeMap的主键自然顺序特性自动排序
 * dictionaries(字典)编码为 : d<bencoding字符串><bencoding编码类型>e 开始标记为d,结束标记为e 关键字必须为bencoding字符串 值可以为任何bencoding编码类型 例如： d3:agei20ee 表示为{"age"=20} d4:path3:C:/8:filename8:test.txte 表示为{"path"="C:/","filename"="test.txt"}
 * @author dgqjava
 *
 */
@SuppressWarnings("serial")
public class BencodeMap extends TreeMap<BencodeString, BencodeType> implements BencodeType {
    
	/**
	 * 从指定位置开始获取一个B编码的字典对象
	 * @param source 源字符串
	 * @param index 指定位置
	 * @return
	 */
    public static BencodeMap getMap(String source, int index) {
        char c = source.charAt(index++);
        if(c == 'd') {
            BencodeMap result = new BencodeMap();
            BencodeString key = null;
            for(;;) {
            	// 如果获取到任意一种类型的B编码数据
                BencodeType element;
                if(null != (element = BencodeString.getString(source, index)) || 
	               null != (element = BencodeInteger.getInt(source, index)) || 
	               null != (element = BencodeList.getList(source, index)) || 
	               null != (element = BencodeMap.getMap(source, index))) {
                	
                	// key不为null, 则获取到的是value, 否则获取到的是key
                    if(null != key) {
                        result.put(key, element);
                        key = null;
                    } else {
                        key = (BencodeString) element;
                    }
                    index += element.getTotalLength();
                    continue;
                }
                
                // 如果无任何类型数据可获取, 就是达到了字典类型的结尾, 如果结尾不是e则是非法的B编码格式
                if(source.charAt(index) == 'e') {
                    break;
                } else {
                	System.out.println(source);
                	return null;
                }
            }
            return result;
        }
        return null;
    }

    public int getLength() {
        int length = 0;
        for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
            length += entry.getKey().getTotalLength();
            length += entry.getValue().getTotalLength();
        }
        return length;
    }
    
    public int getTotalLength() {
        return getLength() + 2;
    }

    public byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
                baos.write(entry.getKey().getTotalData());
                baos.write(entry.getValue().getTotalData());
            }
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
    
    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((byte)'d');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<BencodeString, BencodeType> entry : entrySet()) {
            sb.append(", ");
            sb.append(entry.getKey().toString());
            sb.append(" : ");
            sb.append(entry.getValue().toString());
        }
        return sb.length() > 0 ? "{" + sb.toString().substring(2) + "}" : "{}";
    }
}