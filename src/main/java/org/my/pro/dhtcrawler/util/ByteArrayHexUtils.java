package org.my.pro.dhtcrawler.util;

public class ByteArrayHexUtils {

	public static String byteArrayToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static byte[] hexStringToByteArray(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}
	
	public static void main(String[] args) {
		String s = "797fcfd5b4d35756498306238566f443820e8414";
		
		System.out.println(s);
		byte[] bs = hexStringToByteArray(s);
		System.out.println(GsonUtils.toJsonString(bs));
		
		System.out.println(byteArrayToHexString(bs));
	}
}
