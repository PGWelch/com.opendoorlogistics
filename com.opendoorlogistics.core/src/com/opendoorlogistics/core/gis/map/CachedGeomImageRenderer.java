/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLGeomImpl.AtomicGeomType;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.images.ImageUtils;

public class CachedGeomImageRenderer implements ObjectRenderer{
	private final RecentImageCache geomCache = new RecentImageCache(RecentImageCache.ZipType.LZ4, 64*1024*1024);
	private final DatastoreRenderer renderer = new DatastoreRenderer();
	
	@Override
	public boolean renderObject(Graphics2D g, LatLongToScreen converter, DrawableObject obj, boolean isSelected, long renderFlags){
		if(!DatastoreRenderer.isVisibleAtZoom(obj, converter.getZoomForObjectFiltering())){
			return false;
		}
		
		// test if we have points
		boolean hasPoint = obj.getGeometry()==null;
		if(!hasPoint){
			hasPoint = obj.getGeometry().getAtomicGeomCount(AtomicGeomType.POINT)>0;
		}
		
		// if we have one or more points than draw as normal datastore renderer instead of caching
		if(hasPoint){
			// draw using normal datastore renderer
			if(obj.getGeometry()!=null){
				renderer.renderObject(g, converter, obj, isSelected,0);
			}else if (DatastoreRenderer.hasValidLatLong(obj)) {
				renderer.renderSymbol(g, obj, converter.getOnScreenPixelPosition(obj), isSelected);
			}
		}else{

			// do intersection check to see if we should draw
			Rectangle2D geomBounds =obj.getGeometry().getWorldBitmapBounds(converter);
			Rectangle2D wBBBounds = converter.getViewportWorldBitmapScreenPosition();
			if(wBBBounds==null || geomBounds.intersects(wBBBounds)==false){
				return false;
			}

			// get transformed geometry
			OnscreenGeometry cachedGeometry = DatastoreRenderer.getCachedGeometry(obj.getGeometry(), converter,true);
			if(cachedGeometry==null){
				return false;
			}
	
			// get the region to be drawn
			Rectangle2D drawRegion = wBBBounds.createIntersection(geomBounds);

			// just draw a filled rectangle if its really small
			if(cachedGeometry.isDrawFilledBounds() || (drawRegion.getWidth()<=1 && drawRegion.getHeight()<=1)){
				int minX = (int)Math.floor(drawRegion.getMinX());
				int minY = (int)Math.floor(drawRegion.getMinY());
				int maxX = (int)Math.ceil(drawRegion.getMaxX());
				int maxY = (int)Math.ceil(drawRegion.getMaxY());
				g.fillRect(minX,minY,maxX - minX + 1, maxY - minY+ 1);
			}else{		
			
				// check cache for this image in black and white
				BWCacheKey cacheKey = new BWCacheKey(wBBBounds, converter.getZoomHashmapKey(), obj, isSelected);
				Image bwImage=geomCache.get(cacheKey);
	
				// if no image available, create image it (for the intersection between the object and the tile)
				// and cache it.
				final Color renderCol = DatastoreRenderer.getRenderColour(obj,isSelected);
				if(bwImage==null){
					BufferedImage bufBwImage =  createBWImage(obj, cachedGeometry, renderCol, drawRegion);
					bwImage = bufBwImage;	
					geomCache.put(cacheKey, bufBwImage);
				}
				
				BufferedImage colourImage = createColouredImage(bwImage, renderCol);	
				
				// draw the colour image in the corrent place
				int x = (int) Math.round(drawRegion.getMinX() - wBBBounds.getMinX());
				int y = (int) Math.round(drawRegion.getMinY() - wBBBounds.getMinY());
				g.drawImage(colourImage, x, y, null);	
			}
	
		}
		return true;
	}

	private static class BWCacheKey{
		private Rectangle2D wBBBounds;
		private Object zoom;
		private ODLGeomImpl geom;
		private boolean drawOutline;
		private int pixelWidth;
		private boolean selected;
		
