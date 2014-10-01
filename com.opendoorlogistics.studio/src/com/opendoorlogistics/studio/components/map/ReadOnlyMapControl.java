/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import org.jdesktop.swingx.OSMTileFactoryInfo;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.codefromweb.jxmapviewer2.DesktopPaneMapViewer;
import com.opendoorlogistics.core.gis.map.DatastoreRenderer;
import com.opendoorlogistics.core.gis.map.JXMapUtils;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer.TileReadyListener;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreenImpl;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.SwingUtils;


public class ReadOnlyMapControl extends DesktopPaneMapViewer {
	private final DefaultTileFactory tileFactory;
	private static final double DEFAULT_ZOOM_FRACTION = 0.975;
	
	//private LegendFrame legendFrame;
	protected final FilteredDrawablesContainer drawablesContainer = new FilteredDrawablesContainer(){

		@Override
		public void repaint() {
			ReadOnlyMapControl.this.repaint();
		}

		@Override
		public void zoomOnObjects(String legendKey) {
			zoomer.postZoomRequest(DEFAULT_ZOOM_FRACTION, legendKey);
		}
	};
	protected Rectangle viewport;
	private BufferedImage mapImage;
	private boolean reuseImageOnNextPaint;
	private GetToolTipCB getToolTipCB;
	private boolean isDisposed=false;
	private Timer timer;
	private MapConfig config;
	private ZoomBestFitManager bestFitManager = new ZoomBestFitManager() {
		
		@Override
		public void zoomBestFit(ReadOnlyMapControl viewer,final double maxFraction) {
			zoomer.postZoomRequest(maxFraction,null);
		}

	};
	private final Zoomer zoomer = new Zoomer();
	
	private class Zoomer{

		/**
		 * @param maxFraction
		 */
		private void postZoomRequest(final double maxFraction, final String legendFilter) {
			if(!SwingUtilities.isEventDispatchThread()){
				throw new RuntimeException();
			}
			
			// zoom if all geometry loaded, otherwise set timer to zoom if not already set
			if(!zoomIfLoaded(maxFraction,legendFilter) && timer==null){
				timer = new Timer(100, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
					//	System.out.println(new Date());
						if(isDisposed || zoomIfLoaded(maxFraction,legendFilter)){
							timer.stop();
							timer = null;
						};
					}
				});
				timer.start();
			}
		}

		private boolean zoomIfLoaded(double maxFraction, String legendFilter) {
			if(isAllGeometryLoaded()){
				ZoomUtils.zoomToBestFit(ReadOnlyMapControl.this, getGeopositionPointSet(legendFilter), maxFraction,false);
				return true;
			}
			return false;
		}
	}
	
//	private LegendCreator legendCreator = new LegendCreator() {
//		
//		@Override
//		public BufferedImage createLegend(Iterable<? extends DrawableObject> pnts) {
//			return Legend.createLegendImageFromDrawables( pnts) ;
//		}
//	};
	
	public static interface GetToolTipCB{
		String getToolTipText(ReadOnlyMapControl control,List<DrawableObject> selectedObjs);
	}
	
	public void dispose(){
		tileFactory.dispose();
		drawablesContainer.dispose();
		isDisposed = true;
	}
	
//	public static interface LegendCreator{
//		BufferedImage createLegend(Iterable<? extends DrawableObject> pnts);
//	}
	
	public interface ZoomBestFitManager{
		void zoomBestFit(ReadOnlyMapControl viewer, double maxFraction);
	}
	
//	public void setLegendCreator(LegendCreator legendCreator) {
//		this.legendCreator = legendCreator;
//	}

