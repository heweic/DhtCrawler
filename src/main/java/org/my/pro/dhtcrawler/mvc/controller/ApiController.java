package org.my.pro.dhtcrawler.mvc.controller;

import org.my.pro.dhtcrawler.btdownload.BtInfo;
import org.my.pro.dhtcrawler.mvc.config.DHTConfig;
import org.my.pro.dhtcrawler.mvc.pojo.PageBean;
import org.my.pro.dhtcrawler.mvc.respository.LuceneTorrentRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class ApiController {

	@Autowired
	private LuceneTorrentRespository luceneTorrentRespository;
	
	@Autowired
	private DHTConfig config;

	/**
	 * 
	 * @param size
	 * @param page
	 * @param search
	 * @return
	 */
	@GetMapping("/search")
	public PageBean pageQuery(int page, String search) {
		if(page < 1) {
			page = 1;
		}
		if(page > 100) {
			page = 100;
		}
		//
		long time = System.currentTimeMillis();
		PageBean bean = luceneTorrentRespository.search(search, page, 10);
		bean.setTimeUse(System.currentTimeMillis() - time);
		return bean;
	}

	@GetMapping("/hash")
	@ResponseBody
	public BtInfo hash(String hash) {
		return luceneTorrentRespository.findByHash(hash);
	}
	
	@GetMapping("/isLogin")
	public boolean isLogin(HttpServletRequest req) {
		return req.getSession().getAttribute("_user") != null;
	}
	
	@GetMapping("/count")
	public long count() {
		return luceneTorrentRespository.count();
	}
	
	@PostMapping("/auth")
	public boolean auth(String pass , HttpServletRequest req) {
		if(null == pass) {
			return false;
		}
		boolean rs = pass.equals(config.getPass());
		//
		if(rs) {
			req.getSession().setAttribute("_user", true);
		}
		//
		return rs;
	}
}
