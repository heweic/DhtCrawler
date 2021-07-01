package org.my.pro.dhtcrawler.zdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * B编码的列表
 * lists(列表)编码为：l<bencoding编码类型>e 开始标记为l,结束标记为e 列表里可以包含任何bencoding编码类型，包括整数，字符串，列表，字典。 例如： l4:test5abcdee 表示为二个字符串["test","abcde"]
 * @author dgqjava
 *
 */
@SuppressWarnings("serial")
public class BencodeList extends ArrayList<BencodeType> implements BencodeType {
    
	/**
	 * 从指定位置开始获取B编码的列表对象
	 * @param source 源字符串
	 * @param index 指定位置
	 * @return
	 */
    public static BencodeList getList(String source, int index) {
        char c = source.charAt(index++);
        if(c == 'l') {
            BencodeList result = new BencodeList();
            for(;;) {
                int temp = index;
                index += result.addElement(BencodeString.getString(source, index));
                index += result.addElement(BencodeInteger.getInt(source, index));
                index += result.addElement(BencodeList.getList(source, index));
                index += result.addElement(BencodeMap.getMap(source, index));
                
                // 如果index!=temp则说明添加成功至少一个B编码
                if(index != temp) {
                    continue;
                }
                
                // 没有更多B编码元素可以添加, 说明已到达列表结尾, 如果列表结尾不为e则是非法的B编码格式
                if(source.charAt(index) == 'e') {
                    break;
                } else {
                    throw new RuntimeException();
                }
            }
            return result;
        }
        return null;
    }
    
    public int getLength() {
        int length = 0;
        for(BencodeType element : this) {
            length += element.getTotalLength();
        }
        return length;
    }
    
    public int getTotalLength() {
        return getLength() + 2;
    }

    public byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for(BencodeType element : this) {
                baos.write(element.getTotalData());
            }
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
    
    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((byte)'l');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(BencodeType element : this) {
            sb.append(", ");
            sb.append(element.toString());
        }
        return sb.length() > 0 ? "[" + sb.toString().substring(2) + "]" : "[]";
    }
    
    /**
     * 添加一个B编码类型的元素并返回被添加的元素的数据长度
     * @param element 被添加的B编码类型元素
     * @return 被添加的B编码类型元素的长度
     */
    private int addElement(BencodeType element) {
        if(null != element) {
            add(element);
            return element.getTotalLength();
        }
        return 0;
    }
}