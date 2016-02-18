package com.gaozhe.toolbox.file;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

public class PathUtils {
	private static final File tempFolder = new File(new File(System.getProperty("java.io.tmpdir")), "coolchuan");

	public static File getTempFolder() {
		if (tempFolder.exists() && tempFolder.isFile()) {
			FileUtils.deleteQuietly(tempFolder);
		}

		if (!tempFolder.exists()) {
			tempFolder.mkdirs();
		}

		return tempFolder;
	}

	public static String getTempFolderPath() {
		return getTempFolder().getAbsolutePath() + File.separator + "image";
	}

	/**
	 * 生产验证码图片地址
	 * 
	 * @return
	 */
	public static String getVerfiyCodePath() {
		return getTempFolder().getAbsolutePath() + File.separator + "verifyCode" + File.separator + UUID.randomUUID()
				+ ".jpg";
	}

	/**
	 * 返回主站账号绑定验证码图片地址 /data/apk/verifyImgs/*.jpg
	 * 
	 * @return
	 */
	public static String getVerfiyCodePath4Web() {
		return "/data/apk/verifyImgs/" + UUID.randomUUID() + ".jpg";
	}

	public static void main(String[] args) {
		System.out.println(getTempFolder());
		System.out.println(getTempFolderPath());
	}
}
