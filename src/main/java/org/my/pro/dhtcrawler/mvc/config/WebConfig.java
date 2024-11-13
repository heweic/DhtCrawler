package org.my.pro.dhtcrawler.mvc.config;

import org.my.pro.dhtcrawler.mvc.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	private LoginInterceptor loginInterceptor;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		
		registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**").excludePathPatterns("/api/auth").excludePathPatterns("/api/isLogin");
	}

	
	
	
}
