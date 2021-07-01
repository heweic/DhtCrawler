package org.my.pro.dhtcrawler.zdemo;

import java.math.BigInteger;

/**
 * 节点id对象, 节点id为20字节, 这里转化为无符号整数方便比较节点id之间的距离
 * @author dgqjava
 *
 */
public class NodeId extends BencodeString {
    private final BigInteger value;
    
    public NodeId(byte[] bs) {
        super(bs);
        if(bs.length != 20) {
            throw new RuntimeException();
        }
        
        byte[] dest = new byte[bs.length + 1];
        System.arraycopy(bs, 0, dest, 1, bs.length);
        value = new BigInteger(dest);
    }
    
    public BigInteger getValue() {
        return value;
    }
}