package org.my.pro.dhtcrawler.saver;

import org.my.pro.dhtcrawler.util.GsonUtils;

public class MagnetNum {

	
	private String value;
	
	private String magnet;
	private String valueUp;
	private int num;
	
	public MagnetNum() {}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
		this.magnet = "magnet:?xt=urn:btih:" + value.toUpperCase();
		this.valueUp = value.toUpperCase();
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	@Override
	public String toString() {
		return GsonUtils.toJsonString(this);
	}

	public String getMagnet() {
		return magnet;
	}

	public void setMagnet(String magnet) {
		this.magnet = magnet;
	}

	public String getValueUp() {
		return valueUp;
	}

	public void setValueUp(String valueUp) {
		this.valueUp = valueUp;
	}
	
	
	
}
