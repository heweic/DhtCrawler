package org.my.pro.dhtcrawler.btdownload;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.FileUtils;
import org.my.pro.dhtcrawler.util.BDeCoderProxy;
import org.my.pro.dhtcrawler.util.GsonUtils;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

public class TestReadFile {

	public static void main(String[] args) {
		try {
			byte[] bs = FileUtils.readFileToByteArray(new File("D:\\67a547a6ef193953b339efd63268c174493943e5"));

			BEncodedValue bEncodedValue = BDeCoderProxy.bdecode(ByteBuffer.wrap(bs));
			System.out.println(BEncoder.encode(bEncodedValue.getMap()).array().length);

			// System.out.println("剩余待解析:" + (bs.length -
			// BEncoder.encode(bEncodedValue.getMap()).array().length));

			BtInfo btInfo = new BtInfo(bEncodedValue, "2996b84655d1c5efea9dcfab13217cee216e3885");

			System.out.println(GsonUtils.toJsonString(btInfo));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
