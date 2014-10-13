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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.util.List;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.gis.map.ObjectRenderer;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectDecorator;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
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
	
	public long getSizeInBytes(){
		return ids.size() * 8 + filled.getSizeBytes() + borders.getSizeBytes();
	}
	
	private static class ColourDecorator extends DrawableObjectDecorator{
		private Color color;

		public ColourDecorator(DrawableObject decorated, Color color) {
			super(decorated);
			this.color = color;
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
			return 0;
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
	
		// draw all objects on the tile with different colour for each based on id and not showing borders
		BufferedImage img = createDrawnImage(converter, renderer,RenderProperties.SKIP_BORDER_RENDERING);
		filled = new CompressedImage(img, CompressedType.LZ4);
		
		// now do the same for the borders (doing separately allows coping with opaque)
		img = createDrawnImage(converter, renderer,RenderProperties.RENDER_BORDERS_ONLY);
		borders = new CompressedImage(img, CompressedType.LZ4);
		
	}



	/**
	 * @param converter
	 * @param renderer
	 * @param nonOverlappingPolys
	 * @param ret
	 * @return
	 */
	private BufferedImage createDrawnImage(LatLongToScreen converter, ObjectRenderer renderer, long renderFlags) {
		Rectangle2D viewable = converter.getViewportWorldBitmapScreenPosition();
		int w = (int)viewable.getWidth();
		int h = (int)viewable.getHeight();
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try {
			g.setClip(0, 0, w, h);
			for(DrawableObject obj:layer){
				if(obj.getGeometry()!=null){
					int id = ids.get(obj.getGeometry());
					Color dummyIdColour = new Color(id);
					renderer.renderObject(g, converter, new ColourDecorator(obj, dummyIdColour), false,renderFlags);
				}
			}
		} finally {
			g.dispose();

		}
		return img;
	}
	
	public boolean draw(Iterable<DrawableObject> polys, Graphics2D g2d, int x, int y){
		
		// get colour state of each geom and put this in a map using our ids
		final TIntObjectHashMap< Color> colours = new TIntObjectHashMap<>();
		for(DrawableObject obj :polys){
			if(obj.getGeometry()==null){
				continue;
			}
			Integer id = ids.get(obj.getGeometry());
			if(id==null){
				// unknown id.. rendering went wrong
				return false;
			}
			
			Color col = obj.getColour();
			colours.put(id, col);
		}
		

		// Create colour filter which filters out anything not visible and colours everything else
		RGBImageFilter rgbFilter =new RGBImageFilter() {
			int lastInputRGB;
			int lastOutputRGB;
			
			@Override
			public int filterRGB(int x, int y, int rgb) {
				if(rgb!=0){
					
					// Do a speedup to stop to many hashtable queries. 
					// Often we will call this method for the same object twice in a row
					if(rgb == lastInputRGB){
						return lastOutputRGB;
					}
					
					// Get colour from input map using rgb as id
					Color col = colours.get(rgb);	
					if(col!=null){
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
		
		BufferedImage img = toImage(filled.get(), rgbFilter);
		g2d.drawImage(img, x, y,null);
		
		// draw borders
		img = toImage(borders.get(), rgbFilter);
		g2d.drawImage(img, x, y,null);
		
		return true;
	}


	private BufferedImage toImage(Image uncompressed, RGBImageFilter rgbFilter) {
		ImageProducer ip = new FilteredImageSource(uncompressed.getSource(), rgbFilter);
		Image coloured = Toolkit.getDefaultToolkit().createImage(ip);
		return ImageUtils.toBufferedImage(coloured);
	}

	

}
