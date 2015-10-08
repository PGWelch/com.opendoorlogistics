/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.UserRenderFlags;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

class LowLevelTextRenderer {

	static final private int NO_DRAW_BUFFER_AROUND_TEXT = 5;
	static final private Dimension MIN_FREE_SPACE_TO_ATTEMPT_TEXT_DRAW = new Dimension(30, 20);
	static final private double STRING_OFFSET_FRACTION = 0.4;
	static final private BasicStroke SMALL_INNER_TEXT_STROKE = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
	static final private BasicStroke MEDIUM_INNER_TEXT_STROKE = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
	static final private BasicStroke LARGE_INNER_TEXT_STROKE = new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
	static final private Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	static final Color LABEL_FONT_COLOUR = new Color(0, 0, 100);

	static final StandardisedStringTreeMap<LabelPositionOption> labelPosOptionMap = new StandardisedStringTreeMap<LabelPositionOption>(false);
	
	static final LabelPositionOption [] AUTOMATIC_POSITIONS = new LabelPositionOption[]{LabelPositionOption.TOP, LabelPositionOption.BOTTOM, LabelPositionOption.RIGHT, LabelPositionOption.LEFT,
		LabelPositionOption.TOP_LEFT, LabelPositionOption.TOP_RIGHT, LabelPositionOption.BOTTOM_LEFT, LabelPositionOption.BOTTOM_RIGHT};
	
	static{
		for(LabelPositionOption lpo:LabelPositionOption.values()){
			for(String k:lpo.getKeywords()){
				labelPosOptionMap.put(k, lpo);				
			}
		}
	}
	
	private static LabelPositionOption getLabelPositionOption(String l){
		
		if(l!=null){
			LabelPositionOption ret = labelPosOptionMap.get(l);
			if(ret!=null){
				return ret;
			}
		}
		
		// default (the original option)
	//	return LabelPositionOption.RIGHT;
		
		return null;
	}
	
	public enum LabelPositionOption{
		RIGHT(PredefinedTags.RIGHT, "r"),
		LEFT(PredefinedTags.LEFT, "l"),
		TOP(PredefinedTags.TOP, "t"),
		BOTTOM(PredefinedTags.BOTTOM , "b"),
		CENTRE(PredefinedTags.CENTRE),
		NO_LABEL(PredefinedTags.NO_LABEL),
		TOP_LEFT(PredefinedTags.TOP_LEFT, "tl"),
		TOP_RIGHT(PredefinedTags.TOP_RIGHT, "tr"),
		BOTTOM_LEFT(PredefinedTags.BOTTOM_LEFT, "bl"),
		BOTTOM_RIGHT(PredefinedTags.BOTTOM_RIGHT, "br"),
		;
		
		private LabelPositionOption(String ...keywords) {
			this.keywords = keywords;
		}

		private final String []keywords;
		
		public String []getKeywords(){
			return keywords;
		}
	}
	
