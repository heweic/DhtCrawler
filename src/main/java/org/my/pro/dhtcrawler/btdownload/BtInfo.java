package org.my.pro.dhtcrawler.btdownload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.my.pro.dhtcrawler.util.BtUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;

import be.adaxisoft.bencode.BEncodedValue;

public class BtInfo {

	/**
	 * 文件名
	 */
	private String name;

	/**
	 * 大小 eg: 12.58M / 470.00 Byte
	 */
	private String size;

	/** */
	private long length;

	/**
	 * hash
	 */
	private String hash;

	/**
	 * 磁力连接
	 */
	private String magnet;

	/**
	 * 文件数量
	 */
	private int fileNum;

	/**
	 * 文件列表
	 */
	private List<BtFileInfo> files = new ArrayList<>();

	public BtInfo() {
	}

	public BtInfo(BEncodedValue value, String hash) {
		try {
			files.clear();
			//

			Map<String, BEncodedValue> info = value.getMap();

			name = null;
			if (info.containsKey("name.utf-8")) {
				name = info.get("name.utf-8").getString();
			} else {
				name = info.get("name").getString();
			}

			if (info.containsKey("files")) {
				//

				List<BEncodedValue> list = info.get("files").getList();
				setFiles(list, files);
				//

				long length = 0;
				for (BtFileInfo b : files) {
					length = b.getLength() + length;
				}
				this.length = length;
				this.size = BtUtils.lengthStr(length);
				fileNum = files.size();
			} else {

				length = info.get("length").getLong();
				size = BtUtils.lengthStr(length);
				fileNum = 1;
				//
				BtFileInfo btFileInfo = new BtFileInfo();

				btFileInfo.setLength(length);
				btFileInfo.setPath(name);
				btFileInfo.setSize(size);

				files.add(btFileInfo);
			}
			//
			this.hash = hash;
			magnet = BtUtils.magnetTotr(hash);

		} catch (Exception e) {

		}
	}

	private void setFiles(List<BEncodedValue> list, List<BtFileInfo> files) throws Exception {

		for (BEncodedValue b : list) {
			Map<String, BEncodedValue> file = b.getMap();

			// name
			List<BEncodedValue> names = null;
			StringBuffer sb = new StringBuffer();
			if (file.containsKey("path.utf-8")) {
				names = file.get("path.utf-8").getList();
			} else {
				names = file.get("path").getList();
			}
			for (int i = 0; i < names.size(); i++) {
				sb.append(names.get(i).getString());
				if ((i + 1) < names.size()) {
					sb.append("/");
				}
			}
			// length

			BtFileInfo btFileInfo = new BtFileInfo();

			btFileInfo.setLength(file.get("length").getLong());
			btFileInfo.setPath(sb.toString());
			btFileInfo.setSize(BtUtils.lengthStr(file.get("length").getLong()));

			files.add(btFileInfo);
		}
	}

	@Override
	public String toString() {
		return GsonUtils.toJsonString(this);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getMagnet() {
		return magnet;
	}

	public void setMagnet(String magnet) {
		this.magnet = magnet;
	}

	public int getFileNum() {
		return fileNum;
	}

	public void setFileNum(int fileNum) {
		this.fileNum = fileNum;
	}

	public List<BtFileInfo> getFiles() {
		return files;
	}

	public void setFiles(List<BtFileInfo> files) {
		this.files = files;
	}

	public static void main(String[] args) {

	}
}
