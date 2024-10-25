package org.my.pro.dhtcrawler.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;

public class BDeCoderProxy {

	/**
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static BEncodedValue decode(InputStream in) throws IOException {
		try {
			return BDecoder.decode(in);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	/**
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static BEncodedValue bdecode(ByteBuffer data) throws IOException {

		return decode(new ByteArrayInputStream(data.array()));
	}

	/**
	 * 
	 * @param bytes
	 * @return
	 * @throws IOException
	 */
	public static BEncodedValue bdecode(byte[] bytes) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		return bdecode(byteBuffer);
	}

}