	private Point2D getScreenPos(LatLongToScreen converter, DrawableObject pnt, Font font, Point2D.Double textSize, LabelPositionOption positionOption) {
		Point2D screenPos=null;
		ODLGeomImpl geom = pnt.getGeometry();
		if (geom == null) {
			// point rendering
			screenPos = converter.getOnScreenPixelPosition(pnt);

			int offset = (int) Math.round(STRING_OFFSET_FRACTION * font.getSize());
			int halfWidth = (int)Math.round(textSize.x/ 2);
			//int halfHeight = (int)Math.round(textSize.y/ 2);
			//int heightSep = pnt.getPixelWidth()
			
			offset = Math.max(offset, (int) Math.ceil(0.5 * pnt.getPixelWidth()) + 1);
			
			double centreX = screenPos.getX() - halfWidth;
			double rightX = screenPos.getX() + offset;
			double leftX = screenPos.getX() - offset - textSize.x;
			double topY = screenPos.getY() - 0.5*pnt.getPixelWidth() - 0.6* textSize.y;
			double bottomY = screenPos.getY() + (0.5*pnt.getPixelWidth()+2) + 0.9*textSize.y;
			double centreY = screenPos.getY();
			switch(positionOption){
			case TOP:
				screenPos = new Point2D.Double(centreX, topY);					
				break;

			case TOP_LEFT:
				screenPos = new Point2D.Double(leftX, topY);					
				break;

			case TOP_RIGHT:
				screenPos = new Point2D.Double(rightX, topY);					
				break;

		
			case BOTTOM:
				screenPos = new Point2D.Double(centreX, bottomY);					
				break;
				
			case BOTTOM_LEFT:
				screenPos = new Point2D.Double(leftX, bottomY);					
				break;

			case BOTTOM_RIGHT:
				screenPos = new Point2D.Double(rightX, bottomY);					
				break;
				
				
			case CENTRE:
				screenPos = new Point2D.Double(centreX,centreY );					
				break;
				
			case RIGHT:
				// get text screen positioning, offsetting by a fraction of the font size and at least the point's pixel half-width
				screenPos = new Point2D.Double(rightX, centreY);				
				break;
				
			case LEFT:
				// get text screen positioning, offsetting by a fraction of the font size and at least the point's pixel half-width
				screenPos = new Point2D.Double(leftX, centreY);				
				break;				
			
				default:
					throw new UnsupportedOperationException();
			}

		} else {
			
			// get the world bitmap position
			Point2D wbPos=null;
			if(geom.isLineString()){
				// draw text in the centre of the object, adjusting for text size
				OnscreenGeometry cachedGeometry = DatastoreRenderer.getCachedGeometry(pnt.getGeometry(), converter, true);
				if (cachedGeometry == null) {
					return null;
				}

				wbPos = cachedGeometry.getLineStringMidPoint();	
			}
			if(wbPos==null){
				wbPos = geom.getWorldBitmapCentroid(converter);
			}
			
			if(wbPos!=null){
				Rectangle2D view = converter.getViewportWorldBitmapScreenPosition();
				screenPos = new Point2D.Double(wbPos.getX() - view.getMinX() - textSize.getX() / 2, 
						wbPos.getY() - view.getMinY() - textSize.getY() / 2);					
			}
		}

		return screenPos;
	}

	private TextLayout createTextLayout(String text, Font font, FontRenderContext frc){
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.TEXT_LAYOUT_CACHE);
		
