/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import static com.opendoorlogistics.core.gis.map.RenderProperties.LEGEND_BOTTOM;
import static com.opendoorlogistics.core.gis.map.RenderProperties.LEGEND_BOTTOM_LEFT;
import static com.opendoorlogistics.core.gis.map.RenderProperties.LEGEND_BOTTOM_RIGHT;
import static com.opendoorlogistics.core.gis.map.RenderProperties.LEGEND_TOP;
import static com.opendoorlogistics.core.gis.map.RenderProperties.LEGEND_TOP_LEFT;
import static com.opendoorlogistics.core.gis.map.RenderProperties.LEGEND_TOP_RIGHT;
import static com.opendoorlogistics.core.gis.map.RenderProperties.SHOW_TEXT;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.Legend.LegendAlignment;
import com.opendoorlogistics.core.gis.map.Symbols.SymbolType;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.IntUtils;
import com.opendoorlogistics.core.utils.SimpleSoftReferenceMap;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedCache;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.math.MathUtil;

final public class DatastoreRenderer {
	// private static final int STANDARD_OUTLINE_INNER_WIDTH = 2;
	// private static final int STANDARD_OUTLINE_OUTER_WIDTH = 4;
	// private static final int LARGER_OUTLINE_INNER_WIDTH = 4;
	// private static final int LARGER_OUTLINE_OUTER_WIDTH = 8;
	private static final int NO_DRAW_BUFFER_AROUND_TEXT = 5;
	private static final double STRING_OFFSET_FRACTION = 0.5;
	private static final BasicStroke SMALL_INNER_TEXT_STROKE = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
	private static final BasicStroke MEDIUM_INNER_TEXT_STROKE = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
	private static final BasicStroke LARGE_INNER_TEXT_STROKE = new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
	private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	public static final Color LABEL_FONT_COLOUR = new Color(0, 0, 100);
	public static final int CACHE_MIN_IMAGE_SIZE_LIMIT = 4 * 4;
	public static final int MAX_IMAGE_SIZE_LIMIT = 3000 * 3000;
	static final Color SELECTION_COLOUR = new Color(0, 0, 255);
	private static final float SELECTION_DARKEN_OUTLINE_FACTOR = 0.6f;
	private final SimpleSoftReferenceMap<DrawnSymbol, DrawnSymbol> circleImageCache = new SimpleSoftReferenceMap<>();
	private static final Symbols symbols = new Symbols();

	// private boolean renderFade=true;
	// private boolean allowDelayedGeometryRendering=false;

	// public RecentImageCache getRecentCache() {
	// return recentCache;
	// }

