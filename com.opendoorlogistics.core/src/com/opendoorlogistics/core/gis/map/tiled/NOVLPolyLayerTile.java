/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.tiled;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.Kernel;
import java.awt.image.RGBImageFilter;

import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.gis.map.DatastoreRenderer;
import com.opendoorlogistics.core.gis.map.ObjectRenderer;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectDecorator;
import com.opendoorlogistics.core.utils.images.CompressedImage;
import com.opendoorlogistics.core.utils.images.CompressedImage.CompressedType;
import com.opendoorlogistics.core.utils.images.ImageUtils;

/**
 * Class to handle optimised rendering of a non-overlapping polygon layer
 * @author Phil
 *
 */
public class NOVLPolyLayerTile {
	private final DrawableObjectLayer layer;
	private final TObjectIntHashMap<ODLGeom> ids;
	private final CompressedImage filled;
	private final CompressedImage borders;
	private final int h;
	private final int w;

	long getSizeInBytes(){
		return ids.size() * 8 + filled.getSizeBytes() + borders.getSizeBytes();
	}
	
	private static class ColourDecorator extends DrawableObjectDecorator{
		private Color color;
		private long drawOutline; 
		
		public ColourDecorator(DrawableObject decorated, Color color, long drawOutline) {
			super(decorated);
			this.color = color;
			this.drawOutline = drawOutline;
		}
		
		@Override
		public Color getColour() {
			return color;
		}

		@Override
		public String getColourKey() {
			return null;
		}
		
		@Override
		public double getOpaque() {
			return 1;
		}
		
		@Override
		public long getDrawOutline() {
			return drawOutline;
		}


		@Override
		public long getMinZoom() {
			// Ensure always visible as visibility filtering is done afterwards
			return Long.MIN_VALUE;
		}

		@Override
		public long getMaxZoom() {
			return Long.MAX_VALUE;
		}
	}

	DrawableObjectLayer getLayer(){
		return layer;
	}


	NOVLPolyLayerTile(LatLongToScreen converter,ObjectRenderer renderer,DrawableObjectLayer layer){
		this.layer =layer;
	
		// assign a unique id to each geom
		int nextId =1;
		ids = new TObjectIntHashMap<>(layer.size(), Constants.DEFAULT_LOAD_FACTOR, -1);
		for(DrawableObject obj:layer){
			ODLGeom geom = obj.getGeometry();
			if(geom==null){
				continue;
			}
			if(ids.containsKey(geom)){
				continue;
			}
			
			ids.put(geom, nextId++);
		}
		
		Rectangle2D viewable = converter.getViewportWorldBitmapScreenPosition();
		w = (int)viewable.getWidth();
		h = (int)viewable.getHeight();
	
		// draw all objects on the tile with different colour for each based on id and not showing borders
		filled = createDrawnImage(converter, renderer,false);

		// now do the same for the borders (doing separately allows coping with opaque)
		borders = createDrawnImage(converter, renderer,true);
		
	}



	/**
	 * @param converter
	 * @param renderer
	 * @param nonOverlappingPolys
	 * @param ret
	 * @return
	 */
	private CompressedImage createDrawnImage(LatLongToScreen converter, ObjectRenderer renderer,boolean isBorders) {
//	private BufferedImage createDrawnImage(LatLongToScreen converter, ObjectRenderer renderer,boolean bordersOnly) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();

		try {
			g.setClip(0, 0, w, h);
			for(DrawableObject obj:layer){
				if(obj.getGeometry()!=null){
					int id = ids.get(obj.getGeometry());			
					Color dummyIdColour = createColourId(id);
					g.setColor(dummyIdColour);			        
					renderer.renderObject(g, converter, new ColourDecorator(obj, dummyIdColour, isBorders?1:0), false,
							isBorders?RenderProperties.RENDER_BORDERS_ONLY | RenderProperties.THIN_POLYGON_BORDERS:RenderProperties.SKIP_BORDER_RENDERING);
				}
			}
		} finally {
			g.dispose();
		}
		
		return new CompressedImage(img, CompressedType.LZ4);
	//	return img;
	}


	/**
	 * @param id
	 * @return
	 */
	private static Color createColourId(int id) {
//		int ia = (0xFF000000) >> 24;
		int ir = (id & 0x00FF0000) >> 16;
		int ig = (id & 0x0000FF00) >> 8;
		int ib = id & 0x000000FF;

		
//        value = ((a & 0xFF) << 24) |
//                ((r & 0xFF) << 16) |
//                ((g & 0xFF) << 8)  |
//                ((b & 0xFF) << 0);

		return new Color(ir,ig,ib,255);
	}
	
