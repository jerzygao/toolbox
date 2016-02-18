package com.gaozhe.toolbox.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class RegexUtils {
	private RegexUtils() {

	}

	public static String getMatchedString(String regex, String str, int flag) {
		Pattern p = Pattern.compile(regex);
		Matcher match = p.matcher(str);

		String result = "";
		if (match.find()) {
			result = match.group(flag);
		}

		return result;
	}

	public static boolean isMatchedString(String regex, String str) {
		Pattern p = Pattern.compile(regex);
		Matcher match = p.matcher(str);
		return match.matches();
	}

	/**
	 * 判断是否是手机号码
	 * 
	 * @param cellPhoneNum
	 * @return
	 */
	public static boolean isCellPhoneNum(String cellPhoneNum) {
		return RegexUtils.isMatchedString("^[1][3578][0-9]{9}$", cellPhoneNum);
	}

	/**
	 * 返回指定字符串是否为一个ip
	 * 
	 * @param ip
	 * @return
	 */
	public static boolean isIP(String ip) {
		if (StringUtils.isNotBlank(ip)) {
			String ipRegex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
			return RegexUtils.isMatchedString(ipRegex, ip);
		}
		return false;
	}
}
