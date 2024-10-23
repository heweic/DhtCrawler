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
			byte[] bs = FileUtils.readFileToByteArray(new File("D:\\453bb0d3a5dcf082fb9b46c5cdc11d766de49406"));

			BEncodedValue bEncodedValue = BDecoder.bdecode(ByteBuffer.wrap(bs));
			System.out.println(BEncoder.encode(bEncodedValue.getMap()).array().length);

			// System.out.println("剩余待解析:" + (bs.length -
			// BEncoder.encode(bEncodedValue.getMap()).array().length));

			BtInfo btInfo = new BtInfo(bEncodedValue, "11136dea456c609542b2b5824e92a8e266ab26b5");

			System.out.println(GsonUtils.toJsonString(btInfo));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
