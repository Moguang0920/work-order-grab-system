package com.opsorder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
@Configuration
public class SaTokenConfig implements WebMvcConfigurer{
	
	/**
	 * 注册 [Sa-Token全局过滤器] 
	 */
	@Bean
	public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
		
        		// 指定 拦截路由 与 放行路由
        		.addInclude("/**").addExclude("/favicon.ico")    /* 排除掉 /favicon.ico */
				
        		// 认证函数: 每次请求执行 
        		.setAuth(obj -> {
					System.out.println("---------- 进入Sa-Token全局认证 -----------");
					
					// 登录认证 -- 拦截所有路由，放行注册、登录与接口文档
					// 注意：原来这里写的是 /user/doLogin，项目里并不存在该接口，
					// 导致连 /user/register、/user/login/email、/user/login/phone 都被拦，全部返回 401
					SaRouter.match("/**")
					        .notMatch("/user/register", "/user/login/**",
						"/v3/api-docs", "/v3/api-docs/**",
						"/swagger-ui.html", "/swagger-ui/**", "/doc.html")
					        .check(() -> StpUtil.checkLogin());
					
					// 更多拦截处理方式，请参考“路由拦截式鉴权”章节 */
        		})
				
        		// 异常处理函数：每次认证函数发生异常时执行此函数 
        		.setError(e -> {
					System.out.println("---------- 进入Sa-Token异常处理 -----------");
                    SaHolder.getResponse().setStatus(401);
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    // 注意：SaServletFilter 会把这里的返回值原样输出，只有 String 才能正确落地；
                    // 直接返回 Result 对象会输出成 com.opsorder.common.Result@xxxx。
                    // 所以手动序列化成 JSON，保证与其他接口的结构一致。
                    try {
                        return new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(com.opsorder.common.Result.error(401,
                                        "未登录或登录已过期：" + e.getMessage()));
                    } catch (Exception ex) {
                        return "{\"statusCode\":401,\"data\":null,\"success\":false,\"message\":\"未登录或登录已过期\"}";
                    }
        		})
				
        		// 前置函数：在每次认证函数之前执行（BeforeAuth 不受 includeList 与 excludeList 的限制，所有请求都会进入）
        		.setBeforeAuth(r -> {
        			// ---------- 设置一些安全响应头 ----------
        			SaHolder.getResponse()
        			// 服务器名称 
        			.setServer("sa-server")
        			// 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以 
        			.setHeader("X-Frame-Options", "SAMEORIGIN")
        			// 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面 
        			.setHeader("X-XSS-Protection", "1; mode=block")
        			// 禁用浏览器内容嗅探 
        			.setHeader("X-Content-Type-Options", "nosniff")
        			;
        		})
        		;
	}
    	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册 Sa-Token 拦截器，打开注解式鉴权功能 
		registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");	
	}
	
}