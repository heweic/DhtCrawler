package org.my.pro.dhtcrawler.btdownload;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.FileUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

public class TestReadFile {

	public static void main(String[] args) {
		try {
			byte[] bs = FileUtils.readFileToByteArray(new File("D:\\21c3a7f2a06259517b02d31501ab4d4f81f32d8c"));

			BEncodedValue bEncodedValue = BDecoder.bdecode(ByteBuffer.wrap(bs));
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