	//	static final int ESTIMATED_SIZE_IN_BYTES  = (8*4) + 8 + 8 + 4 + 4 + 4;
		
		BWCacheKey(Rectangle2D wBBBounds, Object zoom,DrawableObject obj, boolean isSelected) {
			this.wBBBounds = wBBBounds;
			this.zoom =zoom;
			this.geom = obj.getGeometry();
			this.drawOutline = obj.getDrawOutline()!=0;
			this.pixelWidth =(int) obj.getPixelWidth();
			this.selected = isSelected;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (drawOutline ? 1231 : 1237);
			result = prime * result + ((geom == null) ? 0 : geom.hashCode());
			result = prime * result + pixelWidth;
			result = prime * result + (selected ? 1231 : 1237);
			result = prime * result + ((wBBBounds == null) ? 0 : wBBBounds.hashCode());
			result = prime * result + ((zoom == null) ? 0 : zoom.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BWCacheKey other = (BWCacheKey) obj;
			if (drawOutline != other.drawOutline)
				return false;
			if (geom == null) {
				if (other.geom != null)
					return false;
			} else if (!geom.equals(other.geom))
				return false;
			if (pixelWidth != other.pixelWidth)
				return false;
			if (selected != other.selected)
				return false;
			if (wBBBounds == null) {
				if (other.wBBBounds != null)
					return false;
			} else if (!wBBBounds.equals(other.wBBBounds))
				return false;
			if (zoom == null) {
				if (other.zoom != null)
					return false;
			} else if (!zoom.equals(other.zoom))
				return false;
			return true;
		}
		
		
	}
	
	private BufferedImage createColouredImage(Image bwImage, final Color renderCol) {
		// create coloured image
		RGBImageFilter rgbFilter = new RGBImageFilter() {

			@Override
			public int filterRGB(int x, int y, int rgb) {
				Color current = new Color(rgb);
				int red = current.getRed();
				if (current.getAlpha() == 0 || red == 0) {
					return 0;
				}
				double factor = red / 255.0;
				int newRed = Colours.to0To255Int(renderCol.getRed() * factor);
				int newBlue = Colours.to0To255Int(renderCol.getBlue() * factor);
				int newGreen = Colours.to0To255Int(renderCol.getGreen() * factor);
				Color newCol = new Color(newRed, newGreen, newBlue, renderCol.getAlpha());
				return newCol.getRGB();
			}
		};
		
		// transform to colour and save in colour
		ImageProducer ip = new FilteredImageSource(bwImage.getSource(), rgbFilter);
		Image coloured = Toolkit.getDefaultToolkit().createImage(ip);
		BufferedImage colourImage = ImageUtils.toBufferedImage(coloured);
		return colourImage;
	}

	/**
	 * Create a black and white image of the geometry to be coloured later-on
	 * @param obj
	 * @param cachedGeometry
	 * @param renderCol
	 * @param drawRegion
	 * @return
	 */
	private BufferedImage createBWImage(DrawableObject obj, OnscreenGeometry cachedGeometry, final Color renderCol, Rectangle2D drawRegion) {
		// ensure image size is at least one
		int imgWidth = (int)Math.max(1, Math.ceil(drawRegion.getWidth()));
		int imgHeight = (int)Math.max(1, Math.ceil(drawRegion.getHeight()));
		
		// create black and white image
		BufferedImage buffBwImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gImage = buffBwImage.createGraphics();
		try {
			gImage.setClip(0, 0,imgWidth,imgHeight);
			Color bwCol = new Color(255, 255, 255, renderCol.getAlpha());								
			if (cachedGeometry.isDrawFilledBounds()) {
				gImage.setColor(bwCol);
				gImage.fillRect(0, 0, imgWidth, imgHeight);
			} else {
				DatastoreRenderer.renderOrHitTestJTSGeometry(gImage, obj, cachedGeometry.getJTSGeometry(), bwCol, DatastoreRenderer.getDefaultPolygonBorderColour(bwCol), drawRegion, null,0);
			}				
		} 
		finally{
			gImage.dispose();
		}
		return buffBwImage;
	}
}
