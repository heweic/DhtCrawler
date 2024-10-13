package org.my.pro.dhtcrawler.btdownload;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

public class Test {
	
	
	public static void main(String[] args) {
		try {
			byte[] bs =FileUtils.readFileToByteArray(new File("D:\\out.txt"));
			
			BEncodedValue bEncodedValue = BDecoder.bdecode(ByteBuffer.wrap(bs));
			System.out.println(BEncoder.encode(bEncodedValue.getMap()).array().length);
			
			//System.out.println("剩余待解析:" + (bs.length - BEncoder.encode(bEncodedValue.getMap()).array().length));
			
			BtInfo btInfo = new BtInfo(bEncodedValue, "12222333");
			
			System.out.println(GsonUtils.toJsonString(btInfo));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