	/**
	 * Remove small gaps between polygons by detecting any 0-alpha pixel surrounded by a majority of non 0-alpha pixels
	 * 
	 * @param img
	 */
	public static BufferedImage postProcessImage(BufferedImage img) {

		java.awt.Point[] ngbs = new java.awt.Point[8];
		ngbs[0] = new java.awt.Point(-1, -1);
		ngbs[1] = new java.awt.Point(0, -1);
		ngbs[2] = new java.awt.Point(+1, -1);
		ngbs[3] = new java.awt.Point(-1, 0);
		ngbs[4] = new java.awt.Point(+1, 0);
		ngbs[5] = new java.awt.Point(-1, +1);
		ngbs[6] = new java.awt.Point(0, +1);
		ngbs[7] = new java.awt.Point(+1, +1);

		class ToSet {
			int x;
			int y;
			int rgba;
		}
		ArrayList<ToSet> toSets = new ArrayList<>();
		TIntArrayList ngbColours = new TIntArrayList();

		// each pixel, apart from edge pixels, has 8 neighbours
		int width = img.getWidth();
		int height = img.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				// img.getData().geta
				int rgba = img.getRGB(x, y);
				int alpha = (rgba & (0xFF000000)) >> 24;

				if (alpha == 0) {
					// int ngbCount = 0;
					int zeroAlphaNgbCount = 0;
					int nonZeroAlphaNgbCount = 0;
					ngbColours.clear();
					for (java.awt.Point ngb : ngbs) {
						int xi = x + ngb.x;
						int yi = y + ngb.y;
						if (xi < width && xi >= 0 && yi < height && yi >= 0) {
							// ngbCount++;
							int orgba = img.getRGB(xi, yi);
							int oalpha = (orgba & (0xFF000000)) >> 24;
							if (oalpha == 0) {
								zeroAlphaNgbCount++;
							} else {
								nonZeroAlphaNgbCount++;
								ngbColours.add(orgba);
							}
						}
					}

					if (nonZeroAlphaNgbCount > zeroAlphaNgbCount) {
						// set to majority colour
						ToSet toSet = new ToSet();
						toSet.x = x;
						toSet.y = y;
						toSet.rgba = IntUtils.getModal(ngbColours);
						toSets.add(toSet);
					}
				}
			}
		}

		// take copy of input image and do the sets
		BufferedImage correctedImg = ImageUtils.deepCopy(img);
		for (ToSet toSet : toSets) {
			correctedImg.setRGB(toSet.x, toSet.y, toSet.rgba);
		}

		// create the final image with a fade and then everything on top of it
		BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = null;
		try {
			g = (Graphics2D) ret.getGraphics();
			g.setClip(0, 0, width, height);
			renderFade(g);
			g.drawImage(correctedImg, 0, 0, null);
		} finally {
			if (g != null) {
				g.dispose();
			}
		}
		return ret;// ImageUtils.createBlankImage(width, height, Color.BLACK);
	}

	public static void renderFade(Graphics2D g) {
		Color fade = new Color(255, 255, 255, 100);
		Rectangle bounds = g.getClipBounds();
		g.setColor(fade);
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
	}

	// public void renderObjects(Graphics2D g, Iterable<? extends DrawableObject> pnts, LatLongToScreen converter, long renderFlags) {
	// renderObjects(g, pnts, converter, renderFlags, null);
	// }

	public synchronized void renderTexts(final Graphics2D g, Iterable<? extends DrawableObject> pnts, final LatLongToScreen converter, long renderflags) {
		// check for label groups
		StandardisedCache stdCache = new StandardisedCache();
		HashMap<String, ArrayList<DrawableObject>> labelGroups = new HashMap<>();
		for (DrawableObject obj : pnts) {
			if (!Strings.isEmpty(obj.getLabelGroupKey())) {
				String std = stdCache.std(obj.getLabelGroupKey());
				ArrayList<DrawableObject> list = labelGroups.get(std);
				if (list == null) {
					list = new ArrayList<>();
					labelGroups.put(std, list);
				}
				list.add(obj);
			}
		}

		// process label groups to decide which object gets the label
		HashMap<String, DrawableObject> labelGroupWinningObjects = new HashMap<>();
		for (Map.Entry<String, ArrayList<DrawableObject>> entry : labelGroups.entrySet()) {
			ArrayList<DrawableObject> group = entry.getValue();

			// Simple rule - if they are all linestrings then pick the longest segment otherwise pick the most central.
			// Ignore onscreen / offscreen for the moment (so label has a fixed position).
			boolean allLineStrings = true;
			Point2D.Double sum = new Point2D.Double();
			long count = 0;
			double longestLineStringLength = Double.NEGATIVE_INFINITY;
			DrawableObject longestLineString = null;

			int n = group.size();
			ArrayList<Point2D> centroids = new ArrayList<>(n);

			for (DrawableObject obj : group) {
				if (obj.getGeometry() == null) {
					Point2D centroid = converter.getOnScreenPixelPosition(obj);
					centroids.add(centroid);
					sum.x += centroid.getX();
					sum.y += centroid.getY();
					count++;
					allLineStrings = false;
				} else {
					CachedGeometry transformed = getCachedGeometry(obj.getGeometry(), converter, true);
					if (transformed != null) {
						if (transformed.isLineString()) {
							double length = transformed.getLineStringLength();
							if (length > longestLineStringLength) {
								longestLineStringLength = length;
								longestLineString = obj;
							}
						} else {
							allLineStrings = false;
						}

						// add to sum
						Point2D centroid = transformed.getCentroid();
						centroids.add(centroid);
						sum.x += centroid.getX();
						sum.y += centroid.getY();
						count++;
					} else {
						allLineStrings = false;
						centroids.add(null);
					}
				}
			}

			if (allLineStrings) {
				if (longestLineString != null) {
					labelGroupWinningObjects.put(entry.getKey(), longestLineString);
				}
			} else {
				// determine the most central object
				if (count > 0) {
					Point2D.Double centroid = new Point2D.Double(sum.x / count, sum.y / count);
					double minDistSqd = Double.POSITIVE_INFINITY;
					DrawableObject minDistObj = null;
					for (int i = 0; i < n; i++) {
						Point2D objCentroid = centroids.get(i);
						if (objCentroid != null) {
							double distSqd = centroid.distanceSq(objCentroid);
							if (distSqd < minDistSqd) {
								minDistSqd = distSqd;
								minDistObj = group.get(i);
							}
						}
					}

					if (minDistObj != null) {
						labelGroupWinningObjects.put(entry.getKey(), minDistObj);
					}

				}

			}
		}

		final Quadtree textQuadtree = new Quadtree();
		final Rectangle2D viewport = converter.getViewportWorldBitmapScreenPosition();

		// call which checks for on-screen etc
		final LowLevelTextRenderer lowLevelRenderer = new LowLevelTextRenderer();
		class TextDrawer {
			void render(DrawableObject obj) {
				if (Strings.isEmpty(obj.getLabel()) == false) {
					boolean visible = false;
					if (obj.getGeometry() == null) {
						visible = getPointIntersectsScreen(g, obj, converter.getOnScreenPixelPosition(obj));
					} else {

						CachedGeometry transformed = getCachedGeometry(obj.getGeometry(), converter, true);
						if (transformed != null) {
							Rectangle2D bounds = transformed.getWorldBitmapBounds();
							visible = bounds.intersects(viewport);
						}
					}
					if (visible) {
						lowLevelRenderer.renderDrawableText(g, converter, obj, textQuadtree);
					}
				}
			}
		}
		TextDrawer drawer = new TextDrawer();

		// Draw OSM copyright
		if ((renderflags & RenderProperties.DRAW_OSM_COPYRIGHT) == RenderProperties.DRAW_OSM_COPYRIGHT) {
			lowLevelRenderer.renderInBottomRightCorner(AppConstants.OSM_COPYRIGHT, 9, g, textQuadtree);
		}

		// render groups first, rendering the winning object only
		for (DrawableObject obj : pnts) {
			if (!Strings.isEmpty(obj.getLabelGroupKey())) {
				String std = stdCache.std(obj.getLabelGroupKey());
				if (obj == labelGroupWinningObjects.get(std)) {
					drawer.render(obj);
				}
			}
		}

		// then render non-groups (assumed to be less important than groups)
		for (DrawableObject obj : pnts) {
			if (Strings.isEmpty(obj.getLabelGroupKey())) {
				drawer.render(obj);
			}
		}
	}

	/**
	 * Render fade, objects, text and legend
	 * 
	 * @param g
	 * @param pnts
	 * @param converter
	 * @param renderFlags
	 * @param selectedObjectIds
	 */
	public synchronized void renderAll(Graphics2D g, Iterable<? extends DrawableObject> pnts, LatLongToScreen converter, long renderFlags, TLongHashSet selectedObjectIds) {

		if ((renderFlags & RenderProperties.RENDER_FADE) == RenderProperties.RENDER_FADE) {
			renderFade(g);
		}

		// draw objects
		for (DrawableObject pnt : pnts) {
			if (pnt != null) {
				boolean isSelected = selectedObjectIds != null ? selectedObjectIds.contains(pnt.getGlobalRowId()) : false;
				renderObject(g, converter, pnt, isSelected);
			}
		}

		// then text - earlier objects get text rendering priority
		if ((renderFlags & SHOW_TEXT) == SHOW_TEXT) {
			renderTexts(g, pnts, converter, renderFlags);
		}

		renderLegend(g, pnts, renderFlags);
	}

	void renderLegend(Graphics2D g, Iterable<? extends DrawableObject> pnts, long renderFlags) {
		// finally legend (if flagged)
		if ((renderFlags & (LEGEND_TOP_LEFT | LEGEND_TOP_RIGHT | LEGEND_BOTTOM_LEFT | LEGEND_BOTTOM_RIGHT | LEGEND_TOP | LEGEND_BOTTOM)) != 0) {
			boolean horizontal = (renderFlags & (LEGEND_TOP | LEGEND_BOTTOM)) != 0;

			BufferedImage legend = Legend.createLegendImageFromDrawables(pnts, Legend.DEFAULT_FONT_SIZE, horizontal ? LegendAlignment.HORIZONTAL : LegendAlignment.VERTICAL);
			if (legend != null) {
				int lw = legend.getWidth();
				int lh = legend.getHeight();
				int iw = g.getClipBounds().width;
				int ih = g.getClipBounds().height;

				if ((renderFlags & LEGEND_TOP_LEFT) == LEGEND_TOP_LEFT) {
					g.drawImage(legend, 0, 0, null);
				}

				if ((renderFlags & LEGEND_TOP_RIGHT) == LEGEND_TOP_RIGHT) {
					g.drawImage(legend, iw - lw, 0, null);
				}

				if ((renderFlags & LEGEND_BOTTOM_LEFT) == LEGEND_BOTTOM_LEFT) {
					g.drawImage(legend, 0, ih - lh, null);
				}

				if ((renderFlags & LEGEND_BOTTOM_RIGHT) == LEGEND_BOTTOM_RIGHT) {
					g.drawImage(legend, iw - lw, ih - lh, null);
				}

				if ((renderFlags & LEGEND_TOP) == LEGEND_TOP) {
					g.drawImage(legend, iw / 2 - lw / 2, 0, null);
				}

				if ((renderFlags & LEGEND_BOTTOM) == LEGEND_BOTTOM) {
					g.drawImage(legend, iw / 2 - lw / 2, ih - lh, null);
				}
			}
		}
	}

	// /**
	// * Render fade then objects
	// *
	// * @param g
	// * @param pnts
	// * @param converter
	// * @param selectedObjectIds
	// * @return
	// */
	// private void renderObjects(Graphics2D g, Iterable<? extends DrawableObject> pnts, LatLongToScreen converter, TLongHashSet selectedObjectIds) {
	//
	// for (DrawableObject pnt : pnts) {
	// if (pnt != null) {
	// boolean isSelected = selectedObjectIds != null ? selectedObjectIds.contains(pnt.getGlobalRowId()) : false;
	// renderObject(g, converter, pnt, isSelected);
	// }
	// }
	//
	// }

	public static TLongArrayList getWithinRectangle(Iterable<? extends DrawableObject> pnts, LatLongToScreen converter, Rectangle selRectOnScreen) {
		List<DrawableObject> list = getObjectsWithinRectangle(pnts, converter, selRectOnScreen);
		TLongArrayList ret = new TLongArrayList(list.size());
		for (DrawableObject obj : list) {
			ret.add(obj.getGlobalRowId());
		}
		return ret;
	}

	public static List<DrawableObject> getObjectsWithinRectangle(Iterable<? extends DrawableObject> pnts, LatLongToScreen converter, Rectangle selRectOnScreen) {

		List<DrawableObject> ret = new ArrayList<>();

		// create blank image with the bounds that we're testing to give us a valid graphics object
		int w = Math.max(selRectOnScreen.width, 1);
		int h = Math.max(selRectOnScreen.height, 1);
		BufferedImage testImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = testImage.createGraphics();
		Rectangle testRectangle = new Rectangle(0, 0, w, h);
		g.setClip(0, 0, w, h);

		// get the screen viewport's world bitmap bounds
		Rectangle2D wbView = converter.getViewportWorldBitmapScreenPosition();

		// translate the selection rectangle to world bitmap coords
		// Rectangle2D wbSel = new Rectangle2D.Double(wbView.getMinX() + selRectOnScreen.getMinX(),
		// wbView.getMinY() + selRectOnScreen.getMinY(), selRectOnScreen.getWidth(), selRectOnScreen.getHeight());

		try {
			for (DrawableObject pnt : pnts) {

				if (pnt.getGeometry() == null) {
					if (hasValidLatLong(pnt)) {
						Point2D screenPos = converter.getOnScreenPixelPosition(pnt);

						// create bounding box around the point
						int pntWidth = (int) pnt.getPixelWidth();
						Rectangle bounding = createRectangle(screenPos, pntWidth);
						if (selRectOnScreen.intersects(bounding)) {

							// if the bounding box is entirely within the rectangle then include it
							boolean found = selRectOnScreen.contains(bounding);

							if (!found) {
								// otherwise do complex image test

								// get position in our test image by offsetting the from the test rectangle position in screen space
								Point2D imagePos = new Point2D.Double(screenPos.getX() - selRectOnScreen.x, screenPos.getY() - selRectOnScreen.y);
								Shape shape = createShape(getSymbolType(pnt), imagePos, pntWidth);
								if (g.hit(testRectangle, shape, false)) {
									found = true;
									// System.out.println(testRectangle + " hit interior");
								} else if (g.hit(testRectangle, shape, true)) {
									// System.out.println(testRectangle + " hit stroke");
									found = true;
								}
							}

							if (found) {
								ret.add(pnt);
							}
						}
					}
				} else {
					// get the on-screen bounds of the object
					CachedGeometry cachedGeometry = getCachedGeometry(pnt.getGeometry(), converter, true);
					if (cachedGeometry != null) {

						Rectangle2D wbb = cachedGeometry.getWorldBitmapBounds();
						Rectangle2D onScreenBounds = new Rectangle2D.Double(wbb.getMinX() - wbView.getMinX(), wbb.getMinY() - wbView.getMinY(), wbb.getWidth(), wbb.getHeight());
						if (onScreenBounds.intersects(selRectOnScreen)) {
							boolean found = false;

							if (selRectOnScreen.contains(onScreenBounds)) {
								// selection rectangle contains the bounds
								found = true;
							} else if (cachedGeometry.isDrawFilledBounds()) {
								// the entire bounds will be drawn; just check for intersection
								found = selRectOnScreen.intersects(onScreenBounds);
							} else {

								// complex render-based test...
								found = renderOrHitTestJTSGeometry(g, pnt, cachedGeometry.getJTSGeometry(), null, null, wbView, selRectOnScreen);
							}

							if (found) {
								ret.add(pnt);
							}
						}
					}
				}
			}
		} finally {
			g.dispose();
		}

		// System.out.println("NbImageTests: " + nbImageTests);
		return ret;
	}

	/**
	 * Fill the polygon, also widening it slightly so adjacent polygons on-screen will not show gaps between them
	 * 
	 * @param shape
	 * @param exterior
	 * @param col
	 * @param g2d
	 */
	private static void fillWidenedPolygon(Shape shape, Path2D exterior, final Color col, Graphics2D g2d, int viewportWidth, int viewportHeight) {
		Rectangle2D bounds = exterior.getBounds();

		// clip bounds to the viewport, remembering that shape is already relative to viewport
		Rectangle2D viewport = new Rectangle2D.Double(0, 0, viewportWidth, viewportHeight);
		bounds = viewport.createIntersection(bounds);

		// ensure image dimensions at least one
		int width = (int) Math.round(bounds.getWidth());
		width = Math.max(width, 1);
		int height = (int) Math.round(bounds.getHeight());
		height = Math.max(height, 1);

		double size = width * height;
		if (size < MAX_IMAGE_SIZE_LIMIT) {
			// draw on temporary image without alpha channel
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gImage = img.createGraphics();
			try {
				gImage.setClip(0, 0, img.getWidth(), img.getHeight());
				gImage.translate(-bounds.getX(), -bounds.getY());
				gImage.setColor(Colours.setAlpha(col, 255));
				gImage.fill(shape);

				BasicStroke stroke = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
				gImage.setStroke(stroke);
				gImage.draw(exterior);
			} finally {
				gImage.dispose();
			}

			RGBImageFilter rgbFilter = new RGBImageFilter() {

				@Override
				public int filterRGB(int x, int y, int rgb) {
					if (rgb != 0) {
						return (rgb & 0x00FFFFFF) | (col.getAlpha() << 24);
					} else {
						return 0;
					}
				}
			};

			// create image with alpha channel
			ImageProducer ip = new FilteredImageSource(img.getSource(), rgbFilter);
			Image alphaImg = Toolkit.getDefaultToolkit().createImage(ip);

			// draw this to the output graphics object
			g2d.drawImage(alphaImg, (int) bounds.getX(), (int) bounds.getY(), null);

		} else {
			// don't bother widening, just draw (image too big)
			g2d.setColor(col);
			g2d.fill(shape);
		}

		// g2d.drawImage(ImageUtils.createBlankImage(img.getWidth(), img.getHeight(), Color.GREEN), bounds.x, bounds.y,null);

	}

	private class LowLevelTextRenderer {

		Point2D getScreenPos(LatLongToScreen converter, DrawableObject pnt, Font font, Point2D.Double size) {
			Point2D screenPos;
			if (pnt.getGeometry() == null) {
				screenPos = converter.getOnScreenPixelPosition(pnt);

				// get text screen positioning, offsetting by a fraction of the font size and at least the point's pixel half-width
				int offset = (int) Math.round(STRING_OFFSET_FRACTION * font.getSize());
				offset = Math.max(offset, (int) Math.ceil(0.5 * pnt.getPixelWidth()) + 1);
				screenPos = new Point2D.Double(screenPos.getX() + offset, screenPos.getY());

			} else {
				// draw text in the centre of the object, adjusting for text size
				CachedGeometry cachedGeometry = getCachedGeometry(pnt.getGeometry(), converter, true);
				if (cachedGeometry == null) {
					return null;
				}

				Point2D wbPos = null;
				if (cachedGeometry.isLineString()) {
					wbPos = cachedGeometry.getLineStringMidPoint();
				}

				if (wbPos == null) {
					wbPos = cachedGeometry.getCentroid();
				}

				Rectangle2D view = converter.getViewportWorldBitmapScreenPosition();
				screenPos = new Point2D.Double(wbPos.getX() - view.getMinX() - size.getX() / 2, wbPos.getY() - view.getMinY() - size.getY() / 2);
			}

			return screenPos;
		}

		boolean renderDrawableText(Graphics2D g, LatLongToScreen converter, DrawableObject pnt, Quadtree textQuadtree) {
			if (Strings.isEmpty(pnt.getLabel())) {
				return false;
			}

			Font font = getFont((int)pnt.getFontSize());

			// get initial bounds (not in correct screen position)
			TextLayout textLayout = new TextLayout(pnt.getLabel(), font, g.getFontRenderContext());
			Point2D.Double size = getSize(textLayout);

			// get screen position from the point or geometry
			Point2D screenPos = getScreenPos(converter, pnt, font, size);
			if (screenPos == null) {
				return false;
			}

			return drawTextLayout(font, screenPos, textLayout, size, g, textQuadtree);
		}

		void renderInBottomRightCorner(String text, int fontSize,Graphics2D g,Quadtree textQuadtree){
			Font font = getFont(fontSize);
			TextLayout textLayout = new TextLayout(text, font, g.getFontRenderContext());
			Point2D.Double size = getSize(textLayout);
			
			Rectangle bounds = g.getClipBounds();
			Point2D screenPos = new Point2D.Double(bounds.getWidth() - size.x - 0, bounds.getHeight() - size.y - 0);
			drawTextLayout(font, screenPos, textLayout, size, g, textQuadtree);
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
		protected boolean drawTextLayout(Font font, Point2D screenPos, TextLayout textLayout, Point2D.Double size, Graphics2D g, Quadtree textQuadtree) {
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
			@SuppressWarnings("unchecked")
			List<Envelope> found = textQuadtree.query(envelope);
			boolean drawText = true;
			if (found != null) {
				for (Envelope other : found) {
					if (other.intersects(envelope)) {
						drawText = false;
						break;
					}
				}
			}

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
				g.setColor(LABEL_FONT_COLOUR);
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
		protected Point2D.Double getSize(TextLayout textLayout) {
			Rectangle2D initialBounds = textLayout.getBounds();
			Point2D.Double size = new Point2D.Double(initialBounds.getWidth(), initialBounds.getHeight());
			return size;
		}

		/**
		 * @param pnt
		 * @return
		 */
		protected Font getFont(int fontSize) {
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

	private static class DrawnSymbol {
		final boolean drawOutline;
		final Color color;
		final int pixelWidth;
		final boolean selected;
		// final Object objectKey;
		BufferedImage image;
		SymbolType symbolType;

		DrawnSymbol(SymbolType symbolType, boolean drawOutline, Color color, int pixelWidth, boolean selected) {
			this.symbolType = symbolType;
			this.drawOutline = drawOutline;
			this.color = color;
			this.pixelWidth = pixelWidth;
			this.selected = selected;
			// this.objectKey = objectKey;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((color == null) ? 0 : color.hashCode());
			result = prime * result + (drawOutline ? 1231 : 1237);
			// result = prime * result + ((objectKey == null) ? 0 : objectKey.hashCode());
			result = prime * result + pixelWidth;
			result = prime * result + symbolType.ordinal();
			result = prime * result + (selected ? 1231 : 1237);
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
			DrawnSymbol other = (DrawnSymbol) obj;
			if (color == null) {
				if (other.color != null)
					return false;
			} else if (!color.equals(other.color))
				return false;
			if (drawOutline != other.drawOutline)
				return false;
			// if (objectKey == null) {
			// if (other.objectKey != null)
			// return false;
			// } else if (!objectKey.equals(other.objectKey))
			// return false;
			if (pixelWidth != other.pixelWidth)
				return false;
			if (symbolType != other.symbolType) {
				return false;
			}
			if (selected != other.selected)
				return false;
			return true;
		}

		void initSymbol() {
			int dimension = 2 * (pixelWidth + symbols.getMaxOutline());
			image = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			g2.setClip(0, 0, image.getWidth(), image.getHeight());

			Point2D centre = new Point2D.Double(dimension / 2, dimension / 2);
			if (selected) {
				drawSymbol(g2, symbolType, centre, pixelWidth, color);
			} else {
				drawOutlinedSymbol(g2, symbolType, centre, pixelWidth, color, drawOutline);
			}
			g2.dispose();
		}
	}

	// private static Shape convertToShape(ODLSimpleGeom geom, LatLongToScreen converter, Rectangle screenClipBounds) {
	// int n = geom.size();
	// switch (geom.getGeomType()) {
	// case POINT:
	// throw new IllegalArgumentException();
	//
	// case LINE:
	// case POLYGON:
	// // get list of all points
	// if (n >= 2) {
	// PointsList pnts = new PointsList();
	// for (int i = 0; i < n; i++) {
	// Point2D pnt = converter.getOnScreenPixelPosition(geom.get(i));
	// pnts.add(pnt);
	// }
	//
	// if (screenClipBounds == null || pnts.getBounds().intersects(screenClipBounds)) {
	// n = pnts.size();
	// Path2D.Double path = new Path2D.Double();
	// for (int i = 0; i < n; i++) {
	// Point2D pnt = pnts.get(i);
	// if (i == 0) {
	// path.moveTo(pnt.getX(), pnt.getY());
	// } else {
	// path.lineTo(pnt.getX(), pnt.getY());
	// }
	// }
	//
	// if (geom.getGeomType() == SimpleGeomType.POLYGON) {
	// path.closePath();
	// }
	// return path;
	// }
	// }
	// break;
	// }
	//
	// return null;
	// }

	private static Point2D toOnscreen(Coordinate worldBitmapCoord, Rectangle2D viewport) {
		return new Point2D.Double(worldBitmapCoord.x - viewport.getX(), worldBitmapCoord.y - viewport.getY());
	}

	private static Path2D.Double toOnscreenPath(CoordinateSequence cs, Rectangle2D viewport) {
		Path2D.Double path = new Path2D.Double();
		int n = cs.size();
		for (int i = 0; i < n; i++) {
			Coordinate coord = cs.getCoordinate(i);
			double x = coord.x - viewport.getX();
			double y = coord.y - viewport.getY();
			if (i == 0) {
				path.moveTo(x, y);
			} else {
				path.lineTo(x, y);
			}
		}
		return path;
	}

	// private static Point2D.Double diff(Point2D.Double a , Point2D.Double b){
	// return new Point2D.Double(a.x-b.x, a.y-b.y);
	// }

	// private static Path2D.Double toOnscreenPathV2(CoordinateSequence cs, Rectangle2D viewport) {
	// int n = cs.size();
	//
	// class PointsList{
	// ArrayList<Point2D.Double> pnts = new ArrayList<>();
	// double x(int i){
	// return pnts.get(i).x;
	// }
	//
	// double y(int i){
	// return pnts.get(i).y;
	// }
	//
	// Point2D.Double get(int i){
	// return pnts.get(i);
	// }
	//
	// int size(){
	// return pnts.size();
	// }
	//
	// void remove(int i){
	// pnts.remove(i);
	// }
	//
	// Point2D.Double next(int i){
	// if(i+1<pnts.size()){
	// return pnts.get(i);
	// }
	// return pnts.get(0);
	// }
	//
	// Point2D.Double previous(int i){
	// if(i>0){
	// return pnts.get(i-1);
	// }
	// return pnts.get(pnts.size()-1);
	// }
	//
	// }
	//
	// PointsList list = new PointsList();
	//
	// // put in points list
	// for (int i = 0; i < n; i++) {
	// Coordinate coord = cs.getCoordinate(i);
	// double x = coord.x - viewport.getX();
	// double y = coord.y - viewport.getY();
	// list.pnts.add(new Point2D.Double(x, y));
	// }
	//
	// // remove duplications
	// int i =0 ;
	// while(i<list.size()){
	// if(list.get(i).equals(list.next(i))){
	// list.remove(i);
	// }else{
	// i++;
	// }
	// }
	//
	// // expand
	// PointsList expanded = new PointsList();
	// for(i =0 ; i< list.size();i++){
	// Point2D.Double current = list.get(i);
	// Point2D.Double previous = list.previous(i);
	// Point2D.Double next = list.next(i);
	//
	// Point2D.Double pd = diff(current, previous);
	// Point2D.Double nd = diff(current, next);
	// }
	//
	// // turn into path
	// Path2D.Double path = new Path2D.Double();
	// for (i = 0; i < n; i++) {
	// if (i == 0) {
	// path.moveTo(list.x(i),list.y(i));
	// } else {
	// path.lineTo(list.x(i),list.y(i));
	// }
	// }
	// return path;
	// }

	static BufferedImage createBaseImage(int imageWidth, int imageHeight, long renderFlags) {
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		if ((renderFlags & RenderProperties.SKIP_BACKGROUND_COLOUR_RENDERING) != RenderProperties.SKIP_BACKGROUND_COLOUR_RENDERING) {
			ImageUtils.fillImage(image, new Color(200, 200, 255, 255));
		}
		return image;
	}

	static boolean renderOrHitTestJTSGeometry(Graphics2D g, DrawableObject obj, Geometry geometry, Color col, Color outlineCol, Rectangle2D viewport, Rectangle hitTestOnScreen) {

		boolean hit = false;

		if (geometry == null) {
			throw new RuntimeException("Null geometry");
		}

		if (GeometryCollection.class.isInstance(geometry)) {
			int ng = geometry.getNumGeometries();
			for (int i = 0; i < ng; i++) {
				hit |= renderOrHitTestJTSGeometry(g, obj, geometry.getGeometryN(i), col, outlineCol, viewport, hitTestOnScreen);
			}
		} else {
			// Do further bounding box hit test; speeds up case where we have multigeometries
			// and we've only done test on the whole of them. This is particularly important in the
			// case of American Samoa and the mainland USA being the multipolygon, otherwise USA is
			// always rendered as its bounding box spans the Earth...

			Envelope bb = geometry.getEnvelopeInternal();
			Rectangle2D bounds = new Rectangle2D.Double(bb.getMinX(), bb.getMinY(), bb.getWidth(), bb.getHeight());
			if (bounds.intersects(viewport)) {

				Stroke oldStroke = g.getStroke();
				if (col != null) {
					g.setColor(col);
				}

				if (Point.class.isInstance(geometry)) {
					Point2D onscreen = toOnscreen(((Point) geometry).getCoordinate(), viewport);
					int width = (int) obj.getPixelWidth();
					if (hitTestOnScreen == null) {
						drawOutlinedSymbol(g, getSymbolType(obj), onscreen, width, col, obj.getDrawOutline() == 1);
					} else {
						hit |= g.hit(hitTestOnScreen, createShape(getSymbolType(obj), onscreen, width), false);
					}

				} else if (LineString.class.isInstance(geometry)) {
					LineString ls = (LineString) geometry;
					Path2D path = toOnscreenPath(ls.getCoordinateSequence(), viewport);

					BasicStroke stroke = new BasicStroke(obj.getPixelWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
					g.setStroke(stroke);
					if (hitTestOnScreen == null) {
						g.draw(path);
					} else {
						hit |= g.hit(hitTestOnScreen, path, true);
					}

				} else if (Polygon.class.isInstance(geometry)) {
					Polygon polygon = (Polygon) geometry;
					Path2D exterior = toOnscreenPath(polygon.getExteriorRing().getCoordinateSequence(), viewport);
					exterior.closePath();

					Shape shape = exterior;
					int nholes = polygon.getNumInteriorRing();
					if (nholes > 0) {
						Area area = new Area(exterior);
						shape = area;
						for (int i = 0; i < nholes; i++) {
							Path2D hole = toOnscreenPath(polygon.getInteriorRingN(i).getCoordinateSequence(), viewport);
							hole.closePath();
							area.subtract(new Area(hole));
						}
					}

					if (hitTestOnScreen == null) {
						// g.fill(shape);
						fillWidenedPolygon(shape, exterior, col, g, (int) Math.ceil(viewport.getWidth()), (int) Math.ceil(viewport.getHeight()));
					} else {
						hit |= g.hit(hitTestOnScreen, shape, false);
						hit |= shape.intersects(hitTestOnScreen);
					}

					// Draw the outline
					if (obj.getDrawOutline() != 0) {
						BasicStroke stroke = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
						g.setStroke(stroke);

						if (outlineCol != null) {
							g.setColor(outlineCol);
						}
						if (hitTestOnScreen == null) {
							g.draw(exterior);
						} else {
							hit |= g.hit(hitTestOnScreen, exterior, true);
						}
					}

				} else {
					throw new UnsupportedOperationException("Unsupported geometry type: " + geometry.getClass());
				}

				g.setStroke(oldStroke);

			}
		}

		// System.out.println(obj.getName() + " hit=" + hit);
		return hit;
	}

	private boolean renderGeometry(Graphics2D g, final LatLongToScreen converter, DrawableObject pnt, boolean isSelected) {
		boolean rendered = false;

		ODLGeomImpl geom = pnt.getGeometry();
		if (geom == null) {
			return false;
		}

		CachedGeometry transformed = getCachedGeometry(geom, converter, true);
		if (transformed == null) {
			return false;
		}
		// check for intersection with viewport
		Rectangle2D bounds = transformed.getWorldBitmapBounds();
		Rectangle2D viewport = converter.getViewportWorldBitmapScreenPosition();
		if (bounds.intersects(viewport) == false) {
			return false;
		}

		// get render colour
		final Color renderCol = isSelected ? SELECTION_COLOUR : getRenderColour(pnt);

		if (transformed.isDrawFilledBounds()) {
			g.setColor(renderCol);
			int x = (int) Math.round(bounds.getMinX() - viewport.getMinX());
			int y = (int) Math.round(bounds.getMinY() - viewport.getMinY());
			g.fillRect(x, y, (int) Math.round(bounds.getWidth()), (int) Math.round(bounds.getHeight()));
		} else {
			renderOrHitTestJTSGeometry(g, pnt, transformed.getJTSGeometry(), renderCol, getPolyOutlineCol(renderCol), viewport, null);
		}
		rendered = true;

		// if(isFreshRender){
		// System.out.println("Drawing " + pnt.getLabel() + " at zoom " + converter.getZoomHashmapKey()
		// + ", view " + viewport.toString()
		// + ", bounds " + bounds.toString()
		// );
		// }

		return rendered;
	}

	static Color getPolyOutlineCol(Color col) {
		return Colours.multiplyNonAlpha(col, SELECTION_DARKEN_OUTLINE_FACTOR);
	}

	public static CachedGeometry getCachedGeometry(ODLGeomImpl geom, LatLongToScreen converter, boolean createIfNotCached) {
		if (geom.isValid() == false) {
			return null;
		}
		CachedGeometry transformed = (CachedGeometry) geom.getFromCache(converter.getZoomHashmapKey());
		if (transformed == null && createIfNotCached) {
			// Keep geometry simplication off as it actually tends to slow things down, particulary for route editing with road networks
			transformed = new CachedGeometry(geom, false, converter);
			geom.putInCache(converter.getZoomHashmapKey(), transformed);
		}
		return transformed;
	}

	static boolean hasValidLatLong(DrawableObject obj) {
		if (Double.isNaN(obj.getLatitude()) || Double.isNaN(obj.getLongitude())) {
			return false;
		}
		return true;
	}

	boolean renderObject(Graphics2D g, LatLongToScreen converter, DrawableObject pnt, boolean isSelected) {

		boolean rendered = false;
		if (pnt.getGeometry() == null) {
			// get on-screen position
			if (hasValidLatLong(pnt)) {
				rendered = renderSymbol(g, pnt, converter.getOnScreenPixelPosition(pnt), isSelected);
			}
		} else {
			// // speed test
			// for(int i =0 ;i<100;i++){
			// rendered = renderGeometry(g, converter, pnt, isSelected, cache);
			// }
			rendered = renderGeometry(g, converter, pnt, isSelected);
		}

		return rendered;
	}

	private static SymbolType getSymbolType(DrawableObject obj) {
		SymbolType ret = null;
		if (!Strings.isEmpty(obj.getSymbol())) {
			ret = symbols.getType(obj.getSymbol());
		}
		if (ret == null) {
			ret = SymbolType.CIRCLE;
		}
		return ret;
	}

	boolean renderSymbol(Graphics2D g, DrawableObject pnt, Point2D screenPos, boolean isSelected) {
		boolean rendered = false;
		if (getPointIntersectsScreen(g, pnt, screenPos)) {

			// get colour
			Color col = getRenderColour(pnt);

			DrawnSymbol image = new DrawnSymbol(getSymbolType(pnt), pnt.getDrawOutline() == 1, isSelected ? SELECTION_COLOUR : col, (int) pnt.getPixelWidth(), isSelected);
			DrawnSymbol cached = null;
			if (circleImageCache != null) {
				synchronized (this) {
					cached = circleImageCache.get(image);
				}
			}
			if (cached != null) {
				// use cached image
				image = cached;
			} else {
				// create image
				image.initSymbol();
				if (circleImageCache != null) {
					synchronized (this) {
						circleImageCache.put(image, image);
					}
				}
			}
			g.drawImage(image.image, (int) (screenPos.getX() - image.image.getWidth() / 2), (int) (screenPos.getY() - image.image.getHeight() / 2), null);

			rendered = true;

		}
		return rendered;
	}

	boolean getPointIntersectsScreen(Graphics2D g, DrawableObject pnt, Point2D screenPos) {
		Rectangle rectangle = getPointBoundingRectangle(pnt, screenPos);
		boolean intersects = g.getClipBounds().intersects(rectangle);
		return intersects;
	}

	/**
	 * Gets a bounding rectangle in world bitmap coords for the input point (i.e. using the lat longs)
	 * 
	 * @param pnt
	 * @param converter
	 * @return
	 */
	public static Rectangle getWorldBitmapPointBoundingRectangle(DrawableObject pnt, LatLongToScreen converter) {
		if (pnt.getGeometry() != null) {
			throw new IllegalArgumentException();
		}
		if (hasValidLatLong(pnt)) {
			return getPointBoundingRectangle(pnt, converter.getWorldBitmapPixelPosition(pnt));
		}
		return null;
	}

	private static Rectangle getPointBoundingRectangle(DrawableObject pnt, Point2D screenPos) {
		Rectangle rectangle = createRectangle(screenPos, (int) pnt.getPixelWidth());
		return rectangle;
	}

	static Color getRenderColour(DrawableObject pnt) {
		Color col = pnt.getColour();
		if (!Strings.isEmpty(pnt.getColourKey())) {
			col = Colours.getRandomColour(pnt.getColourKey());
		}
		if (col == null) {
			col = DrawableObject.DEFAULT_COLOUR;
		}

		double opaque = pnt.getOpaque();
		opaque = MathUtil.clamp(opaque, 0, 1);
		col = Colours.setAlpha(col, (int) Math.round((255 * opaque)));
		return col;
	}

	static void drawOutlinedSymbol(Graphics2D g, SymbolType symbolType, Point2D screenPos, int circumferenceInPixels, Color col, boolean outlined) {
		int outer = symbolType.getOuterOutline();
		int inner = symbolType.getInnerOutline();

		// // no point outlining if the remaining shape is really small
		// if(circumferenceInPixels - outer <=5){
		// outlined = false;
		// }

		if (outlined) {

			// draw in black with the total circumference
			drawSymbol(g, symbolType, screenPos, circumferenceInPixels, Colours.setAlpha(Color.BLACK, col.getAlpha()));

			// subtract the difference between the inner and outer outline (this is the width of the black line)
			circumferenceInPixels -= (outer - inner);

			// draw in white
			drawSymbol(g, symbolType, screenPos, circumferenceInPixels, Colours.setAlpha(Color.WHITE, col.getAlpha()));
			circumferenceInPixels -= inner;

			// finally draw in correct colour
			drawSymbol(g, symbolType, screenPos, circumferenceInPixels, col);
		} else {
			drawSymbol(g, symbolType, screenPos, circumferenceInPixels, col);
		}
	}

	private static void drawSymbol(Graphics2D g, SymbolType shapeType, Point2D screenPos, int width, Color col) {
		Shape shape = createShape(shapeType, screenPos, width);
		g.setColor(col);
		g.fill(shape);
	}

	private static Shape createShape(SymbolType shapeType, Point2D screenPos, int width) {
		width = Math.max(1, width);
		return symbols.get(shapeType, screenPos.getX(), screenPos.getY(), width);
	}

	private static Rectangle createRectangle(Point2D centre, int length) {
		Rectangle rectangle = new Rectangle((int) centre.getX() - length / 2, (int) centre.getY() - length / 2, length, length);
		return rectangle;
	}

	// public boolean isRenderFade() {
	// return renderFade;
	// }
	//
	// public void setRenderFade(boolean renderFade) {
	// this.renderFade = renderFade;
	// }

	// public boolean isAllowDelayedGeometryRendering() {
	// return allowDelayedGeometryRendering;
	// }
	//
	// public void setAllowDelayedGeometryRendering(boolean allowDelayedGeometryRendering) {
	// this.allowDelayedGeometryRendering = allowDelayedGeometryRendering;
	// }

	// @Override
	// public void paint(Graphics2D g, Object object, int width, int height) {
	// ODLDatastore<MapTable> ds = mds.prepareDBForRendering();
	// renderLatLongPoints(g, ds, converter,false);
	//
	// }

}
