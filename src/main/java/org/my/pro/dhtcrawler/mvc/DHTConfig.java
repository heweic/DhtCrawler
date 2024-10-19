package org.my.pro.dhtcrawler.mvc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dht")
public class DHTConfig {

	private boolean runbep09;

	private int port;
	private int num;
	public boolean isRunbep09() {
		return runbep09;
	}
	
	public int getPort() {
		return port == 0 ? 60000 : port;
	}
	
	public int getNum() {
		return num == 0 ? 1 : num;
	}
	public void setNum(int num) {
		this.num = num;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public void setRunbep09(boolean runbep09) {
		this.runbep09 = runbep09;
	}
	
	
}