		class TLKey{
			final String text;
			final Font font;
			final FontRenderContext frc;
			TLKey(String text, Font font, FontRenderContext frc) {
				super();
				this.text = text;
				this.font = font;
				this.frc = frc;
			}
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((font == null) ? 0 : font.hashCode());
				result = prime * result + ((frc == null) ? 0 : frc.hashCode());
				result = prime * result + ((text == null) ? 0 : text.hashCode());
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
				TLKey other = (TLKey) obj;
				if (font == null) {
					if (other.font != null)
						return false;
				} else if (!font.equals(other.font))
					return false;
				if (frc == null) {
					if (other.frc != null)
						return false;
				} else if (!frc.equals(other.frc))
					return false;
				if (text == null) {
					if (other.text != null)
						return false;
				} else if (!text.equals(other.text))
					return false;
				return true;
			}	
		}
	
		TLKey key = new TLKey(text, font, frc);
		TextLayout ret = (TextLayout)cache.get(key);
		if(ret==null){
			ret = new TextLayout(text, font, frc);
			
			// no idea how large this object is! take a guess!
			int assumedSize =5* 1024;
			cache.put(key, ret, assumedSize);
		}
		return ret;
	}
	
	/**
	 * Do a quick and dirty check to see if space around the text is free.
	 * This check is inaccurate but quick...
	 * @param converter
	 * @param obj
	 * @param font
	 * @param textQuadtree
	 * @return
	 */
	private boolean approxQuadtreeCheck( LatLongToScreen converter,DrawableObject obj, Font font ,Quadtree textQuadtree, LabelPositionOption option){
		Envelope envelope = getApproxQuadtreeCheckEnvelope(converter, obj, font, option);
		if(envelope==null){
			return false;
		}
		return isPositionFree(envelope, textQuadtree);
	}

	/**
	 * Get the envelope used for our approximate quadtree check
	 * @param converter
	 * @param obj
	 * @param font
	 * @param option
	 * @return
	 */
	private Envelope getApproxQuadtreeCheckEnvelope(LatLongToScreen converter, DrawableObject obj, Font font, LabelPositionOption option) {
		// get rough size...
		int approxWidth = font.getSize() * obj.getLabel().length();
		int approxHeight = font.getSize();
		approxWidth += NO_DRAW_BUFFER_AROUND_TEXT;
		approxHeight += NO_DRAW_BUFFER_AROUND_TEXT;
		
		approxWidth = Math.max(approxWidth, MIN_FREE_SPACE_TO_ATTEMPT_TEXT_DRAW.width);
		approxHeight = Math.max(approxHeight, MIN_FREE_SPACE_TO_ATTEMPT_TEXT_DRAW.height);
		
		Point2D screenPos = getScreenPos(converter, obj, font, new Point2D.Double(approxWidth,approxHeight),option);
		if(screenPos==null){
			return null;
		}
		Envelope envelope = new Envelope(screenPos.getX(),screenPos.getX() + approxWidth, screenPos.getY(),screenPos.getY() + approxHeight);
		return envelope;
	}
	
	boolean renderDrawableText(Graphics2D g, LatLongToScreen converter, DrawableObject pnt, Quadtree textQuadtree) {
		if (Strings.isEmpty(pnt.getLabel()) || pnt.getFontSize() < 0) {
			return false;
		}

		// do an initial rough quadtree check with an assumed bounds as calculating proper bounds is slow			
		Font font = getFont((int)pnt.getFontSize());

		LabelPositionOption option = getLabelPositionOption(pnt.getLabelPositioningOption());
		if(option == LabelPositionOption.NO_LABEL){
			return false;
		}
		
		boolean forceShowLabel = (pnt.getFlags() & UserRenderFlags.ALWAYS_SHOW_LABEL) == UserRenderFlags.ALWAYS_SHOW_LABEL;
		
		// choose the option automatically
		if(option == null){
	
			double best = Double.POSITIVE_INFINITY;
			for(LabelPositionOption test:AUTOMATIC_POSITIONS){
				
				if(forceShowLabel){
					// get the one with least overlap
					Envelope envelope = getApproxQuadtreeCheckEnvelope(converter, pnt, font, test);
					if(envelope!=null){
						double measure = measureOverlap(envelope, textQuadtree);
						if(measure < best){
							option = test;
							best = measure;
						}
					}
					
				}else{
					// get the first with no overlap
					if(approxQuadtreeCheck(converter,pnt,font,textQuadtree, test)){
						option = test;
						break;
					}									
				}
			}
	
		}
		else{
			// check position ok
			if(!forceShowLabel){
				if(!approxQuadtreeCheck(converter,pnt,font,textQuadtree, option)){
					return false;
				}					
			}
		}
		
		if(option==null){
			return false;
		}
		
		// get initial bounds (not in correct screen position)
		TextLayout textLayout = createTextLayout(pnt.getLabel(), font, g.getFontRenderContext());
		Point2D.Double textSize = getSize(textLayout);

		// get screen position from the point or geometry
		Point2D screenPos = getScreenPos(converter, pnt, font, textSize, option);
		if (screenPos == null) {
			return false;
		}

		return drawTextLayout(font, screenPos, textLayout, textSize,pnt.getLabelColour(), g,forceShowLabel, textQuadtree);
	}

	/**
	 * Renders text in the bottom right corner; used for showing OSM copyright
	 * @param text
	 * @param fontSize
	 * @param g
	 * @param textQuadtree
	 */
	void renderInBottomCorner(String text, int fontSize,Graphics2D g,Quadtree textQuadtree, boolean rightCorner){
		// Measure text
		Font font = getFont(fontSize);
		TextLayout textLayout =createTextLayout(text, font, g.getFontRenderContext());
		Point2D.Double size = getSize(textLayout);
		
		// Get bounds of the screen
		Rectangle bounds = g.getClipBounds();
		Point2D screenPos = new Point2D.Double(rightCorner? bounds.getWidth() - size.x - 2:0, bounds.getHeight() - size.y - 0);
		drawTextLayout(font, screenPos, textLayout, size, null,g, true, textQuadtree);
	}
	
	private boolean isPositionFree(Envelope text,Quadtree textQuadtree){
		@SuppressWarnings("unchecked")
		List<Envelope> found = textQuadtree.query(text);
		if (found != null) {
			for (Envelope other : found) {
				if (other.intersects(text)) {
					return false;
				}
			}
		}	
		return true;
	}
	
	private double measureOverlap(Envelope text,Quadtree textQuadtree){
		double ret = 0;
		@SuppressWarnings("unchecked")
		List<Envelope> found = textQuadtree.query(text);
		if (found != null) {
			for (Envelope other : found) {
				if (other.intersects(text)) {
					Envelope overlap = other.intersection(text);
					ret +=Math.max(0, overlap.getArea());
				}
			}
		}			
		return ret;
	}
	
	/**
	 * @param font
	 * @param screenPos
	 * @param textLayout
	 * @param size
	 * @param g
	 * @param textQuadtree
	 * @return
	 */
	private boolean drawTextLayout(Font font, Point2D screenPos, TextLayout textLayout, Point2D.Double size,Color col, Graphics2D g,boolean skipQuadtreeCheck, Quadtree textQuadtree) {
		// get bounds by getting the shape and translating to screen position
		AffineTransform affineTransform = new AffineTransform();
		affineTransform.translate((int) screenPos.getX(), (int) screenPos.getY() + size.getY() / 2);
		Shape shape = textLayout.getOutline(affineTransform);
		Rectangle2D onscreenBounds = shape.getBounds2D();

		// save current hints and change to high quality rendering
		Object oldAAHintVal = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Object oldRHintVal = g.getRenderingHint(RenderingHints.KEY_RENDERING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// test quadtree to see if we can draw here
		Envelope envelope = new Envelope(onscreenBounds.getMinX(), onscreenBounds.getMaxX(), onscreenBounds.getMinY(), onscreenBounds.getMaxY());
		envelope.expandBy(NO_DRAW_BUFFER_AROUND_TEXT, NO_DRAW_BUFFER_AROUND_TEXT);

		
		boolean drawText =skipQuadtreeCheck? true: isPositionFree(envelope,textQuadtree);

		if (drawText) {
			// draw outline
			if (font.getSize() < 20) {
				g.setStroke(SMALL_INNER_TEXT_STROKE);
			} else if (font.getSize() < 30) {
				g.setStroke(MEDIUM_INNER_TEXT_STROKE);
			} else {
				g.setStroke(LARGE_INNER_TEXT_STROKE);
			}

			g.setColor(Color.WHITE);
			g.draw(shape);

			// draw inner
			g.setColor(col!=null ? col : LABEL_FONT_COLOUR);
			textLayout.draw(g, (int) screenPos.getX(), (int) (screenPos.getY() + size.getY() / 2));

			textQuadtree.insert(envelope, envelope);
		}

		// set back original hints
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAAHintVal);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, oldRHintVal);
		return drawText;
	}

	/**
	 * @param textLayout
	 * @return
	 */
	private Point2D.Double getSize(TextLayout textLayout) {
		Rectangle2D initialBounds = textLayout.getBounds();
		Point2D.Double size = new Point2D.Double(initialBounds.getWidth(), initialBounds.getHeight());
		return size;
	}

	/**
	 * @param pnt
	 * @return
	 */
	private Font getFont(int fontSize) {
		// get correct Font
		Font font = LABEL_FONT;
		if (fontSize > 0) {
			// Font is apparently a lightweight object (see http://stackoverflow.com/questions/6102602/java-awt-is-font-a-lightweight-object)
			// so this should be OK.
			font = new Font(Font.SANS_SERIF, Font.BOLD,fontSize);
		}
		return font;
	}

}