/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.background;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.jdesktop.swingx.mapviewer.AbstractTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.util.GeoUtil;

import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.utils.images.CompressedImage;
import com.opendoorlogistics.core.utils.images.CompressedImage.CompressedType;

class ODLWebTileFactory extends AbstractTileFactory implements ODLSynchronousTileFactory {

	public ODLWebTileFactory(TileFactoryInfo info) {
		super(info);
		setThreadPoolSize(2);			
	}

	@Override
	public void close() throws IOException {
		dispose();
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

		String url = getInfo().getTileUrl(tileX, tileY, zoom);
		CompressedImage img = (CompressedImage) cache.get(url);
		if (img != null) {
			return img.getBufferedImage();
		}

		BufferedImage bimg = multiAttemptDownload(url);
		if (bimg != null) {
			img = new CompressedImage(bimg, CompressedType.PNG);
			cache.put(url, img, img.getSizeBytes());
		}

		return bimg;
	}

}
