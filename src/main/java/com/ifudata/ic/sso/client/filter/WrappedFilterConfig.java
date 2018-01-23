package com.ifudata.ic.sso.client.filter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FilterConfig
 * Date: 2017年2月9日 <br>
 * Copyright (c) 2017 ifudata.com <br>
 * 
 * @author
 */
public class WrappedFilterConfig implements FilterConfig {
	private static final Logger LOG = LoggerFactory.getLogger(WrappedFilterConfig.class);
	private ThreadLocal<Map<String, String>> params=new ThreadLocal<Map<String, String>>();
	
	private FilterConfig filterConfig;
	
	
	public WrappedFilterConfig(FilterConfig filterConfig){
		this.filterConfig = filterConfig;
		//initParams();
	}
	/**
	 * 根据内外网自动调整IP参数
	 * @param currentFilterConfig
	 * @param params
	 */
	public WrappedFilterConfig(FilterConfig currentFilterConfig, ThreadLocal<Map<String, String>> params) {
		this.filterConfig = currentFilterConfig;
		this.params=params;
	}

	@Override
	public String getFilterName() {
		return filterConfig.getFilterName();
	}

	/**
	 * 获取初始化参数
	 */
	@Override
	public String getInitParameter(String key) {
		String value = filterConfig.getInitParameter(key);
		if(value!=null){
			return value;
		}
		return StringUtils.isBlank(System.getenv().get(key))?params.get().get(key):System.getenv().get(key);
	}

	/**
	 * 获取初始化枚举
	 */
	@Override
	public Enumeration<String> getInitParameterNames() {
		final Iterator<String> iterator = params.get().keySet().iterator();
		return new Enumeration<String>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public String nextElement() {
				return iterator.next();
			}
		};
	}

	/**
	 * 获取上下文
	 */
	@Override
	public ServletContext getServletContext() {
		return filterConfig.getServletContext();
	}
	
	/**
	 * 初始化参数
	 * @param httpRequest
	 * @author
	 */
	private void initParams(HttpServletRequest httpRequest){
		String serverName=httpRequest.getServerName();
		boolean innerFlag=IPHelper.isInnerIP(serverName,SSOClientUtil.getInnerDomains());
		//initParams();
		try {
			if(innerFlag){
				//若是内网访问，则单点登录走内网
				Iterator iter = params.get().entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
						String key = entry.getKey();
						if(null!=key&&!"".equals(key)&&key.endsWith("_Inner")){
							String val = entry.getValue();
							String keyNormal=key.replace("_Inner", "");
							params.get().put(keyNormal, val);
						}
						
						
					}
			}
		} catch (Exception e) {
			LOG.error("init WrappedFilterConfig failure",e);
		}
	}
	
	/**
	 * 初始化参数
	 * 
	 * @author
	 */
	private void initParams(){
		//jvm里如果有map，则直接返回
		if(!params.get().isEmpty()){
			return;
		}
		//jvm里如果没有map，则读取sso.properties文件
		else{
			//同步加锁
			synchronized (WrappedFilterConfig.class) {
				//加锁后，还没有的话，则读取sso.properties文件，否则说明其他线程已加载，无需重复加载
				if(params.get().isEmpty()){
					Properties properties = new Properties();		
					try {
						ClassLoader loader = WrappedFilterConfig.class.getClassLoader();
						properties.load(loader.getResourceAsStream("sso.properties"));
						for (Object obj : properties.keySet()) {
							String key = (String) obj;
							if(key!=null){
								params.get().put(key.trim(), properties.getProperty(key).trim());
							}
						}
					} catch (IOException e) {
						LOG.error("init WrappedFilterConfig failure",e);
					}
						
					
				}
			}// end synchronized
			
		}//end else
	}
	
	/**
	 * 打印参数
	 * 
	 * @author
	 */
	private void printParams(){
		Iterator iter = this.params.get().entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
				String key = entry.getKey();
				if(null!=key&&!"".equals(key)){
					String val = entry.getValue();
					System.out.println("WrappedFilterConfig key【"+key+"】="+val);
				}
				
			}
	}
	
}
