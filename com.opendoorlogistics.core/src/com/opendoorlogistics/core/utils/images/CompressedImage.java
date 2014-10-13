/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.images;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * Class holding an image which is compressed either as png or using LZ4. When using LZ4 currently only image type TYPE_INT_ARGB is supported. Image
 * is returned uncompressed.
 * 
 * @author Phil
 * 
 */
public class CompressedImage {
	private final int width;
	private final int height;
	private final CompressedType type;
	private final byte[] data;
	private final int uncompressedLength;

	/**
	 * Returns an estimate of the total amount of memory consumed by this class.
	 * 
	 * @return
	 */
	public int getSizeBytes() {
		return 8 + 4 + 4 + 4 + data.length + 4;
	}

	public enum CompressedType {
		/**
		 * Gives quickest compression / decompression but larger compressed size than png.
		 */
		LZ4,

		/**
		 * Slowest to compress / decompress but best compression
		 */
		PNG
	}

	public CompressedImage(BufferedImage img, CompressedType type) {
		this.width = img.getWidth();
		this.height = img.getHeight();
		this.type = type;

		switch (type) {
		case PNG:
			this.data = ImageUtils.toPNG(img);
			this.uncompressedLength = -1;
			break;

		case LZ4:
			byte[] uncompressed = getUncompressedBytes(img);
			this.uncompressedLength = uncompressed.length;
			this.data = LZ4Factory.unsafeInstance().fastCompressor().compress(uncompressed);
			break;

		default:
			throw new IllegalArgumentException();
		}

	}

	public Image get() {
		switch (type) {
		case PNG:
			return ImageUtils.fromPNG(data);

		case LZ4:
			LZ4FastDecompressor lz4Decompressor = LZ4Factory.unsafeInstance().fastDecompressor();
			byte[] uncompressed = new byte[uncompressedLength];
			lz4Decompressor.decompress(data, uncompressed);
			return uncompressImage(width, height, uncompressed);
			// byte[]decompressed = lz4Decompressor.d
			// case LZ4:
			// return LZ4Factory.unsafeInstance().fastCompressor().compress(getUncompressedBytes(img));
			//
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Get compressed image as a buffered image. This can have more overhead than just getting as an Image.
	 * 
	 * @return
	 */
	public BufferedImage getBufferedImage() {
		return ImageUtils.toBufferedImage(get());

	}

	private static byte[] getUncompressedBytes(BufferedImage img) {
		if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
			throw new RuntimeException("Image must be type TYPE_INT_ARGB to be compressed.");
		}

		DataBuffer uncastbuffer = img.getRaster().getDataBuffer();
		byte[] uncompressed = null;
		if (DataBufferInt.class.isInstance(uncastbuffer)) {
			int[] ints = ((DataBufferInt) uncastbuffer).getData();
			uncompressed = integersToBytes(ints);
		} else {
			uncompressed = ((DataBufferByte) uncastbuffer).getData();
		}
		return uncompressed;
	}

	private static byte[] integersToBytes(int[] values) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		for (int i = 0; i < values.length; ++i) {
			try {
				int v = values[i];
				dos.writeInt(v);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return baos.toByteArray();
	}

//	private static String printUnsignedByte(byte b) {
//		StringBuilder sb = new StringBuilder();
//		while (b > 0) {
//			sb.insert(0, b % 2 == 0 ? 0 : 1);
//			b >>= 1;
//		}
//		for (int i = 8 - sb.length(); i > 0; i--) {
//			sb.insert(0, 0);
//		}
//		return sb.toString();
//	}
//
//	private static String byteArrayToHex(byte[] a) {
//		StringBuilder sb = new StringBuilder();
//		for (byte b : a)
//			sb.append(String.format("%02x", b & 0xff));
//		return sb.toString();
//	}

	private static Image uncompressImage(final int width, final int height, byte[] bytes) {

		// ColorModel cm = new ColorModel(32) {
		//
		// @Override
		// public int getRed(int pixel) {
		// return (pixel & 0x00FF0000) >>> 16;
		// }
		//
		// @Override
		// public int getGreen(int pixel) {
		// return (pixel & 0x0000FF00) >>> 8;
		// }
		//
		// @Override
		// public int getBlue(int pixel) {
		// return pixel & 0x000000FF;
		// }
		//
		// @Override
		// public int getAlpha(int pixel) {
		// return (pixel & 0xFF000000) >>> 24;
		// }
		// };

		int[] data = new int[bytes.length / 4];
		for (int i = 0; i < data.length; i++) {
			// for (int j = 0; j < 4; j++) {
			// data[i] |= (bytes[i * 4 + j]) << ((3 - j) * 8);
			// }

			int j = i * 4;
			data[i] = ((bytes[j++] & 0xFF) << 24) | ((bytes[j++] & 0xFF) << 16) | ((bytes[j++] & 0xFF) << 8) | ((bytes[j++] & 0xFF) << 0);

			// int a = cm.getAlpha(data[i]);
			// int r = cm.getRed(data[i]);
			// int g = cm.getGreen(data[i]);
			// int b = cm.getBlue(data[i]);
			// System.out.print(" data=" + data[i] + " hex=" + Integer.toHexString(data[i]) + " ");
			// System.out.print(" - a=" + a + " r=" + r + " g=" + g + " b=" + b);
			// System.out.println();
		}

		MemoryImageSource mis = new MemoryImageSource(width, height, data, 0, width);

		// MemoryImageSource mis2=new MemoryImageSource(width,height, ColorModel.getRGBdefault(),
		// bytes,0, width*4);

		return Toolkit.getDefaultToolkit().createImage(mis);
	}

	public static void main(String[] args) {
		BufferedImage img = ImageUtils.createBlankImage(256, 256, BufferedImage.TYPE_INT_ARGB, Color.BLUE);
		CompressedImage compressed = new CompressedImage(img, CompressedType.LZ4);
		ImageUtils.showImage(compressed.get());

		// byte[] test = new byte[]{20,40,60,80};
		//
		// int[] data = new int[test.length / 4];
		// for (int i = 0; i < data.length; i++) {
		// for (int j = 0; j < 4; j++) {
		// byte original = test[i * 4 + j];
		// int bitshift = (3 - j) * 8;
		// int shifted = original << bitshift;
		//
		// data[i] |= shifted;
		// int mask = 0xFF <<bitshift;
		// int retrieved = data[i] & mask;
		// retrieved = retrieved >> bitshift;
		// System.out.println("original="+original
		// + " bitshift=" + bitshift
		// + " shifted="+shifted + " data=" + data[i]
		// + " mask=" + Integer.toHexString(mask)
		// + " retrieved=" + retrieved);
		// }
		// }

	}
}
