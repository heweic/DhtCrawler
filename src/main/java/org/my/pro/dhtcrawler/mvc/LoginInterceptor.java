package org.my.pro.dhtcrawler.mvc;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor{

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		
		Object user = request.getSession().getAttribute("_user");
		if(null == user) {
			response.sendRedirect("/login");
			return false;
		}
		
		return true;
	}

	
}
