package com.gaozhe.toolbox.net;

import java.lang.reflect.Field;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gaozhe.toolbox.text.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.util.Cookie;
import com.sun.webkit.network.CookieManager;
import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.Page;
import com.ui4j.api.util.Ui4jException;
import com.ui4j.webkit.WebKitBrowserProvider;
import com.ui4j.webkit.WebKitIsolatedCookieHandler;

/**
 * UI4J常用工具类
 * 
 * @author gaozhe
 * @date 2015年12月5日
 */
@SuppressWarnings("restriction")
public class Ui4jUtils {

	/**
	 * 使用ui4j的地方需使用此锁保证顺序执行，以免并发
	 */
	public static final Object UI4JLOCK = new Object();

	private static Logger logger = LoggerFactory.getLogger(Ui4jUtils.class);

	public static void processCookie4Client(Page page, String hostName, SimpleWebClient client) {
		String cookieStr = (String) page.executeScript("document.cookie");
		String cookie = StringUtils.replace(cookieStr, " ", "");
		logger.info("Ui4jUtils处理cookie，获取cookie为:{},hostName:{}", cookie, hostName);
		client.setCookies4Domain(cookie, hostName);
	}

	@SuppressWarnings("rawtypes")
	public static void processAllCookie4Client(SimpleWebClient client) {
		CookieHandler cookieHandler = CookieHandler.getDefault();
		if (cookieHandler == null) {
			return;
		}
		if (cookieHandler instanceof CookieManager) {
			CookieManager manager = (CookieManager) cookieHandler;
			Field fieldStore;
			try {
				fieldStore = manager.getClass().getDeclaredField("store");
				fieldStore.setAccessible(true);
				Object store = fieldStore.get(manager);
				Field fieldBuckets = store.getClass().getDeclaredField("buckets");
				fieldBuckets.setAccessible(true);
				Map buckets = (Map) fieldBuckets.get(store);
				Set cookieSet = buckets.entrySet();
				Object[] cookieStrs = cookieSet.toArray();
				List<Cookie> cookieList = new ArrayList<>();
				for (Object cookieStr : cookieStrs) {
					String[] cookikes = StringUtils.substringsBetween(cookieStr.toString(), "[", "]");
					for (String cookieInfo : cookikes) {
						Cookie cookie = coverStr2Cookie(cookieInfo);
						if (!cookieList.contains(cookie)) {
							cookieList.add(cookie);
						}
					}
				}
				client.setCookiesFromList(cookieList);

			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new Ui4jException(e);
			}
		}

	}

	/**
	 * 将字符串转化为htmlunit 的cookie对象
	 * 
	 * @param cookieStr
	 * @return
	 */
	public static Cookie coverStr2Cookie(String cookieStr) {
		String domain = RegexUtils.getMatchedString("domain=(.*?),", cookieStr, 1);
		String name = RegexUtils.getMatchedString("name=(.*?),", cookieStr, 1);
		String value = RegexUtils.getMatchedString("value=(.*?),", cookieStr, 1);
		Cookie cookie = new Cookie(domain, name, value);
		return cookie;
	}

	public static void processCookie4WebKit(Page page, String cookie) {
		String setCookieJS = "document.cookie=\"" + cookie + "\";";
		page.executeScript(setCookieJS);
	}

	/**
	 * 清除cookies关闭页面，在使用完ui4j最后一步调用
	 * 
	 * @param browser
	 * @param page
	 */
	public static void clearUi4j(BrowserEngine browser, Page page) {
		// browser.clearCookies();

		CookieHandler cookieHandler = CookieHandler.getDefault();
		if (cookieHandler == null) {
			return;
		}
		if (cookieHandler instanceof CookieManager) {
			CookieManager manager = (CookieManager) cookieHandler;
			Field fieldStore;
			try {
				fieldStore = manager.getClass().getDeclaredField("store");
				fieldStore.setAccessible(true);
				Object store = fieldStore.get(manager);
				Field fieldBuckets = store.getClass().getDeclaredField("buckets");
				fieldBuckets.setAccessible(true);
				Map buckets = (Map) fieldBuckets.get(store);
				Set keySet = buckets.keySet();
				List keys2Remove = new ArrayList();
				for (Object key : keySet) {
					String keyStr = String.valueOf(key);
					if (!isCoolchuanCookie(keyStr)) {
						keys2Remove.add(key);
					}
				}
				for (Object key : keys2Remove) {
					buckets.remove(key);
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new Ui4jException(e);
			}
		} else if (cookieHandler instanceof WebKitIsolatedCookieHandler) {
			((WebKitIsolatedCookieHandler) cookieHandler).clear();
		}

		page.close();
	}

	public static BrowserEngine getBrowser() {
		WebKitBrowserProvider provider = new WebKitBrowserProvider();
		logger.info("Ui4jUtils开始创建browser");
		BrowserEngine browser = null;
		try {
			browser = provider.create();
		} catch (Throwable e) {
			logger.info("Ui4jUtils创建browser异常", e);
		}
		logger.info("Ui4jUtils创建browser创建完毕");
		return browser;
	}

	private static boolean isCoolchuanCookie(String cookieDomainStr) {
		if (StringUtils.containsIgnoreCase(cookieDomainStr, "coolchuan")
				|| StringUtils.containsIgnoreCase(cookieDomainStr, "kuchuan")) {
			return true;
		}
		return false;
	}

}
