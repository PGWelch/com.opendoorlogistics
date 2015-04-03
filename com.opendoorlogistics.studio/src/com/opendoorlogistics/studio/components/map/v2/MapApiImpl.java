package com.opendoorlogistics.studio.components.map.v2;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLListener.ODLListenerType;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.DatastoreRenderer;
import com.opendoorlogistics.core.gis.map.JXMapUtils;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.LatLongBoundingBox;
import com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer;
import com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer.TileReadyListener;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.opendoorlogistics.core.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.core.tables.beans.BeanMappedRow;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.ListenerDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.SetUtils;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.components.map.LayeredDrawables;
import com.opendoorlogistics.studio.components.map.MapConfig;
import com.opendoorlogistics.studio.components.map.MapModePermissions;
import com.opendoorlogistics.studio.components.map.MapViewerComponent;
import com.opendoorlogistics.studio.components.map.v2.MapApiListeners.OnDisposedListener;
import com.opendoorlogistics.studio.components.map.v2.plugins.CreatePointPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.DeleteSelectedObjects;
import com.opendoorlogistics.studio.components.map.v2.plugins.FillPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.LegendPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.MovePointPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.PanMapPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.RenderCheckboxesPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.SelectPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.SnapshotPlugin;


/**
 * The implementation of the api object is just an aggregate of other objects which pumps
 * messages between them as needed.
 * @author Phil
 *
 */
public class MapApiImpl extends MapApiListenersImpl implements MapApi , Disposable{
	private final MapSelectionState selectionState;
	private final ViewPosition position;
	private final DisposablePanel containerLevel1Panel;
	private final JPanel containerLevel2Panel;
	private final MapToolbar toolBar;
	private final ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs;
	private final JPanel [] sidePanels = new JPanel[PanelPosition.values().length];
	private final ComponentControlLauncherApi componentControlLauncherApi;
	//private final double []sidePanelSizes = new double[PanelPosition.values().length];
	private final MapViewPanel mapViewPanel;
	private MapConfig mapConfig;
	private MapModePermissions permissions;
	private TileCacheRenderer renderer;
	private long renderFlags = RenderProperties.SHOW_ALL;
//	private LayeredDrawables allObjects;
	private LayeredDrawables filteredObjects;
	private MapMode mode;
	private ODLDatastore<? extends ODLTable> mapDatastore;
	
	class DisposablePanel extends JPanel implements Disposable{

		@Override
		public void dispose() {
			MapApiImpl.this.dispose();
		}
		
	}
	
