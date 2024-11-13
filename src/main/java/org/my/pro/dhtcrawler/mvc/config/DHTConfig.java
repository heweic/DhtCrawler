package org.my.pro.dhtcrawler.mvc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dht")
public class DHTConfig {

	private int port;
	private int num;
	private boolean enabled;

	private String pass;

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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

}
