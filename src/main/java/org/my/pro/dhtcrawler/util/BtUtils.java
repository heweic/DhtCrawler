package org.my.pro.dhtcrawler.util;

public class BtUtils {

	public static String B = " B";
	public static String KB = " KB";
	public static String MB = " MB";
	public static String GB = " GB";

	private static String forM = "magnet:?xt=urn:btih:%s";

	private static java.text.DecimalFormat df = new java.text.DecimalFormat("0.00");

	public static String lengthStr(long length) {

		if (length < 1024) {
			return length + B;
		}

		// KB
		float result = length / 1024;
		if (result < 1024) {
			return result + KB;
		}
		// MB
		result = result / 1024;
		if (result < 1024) {
			return df.format(result) + MB;
		}
		// GB
		result = result / 1024;
		return df.format(result) + GB;
	}

	public static String magnetTotr(String hash) {

		return String.format(forM, hash.toLowerCase());
	}
	
	
	public static void main(String[] args) {
		System.out.println("http://qr.fyxyun.com/q/r.html?mac=EC:6C:9F:12:34&sn=1234567891&p=B2W-12345".length());
	}
}
