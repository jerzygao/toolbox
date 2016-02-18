package com.gaozhe.toolbox.net;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.KeyDataPair;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;


public class SimpleWebClient {
	private Logger logger = LoggerFactory.getLogger(SimpleWebClient.class);
	public static final String DEFAULT_CHARSET = "UTF-8";
	private static SimpleWebClient client = new SimpleWebClient();

	/**
	 * getWebClient()
	 * 方法返回的是一个ThreadLocal对象，特殊情况不希望使用ThreadLocal对象时，初始化此变量，默认为null。
	 * 暂时只用于主站重构账号绑定的处理 2015/12/16 add by gaozhe
	 */
	private WebClient webClient4Special = null;

	private boolean hasProxy = false;
	private String proxyIp;
	private int proxyPort;
	private String proxyUsername;
	private String proxyPassword;

	public synchronized void setGlobalProxy(String ip, int port) {
		setProxy(ip, port);

		// ui4j代理有用
		System.setProperty("htttps.proxyHost", proxyIp);
		System.setProperty("https.proxyPort", String.valueOf(port));
		System.setProperty("htttp.proxyHost", proxyIp);
		System.setProperty("http.proxyPort", String.valueOf(port));
	}

	public synchronized void setProxy(String ip, int port) {
		Preconditions.checkArgument(StringUtils.isNoneBlank(ip), "ip 不能为空");
		Preconditions.checkArgument(port != 0, "port 不能为空");
		this.proxyIp = ip;
		this.proxyPort = port;
		this.hasProxy = true;
	}

	public synchronized void setGlobalProxyUserPassword(String username, String password) {
		setProxyUserPassword(username, password);
		// ui4j代理有用
		System.setProperty("http.proxyUser", username);
		System.setProperty("http.proxyPassword", password);

	}

	public synchronized void setProxyUserPassword(String username, String password) {
		this.proxyUsername = username;
		this.proxyPassword = password;
		this.hasProxy = true;
	}

	public synchronized void clearGlobalProxy() {

		clearProxy();

		// ui4j代理有用
		System.getProperties().remove("htttps.proxyHost");
		System.getProperties().remove("https.proxyPort");
		System.getProperties().remove("htttp.proxyHost");
		System.getProperties().remove("http.proxyPort");
		System.getProperties().remove("http.proxyUser");
		System.getProperties().remove("http.proxyPassword");

	}

	public synchronized void clearProxy() {
		this.hasProxy = false;
		this.proxyIp = "";
		this.proxyPort = 0;
		this.proxyUsername = "";
		this.proxyPassword = "";
	}

	/**
	 * 使用httpClient必须使用此接口创建<br>
	 * 为了能够控制代理
	 * 
	 * @return
	 */
	public HttpClient createHttpClient() {
		HttpClientBuilder builder = HttpClients.custom();
		if (hasProxy) {
			HttpHost proxy = new HttpHost(proxyIp, proxyPort);
			builder.setProxy(proxy);
			if (StringUtils.isNotBlank(proxyUsername) && StringUtils.isNotBlank(proxyPassword)) {
				DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider();
				credentialsProvider.addCredentials(proxyUsername, proxyPassword);
				builder.setDefaultCredentialsProvider(credentialsProvider);
			}
		}
		return builder.build();
	}

	/**
	 * 静态方法创建httpClient但是不会使用代理配置
	 * 
	 * @return
	 */
	public static HttpClient createHttpClientWithoutProxy() {
		HttpClientBuilder builder = HttpClients.custom();
		return builder.build();
	}

	private ThreadLocal<WebClient> webClientThreadLocal = ThreadLocal.withInitial(() -> new WebClient(
			BrowserVersion.CHROME));

	private SimpleWebClient() {
	}

	/**
	 * 调用此方法时已经清理了所有cookie
	 * 
	 * @return
	 */
	public static SimpleWebClient getInstance() {
		client.setTimeout(120);
		client.disabelJavascript();
		client.clear();
		client.getWebClient().getOptions().setUseInsecureSSL(true);
		client.getWebClient().getOptions().setThrowExceptionOnFailingStatusCode(false);
		client.getWebClient().getOptions().setThrowExceptionOnScriptError(false);
		return client;
	}

	/**
	 * 此方法最好在构造函数中调用
	 * 
	 * @param version
	 */
	public void changeBrowseVersion(BrowserVersion version) {
		if (!(webClientThreadLocal.get().getBrowserVersion() == version)) {
			WebClient client = new WebClient(version);
			client.getOptions().setUseInsecureSSL(true);
			client.getOptions().setThrowExceptionOnFailingStatusCode(false);
			client.getOptions().setThrowExceptionOnScriptError(false);
			webClientThreadLocal.set(client);
		}
	}

