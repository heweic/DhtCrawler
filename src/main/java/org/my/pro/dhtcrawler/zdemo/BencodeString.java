package org.my.pro.dhtcrawler.zdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * B编码的字符串类型
 * 编码规则如下： 
 * strings(字符串)编码为：<字符串长度>：<字符串> 例如： 4:test 表示为字符串"test" 4:例子 表示为字符串“例子” 字符串长度单位为字节 没开始或结束标记
 * @author dgqjava
 *
 */
public class BencodeString implements BencodeType, Comparable<BencodeString> {
    
    private final String content;
    
    public BencodeString(String content) {
        this.content = content;
    }
    
    public BencodeString(byte[] bs) {
        try {
            this.content = new String(bs, "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 从指定位置开始获取一个B编码的字符串
     * @param source 源字符串
     * @param index 指定位置
     * @return
     */
    public static BencodeString getString(String source, int index) {
        char c = source.charAt(index);
        if(c >= '0' && c <= '9') {
            source = source.substring(index);
            String lengthStr = source.split(":")[0];
            int length = Integer.parseInt(lengthStr);
            return new BencodeString(source.substring(lengthStr.length() + 1, lengthStr.length() + 1 + length));
            
        }
        return null;
    }

    public int getLength() {
        return content.length();
    }
    
    public int getTotalLength() {
        return getLength() + String.valueOf(getLength()).length() + 1;
    }

    public byte[] getData() {
        return getData("iso-8859-1");
    }
    
    public byte[] getData(String charsetName) {
        try {
            return content.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public byte[] getTotalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(String.valueOf(getLength()).getBytes("iso-8859-1"));
            baos.write((byte)':');
            baos.write(getData());
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public String toString() {
        return "\"" + content + "\"";
    }

    public int compareTo(BencodeString o) {
        return this.content.compareTo(o.content);
    }
}