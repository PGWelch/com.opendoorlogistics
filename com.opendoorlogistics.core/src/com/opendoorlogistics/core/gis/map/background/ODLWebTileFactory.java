/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.AbstractTileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.util.GeoUtil;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.utils.images.CompressedImage;
import com.opendoorlogistics.core.utils.images.CompressedImage.CompressedType;

class ODLWebTileFactory extends AbstractTileFactory  {
	private final FadeConfig fade;
	
	public ODLWebTileFactory(TileFactoryInfo info, FadeConfig fadeColor) {
		super(info);
		setThreadPoolSize(2);
		this.fade = fadeColor;
	}



	private BufferedImage multiAttemptDownload(String url) {
		int tries = 0;
		while (tries < 3) {
			tries++;
			try {
				URI uri = new URI(url);
				byte[] bimg = download(uri.toURL());
				BufferedImage img = null;
				if (bimg != null) {
					img = ImageIO.read(new ByteArrayInputStream(bimg));
				}

				if (img != null) {
					return img;
				}
			} catch (Throwable e) {
			}
		}
		return null;
	}

	private byte[] download(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("User-Agent", AppConstants.ORG_NAME + "/" + AppConstants.getAppVersion().toString());
		InputStream ins = connection.getInputStream();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] buf = new byte[256];
		while (true) {
			int n = ins.read(buf);
			if (n == -1)
				break;
			bout.write(buf, 0, n);
		}

		return bout.toByteArray();
	}

	@Override
	public BufferedImage renderSynchronously(int tpx, int tpy, int zoom) {

		// wrap the tiles horizontally --> mod the X with the max width and use that
		int tileX = tpx;
		int numTilesWide = (int) GeoUtil.getMapSize(zoom, getInfo()).getWidth();
		if (tileX < 0) {
			tileX = numTilesWide - (Math.abs(tileX) % numTilesWide);
		}

		tileX = tileX % numTilesWide;
		int tileY = tpy;

		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.SYNCHRONOUS_RETRIEVED_TILE_CACHE);

		// try getting from cache
		String url = getInfo().getTileUrl(tileX, tileY, zoom);
		CompressedImage img = (CompressedImage) cache.get(url);
		if (img != null) {
			return img.getBufferedImage();
		}

		// try downloading
		BufferedImage bimg = multiAttemptDownload(url);
		if(bimg==null){
			return null;
		}
		
		// turn image into argb so we can fade, then do fade
		BufferedImage workImg = greyscaleFade(bimg);
		
		// save to cache
		CompressedImage compressed = new CompressedImage(workImg, CompressedType.PNG);
		cache.put(url, compressed, compressed.getSizeBytes());
	

		return workImg;
	}

	/**
	 * @param bimg
	 * @return
	 */
	private BufferedImage greyscaleFade(BufferedImage bimg) {
		BufferedImage workImg = new BufferedImage(bimg.getWidth(), bimg.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g=null;
		try {
			g = workImg.createGraphics();
			g.setClip(0, 0, bimg.getWidth(), bimg.getHeight());
			g.drawImage(bimg, 0, 0, null);
			if(fade!=null){
				BackgroundMapUtils.renderFade(g, fade.getColour());				
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		finally{
			if(g!=null){
				g.dispose();
				g = null;
			}
		}
		
		if(fade!=null){
			workImg = BackgroundMapUtils.greyscale(workImg, fade.getGreyscale());			
		}

		return workImg;
	}
	
	
	@Override
	protected BufferedImage processLoadedImage(BufferedImage img){
		return greyscaleFade(img);
	}



	@Override
	public boolean isRenderedOffline() {
		return false;
	}

}
