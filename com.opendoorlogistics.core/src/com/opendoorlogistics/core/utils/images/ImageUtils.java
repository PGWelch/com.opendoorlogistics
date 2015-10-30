/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.images;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.opendoorlogistics.codefromweb.hqx.Hqx_2x;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

final public class ImageUtils {

	public static Dimension getSize(BufferedImage image){
		return new Dimension(image.getWidth(), image.getHeight());
	}
	
	/**
	 * Decode string to image
	 * 
	 * @param imageString
	 *            The string to decode
	 * @return decoded image
	 */
	public static BufferedImage base64StringToImage(String imageString) {
		BufferedImage ret = null;
		byte[] imageByte;
		try {
			BASE64Decoder decoder = new BASE64Decoder();
			imageByte = decoder.decodeBuffer(imageString);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			ret = ImageIO.read(bis);
			bis.close();
		} catch (Throwable e) {

		}
		return ret;
	}

	/**
	 * Encode image to string
	 * 
	 * @param image
	 *            The image to encode
	 * @param type
	 *            jpeg, bmp, ...
	 * @return encoded string
	 */
	public static String imageToBase64String(RenderedImage image, String type) {
		String ret = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, type, bos);
			byte[] bytes = bos.toByteArray();
			BASE64Encoder encoder = new BASE64Encoder();
			ret = encoder.encode(bytes);
			ret = ret.replace(System.lineSeparator(), "");
		} catch (IOException e) {
			throw new RuntimeException();
		}
		return ret;
	}

	/**
	 * Add the input images together
	 * 
	 * @param images
	 * @return
	 */
	public static BufferedImage addImages(BufferedImage... images) {
		if (images.length == 0) {
			throw new IllegalArgumentException();
		} else if (images.length == 1) {
			return images[0];
		}

		// general case
		BufferedImage ret = deepCopy(images[0]);
		Graphics2D g = ret.createGraphics();
		try {
			g.setClip(0, 0, ret.getWidth(), ret.getHeight());
			for (int i = 1; i < images.length; i++) {
				g.drawImage(images[i], 0, 0, null);
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			if (g != null) {
				g.dispose();
			}
		}

		return ret;
	}

	public static BufferedImage createBlankImage(int width, int height, Color color) {
		int type = BufferedImage.TYPE_INT_RGB;
		return createBlankImage(width, height, type, color);
	}

	public static BufferedImage createBlankImage(int width, int height, int type, Color color) {
		BufferedImage image = new BufferedImage(width, height, type);
		fillImage(image, color);
		return image;
	}

	public static byte[] toPNG(BufferedImage img) {
		return toByteArray(img, "png");
	}

	public static byte[] toBMP(BufferedImage img) {
		return toByteArray(img, "BMP");
	}

	
	private static byte[] toByteArray(BufferedImage img, String imageFileType) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, imageFileType, baos);
			return baos.toByteArray();
		} catch (Throwable e) {
			throw new RuntimeException();
		}
	}

	
	public static void toPNGFile(RenderedImage img, File file){
		try{
			ImageIO.write(img, "png",file);			
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static BufferedImage fromPNG(byte[] bytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			return ImageIO.read(bais);
		} catch (Throwable e) {
			throw new RuntimeException();
		}
	}

	public static void fillImage(BufferedImage image, Color color) {
		Graphics g = image.getGraphics();
		g.setColor(color);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.dispose();
	}

	public static void showImage(Image image) {
		createImageFrame(image).setVisible(true);
	}

	public static JFrame createImageFrame(Image image) {
		JFrame ret = new JFrame();
		ret.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ret.add(createImagePanel(image, null));
		ret.pack();
		return ret;
	}

	public static JInternalFrame createImageInternalFrame(Image image, Color backgroundColour) {
		JInternalFrame ret = new JInternalFrame();
		ret.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ret.add(new JScrollPane(createImagePanel(image, backgroundColour)));
		ret.pack();
		if (backgroundColour != null) {
			ret.setBackground(backgroundColour);
		}
		return ret;
	}

	public static JPanel createImagePanel(Image image, Color backColor) {
		JPanel panel = new JPanel();
		panel.add(new JLabel(new ImageIcon(image)));
		if (backColor != null) {
			panel.setBackground(backColor);
		}
		return panel;
	}

	/**
	 * Scale the image by the input fraction using a combination of Hqx 2x and bicubic interpolation. Hqx 2x applied multiplied times gives better
	 * looking results than Hqx 4x applied less times. Bicubic interpolation is used to do any scaling that's less than 2 times the original (or
	 * downscaling).
	 * 
	 * @param image
	 * @param fraction
	 * @return
	 */
	public static BufferedImage scaleImage(BufferedImage image, Dimension newSize) {
		class Scaler {
			BufferedImage scaleImage(BufferedImage image, Dimension newSize) {
				while (true) {
					double xFraction = Math.round(newSize.getWidth() / image.getWidth());
					double yFraction = Math.round(newSize.getHeight() / image.getHeight());
					if (xFraction > 1.5 && yFraction > 1.5) {
						// double using hqx_2x (repeated doubles give better quality than hqx_4x )
						image = hq2x(image);
					} else {
						// use bicubic interpolation to get to exact fraction
						image = bicubicScaling(image, newSize);
						return image;
					}
				}
			}

			private BufferedImage bicubicScaling(BufferedImage image, Dimension newSize) {

				BufferedImage newImage = ImageUtils.createBlankImage(newSize.width, newSize.height, Color.WHITE);
				Graphics2D g2 = (Graphics2D) newImage.createGraphics();
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.drawImage(image, 0, 0, newSize.width, newSize.height, null);
				g2.dispose();
				return newImage;
			}

			private BufferedImage hq2x(BufferedImage image) {
				int nw = image.getWidth() * 2;
				int nh = image.getHeight() * 2;
				BufferedImage newImage = ImageUtils.createBlankImage(nw, nh, Color.WHITE);

				// test upscaling...
				int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
				int[] upscaled = new int[nw * nh];
				Hqx_2x.hq2x_32_rb(rgb, upscaled, image.getWidth(), image.getHeight());
				newImage.setRGB(0, 0, newImage.getWidth(), newImage.getHeight(), upscaled, 0, newImage.getWidth());
				return newImage;

			}
		}
		return new Scaler().scaleImage(image, newSize);
	}

	/**
	 * See http://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
	 * 
	 * @param bi
	 * @return
	 */
	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	/**
	 * See http://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
	 * 
	 * @param img
	 * @return
	 */
	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}
	
	public static void main(String []args)throws Exception{
		BufferedImage img = ImageIO.read(new File("img.png"));
		img = scaleImage(img, new Dimension(img.getWidth()*2, img.getHeight()*2));
		ImageIO.write(img, "png",new File("upscaled.png"));	
	}
}