	//static final HashSet<Integer> distinctRgbs = new HashSet<>();
	
	boolean draw(Iterable<DrawableObject> polys, Graphics2D g2d,TLongHashSet selectedIds, int x, int y, int zoom){
		
		// get colour state of each geom and put this in a map using our ids
		final TIntObjectHashMap< Color> colours = new TIntObjectHashMap<>();
		for(DrawableObject obj :polys){
			if(obj.getGeometry()==null){
				continue;
			}
			int id = ids.get(obj.getGeometry());
			if(id==-1){
				// unknown id.. rendering went wrong
				return false;
			}
			
			// Check visible at zoom, get colour if so
			if(DatastoreRenderer.isVisibleAtZoom(obj, zoom)){
				Color col = DatastoreRenderer.getRenderColour(obj, selectedIds.contains(obj.getGlobalRowId()));
				colours.put(id, col);
				
			}
		}
		
		BufferedImage baseImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gTmp = baseImg.createGraphics();

		try {
			BufferedImage img =null;
			img = applyColourFiltering(filled.get(), createRGBFilter(colours,false));
			gTmp.drawImage(img, x, y,null);
			
			// draw borders
			img = applyColourFiltering(borders.get(),  createRGBFilter(colours,true));
			
			// soften borders with a blur
	        float[] blurKernel = {
		            0, 1 / 8f, 0,
		            1/8f, 1 / 2f, 1/8f,
		           0, 1 / 8f, 0f
		        };
	        
		    BufferedImageOp blur = new ConvolveOp(new Kernel(3, 3, blurKernel), ConvolveOp.EDGE_NO_OP, null);
		    BufferedImage blurredBorders = blur.filter(img, new BufferedImage(baseImg.getWidth(),
		                baseImg.getHeight(), baseImg.getType()));
		    
		    // draw blurred borders first
			gTmp.drawImage(blurredBorders, x, y,null);
			
			// then draw sharper ones on-top (like anti-aliasing)
			gTmp.drawImage(img, x, y,null);
		} finally {
			gTmp.dispose();
		}
		

//	      databuf = new BufferedImage(mshi.getWidth(null),
//	                mshi.getHeight(null),
//	                BufferedImage.TYPE_INT_BGR);
//
//	        Graphics g = databuf.getGraphics();
//	        g.drawImage(mshi, 455, 255, null);

//	        float[] blurKernel = {
//	            1 / 9f, 1 / 9f, 1 / 9f,
//	            1 / 9f, 1 / 9f, 1 / 9f,
//	            1 / 9f, 1 / 9f, 1 / 9f
//	        };



	        
		g2d.drawImage(baseImg, x, y,null);
		
		return true;
	}


	/**
	 * @param colours
	 * @return
	 */
	private RGBImageFilter createRGBFilter(final TIntObjectHashMap<Color> colours,final boolean isBorderRendering) {
		return new RGBImageFilter() {
			int lastInputRGB;
			int lastOutputRGB;
			
			@Override
			public int filterRGB(int x, int y, int rgb) {
				// remove alpha channel as can't get it to work right.... may give max 16 million objects
				rgb = rgb & 0x00FFFFFF;
				
				if(rgb!=0){
					
					// Do a speedup to stop to many hashtable queries. 
					// Often we will call this method for the same object twice in a row
					if(rgb == lastInputRGB){
						return lastOutputRGB;
					}		

					// Get colour from input map using rgb as id
					Color col = colours.get(rgb);
					if(col!=null){
						
						if(isBorderRendering){
							col = DatastoreRenderer.getDefaultPolygonBorderColour(col);
						}
						
						lastInputRGB = rgb;
						rgb = col.getRGB();
						lastOutputRGB = rgb;
					}
					else{
						// Hide it as not visible
						rgb = 0;
					}
				}
				return rgb;
			}
		};
	}


	private static BufferedImage applyColourFiltering(Image uncompressed, RGBImageFilter rgbFilter) {
		ImageProducer ip = new FilteredImageSource(uncompressed.getSource(), rgbFilter);
		Image coloured = Toolkit.getDefaultToolkit().createImage(ip);
		return ImageUtils.toBufferedImage(coloured);
	}

//	public static void main(String [] args){
//		int val = 1;
//		Random random = new Random(123);
//		while(val < Integer.MAX_VALUE / 2){
//			Color col = createColourId(val);
//			System.out.println("val = " + val + " -> " + col.getRGB() + " -> " + new Color(val).getRGB());
//			val += (int)Math.round(val * random.nextDouble());
//		}
//	}

}
