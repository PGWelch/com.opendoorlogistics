/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TByteLongHashMap;
import gnu.trove.map.hash.TObjectByteHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TByteLongProcedure;
import gnu.trove.procedure.TObjectLongProcedure;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.gis.map.Symbols.SymbolType;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.Colours.CalculateAverageColour;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class Legend {
	private static final Color LEGEND_BACKGROUND_COLOUR = new Color(240, 240, 240);
	public static final int DEFAULT_FONT_SIZE = 16;
	public static final LegendAlignment DEFAULT_ALIGNMENT = LegendAlignment.VERTICAL;
	
	public static void main(String[] args) {

		List<Map.Entry<String, Color>> list = createDummyEntries();

		for (int font : new int[] { 100,50, 30, 20, 10  }) {
			final BufferedImage img = createLegendImage(list, font, LegendAlignment.VERTICAL);

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					ImageUtils.createImageFrame(img).setVisible(true);
				}
			});
		}
	}

	public static BufferedImage createLegendImageFromDrawables(Iterable<? extends DrawableObject> pnts,int fontSize,  LegendAlignment align) {
		List<Map.Entry<String, Color>> text = getLegendText(pnts);
		if (text.size() > 0) {
			BufferedImage image = createLegendImage(text,fontSize,align);
			return image;
		}
		return null;
	}

	public static List<Map.Entry<String, BufferedImage>> getLegendItemImages(Iterable<? extends DrawableObject> drawables, Dimension size){
		List<Map.Entry<String, Color>> cols = getLegendText(drawables);
		Point2D centre = new Point2D.Double(size.width/2.0, size.height/2.0);
		int circum = Math.min(size.width, size.height);
		circum = 2 * circum/3;
		ArrayList<Map.Entry<String, BufferedImage>> ret = new ArrayList<>();
		for(Map.Entry<String, Color> entry : cols){
			BufferedImage img = ImageUtils.createBlankImage(size.width, size.height,Color.WHITE);
			Graphics2D g = (Graphics2D)img.getGraphics();
			DatastoreRenderer.drawOutlinedSymbol(g, SymbolType.CIRCLE,centre, circum, entry.getValue(), true);
			g.dispose();
			ret.add(new AbstractMap.SimpleEntry<String, BufferedImage>(entry.getKey(),img));
		}
		return ret;
	}
	
	
	/**
	 * Gets the text for the legend by averaging the colour for each distinct value of the legend key
	 * 
	 * @param pnts
	 * @return
	 */
	private static List<Map.Entry<String, Color>> getLegendText(Iterable<? extends DrawableObject> pnts) {

		// get average column for each legend item
		TreeMap<String, CalculateAverageColour> map = new TreeMap<>();
		for (DrawableObject pnt : pnts) {
			if (Strings.isEmpty(pnt.getLegendKey()) == false) {
				CalculateAverageColour av = map.get(pnt.getLegendKey());
				if (av == null) {
					av = new CalculateAverageColour();
					map.put(pnt.getLegendKey(), av);
				}

				Color col = pnt.getLegendColour();
				if(col==null){
					col = DatastoreRenderer.getRenderColour(pnt,false);
				}
				av.add(col);
			}
		}

		// return
		ArrayList<Map.Entry<String, Color>> ret = new ArrayList<>();
		for (Map.Entry<String, CalculateAverageColour> entry : map.entrySet()) {
			AbstractMap.SimpleEntry<String, Color> pair = new SimpleEntry<>(entry.getKey(), entry.getValue().getAverage());
			ret.add(pair);
		}
		
		// check if all entries are numbers and sort numerically if so
		boolean numeric=true;
		for(Map.Entry<String, Color> entry:ret){
			if(Strings.isNumber(entry.getKey())==false){
				numeric = false;
				break;
			}
		}
		
		if(numeric){
			Collections.sort(ret, new Comparator<Map.Entry<String, Color> >() {

				@Override
				public int compare(Entry<String, Color> o1, Entry<String, Color> o2) {
					try {
						// parse as doubles
						double d1 = Double.parseDouble(o1.getKey());
						double d2 = Double.parseDouble(o2.getKey());
						return Double.compare(d1, d2);
					} catch (Throwable e) {
						// parse as string if double parsing fails
						return o1.getKey().compareTo(o2.getKey());
					}
				}
			});
		}
		return ret;
	}

	public static BufferedImage createLegendImage(Iterable<Map.Entry<String, Color>> list) {
		return createLegendImage(list, DEFAULT_FONT_SIZE,  DEFAULT_ALIGNMENT);
	}

	public enum LegendAlignment {
		HORIZONTAL, VERTICAL
	}

	public static BufferedImage createLegendImage(Iterable<Map.Entry<String, Color>> list, final int fontSize, final LegendAlignment align) {
		class Item {
			Color col;
			TextLayout textLayout;
			Rectangle2D textBounds;
		}

		// get layouts and bounds for each string
		int maxTextHeight = 0;
		int maxTextWidth = 0;
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
		FontRenderContext frc = new FontRenderContext(font.getTransform(), true, true);
		ArrayList<Item> items = new ArrayList<>();
		for (Map.Entry<String, Color> pair : list) {
			Item item = new Item();
			item.col = pair.getValue();
			item.textLayout = new TextLayout(pair.getKey(), font, frc);
			item.textBounds = item.textLayout.getBounds();
			maxTextHeight = Math.max((int) Math.ceil(item.textBounds.getHeight()), maxTextHeight);
			maxTextWidth = Math.max((int) Math.ceil(item.textBounds.getWidth()), maxTextWidth);
			items.add(item);
		}

		// calculate sizes based on font size
		final int gapBetweenRows = 2 + fontSize / 3;
		final int pointCircumference = 3 * fontSize / 4;
		final int gapAfterPoint = fontSize / 3;
		final int gapBeforePoint = align == LegendAlignment.VERTICAL?0: 4 * gapAfterPoint;
		final int border = 2 + fontSize / 3;
		final double pointVOffsetFraction = 0.475;

		// get the fixed row height
		int fixedRowHeight = Math.max(maxTextHeight, pointCircumference) + gapBetweenRows;

		// get boxes for the point, text, whole row and finally image
		Dimension pointsBox = new Dimension(gapBeforePoint + pointCircumference + gapAfterPoint, fixedRowHeight);
		Dimension textBox = new Dimension(maxTextWidth, fixedRowHeight);
		Dimension rowBox = new Dimension(pointsBox.width + textBox.width, fixedRowHeight);
		Dimension imageSize = align == LegendAlignment.VERTICAL ? new Dimension(rowBox.width + 2 * border, rowBox.height * items.size() + 2 * border)
				: new Dimension(rowBox.width * items.size() + 2 * border, rowBox.height  + 2 * border);

		final BufferedImage img = ImageUtils.createBlankImage(imageSize.width, imageSize.height, LEGEND_BACKGROUND_COLOUR);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		// draw all
		int y = fixedRowHeight;
		int x = border;
		Color fontColor = LowLevelTextRenderer.LABEL_FONT_COLOUR;
		for (Item item : items) {

			DatastoreRenderer.drawOutlinedSymbol(g,SymbolType.CIRCLE, new Point2D.Float(gapBeforePoint + x + pointCircumference / 2,
					y - (int) (pointVOffsetFraction * pointCircumference)), pointCircumference, item.col, true);

			g.setColor(fontColor);
			item.textLayout.draw(g, x + pointsBox.width, y);
	
			if(align == LegendAlignment.VERTICAL){
				y += rowBox.height;				
			}
			else{
				x += pointsBox.width + (int)Math.round(item.textBounds.getWidth());
			}
		}

		// draw border
		g.setColor(Color.DARK_GRAY);
		g.drawRect(0, 0, imageSize.width - 1, imageSize.height - 1);

		g.dispose();
		return img;
	}

	private static List<Map.Entry<String, Color>> createDummyEntries() {
		ArrayList<Map.Entry<String, Color>> ret = new ArrayList<>();
		for (String s : ExampleData.getExampleNouns()) {
			AbstractMap.SimpleEntry<String, Color> entry = new SimpleEntry<>(s, Colours.getRandomColour(s));
			ret.add(entry);
			if (ret.size() >= 10) {
				break;
			}
		}
		return ret;
	}
	
	public static String getStandardisedLegendKey(ODLTableReadOnly drawables, int row){
		String key =(String) drawables.getValueAt(row, DrawableObjectImpl.COL_LEGEND_KEY);
		return getStandardisedLegendKey(key);
	}

	/**
	 * Get the standardised legend key or return null if key would not be used as a legend
	 * @param rawKeyFieldValue
	 * @return
	 */
	public static String getStandardisedLegendKey(String rawKeyFieldValue) {
		if(rawKeyFieldValue!=null && rawKeyFieldValue.length()>0){
			String stdKey = Strings.std(rawKeyFieldValue);
			if(stdKey!=null && stdKey.length()>0){
				return stdKey;				
			}
		}
		return null;
	}
	
	/**
	 * A class which processes the legend logic directly from a drawable table
	 * @author Phil
	 *
	 */
	public static class LegendDrawableTableBuilder{
		private static class CalcLegendEntry{
			String unstandard;
			String std;
			CalculateAverageColour col = new CalculateAverageColour();
			//StandardisedStringTreeMap<Long> symbolCount = new StandardisedStringTreeMap<Long>(); 
			TObjectLongHashMap<String> symbolCount = new TObjectLongHashMap<String>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, 0);
			TByteLongHashMap outlinedCount = new TByteLongHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR,(byte) 0, (byte)0);
			
		}
		
		private final HashMap<String,CalcLegendEntry> workingMap = new HashMap<String,Legend.LegendDrawableTableBuilder.CalcLegendEntry>();
		
