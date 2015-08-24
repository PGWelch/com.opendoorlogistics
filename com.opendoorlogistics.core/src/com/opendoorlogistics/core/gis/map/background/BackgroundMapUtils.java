/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.background;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.images.ImageUtils;

class BackgroundMapUtils {
	private BackgroundMapUtils() {

	}

	// static void renderFade(Graphics2D g) {
	// renderFade(g, new Color(255, 255, 255, 100));
	// }

	static void renderFade(Graphics2D g, Color fadeColour) {
		if (fadeColour != null && fadeColour.getAlpha() > 0) {
			Rectangle bounds = g.getClipBounds();
			if(bounds!=null){
				g.setColor(fadeColour);
				g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);				
			}
		}
	}
	
	static BufferedImage renderFade(BufferedImage img, Color fadeColour){
		// copy image so we don't modify the original
		img = ImageUtils.deepCopy(img);
		Graphics2D g=null;
		try {
			g = (Graphics2D)img.getGraphics();
			g.setClip(0, 0, img.getWidth(), img.getHeight());
			renderFade(g, fadeColour);
		} catch (Exception e) {
		}finally{
			if(g!=null){
				g.dispose();
			}
		}
		return img;
	}

	static BufferedImage fadeWithGreyscale(BufferedImage img, FadeConfig config){
		if(config==null){
			return img;
		}
		return greyscale(renderFade(img, config.getColour()), config.getGreyscale());
	}
	
	static BufferedImage greyscale(BufferedImage img, double greyFactor){
		if(greyFactor<=0 || img==null){
			return img;
		}
		ImageProducer ip = new FilteredImageSource(img.getSource(), new GreyFilter(greyFactor));
		Image coloured = Toolkit.getDefaultToolkit().createImage(ip);
		return ImageUtils.toBufferedImage(coloured);
	}
	
	static class GreyFilter extends RGBImageFilter {
		private final double greyFactor;
		
		public GreyFilter(double greyFactor) {
			this.greyFactor = greyFactor;
		}

		public int filterRGB(int x, int y, int argb) {
			Color original = new Color(argb);
			int av = (original.getRed() + original.getGreen() + original.getBlue()) / 3;
			Color newCol = Colours.lerp(original, new Color(av, av, av, original.getAlpha()), greyFactor);
			return newCol.getRGB();
		}
	}

}