	public String getPageXml(String url) {
		return ((DomNode) getPage(url)).asXml();
	}

	public String getPageXml(HttpMethod method, String url, ArrayListMultimap<String, String> params) {
		Page htmlPage = getPage(method, url, params);
		if (htmlPage.isHtmlPage()) {
			return ((HtmlPage) htmlPage).asXml();
		} else {
			return htmlPage.getWebResponse().getContentAsString();
		}
	}

	public Page getPage(HttpMethod method, String url, ArrayListMultimap<String, String> params) {

		return getPage(method, url, params, DEFAULT_CHARSET);
	}

	public Page getPage(HttpMethod method, String url, ArrayListMultimap<String, String> params,
			KeyDataPair... keyDataPairs) {

		return getPage(method, url, params, DEFAULT_CHARSET, keyDataPairs);
	}

	public Page getPage(HttpMethod method, String url, ArrayListMultimap<String, String> params, String charset){

		List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
		for (String key : params.keySet()) {
			List<String> values = params.get(key);
			for (String value : values) {
				NameValuePair nameValuePair = new NameValuePair(key, value);
				paramsList.add(nameValuePair);
			}
		}

		WebRequest request = null;
		try {
			request = new WebRequest(new URL(url), method);
		} catch (MalformedURLException e) {
			throw new HttpException("", e);
		}
		request.setCharset(charset);
		request.setRequestParameters(paramsList);
		Page responsePage;
		try {
			responsePage = getWebClient().getPage(request);
		} catch (Exception e) {
			throw new HttpException("", e);
		}

		return responsePage;
	}

	public Page getPage(HttpMethod method, String url, ArrayListMultimap<String, String> params, String charset,
			KeyDataPair... keyDataPairs) {
		List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
		for (String key : params.keySet()) {
			List<String> values = params.get(key);
			for (String value : values) {
				NameValuePair nameValuePair = new NameValuePair(key, value);
				paramsList.add(nameValuePair);
			}
		}

		if (keyDataPairs != null)
			paramsList.addAll(Arrays.asList(keyDataPairs));

		WebRequest request;
		try {
			request = new WebRequest(new URL(url), method);
		} catch (MalformedURLException e) {
			throw new HttpException(e);
		}
		request.setCharset(charset);
		request.setRequestParameters(paramsList);
		request.setEncodingType(FormEncodingType.MULTIPART);
		Page responsePage;
		try {
			responsePage = getWebClient().getPage(request);
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new HttpException(e);
		}

		return responsePage;
	}

	public Page getPage(WebRequest request, ArrayListMultimap<String, String> params, String charset,
			KeyDataPair... keyDataPairs) {
		List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
		for (String key : params.keySet()) {
			List<String> values = params.get(key);
			for (String value : values) {
				NameValuePair nameValuePair = new NameValuePair(key, value);
				paramsList.add(nameValuePair);
			}
		}

		if (keyDataPairs != null)
			paramsList.addAll(Arrays.asList(keyDataPairs));

		request.setCharset(charset);
		request.setRequestParameters(paramsList);
		request.setEncodingType(FormEncodingType.MULTIPART);
		Page responsePage;
		try {
			responsePage = getWebClient().getPage(request);
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new HttpException(e);
		}

		return responsePage;
	}

	public Page getPage(String url) {
		return this.getPage(url, "UTF8");
	}

	public Page getPage(WebRequest request) {
		try {
			return getWebClient().getPage(request);
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new HttpException(e);
		}
	}

	public Page getPage(String url, String encoding) {
		return this.getPage(HttpMethod.GET, url, ArrayListMultimap.create(), encoding);
	}

	public String getPageText(String url) {
		return ((DomNode) getPage(url)).asText();
	}

	public String getPageText(String url, ArrayListMultimap<String, String> param, HttpMethod method) {
		HtmlPage htmlPage = (HtmlPage) getPage(method, url, param);
		return htmlPage.asText();
	}

	/**
	 * 如果webClient4Special不为空 则返回webClient4Special实例，否则返回
	 * webClientThreadLocal.get()
	 * 
	 * @return
	 */
	public WebClient getWebClient() {
		WebClient client = null;
		if (this.webClient4Special != null) {
			client = this.webClient4Special;
		} else {
			client = webClientThreadLocal.get();
		}
		// client.getCredentialsProvider()
		if (hasProxy) {
			client.getOptions().getProxyConfig().setProxyHost(proxyIp);
			client.getOptions().getProxyConfig().setProxyPort(proxyPort);
			if (StringUtils.isNotBlank(proxyUsername) && StringUtils.isNotBlank(proxyPassword)) {
				DefaultCredentialsProvider credentialsProvider = (DefaultCredentialsProvider) client
						.getCredentialsProvider();
				credentialsProvider.addCredentials(proxyUsername, proxyPassword);
			}
		}
		return client;
	}

