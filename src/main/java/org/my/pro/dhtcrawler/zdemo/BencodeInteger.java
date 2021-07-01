package org.my.pro.dhtcrawler.zdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * B编码的整数类型
 * integers(整数)编码为：i<整数>e 开始标记i，结束标记为e 例如： i1234e 表示为整数1234 i-1234e 表示为整数-1234 整数没有大小限制 i0e 表示为整数0 i-0e 为非法 以0开头的为非法如： i01234e 为非法
 * @author dgqjava
 *
 */
public class BencodeInteger implements BencodeType {
    
    private final String content;
    
    public BencodeInteger(String content) {
        this.content = content;
    }
    
    /**
     * 从指定位置开始获取一个B编码的整数对象
     * @param source 源字符串
     * @param index 指定位置
     * @return
     */
    public static BencodeInteger getInt(String source, int index) {
        char c = source.charAt(index);
        if(c == 'i') {
            source = source.substring(index + 1);
            return new BencodeInteger(source.substring(0, source.indexOf("e")));
        }
        return null;
    }

    public int getLength() {
        return content.length();
    }
    
    public int getTotalLength() {
        return getLength() + 2;
    }

    public byte[] getData() {
        try {
            return content.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((byte)'i');
            baos.write(getData());
            baos.write((byte)'e');
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public String toString() {
        return content;
    }
}