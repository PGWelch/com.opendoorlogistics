package com.opendoorlogistics.studio.components.map;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JPanel;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider;
import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider.MapTile;
import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider.MapTileLoadedListener;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileListener;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;
import com.opendoorlogistics.core.gis.map.background.ODLTileFactory;
import com.opendoorlogistics.core.gis.map.data.BackgroundImage;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreenImpl;

public class MapViewPanel extends JPanel implements Disposable, MapTileLoadedListener {

	private final ODLTileFactory factory;

	private final MapApi mapi;

	private BufferedImage mapImage;

	// private boolean repaintPluginOverlapOnly;

	private boolean disablePaint;

	private boolean pendingFullRepaint;

	private boolean pendingRepaintPluginOverlapOnly;

	// private boolean skipMapDraw;

	// private final MapObjectsRenderer renderer;

	public MapViewPanel(MapApi mapi) {
		this.factory = BackgroundTileFactorySingleton.getFactory();
		this.mapi = mapi;

		// listen for map tiles being loaded
		BackgroundTileFactorySingleton.getFactory().addLoadedListener(this);

		// zoom is controlled by the map view panel (as its always valid)
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent evt) {
				Point current = evt.getPoint();
				Rectangle bound = getViewportBounds();
				int zoom = mapi.getZoom();
				int newZoom = zoom + evt.getWheelRotation();

				Point zoomCentreWorldBitmap = new Point(current.x + bound.x, current.y + bound.y);

				doZoom(newZoom, zoomCentreWorldBitmap);
			}
		});

		addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
					int code = e.getKeyCode();

					Point centre = new Point((int) mapi.getWorldBitmapMapCentre().getX(), (int) mapi.getWorldBitmapMapCentre().getY());

					// remember that equals shares the key with +
					if (code == KeyEvent.VK_PLUS || code == KeyEvent.VK_ADD || code == KeyEvent.VK_EQUALS) {
						doZoom(mapi.getZoom() - 1, centre);
					}
					if (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT) {
						doZoom(mapi.getZoom() + 1, centre);
					}
				}

			}
		});
	}

	public LatLongToScreen createImmutableConverter() {
		final int currentZoom = mapi.getZoom();
		final Rectangle currentViewport = getViewportBounds();
		return new LatLongToScreenImpl() {

			@Override
			public Rectangle2D getViewportWorldBitmapScreenPosition() {
				return currentViewport;
			}

			@Override
			public Point2D getWorldBitmapPixelPosition(LatLong latLong) {
				GeoPosition pos = new GeoPosition(latLong.getLatitude(), latLong.getLongitude());
				Point2D point = factory.geoToPixel(pos, currentZoom);
				return point;
			}

			@Override
			public LatLong getLongLat(double pixelX, double pixelY) {
				GeoPosition pos = factory.pixelToGeo(new Point2D.Double(pixelX + currentViewport.x, pixelY + currentViewport.y), currentZoom);
				return new LatLongImpl(pos.getLatitude(), pos.getLongitude());
			}

			@Override
			public Object getZoomHashmapKey() {
				return currentZoom;
			}

			@Override
			public int getZoomForObjectFiltering() {
				return currentZoom;
			}

		};
	}

	public Rectangle getViewportBounds() {
		Insets insets = getInsets();
		// calculate the "visible" viewport area in pixels
		int viewportWidth = getWidth() - insets.left - insets.right;
		int viewportHeight = getHeight() - insets.top - insets.bottom;
		Point2D centre = mapi.getWorldBitmapMapCentre();
		double viewportX = (centre.getX() - viewportWidth / 2);
		double viewportY = (centre.getY() - viewportHeight / 2);
		return new Rectangle((int) viewportX, (int) viewportY, viewportWidth, viewportHeight);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (isDisablePaint()) {
			return;
		}

		// draw to an image first. The image can be reused to allow the selection box to redrawn but not anything else
		boolean pluginOverlayOnlyRepaint = !pendingFullRepaint && pendingRepaintPluginOverlapOnly;
		if (mapImage == null || mapImage.getWidth() != getWidth() || mapImage.getHeight() != getHeight() || pluginOverlayOnlyRepaint == false) {

			// System.out.println("Full repaint " + new Date());

			mapImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = mapImage.createGraphics();
			try {
				g2.setClip(0, 0, getWidth(), getHeight());

				Rectangle viewportBounds = getViewportBounds();

				if ((mapi.getRenderFlags() & RenderProperties.SHOW_BACKGROUND) == RenderProperties.SHOW_BACKGROUND) {
					drawMapTiles(g2, viewportBounds);
				} else {
					// fill white
					g2.setColor(Color.WHITE);
					g2.fillRect(0, 0, getWidth(), getHeight());
				}

				paintMapObjects(g2);

			} finally {
				g2.dispose();
			}

		}

		pendingFullRepaint = false;
		pendingRepaintPluginOverlapOnly = false;

		BufferedImage modifiedImage = ((MapApiImpl) mapi).fireModifyMapImageListeners(mapi, mapImage);
		g.drawImage(modifiedImage, 0, 0, getWidth(), getHeight(), null, null);

		paintPluginOverlay((Graphics2D) g);
	}

	protected void paintMapObjects(Graphics2D g) {

	}

	protected void paintPluginOverlay(Graphics2D g) {

	}

	/**
	 * Adapted From jxmapviewer2
	 * 
	 * @param g
	 * @param zoom
	 * @param viewportBounds
	 */
	private void drawMapTiles(final Graphics g, Rectangle viewportBounds) {
		// get the tile factories...
		ODLTableReadOnly imagesTable = mapi.getMapDataApi().getBackgroundImagesTable();
		ArrayList<MapTileProvider> factories = new ArrayList<MapTileProvider>();
		if (imagesTable != null) {
			for (Object provider : BackgroundImage.BEAN_MAPPING.readObjectsFromTable(imagesTable)) {
				BackgroundImage mtp = (BackgroundImage) provider;
				if (mtp != null && mtp.getTileProvider()!=null) {
					factories.add(mtp.getTileProvider());
				}
			}
		}

		// add default factory if we haven't got any
		if (factories.size() == 0) {
			factories.add(BackgroundTileFactorySingleton.getFactory());
		}

		int zoomLevel = mapi.getZoom();
		int tileSize = factory.getTileSize(zoomLevel);
		Dimension mapSize = factory.getMapSize(zoomLevel);

		// calculate the "visible" viewport area in tiles
		int numWide = viewportBounds.width / tileSize + 2;
		int numHigh = viewportBounds.height / tileSize + 2;

		int tpx = (int) Math.floor(viewportBounds.getX() / tileSize);
		int tpy = (int) Math.floor(viewportBounds.getY() / tileSize);

		for (int x = 0; x <= numWide; x++) {
			for (int y = 0; y <= numHigh; y++) {
				int itpx = x + tpx;
				int itpy = y + tpy;

				if (g.getClipBounds().intersects(new Rectangle(itpx * tileSize - viewportBounds.x, itpy * tileSize - viewportBounds.y, tileSize, tileSize))) {

					for (MapTileProvider currentFactory : factories) {
						MapTile tile = currentFactory.getMapTile(itpx, itpy, zoomLevel);
						if (tile == null) {
							continue;
						}
						int ox = ((itpx * tileSize) - viewportBounds.x);
						int oy = ((itpy * tileSize) - viewportBounds.y);

						// if the tile is off the map to the north/south, then just
						// don't paint anything
						if (itpx < 0 || itpy < 0 || itpx > mapSize.width || itpy > mapSize.height) {
							if (isOpaque()) {
								g.setColor(Color.WHITE);
								g.fillRect(ox, oy, tileSize, tileSize);
							}

							// assuming tile size is 256, draw grid
							g.setColor(Color.GRAY);
							int xy = 0;
							while (xy <= tileSize) {
								g.drawLine(ox + xy, oy, ox + xy, oy + tileSize);
								g.drawLine(ox, oy + xy, ox + tileSize, oy + xy);
								xy += 32;
							}
						} else if (tile.isLoaded()) {
							g.drawImage(tile.getImage(), ox, oy, null);
						} else {
							// Use tile at higher zoom level with 200% magnification
							MapTile superTile = factory.getMapTile(itpx / 2, itpy / 2, zoomLevel + 1);

							if (superTile.isLoaded()) {
								int offX = (itpx % 2) * tileSize / 2;
								int offY = (itpy % 2) * tileSize / 2;
								g.drawImage(superTile.getImage(), ox, oy, ox + tileSize, oy + tileSize, offX, offY, offX + tileSize / 2, offY + tileSize / 2,
										null);
							} else {
								g.setColor(Color.GRAY);
								g.fillRect(ox, oy, tileSize, tileSize);
							}
						}
					}
				}
			}
		}
	}

	private void doZoom(int newZoom, Point zoomCentreWorldBitmap) {
		// get pixel offset between zoom position and control centre
		Point2D center = mapi.getWorldBitmapMapCentre();
		Point2D offset = new Point2D.Double(zoomCentreWorldBitmap.x - center.getX(), zoomCentreWorldBitmap.y - center.getY());

		// convert position to fraction of map size
		int zoom = mapi.getZoom();
		int tileSize = factory.getTileSize(zoom);
		Dimension mapSize = factory.getMapSize(zoom);
		long mapWidthPixels = mapSize.width * tileSize;
		long mapHeightPixels = mapSize.height * tileSize;
		double x = zoomCentreWorldBitmap.getX() / mapWidthPixels;
		double y = zoomCentreWorldBitmap.getY() / mapHeightPixels;

		// get new zoom
		newZoom = Math.max(newZoom, mapi.getMinZoom());
		newZoom = Math.min(newZoom, mapi.getMaxZoom());

		// get new map dimensions
		tileSize = factory.getTileSize(newZoom);
		mapSize = factory.getMapSize(newZoom);
		mapWidthPixels = mapSize.width * tileSize;
		mapHeightPixels = mapSize.height * tileSize;

		// get zoom position in the new map world bitmap dimensions
		Point newPosition = new Point((int) Math.abs(x * mapWidthPixels), (int) Math.abs(y * mapHeightPixels));

		// get the new map centre
		Point2D newCentre = new Point2D.Double(newPosition.getX() - offset.getX(), newPosition.getY() - offset.getY());

		mapi.setView(newZoom, newCentre);
	}

	@Override
	public void dispose() {
		BackgroundTileFactorySingleton.getFactory().removeLoadedListener(this);
	}

	@Override
	public void tileLoaded(MapTile tile) {
		if (tile.getZoom() == mapi.getZoom()) {
			repaint();
		}
	}

	public void repaint(boolean repaintPluginOverlapOnly) {
		// this.repaintPluginOverlapOnly = repaintPluginOverlapOnly;

		if (repaintPluginOverlapOnly) {
			pendingRepaintPluginOverlapOnly = true;
		} else {
			pendingFullRepaint = true;
		}
		super.repaint();
	}

	@Override
	public void repaint() {
		pendingFullRepaint = true;
		super.repaint();
	}

	public boolean isDisablePaint() {
		return disablePaint;
	}

	public void setDisablePaint(boolean disablePaint) {
		this.disablePaint = disablePaint;
	}

}
