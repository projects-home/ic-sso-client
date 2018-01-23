package com.ifudata.ic.demo.filter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ifudata.ic.sso.client.filter.SSOClientConstants;
import com.ifudata.ic.sso.client.filter.SSOClientUser;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;


/**
 * 登陆用户信息过滤器
 * Date: 2017年2月9日 <br>
 * Copyright (c) 2017 ifudata.com <br>
 * 
 * @author wangyongxin
 */
public class AssembleUserInfoFilter implements Filter {
    private String[] ignor_suffix = {};
    private static final Logger LOG = LoggerFactory.getLogger(AssembleUserInfoFilter.class);

    /**
     * 初始化
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        String ignore_res = filterConfig.getInitParameter("ignore_suffix");
        if (!"".equals(ignore_res)) {
            this.ignor_suffix = filterConfig.getInitParameter("ignore_suffix").split(",");
        }
    }

    /**
     * doFilter接口
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (!shouldFilter(req)) {
            chain.doFilter(req, response);
            return;
        }
        HttpSession session = req.getSession();
        SSOClientUser user = (SSOClientUser) session.getAttribute(SSOClientConstants.USER_SESSION_KEY);
        if (user == null) {
            user = assembleUser(req);
            if(user!=null){
            	session.setAttribute(SSOClientConstants.USER_SESSION_KEY, user);
            	LOG.info("已封装的用户信息为：" + JSON.toJSONString(user));
            }
            else{
            	LOG.info("未获取到用户信息");
            }
            chain.doFilter(req, response);

        } else {
            chain.doFilter(req, response);
        }
    }

    @Override
    public void destroy() {

    }

    /**
     * 封装用户信息
     *
     * @param request
     * @return
     */
    private SSOClientUser assembleUser(HttpServletRequest request) {
    	SSOClientUser user = null;
        try {
            Principal principal = request.getUserPrincipal();
            if (principal != null) {
                user = new SSOClientUser();
                AttributePrincipal attributePrincipal = (AttributePrincipal) principal;
                Map<String, Object> attributes = attributePrincipal.getAttributes();
                Field[] fields = SSOClientUser.class.getDeclaredFields();
                for (Field field : fields) {
                    String value = (String) attributes.get(field.getName());
                    if (value != null) {
                        field.setAccessible(true);
                        if ("long".equalsIgnoreCase(field.getType().toString())) {
                            field.set(user, Long.parseLong(value));
                        } else {
                            field.set(user, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("封装用户信息失败", e);
        }
        return user;
    }

    /**
     * 判断白名单
     * @param req
     * @return
     * @author
     */
    private boolean shouldFilter(HttpServletRequest req) {
        if (ignor_suffix != null && ignor_suffix.length > 0) {
            String uri = req.getRequestURI().toLowerCase();
            for (String suffix : ignor_suffix) {
                if (uri.endsWith(suffix)) {
                    return false;
                }
            }
        }
        return true;
    }
}
