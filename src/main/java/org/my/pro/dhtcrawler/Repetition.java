package org.my.pro.dhtcrawler;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class Repetition {

	
	public static void main(String[] args) {
		try {
			List<String> all = FileUtils.readLines(new File("/opt/magnet/data.mg"));
			
			HashSet<String> bts = new HashSet<>(all);
			
			System.out.println("总记录条数." + bts.size());

			
			FileUtils.writeLines(new File("/opt/magnet/data_result.mg"), bts);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