	public MapApiImpl( List<MapPlugin> pluginFactories, MapConfig config,
			MapModePermissions permissions,
			ComponentControlLauncherApi componentControlLauncherApi,
			ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs,
			ODLDatastore<? extends ODLTable> mapDatastore){
		this.globalDs = globalDs;
		this.mapConfig = config;
		this.permissions = permissions;
		this.componentControlLauncherApi = componentControlLauncherApi;
		this.selectionState = new MapSelectionState();
		this.position = new ViewPosition();
			
		// Set default side panel sizes
	//	Arrays.fill(sidePanelSizes, 0.2);
		
		// Init renderer and set callback for when our own tiles are loaded
		renderer = new TileCacheRenderer();
		renderer.addTileReadyListener(new TileReadyListener() {

			@Override
			public void tileReady(final Rectangle2D worldBitmapBounds, final Object zoom) {
				SwingUtils.invokeLaterOnEDT(new Runnable() {

					@Override
					public void run() {
						// repaint if an on-screen tile has been updated
						if (zoom.equals(getZoom())) {
							Rectangle2D current = createImmutableConverter().getViewportWorldBitmapScreenPosition();
							if (current.intersects(worldBitmapBounds)) {
								repaint(false);
							}
						}
					}
				});

			}
		});
		
		// Init container panel
		containerLevel1Panel = new DisposablePanel();
		containerLevel1Panel.setLayout(new BorderLayout());
		containerLevel2Panel = new JPanel();
		containerLevel2Panel.setLayout(new BorderLayout());
		containerLevel1Panel.add(containerLevel2Panel, BorderLayout.CENTER);
		
		// init the map panel
		mapViewPanel = new MapViewPanel(this){
			
			@Override
			protected void paintMapObjects(Graphics2D g){
				renderer.renderObjects(g, mapViewPanel.createImmutableConverter(), renderFlags, selectionState.copySet());				
			}
			
			@Override
			protected void paintPluginOverlay(Graphics2D g){
				// the plugins
				fireOnPaintListeners(MapApiImpl.this,g);
				
				// the mode
				if(mode!=null){
					mode.paint(MapApiImpl.this, g);
				}	
			}
			
		};
		mapViewPanel.addMouseListener(this);
		mapViewPanel.addMouseMotionListener(this);
		mapViewPanel.addKeyListener(this);
	//	containerPanel.add(mapViewPanel,BorderLayout.CENTER);
		
		// Create plugins
		if(pluginFactories!=null){
			for(MapPlugin factory : pluginFactories){
				factory.initMap(this);
			}			
		}
		
		// Init toolbar
		toolBar = new MapToolbar();
		toolBar.setFloatable(false);
		fireBuildToolbarListeners(this,toolBar);
		containerLevel1Panel.add(toolBar, BorderLayout.NORTH);

	//	containerPanel.add(toolBar, BorderLayout.NORTH);
		
		// Add all controls to the main panel
		initContainerPanel(false);
		
		// Init right-click menu on the map
		mapViewPanel.addMouseListener(new PopupMenuMouseAdapter() {

			@Override
			protected void launchMenu(MouseEvent me) {
				MapPopupMenu menu = new MapPopupMenu();
				fireBuildContextMenuListeners(MapApiImpl.this, menu);
				menu.show(me.getComponent(), me.getX(), me.getY());
			}
		});
		
		
		setObjects(mapDatastore);
		
		// Zoom all by default
		addOnGeometryLoadedCallback(new Runnable() {
			
			@Override
			public void run() {
				setViewToBestFit(getMapDataApi().getFilteredAllLayersTable());
			}
		});
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		mapViewPanel.requestFocus();
		super.mouseClicked(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mapViewPanel.requestFocus();
		super.mousePressed(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mapViewPanel.requestFocus();
		super.mouseDragged(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mapViewPanel.requestFocus();
		super.mouseMoved(e);
	}

	
	@Override
	public void mouseEntered(MouseEvent e) {
		mapViewPanel.requestFocus();
		super.mouseEntered(e);
	}
	
	void setObjects(ODLDatastore<? extends ODLTable> mapDatastore){
		this.mapDatastore = mapDatastore;		
		updateObjectFiltering();		
	}
	
	
	public DisposablePanel getPanel(){
		return containerLevel1Panel;
	}

	private LayeredDrawables combineInputTables(final boolean isFiltered){
		final ODLTable background =TableUtils.findTable(mapDatastore,MapViewerComponent.INACTIVE_BACKGROUND);
		final ODLTable activeTable =TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES);
		final ODLTable foreground =TableUtils.findTable(mapDatastore,MapViewerComponent.INACTIVE_FOREGROUND);
		
		// filter the objects
		class Filter{
			
			Iterable<? extends DrawableObject> filter(ODLTableReadOnly table){
				if(table == null){
					return null;
				}
				
				BeanTableMapping btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
				int n = table.getRowCount();
				ArrayList<DrawableObject> ret = new ArrayList<DrawableObject>(n);
				for(int i =0 ; i < n ; i++){
					boolean accept = !isFiltered || fireFilterObject(MapApiImpl.this, table, i);
					if(accept){
						DrawableObject obj = btm.readObjectFromTableByRow(table, i);
						if(obj!=null){
							ret.add(obj);
						}					
					}
	
				}
				return ret;
			}
		}
		
		Filter filter = new Filter();
		
		LayeredDrawables layered = new LayeredDrawables(filter.filter(background),filter.filter(activeTable), filter.filter(foreground));

		return layered;
	}
	
	@Override
	public void updateObjectFiltering() {
	
		filteredObjects = combineInputTables(true);
			
		renderer.setObjects(filteredObjects);	
		
		// update select ids
		TLongHashSet newSelected = new TLongHashSet();
		for(DrawableObject o : filteredObjects){
			if(selectionState.contains(o.getGlobalRowId())){
				newSelected.add(o.getGlobalRowId());
			}
		}
		
		if(!selectionState.equals(newSelected)){
			setSelectedIds(newSelected.toArray());
		}
		
		fireObjectsChangedListeners(this);
		mapViewPanel.repaint();
	}
	
	@Override
	public int getZoom() {
		return position.getZoom();
	}



	@Override
	public Point2D getWorldBitmapMapCentre() {
		return position.getCenter();
	}

//	@Override
//	public void setReuseImageOnNextPaint() {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public void repaint(boolean repaintPluginOverlapOnly) {
		mapViewPanel.repaint(repaintPluginOverlapOnly);
	}

	@Override
	public long[] getSelectedIds() {
		return selectionState.copyIds();
	}

	@Override
	public long[] getSelectableIdsWithinPixelRectangle(Rectangle screenCoordinatesRectangle) {
		TLongArrayList within = DatastoreRenderer.getWithinRectangle(filteredObjects, createImmutableConverter(), screenCoordinatesRectangle,true);
		return within.toArray();
	}


	@Override
	public void clearSelection() {
		setSelectedIds(new long[]{});
	}


	@Override
	public void setSelectedIds(long... ids) {
		selectionState.set(ids);
		fireSelectionChangedListeners(this);
		repaint(false);
	}

	@Override
	public Component getMapUIComponent() {
		return mapViewPanel;
	}


	@Override
	public void setCursor(Cursor cursor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMapMode(MapMode newMode) {
		MapMode oldMode = mode;
		
		if(oldMode!=null){
			removeMouseInputListener(oldMode);
			oldMode.onExitMode(this);
		}
		
		// turn the cursor back to normal
		mapViewPanel.setCursor(Cursor.getDefaultCursor());
		
		fireModeChangingListener(this,oldMode, newMode);
		mode = newMode;
		fireModeChangedListener(this,oldMode, mode);
		
		if(newMode!=null){
			registerMouseInputListener(newMode, Integer.MIN_VALUE);
			newMode.onEnterMode(this);
			
			if(newMode.getCursor()!=null){
				mapViewPanel.setCursor(newMode.getCursor());
			}
		}
	}

	@Override
	public long getRenderFlags() {
		return renderFlags;
	}

	@Override
	public void setRenderFlags(long flags) {
		this.renderFlags = flags;
		mapViewPanel.repaint();
	}

	@Override
	public LatLongToScreen createImmutableConverter() {
		return mapViewPanel.createImmutableConverter();
	}

	@Override
	public void dispose() {
		renderer.dispose();
		fireDisposedListeners(this);
		
		for(JPanel d : sidePanels){
			if(d!=null){
				((Disposable)d).dispose();
			}
		}
	}

	@Override
	public void setView(int zoom, Point2D centreInPixels) {
		position.setCenter(centreInPixels);
		position.setZoom(zoom);
		fireViewChangedListeners(this);
		mapViewPanel.repaint();
	
	}

	@Override
	public int getMinZoom() {
		return position.getMinZoom();
	}

	@Override
	public int getMaxZoom() {
		return position.getMaxZoom();
	}

	@Override
	public boolean isSelectedId(long id) {
		return selectionState.contains(id);
	}

	public static void main(String [] args){
		InitialiseStudio.initialise(false);
		
		final ODLDatastoreAlterable<? extends ODLTableAlterable> exampleds = MapUtils.createExampleDatastore();
		ODLDatastoreAlterable<? extends ODLTableAlterable> listener = new ListenerDecorator(ODLTableAlterable.class, exampleds);
		ODLDatastoreUndoable<? extends ODLTableAlterable> undoable = new UndoRedoDecorator<ODLTableAlterable>(ODLTableAlterable.class, listener);
		
		GlobalMapSelectedRowsManager gsm = new GlobalMapSelectedRowsManager() {
			
			@Override
			public void onMapSelectedChanged() {
				// TODO Auto-generated method stub
				
			}
		};

		
		List<MapPlugin> plugins = new ArrayList<MapPlugin>();
		plugins.add(new PanMapPlugin());
		plugins.add(new LegendPlugin());
		plugins.add(new SelectPlugin());
		plugins.add(new FillPlugin());
		plugins.add(new RenderCheckboxesPlugin());
		plugins.add(new SnapshotPlugin());
		plugins.add(new CreatePointPlugin());
		plugins.add(new MovePointPlugin());
		plugins.add(new DeleteSelectedObjects());
		MapConfig config = new MapConfig();
		MapModePermissions permissions = new MapModePermissions(exampleds.getTableAt(0).getFlags());
		

		final ODLApi api = new ODLApiImpl();
		ComponentControlLauncherApi dummyApi = new ComponentControlLauncherApi(){

			@Override
			public JPanel getRegisteredPanel(String panelId) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T extends JPanel & Disposable> boolean registerPanel(String panelId, String title, T panel, boolean refreshable) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public List<JPanel> getRegisteredPanels() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void disposeRegisteredPanel(JPanel panel) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setTitle(JPanel panel, String title) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void toFront(JPanel panel) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public ODLApi getApi() {
				return api;
			}
			
		};
		
		final MapApiImpl mapApi =new MapApiImpl(plugins, config, permissions,dummyApi, undoable, exampleds);
		mapApi.connectToGSM( gsm);
		
		undoable.addListener(new ODLListener() {
			
			@Override
			public void tableChanged(int tableId, int firstRow, int lastRow) {
				mapApi.setObjects(exampleds);
			}
			
			@Override
			public ODLListenerType getType() {
				return ODLListenerType.TABLE_CHANGED;
			}
			
			@Override
			public void datastoreStructureChanged() {
				// TODO Auto-generated method stub
				
			}
		}, exampleds.getTableAt(0).getImmutableId());
		
		ShowPanel.showPanel(mapApi.getPanel(), true);
	}

	@Override
	public Rectangle getWorldBitmapViewport() {
		return mapViewPanel.getViewportBounds();
	}

	public void connectToGSM(GlobalMapSelectedRowsManager gsm){
		gsm.registerMapSelectionList(this);
		registerDisposedListener(new OnDisposedListener() {
			
			@Override
			public void onDispose(MapApi api) {
				gsm.unregisterMapSelectionList(api);
			}
		}, 0);
	}
	
	private void addOnGeometryLoadedCallback(final Runnable runnable) {
		
		class Handler implements ActionListener{
			Timer timer;
			
			Handler(){
				timer = new Timer(100,this);
				timer.start();
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(filteredObjects!=null){
					for (DrawableObject pnt : filteredObjects) {
					if (pnt.getGeometry() != null && pnt.getGeometry().isLoaded() == false) {
						return;
					}
				}	
				}
				timer.stop();
				runnable.run();
			}
			
		}
		new Handler();
	}

	@Override
	public void setViewToBestFit(ODLTableReadOnly drawables){
		double defaultEdgeSizeDegrees = 1;
		double maxFraction = 0.975;
		Iterable<? extends DrawableObject> objs = MapUtils.getDrawables(drawables);
		LatLongBoundingBox llbb = MapUtils.getLatLongBoundingBox(objs, null);
		class Helper{
			 Set<GeoPosition> createGeopositionsSquareAroundPoint(double latitude, double longitude, double edgeSizeDegrees) {
				HashSet<GeoPosition> dummies = new HashSet<>();
				for (int lat = -1; lat <= 1; lat += 2) {
					for (int lng = -1; lng <= 1; lng += 2) {
						dummies.add(new GeoPosition(latitude + lat * edgeSizeDegrees, longitude + lng * edgeSizeDegrees));
					}
				}
				return dummies;
			}	
		 
		}	
		Helper helper = new Helper();
		TileFactory tileFactory = BackgroundTileFactorySingleton.getFactory();
		TileFactoryInfo info = tileFactory.getInfo();		
		
		Set<GeoPosition> positions = llbb.getCornerSet();
		if (positions.size() > 0) {
			if(positions.size()==1){
				GeoPosition pos = SetUtils.getFirst(positions);
				positions = helper.createGeopositionsSquareAroundPoint(pos.getLatitude(), pos.getLongitude(), defaultEdgeSizeDegrees);
			}
			
			if (info != null) {
				Rectangle viewBounds = getWorldBitmapViewport();
				int bfz = JXMapUtils.getBestFitZoom(info, positions, viewBounds.width, viewBounds.height, maxFraction).getFirst();
				Rectangle2D bounds = JXMapUtils.generateBoundingRect(info, positions, bfz);
//				GeoPosition centre = tileFactory.pixelToGeo(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), bfz);
				setView(bfz, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
			}
		} 		
	}

	@Override
	public Dimension getWorldBitmapMapSize(int zoom) {
		zoom = clampZoom(zoom);
		Dimension inTiles = BackgroundTileFactorySingleton.getFactory().getMapSize(zoom);
		int sz = BackgroundTileFactorySingleton.getFactory().getTileSize(zoom);
		
		return new Dimension(inTiles.width * sz, inTiles.height * sz);
	}

	private int clampZoom(int zoom) {
		if(zoom > getMaxZoom()){
			zoom = getMaxZoom();
		}
		if(zoom < getMinZoom()){
			zoom = getMinZoom();
		}
		return zoom;
	}

	@Override
	public void setZoom(int newZoom) {
		newZoom = clampZoom(newZoom);
		int oldZoom = getZoom();
		Dimension oldSize = getWorldBitmapMapSize(oldZoom);
		Dimension newSize = getWorldBitmapMapSize(newZoom);
		double xRatio = (double)newSize.width / oldSize.width;
		double yRatio = (double)newSize.height / oldSize.height;
		Point2D.Double newCentre = new Point2D.Double(getWorldBitmapMapCentre().getX() * xRatio, getWorldBitmapMapCentre().getY() * yRatio);
		setView(newZoom, newCentre);
	}

	@Override
	public <T extends JPanel & Disposable> void setSidePanel(T panel, PanelPosition pos) {
		int i = pos.ordinal();
		
		if(sidePanels[i]!=panel){
			if(sidePanels[i]!=null && sidePanels[i] !=panel){
				((Disposable)sidePanels[i]).dispose();
			}
			
			sidePanels[i] = panel;
			initContainerPanel(true);			
		}
	
	}

	private void initContainerPanel(boolean isReinit){
		class Helper{
			Point getMapViewOnscreenCentre(){
				Rectangle viewBounds = mapViewPanel.getBounds();
				Point onScreenLocation =getLocationRelToContainerPanel( mapViewPanel);
			//	System.out.println(viewBounds);
				return new Point(onScreenLocation.x + viewBounds.width/2, onScreenLocation.y + viewBounds.height/2);
			}
			
			Point getLocationRelToContainerPanel(Component component){
				if(component == containerLevel2Panel){
					return new Point(0, 0);
				}else{
					Point relToParent = component.getLocation();
					Point parentRel = getLocationRelToContainerPanel(component.getParent());
					return new Point(relToParent.x + parentRel.x, relToParent.y + parentRel.y);
				}
			}
		}
		Helper helper = new Helper();
		final Point originalMapCentreOnScreenLoc;
		if(isReinit){
			originalMapCentreOnScreenLoc= helper.getMapViewOnscreenCentre();
			containerLevel2Panel.removeAll();			
		}else{
			originalMapCentreOnScreenLoc = null;
		}

		Component component = mapViewPanel;
		if(sidePanels[PanelPosition.RIGHT.ordinal()]!=null){
			JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapViewPanel, sidePanels[PanelPosition.RIGHT.ordinal()]);
			splitter.setResizeWeight(0.8);
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					Container container = splitter.getParent();
					if(container!=null){
						int width = container.getWidth();
						splitter.setDividerLocation(width- width / 5);						
					}
				}
			});	
			component = splitter;
		}
		