//		public static interface LegendRowData{
//			public String key(); 
//			public Color color();
//			public Color legendColour();
//			public String colourKey();
//			public String symbol();
//			public Long outline();
//		}
		
		public void processRow(ODLTableReadOnly drawables, int row){
			String key =(String) drawables.getValueAt(row, DrawableObjectImpl.COL_LEGEND_KEY);
			String stdKey = getStandardisedLegendKey(key);
			if(stdKey!=null){

				// get entry or create if needed
				CalcLegendEntry entry = workingMap.get(stdKey);
				if(entry==null){
					entry = new CalcLegendEntry();
					entry.unstandard = key;
					entry.std = stdKey;
					workingMap.put(stdKey, entry);
				}
				
				// process the colour
				Color color = (Color )drawables.getValueAt(row, DrawableObjectImpl.COL_LEGEND_COLOUR);
				if(color==null){
					color = DatastoreRenderer.getNoAlphaColour((Color)drawables.getValueAt(row, DrawableObjectImpl.COL_COLOUR), (String)drawables.getValueAt(row, DrawableObjectImpl.COL_COLOUR_KEY));						
				}
				entry.col.add(color);
				
				// process the symbol
				String symbol = (String)drawables.getValueAt(row, DrawableObjectImpl.COL_SYMBOL);
				if(symbol == null){
					symbol = "";
				}
				else{
					symbol = Strings.std(symbol);
				}
				long count = entry.symbolCount.get(symbol);
				count++;
				entry.symbolCount.put(symbol, count);
				
				// process outlined
				Long outlined = (Long)drawables.getValueAt(row, DrawableObjectImpl.COL_OUTLINE);
				if(outlined==null){
					outlined = DrawableObjectImpl.DEFAULT_DRAW_OUTLINE;
				}
				byte byteOutlined = (byte)(outlined==1 ? 1 : 0);
				count = entry.outlinedCount.get(byteOutlined);
				count++;
				entry.outlinedCount.put(byteOutlined, count);
			}				
		
		}
		
		public List<Map.Entry<String, BufferedImage>> build(Dimension imageSize){
			// sort all entries in a manner sensitive to numbers etc...
			ArrayList<CalcLegendEntry> entries = new ArrayList<Legend.LegendDrawableTableBuilder.CalcLegendEntry>(workingMap.values());
			Collections.sort(entries, new Comparator<CalcLegendEntry>() {

				@Override
				public int compare(CalcLegendEntry o1, CalcLegendEntry o2) {
					return Strings.compareStd(o1.std, o2.std, true);
				}
			});
			
			// work out symbol size
			Point2D centre = new Point2D.Double(imageSize.width/2.0, imageSize.height/2.0);
			int circum = Math.min(imageSize.width, imageSize.height);
			circum = 2 * circum/3;
			ArrayList<Map.Entry<String, BufferedImage>> ret = new ArrayList<>();
			
			// process each one
			for(CalcLegendEntry entry: entries){
				
				// get average colour
				Color col = entry.col.getAverage();

				// get modal average symbol
				class CountMax{
					long max;
					String symbol;
					long outlined;
				}
				CountMax countMax = new CountMax();
				entry.symbolCount.forEachEntry(new TObjectLongProcedure<String>() {

					@Override
					public boolean execute(String a, long b) {
						if(countMax.symbol==null || countMax.max < b){
							countMax.symbol = a;
							countMax.max = b;
						}
						return true;
					}
				});
				
				// get modal average outlined boolean
				countMax.max=0;
				countMax.outlined = DrawableObjectImpl.DEFAULT_DRAW_OUTLINE;
				entry.outlinedCount.forEachEntry(new TByteLongProcedure() {
					
					@Override
					public boolean execute(byte a, long b) {
						if(countMax.max < b){
							countMax.outlined = a;
							countMax.max = b;
						}
						return true;
					}
				});
				
				// draw the picture using average colour, symbol and outlining across all entries with the same std legend key
				BufferedImage img = ImageUtils.createBlankImage(imageSize.width, imageSize.height,Color.WHITE);
				Graphics2D g = (Graphics2D)img.getGraphics();
				SymbolType type = DatastoreRenderer.getSymbolType(countMax.symbol);
				DatastoreRenderer.drawOutlinedSymbol(g, type,centre, circum, col, countMax.outlined==1);
				g.dispose();
				ret.add(new AbstractMap.SimpleEntry<String, BufferedImage>(entry.unstandard,img));

			}
			
			return ret;
		}
	}
	
}
