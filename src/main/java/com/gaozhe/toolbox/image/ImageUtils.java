package com.gaozhe.toolbox.image;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import com.gaozhe.toolbox.file.PathUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.google.common.base.Strings;
import com.google.common.io.Files;

/**
 * use http://www.thebuzzmedia.com/software/imgscalr-java-image-scaling-library/
 * 
 * @author gaozhe
 *
 */
@SuppressWarnings("restriction")
public class ImageUtils {

	private ImageUtils() {
	}

	/**
	 * 如果原图大小和修改后相同，那么就不要变大小了
	 * 
	 * @param sourcePath
	 * @param destPath
	 * @param with
	 * @param length
	 * @throws IOException
	 */
	public static void resize(String sourcePath, String destPath, int with, int length) {
		checkArgument(!Strings.isNullOrEmpty(sourcePath), "sourcePath is empty");
		checkArgument(!Strings.isNullOrEmpty(destPath), "destPath is empty");

		File destFileParent = new File(new File(destPath).getParent());
		if (!destFileParent.exists()) {
			destFileParent.mkdirs();
		}
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(new File(sourcePath));
		} catch (IOException e1) {
			/* 部分图片模式CMYK不支持，这里使用javafx转换 */
			bufferedImage = SwingFXUtils
					.fromFXImage(ImageUtils.loadImage(new File(sourcePath).getAbsolutePath()), null);
		}
		try {
			if (bufferedImage == null || (bufferedImage.getWidth() == with && bufferedImage.getHeight() == length)) {
				File destFile = new File(destPath);
				Files.copy(new File(sourcePath), destFile);
				return;
			}else {
				/* 解决图片变红的问题 */
				BufferedImage target = new BufferedImage(with, length,
						bufferedImage.isAlphaPremultiplied() ? BufferedImage.TRANSLUCENT : BufferedImage.TYPE_INT_RGB);

				Graphics2D g = target.createGraphics();
				g.setComposite(AlphaComposite.Src);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				/* 解决图片背景变黑的问题,添加压缩平滑 */
				g.drawImage(bufferedImage.getScaledInstance(with, length, BufferedImage.SCALE_SMOOTH), 0, 0,
						Color.WHITE, null);
				g.dispose();
				BufferedImage dest = Scalr.resize(bufferedImage, Method.BALANCED, Mode.FIT_EXACT, with, length);
				ImageIO.write(target, FilenameUtils.getExtension(destPath), new File(destPath));
			}
		} catch (Throwable e) {
			throw new RuntimeException("source path " + sourcePath + " destpath is " + destPath, e);
		}
	}

	protected static String generateDestPath(String suffix) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(PathUtils.getTempFolder()).append(File.separator);
		sb.append(Thread.currentThread().getName()).append(File.separator);
		sb.append(UUID.randomUUID()).append(".");
		sb.append(suffix);

		String filePath = sb.toString();
		Files.createParentDirs(new File(filePath));
		return filePath;
	}

	/**
	 * 仅仅修改后缀
	 * 
	 * @param sourcePath
	 * @param destPath
	 * @param subfix
	 */
	public static void changeType(String sourcePath, String destPath, String subfix) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param imagePath
	 * @return 400*800 400 is width 800 is height
	 * @throws IOException
	 */
	public static String getWidthHeight(String imagePath) {

		BufferedImage bufferedImage;
		try {
			bufferedImage = ImageIO.read(new File(imagePath));
		} catch (IOException e) {
			/* 部分图片模式CMYK不支持，这里使用javafx转换 */
			bufferedImage = SwingFXUtils
					.fromFXImage(ImageUtils.loadImage(new File(imagePath).getAbsolutePath()), null);
		}
		return bufferedImage.getWidth() + "*" + bufferedImage.getHeight();

	}

	/**
	 * 切割图片
	 * 
	 * @param sourceFile
	 *            源文件
	 * @param targetDir
	 *            存储目录
	 * @param x
	 *            切割原点横坐标
	 * @param y
	 *            切割原点纵坐标
	 * @param width
	 *            切片宽度
	 * @param height
	 *            切片高度
	 * @return
	 * @throws Exception
	 */
	public static void cut(File sourceFile, String targetDir, int x, int y, int width, int height) throws Exception {
		BufferedImage source = ImageIO.read(sourceFile);
		int sWidth = source.getWidth(); // 图片宽度
		int sHeight = source.getHeight(); // 图片高度
		if (sWidth > width && sHeight > height) {
			File file = new File(targetDir);
			if (!file.exists()) { // 存储目录不存在，则创建目录
				file.mkdirs();
			}
			BufferedImage image = null;
			image = source.getSubimage(x, y, width, height);
			file = new File(targetDir);
			ImageIO.write(image, "JPEG", file);
		}
	}

	/**
	 * 图片格式转换
	 * 
	 * @param srcPath
	 *            源图片地址
	 * @param formatName
	 *            转换后格式eg: JPEG png
	 * @param destPath
	 *            转换后图片地址
	 * @throws Exception
	 */
	public static void convert(String srcPath, String formatName, String destPath) throws Exception {
		File f = new File(srcPath);
		File destFile = new File(destPath);
		if (!destFile.exists()) {
			destFile.mkdirs();
		}
		f.canRead();
		BufferedImage src = ImageIO.read(f);
		// create a blank, RGB, same width and height, and a white background
		BufferedImage newBufferedImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		newBufferedImage.createGraphics().drawImage(src, 0, 0, Color.WHITE, null);

		// write to jpeg file
		ImageIO.write(newBufferedImage, formatName, destFile);
	}

	/**
	 * 压缩jpg图片
	 * 
	 * @param sourcePath
	 * @param destPath
	 * @param size
	 */
	public static void reduceJpgSize(String sourcePath, String destPath, int size) {
		Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
		ImageWriter writer = iter.next();
		ImageWriteParam iwp = writer.getDefaultWriteParam();

		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwp.setCompressionQuality(0.5F);

		File file = new File(destPath);
		FileImageOutputStream output = null;
		try {
			output = new FileImageOutputStream(file);
			writer.setOutput(output);
			IIOImage image = new IIOImage(ImageIO.read(new File(sourcePath)), null, null);
			writer.write(null, image, iwp);
			writer.dispose();
		} catch (Exception e) {
			throw new RuntimeException("source path " + sourcePath + " destpath is " + destPath + " size is "
					+ size, e);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 压缩png
	 * 
	 * @param sourcePath
	 * @param destPath
	 * @param size
	 */
	public static void reducePngSize(String sourcePath, String destPath, int size) {
		BufferedImage src;
		try {
			src = ImageIO.read(new File(sourcePath));
			int width = src.getWidth();
			int height = src.getHeight();
			BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
			Graphics2D graphics2D = temp.createGraphics();
			graphics2D.drawImage(src, 0, 0, null);
			graphics2D.dispose();

			BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			graphics2D = dest.createGraphics();
			graphics2D.drawImage(temp, 0, 0, null);
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					int rgb = dest.getRGB(i, j);
					/* 将黑色背景变透明 */
					if (rgb == Color.BLACK.getRGB() && src.getRGB(i, j) != Color.BLACK.getRGB()) {
						dest.setRGB(i, j, 0);
					}
				}
			}
			graphics2D.dispose();
			ImageIO.write(dest, "png", new File(destPath));
		} catch (IOException e) {
			throw new RuntimeException("source path " + sourcePath + " destpath is " + destPath + " size is "
					+ size, e);
		}

	}

	public static Image loadImage(String fileName) {
		if (StringUtils.isEmpty(fileName))
			return null;

		Image result = null;
		FileInputStream image = null;
		try {
			image = new FileInputStream(fileName);
			result = new Image(image);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (image != null) {
				try {
					image.close();
				} catch (IOException e) {
				}
			}
		}

		return result;
	}
}