		if(sidePanels[PanelPosition.LEFT.ordinal()]!=null){
			JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidePanels[PanelPosition.LEFT.ordinal()],component);
			splitter.setResizeWeight(0.2);
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					Container container = splitter.getParent();
					if(container!=null){
						int width = container.getWidth();
						splitter.setDividerLocation( width / 5);						
					}
				}
			});			
			component = splitter;
		}

		if(sidePanels[PanelPosition.BOTTOM.ordinal()]!=null){
			final JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, component, sidePanels[PanelPosition.BOTTOM.ordinal()]);
			splitter.setResizeWeight(0.8);
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					Container container = splitter.getParent();
					if(container!=null){
						int height = container.getHeight();
						splitter.setDividerLocation(height - height / 4);						
					}
				}
			});
			component = splitter;
		}
		
		containerLevel2Panel.add(component,BorderLayout.CENTER);
		
		if(isReinit){
			mapViewPanel.setNextFrameBlank(true);
			containerLevel2Panel.revalidate();
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					Point newMapCentreOnScreenLoc = helper.getMapViewOnscreenCentre();
					Point offset = new Point(newMapCentreOnScreenLoc.x- originalMapCentreOnScreenLoc.x , newMapCentreOnScreenLoc.y- originalMapCentreOnScreenLoc.y );
					Point2D centre = getWorldBitmapMapCentre();
					setView(getZoom(), new Point2D.Double(centre.getX() + offset.getX(), centre.getY() + offset.getY()));					
				}
			});
