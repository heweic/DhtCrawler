package org.my.pro.dhtcrawler.mvc;

import org.my.pro.dhtcrawler.mvc.respository.LuceneTorrentRespository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneConfig {

	@Bean
	LuceneTorrentRespository luceneTorrentRespository() {
		return LuceneTorrentRespository.getInstance();
	}
}
