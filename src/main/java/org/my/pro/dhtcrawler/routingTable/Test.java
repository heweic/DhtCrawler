package org.my.pro.dhtcrawler.routingTable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Test {
	
	
	public static void main(String[] args) {
		
		
		int num = 10;
		
		Set<Integer> randomSet = new LinkedHashSet<Integer>();
		for(int i = 0 ; i < 8 ; i ++) {
			randomIndex(randomSet, num);
		}
		for(Integer i : randomSet) {
			System.out.println(i);
		}
		
	}
	
	public static void randomIndex(Set<Integer> randomSet , int size) {
		
		int tmpRandom = new Random().nextInt(size);
		if(randomSet.contains(tmpRandom)) {
			randomIndex(randomSet, size);
		}else {
			randomSet.add(tmpRandom);
		}
	}

}
