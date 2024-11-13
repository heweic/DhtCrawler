package org.my.pro.dhtcrawler.mvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

	@GetMapping("/")
	public String login() {
		return "login.html";
	}
	
	@GetMapping("/login")
	public String dologin() {
		return "login.html";
	}
	
	@GetMapping("/index")
	public String index() {
		return "index.html";
	}

}