	public void downlodFile(String sourceUrl, String tergatPath) {
		Page page = getPage(sourceUrl, "iso-8859-1");
		try {
			FileUtils.copyInputStreamToFile(page.getWebResponse().getContentAsStream(), new File(tergatPath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void downlodFile(WebRequest request, String tergatPath) {
		request.setCharset("iso-8859-1");
		Page page = getPage(request);
		try {
			FileUtils.copyInputStreamToFile(page.getWebResponse().getContentAsStream(), new File(tergatPath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void enabelJavascript() {
		getWebClient().getOptions().setJavaScriptEnabled(true);
	}

	/**
	 * 获取所有domain 的cookies字符串 格式：domain:name=value;domain:name=value
	 * 
	 * @return 所有domain 的cookies字符串 格式：domain:name=value;domain:name=value
	 */
	public String getCookies() {
		Set<Cookie> cookies;
		cookies = getWebClient().getCookieManager().getCookies();
		StringBuilder sb = new StringBuilder();
		for (Cookie cookie : cookies) {
			sb.append(cookie.getDomain()).append(":").append(cookie.getName()).append("=").append(cookie.getValue())
					.append("; ");
		}
		return sb.toString();
	}

	/**
	 * 获取指定domain的cookies 格式name=value;name=value
	 * 
	 * @param hostNmae
	 *            domain名称
	 * @return 获取指定domain的cookies
	 */
	public String getCookies4Domain(String hostNmae) {
		Set<Cookie> cookies;
		cookies = getWebClient().getCookieManager().getCookies();
		StringBuilder sb = new StringBuilder();
		for (Cookie cookie : cookies) {
			if (StringUtils.equalsIgnoreCase(cookie.getDomain(), hostNmae)) {
				sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
			}
		}
		return sb.toString();
	}

	/**
	 * 获取指定domain的指定名称cookie的值
	 * 
	 * @param cookieName
	 *            cookie名称
	 * @param hostNmae
	 *            domain名称
	 * @return
	 */
	public String getCookieValue4Domain(String cookieName, String hostNmae) {
		Set<Cookie> cookies;
		cookies = getWebClient().getCookieManager().getCookies();
		for (Cookie cookie : cookies) {
			if (StringUtils.equalsIgnoreCase(cookie.getDomain(), hostNmae)
					&& StringUtils.equalsIgnoreCase(cookie.getName(), cookieName)) {
				return cookie.getValue();
			}
		}
		return "";
	}

	/**
	 *获取所有cookies没有domain标志 cookie格式：格式name=value;name=value
	 *
	 * @return cookie
	 */
	public String getCookiesNoDomain() {
		Set<Cookie> cookies;
		cookies = getWebClient().getCookieManager().getCookies();
		StringBuilder sb = new StringBuilder();
		for (Cookie cookie : cookies) {
			sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
		}
		return sb.toString();
	}

	public static class MyCookieManager {
		public boolean cookiesEnabled;
		public final Set<MyCookie> cookies = new LinkedHashSet<>();

		public boolean isCookiesEnabled() {
			return cookiesEnabled;
		}

		public void setCookiesEnabled(boolean cookiesEnabled) {
			this.cookiesEnabled = cookiesEnabled;
		}

		public Set<MyCookie> getCookies() {
			return cookies;
		}

	}

	public static class MyCookie {
		public String domain;
		public boolean httpOnly;
		public String name;
		public String path;
		public boolean secure;
		public String value;

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public boolean isHttpOnly() {
			return httpOnly;
		}

		public void setHttpOnly(boolean httpOnly) {
			this.httpOnly = httpOnly;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public void disabelJavascript() {
		getWebClient().getOptions().setJavaScriptEnabled(false);
		getWebClient().getOptions().setCssEnabled(false);
	}

	/**
	 * 
	 * @param seconds
	 *            unit second
	 */
	public void setTimeout(int seconds) {
		getWebClient().getOptions().setTimeout(seconds * 1000);
	}

	public void clear() {
		getWebClient().close();
		getWebClient().getCookieManager().clearCookies();
	}

	/**
	 * 设置cookies 格式：domain:name=value;domain:name=value
	 * 
	 * @param cookies
	 */
	// 用setCookieUseCookieManagerJson
	public void setCookies(String cookies) {
		Preconditions.checkArgument(!(StringUtils.isBlank(cookies) || "null".equals(cookies)),
				"cookie should not be null");

		CookieManager cookieManager = client.getWebClient().getCookieManager();
		List<String> lists = Lists.newArrayList(cookies.trim().split(";"));
		logger.debug("lists-----{}------", lists);
		for (String list : lists) {
			String[] cookieStr = StringUtils.splitPreserveAllTokens(list, "=", 2);
			String[] domainStr = StringUtils.splitPreserveAllTokens(cookieStr[0].trim(), ":", 2);
			Cookie cookie = new Cookie(domainStr[0].trim(), domainStr[1].trim(), cookieStr[1]);
			cookieManager.addCookie(cookie);
		}

	}

	/**
	 * 设置cookies,如果cookies中不包含指定domain的cookie则为其设值，
	 * 
	 * @param cookies
	 *            cookie字符串，格式：domain:name=value;domain:name=value
	 * @param domain
	 *            指定的domain
	 */
	public void setCookies(String cookies, String domain) {
		Preconditions.checkArgument(!(StringUtils.isBlank(cookies) || "null".equals(cookies)),
				"cookie should not be null");

		CookieManager cookieManager = client.getWebClient().getCookieManager();
		List<String> lists = Lists.newArrayList(cookies.trim().split(";"));
		logger.debug("lists-----{}------", lists);
		for (String list : lists) {
			String[] cookieStr = StringUtils.splitPreserveAllTokens(list, "=", 2);
			String[] domainStr = StringUtils.splitPreserveAllTokens(cookieStr[0].trim(), ":", 2);
			Cookie cookie = new Cookie(domainStr[0].trim(), domainStr[1].trim(), cookieStr[1]);
			cookieManager.addCookie(cookie);
			if (!StringUtils.equalsIgnoreCase(domain, domainStr[0])) {
				Cookie cookie4Domain = new Cookie(domain, domainStr[1].trim(), cookieStr[1]);
				if (!cookieManager.getCookies().contains(cookie4Domain)) {
					cookieManager.addCookie(cookie4Domain);
				}
			}
		}

	}

	/**
	 * 为单一domain设置cookie 格式：name=value;name=value
	 * 
	 * @param cookies
	 * @param hostName
	 */
	public void setCookies4Domain(String cookies, String hostName) {
		Preconditions.checkArgument(!(StringUtils.isBlank(cookies) || "null".equals(cookies)),
				"cookie should not be null");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(hostName), "hostName should not be null");

		CookieManager cookieManager = client.getWebClient().getCookieManager();
		List<String> lists = Lists.newArrayList(cookies.trim().split(";"));
		logger.debug("lists-----{}------", lists);
		for (String list : lists) {
			String[] strs = StringUtils.splitPreserveAllTokens(list, "=", 2);
			Cookie cookie = new Cookie(hostName, strs[0].trim(), strs[1]);
			cookieManager.addCookie(cookie);
		}

	}

	public void setCookiesFromList(List<Cookie> cookieList) {
		CookieManager cookieManager = client.getWebClient().getCookieManager();
		for (Cookie cookie : cookieList) {
			cookieManager.addCookie(cookie);
		}
	}

	public Page getPage(String url, JSONObject jsonObject, Map<String, String> requestHeaders) {
		return getPage(url, jsonObject, requestHeaders, DEFAULT_CHARSET);
	}

	public Page getPage(String url, JSONObject jsonObject, Map<String, String> requestHeaders, String charset) {
		try {
			WebRequest request = new WebRequest(new URL(url), HttpMethod.POST);
			request.setRequestBody(JSONObject.toJSONString(jsonObject, SerializerFeature.WriteMapNullValue));
			request.getAdditionalHeaders().putAll(requestHeaders);
			request.setCharset(charset);
			return getWebClient().getPage(request);
		} catch (RuntimeException | IOException e) {
			throw new HttpException(e);
		}
	}

	/**
	 * 是否使用代理
	 * 
	 * @return
	 */
	public boolean isHasProxy() {
		return this.hasProxy;
	}

	public void setUseInsecureSSL(boolean isUseInsecureSSL) {
		getWebClient().getOptions().setUseInsecureSSL(isUseInsecureSSL);
	}

	/**
	 * getWebClient()
	 * 方法返回的是一个ThreadLocal对象，特殊情况不希望使用ThreadLocal对象时，初始化此变量，默认为null。
	 * 暂时只用于主站重构账号绑定的处理 2015/12/16 add by gaozhe
	 * 
	 * @param webClient4Special
	 */
	public void setSpecialWebClient(WebClient webClient4Special) {
		this.webClient4Special = webClient4Special;
	}

	/**
	 * 关闭并释放htmlunit资源
	 */
	public void close() {
		getWebClient().getWebWindows().forEach((window) -> window.getJobManager().removeAllJobs());
		getWebClient().getCache().clear();
		getWebClient().close();
		if (this.webClient4Special != null) {
			this.webClient4Special = null;
		}
	}

}