//			helper.getMapViewOnscreenCentre();
		}
		
//		else{
//			containerLevel2Panel.revalidate();
//			containerLevel2Panel.repaint();			
//		}
		
	//	System.out.println();
	}

	@Override
	public Point getWorldBitmapPosition(LatLong ll, int zoom) {
		Point2D pnt= BackgroundTileFactorySingleton.getFactory().geoToPixel(new GeoPosition(ll.getLatitude(), ll.getLongitude()), zoom);
		return new Point((int) Math.round(pnt.getX()), (int)Math.round(pnt.getY()));
	}

	@Override
	public ComponentControlLauncherApi getControlLauncherApi() {
		return componentControlLauncherApi;
	}

	@Override
	public MapDataApi getMapDataApi() {
		return new MapDataApi() {
			

			@Override
			public ODLTable getGlobalTable(int tableId) {
				return globalDs!=null ? globalDs.getTableByImmutableId(tableId):null;
			}

			@Override
			public void runTransactionOnGlobalDatastore(Callable<Boolean> toRun) {
				if(globalDs!=null){
					TableUtils.runTransaction(globalDs, toRun);			
				}
			}

			@Override
			public int getLatitudeColumn() {
				return DrawableObjectImpl.COL_LATITUDE;
			}

			@Override
			public int getLongitudeColumn() {
				return DrawableObjectImpl.COL_LONGITUDE;
			}

			@Override
			public int getGeomColumn() {
				return DrawableObjectImpl.COL_GEOMETRY;
			}
			
			@Override
			public ODLTable getUnfilteredDrawableTable() {
				return TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES);
			}

			@Override
			public ODLTable getUnfilteredInactiveForegroundTable() {
				return TableUtils.findTable(mapDatastore,MapViewerComponent.INACTIVE_FOREGROUND);
			}

			@Override
			public ODLTable getUnfilteredInactiveBackgroundTable() {
				return TableUtils.findTable(mapDatastore,MapViewerComponent.INACTIVE_BACKGROUND);
			}

			@Override
			public int getLegendKeyColumn() {
				return DrawableObjectImpl.COL_LEGEND_KEY;
			}

			@Override
			public ODLTableReadOnly getFilteredAllLayersTable() {
				return toTable(filteredObjects);
			}


			@Override
			public ODLDatastoreUndoable<? extends ODLTableAlterable> getGlobalDatastore() {
				return globalDs;
			}

			@Override
			public ODLTableReadOnly getUnfilteredAllLayersTable() {
				return toTable(combineInputTables(false));
			}

		};
	}
	
	private ODLTableReadOnly toTable(Iterable<? extends DrawableObject> objs) {
		BeanTableMapping btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
		LinkedList<BeanMappedRow> rows = new LinkedList<BeanMappedRow>();
		for(DrawableObject o : objs){
			rows.add((BeanMappedRow)o);
		}
		return btm.writeObjectsToTable(rows.toArray(new BeanMappedRow[rows.size()]));
	}

	@Override
	public <T extends JPanel & Disposable> T getSidePanel(PanelPosition pos) {
		return (T)sidePanels[pos.ordinal()];
	}
}