//	protected final LatLongToScreen converter = new LatLongToScreenImpl() {
//
//		@Override
//		public Rectangle2D getViewportWorldBitmapScreenPosition(){
//			return viewport;
//		}
//
//		@Override
//		public Point2D getWorldBitmapPixelPosition(LatLong latLong) {
//			GeoPosition pos = new GeoPosition(latLong.getLatitude(), latLong.getLongitude());
//			Point2D point = getTileFactory().geoToPixel(pos, getZoom());
//			return point;
//		}
//
//		@Override
//		public GeoPosition getLongLat(int pixelX, int pixelY) {
//			GeoPosition pos = getTileFactory().pixelToGeo(new Point2D.Float(pixelX + viewport.x, pixelY + viewport.y), getZoom());
//			return pos;
//		}
//
//		@Override
//		protected int getZoomLevel() {
//			return getZoom();
//		}
//	};

	/**
	 * Create a converter object that's immutable - i.e. later changes to the view
	 * or zoom are not reflected in the object.
	 * @return
	 */
	public LatLongToScreen createImmutableConverter(){
		final int currentZoom = getZoom();
		final Rectangle currentViewport = new Rectangle(viewport);
		return new LatLongToScreenImpl() {

			@Override
			public Rectangle2D getViewportWorldBitmapScreenPosition(){
				return currentViewport;
			}

			@Override
			public Point2D getWorldBitmapPixelPosition(LatLong latLong) {
				GeoPosition pos = new GeoPosition(latLong.getLatitude(), latLong.getLongitude());
				Point2D point = getTileFactory().geoToPixel(pos, currentZoom);
				return point;
			}

			@Override
			public LatLong getLongLat(int pixelX, int pixelY) {
				GeoPosition pos = getTileFactory().pixelToGeo(new Point2D.Float(pixelX + currentViewport.x, pixelY + currentViewport.y),currentZoom);
				return new LatLongImpl(pos.getLatitude(), pos.getLongitude());
			}

			@Override
			public Object getZoomHashmapKey() {
				return currentZoom;
			}

		};
	}
	
	public Iterable<? extends DrawableObject> getDrawables(){
		return drawablesContainer.getDrawables();
	}
	
	private boolean isAllGeometryLoaded(){
		for (DrawableObject pnt : getDrawables()) {
			if(pnt.getGeometry()!=null && pnt.getGeometry().isLoaded()==false){
				return false;
			}
		}
		return true;
	}
	
	public Set<GeoPosition> getGeopositionPointSet(String legendKeyFilter){
		HashSet<GeoPosition> positions = new HashSet<>();
		for(LatLong pnt: MapUtils.getLatLongs(getDrawables(), legendKeyFilter)){
			positions.add(new GeoPosition(pnt.getLatitude(), pnt.getLongitude()));							
		}
//		for (DrawableObject pnt : getPoints()) {
//			if(pnt.getGeometry()==null){
//				positions.add(new GeoPosition(pnt.getLatitude(), pnt.getLongitude()));				
//			}else if(pnt.getGeometry().isValid()){
//				for(Coordinate coord:pnt.getGeometry().getJTSGeometry().getCoordinates()){
//					// we use long-lat, not lat-long
//					positions.add(new GeoPosition(coord.y,coord.x));															
//				}
//			}
//		}		
		return positions;
	}
	
	public void setReuseImageOnNextPaint(){
		reuseImageOnNextPaint = true;
	}
	
	
	public RenderProperties getRenderFlags(){
		return drawablesContainer.getRenderFlags();
	}


	/**
	 * Set the objects to render. They are assumed to be immutable
	 * (i.e. do not change once set).
	 * @param pnts
	 */
	public void setDrawables( Iterable<? extends DrawableObject> pnts){
		drawablesContainer.setDrawables(pnts);
		
		//updateLegend();
		//showLegend();

//		desktopPane.add(frame);
//		frame.pack();
//		frame.setVisible(true);
//		LayoutUtils.placeInternalFrame(desktopPane, frame);
//		frame.toFront();
	}

	void showLegend() {
		SwingUtils.invokeLaterOnEDT(new Runnable() {
			
			@Override
			public void run() {
				LegendFrame frame = drawablesContainer.openLegend();
				add(frame);
				frame.pack();
				frame.setVisible(true);
				frame.toFront();
			}
		});

	}
	

	public ReadOnlyMapControl(MapConfig config) {
		this.config = config;
//		// Set loading image
//		try {
//			URL url = this.getClass().getResource("/resources/icons/image-loading.png");
//			this.setLoadingImage(ImageIO.read(url));
//
//		} catch (Throwable e) {
//			// TODO: handle exception
//		}

		// Create a TileFactoryInfo for OpenStreetMap
		TileFactoryInfo info = new OSMTileFactoryInfo();
		tileFactory = new DefaultTileFactory(info);
		tileFactory.setThreadPoolSize(4);

		// Setup local file cache
		JXMapUtils.initLocalFileCache();

		setTileFactory(tileFactory);

		// Set the focus and a sensible default location
		if (tileFactory.getInfo() != null) {
			setZoom(tileFactory.getInfo().getMaximumZoomLevel());
		}

		// set callback for when our own tiles are loaded
		drawablesContainer.addTileReadyListener(new TileReadyListener() {
			
			@Override
			public void tileReady(final Rectangle2D worldBitmapBounds, final Object zoom) {
				SwingUtils.invokeLaterOnEDT(new Runnable() {
					
					@Override
					public void run() {
						// repaint if an on-screen tile has been updated
						if(zoom.equals(getZoom())){
							Rectangle2D current = createImmutableConverter().getViewportWorldBitmapScreenPosition();
							if(current.intersects(worldBitmapBounds)){
								repaint();
							}					
						}
					}
				});
				
			}
		});
		// Add interactions
		initMouseListeners();
	//	addMouseListener(new CenterMapListener(this));
		addMouseWheelListener(new DesktopPaneZoomMouseWheelListenerCursor());
	//	addKeyListener(new PanKeyListener(this));
		addPainters();

		// register an empty tooltip so we get the getToolTipText callback
		setToolTipText("");
	}
	
	protected void initMouseListeners(){
		MouseInputListener mia = new DesktopPanePanMouseInputListener();
		addMouseListener(mia);
		addMouseMotionListener(mia);	
	}
	
	protected void addPainters(){
		CompoundPainter<Object> compoundPainter = new CompoundPainter<>();
		setOverlayPainter(compoundPainter);

		compoundPainter.addPainter(new Painter<Object>() {

			@Override
			public void paint(Graphics2D g, Object object, int width, int height) {
				drawablesContainer.renderObjects(g, createImmutableConverter(), null);
			}
		});	
	}

	@Override
	protected void paintComponent(Graphics g) {
		// hack... taken from jcomponent as don't want to call jxmapviewer's paintComponent method  
        if (ui != null) {
            Graphics scratchGraphics = (g == null) ? null : g.create();
            try {
                ui.update(scratchGraphics, this);
            }
            finally {
                scratchGraphics.dispose();
            }
        }
        
		// update viewport before rendering
		viewport = getViewportBounds();

		// draw to an image first. The image can be reused to allow the selection box to redrawn but not anything else
		if(mapImage == null || mapImage.getWidth() != getWidth() || mapImage.getHeight()!=getHeight() || reuseImageOnNextPaint==false){
			mapImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = mapImage.createGraphics();
			try {
				g2.setClip(0, 0, getWidth(), getHeight());
				
				int z = getZoom();
				Rectangle viewportBounds = getViewportBounds();
				drawMapTiles(g2, z, viewportBounds);
				if (getOverlayPainter() != null){
					getOverlayPainter().paint((Graphics2D) g2, this, getWidth(), getHeight());
				}	
			} finally {
				g2.dispose();	
			}
	
		}
		reuseImageOnNextPaint = false;
		
		g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null, null);
		
	}

	public void zoomBestFit() {
		zoomBestFit(DEFAULT_ZOOM_FRACTION);
	}
	
	public void zoomBestFit(double maxFraction) {
		if(bestFitManager!=null){
			bestFitManager.zoomBestFit(this, maxFraction);
		}
	}

	@Override
	protected void drawMapTiles(final Graphics g, final int zoom, Rectangle viewportBounds){
		if(drawablesContainer.getRenderFlags().hasFlag(RenderProperties.SHOW_BACKGROUND)){
			super.drawMapTiles(g, zoom, viewportBounds);
		}else{
			// fill white
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);
		}
	}
	
	public void setZoomBestFitManager(ZoomBestFitManager bestFitManager) {
		this.bestFitManager = bestFitManager;
	}


	@Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
            int condition, boolean pressed) {
		return super.processKeyBinding(ks, e, condition, pressed);
    }
    
	@Override
	public String getToolTipText(MouseEvent event){
		Rectangle rect = new Rectangle(event.getX()-1, event.getY()-1, 2, 2);
		List<DrawableObject> within = DatastoreRenderer.getObjectsWithinRectangle(drawablesContainer.getDrawables(), createImmutableConverter(), rect);
		if(within.size()>0 && getToolTipCB!=null){
			return getToolTipCB.getToolTipText(this,within);
		}

		return null;
	}
	

	public GetToolTipCB getGetToolTipCB() {
		return getToolTipCB;
	}

	public void setGetToolTipCB(GetToolTipCB getToolTipCB) {
		this.getToolTipCB = getToolTipCB;
	}

	public MapConfig getConfig() {
		return config;
	}

	public void setConfig(MapConfig config) {
		this.config = config;
	}
	
	
}
