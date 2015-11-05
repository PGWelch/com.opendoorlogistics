package com.opendoorlogistics.studio.components.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.standardcomponents.map.MapActionFactory;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapDataApi;
import com.opendoorlogistics.api.standardcomponents.map.MapMode;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.MapSelectionList;
import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.gis.map.DatastoreRenderer;
import com.opendoorlogistics.core.gis.map.JXMapUtils;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;
import com.opendoorlogistics.core.gis.map.background.ODLTileFactory;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.LatLongBoundingBox;
import com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer;
import com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer.TileReadyListener;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.tables.decorators.datastores.ListenerDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.undoredo.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.SetUtils;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.components.map.plugins.CustomTooltipPlugin;
import com.opendoorlogistics.studio.components.map.plugins.SummariseFieldValuesTooltipPlugin;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

/**
 * The implementation of the api object is just an aggregate of other objects
 * which pumps messages between them as needed.
 * 
 * @author Phil
 *
 */
public class MapApiImpl extends MapApiListenersImpl implements MapApi, Disposable , MapSelectionList{
	private final MapSelectionState selectionState;
	private final ViewPosition position;
	private final DisposableMapPanel containerLevel1Panel;
	private final JPanel containerLevel2Panel;
	private final MapToolbar toolBar;
	private final ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs;
	private final JPanel[] sidePanels = new JPanel[PanelPosition.values().length];
	private final ComponentControlLauncherApi componentControlLauncherApi;
	private final MapViewPanel mapViewPanel;
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);
	private MapMode defaultMode;
	private TileCacheRenderer renderer;
	private long renderFlags = RenderProperties.SHOW_ALL;
	private MapMode mode;
	private FilteredTables filtered;
	private BeanMappedObjects objs;
	private ODLDatastore<? extends ODLTable> mapDatastore;
	private MeasureComponents lastMeasure;
	private BufferedImage disabledPaintImage;
	private boolean isPendingInitContainerPanels=false;
	private volatile boolean isDisposed=false;
	
	public class DisposableMapPanel extends JPanel implements Disposable {

		@Override
		public void dispose() {
			MapApiImpl.this.dispose();
		}

		public MapApiImpl getApi(){
			return MapApiImpl.this;
		}
		
	}

	/**
	 * Disable painting temporarily and take an image of the current component
	 * @param disabled
	 */
	private void setDisablePaint(boolean disabled){
		if(disabled != mapViewPanel.isDisablePaint()){
			if(disabled){
				// take image to show
				disabledPaintImage = new BufferedImage(containerLevel2Panel.getWidth(), containerLevel2Panel.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
			    Graphics g = disabledPaintImage.getGraphics();
			    g.setColor(containerLevel2Panel.getForeground());
			    g.setFont(containerLevel2Panel.getFont());
			    containerLevel2Panel.paintAll(g);
			    g.dispose();
			    		
			}

			mapViewPanel.setDisablePaint(disabled);
		}
	}

	public MapApiImpl(Iterable<MapPlugin> plugins,  ComponentControlLauncherApi componentControlLauncherApi,
			ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs, ODLDatastore<? extends ODLTable> mapDatastore) {
		this(plugins, componentControlLauncherApi, globalDs, BeanMappedObjects.create(mapDatastore), mapDatastore);
	}
		
	private MapApiImpl(Iterable<MapPlugin> plugins,  ComponentControlLauncherApi componentControlLauncherApi,
			ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs,BeanMappedObjects beanMappedObjects, ODLDatastore<? extends ODLTable> mapDatastore) {
		this.globalDs = globalDs;
		this.componentControlLauncherApi = componentControlLauncherApi;
		this.selectionState = new MapSelectionState();
		this.position = new ViewPosition();

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
		containerLevel1Panel = new DisposableMapPanel();
		containerLevel1Panel.setPreferredSize(new Dimension(700, 600));
		containerLevel1Panel.setLayout(new BorderLayout());
		containerLevel2Panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
						
				lastMeasure = new MeasureComponents();
				
				// Painting updates control sizes which are needed various calculations.
				// We therefore just paint over the control if painting is 'disabled' so 
				// we don't see controls in odd positions.
				if(mapViewPanel.isDisablePaint()){
					if(disabledPaintImage!=null){
						g.drawImage(disabledPaintImage, 0, 0, null);
					}else{
						g.setColor(Color.WHITE);
						g.fillRect(0, 0, getWidth(), getHeight());						
					}
				}
			}
		};
		containerLevel2Panel.setLayout(new BorderLayout());
		containerLevel1Panel.add(containerLevel2Panel, BorderLayout.CENTER);

		// init the map panel
		mapViewPanel = new MapViewPanel(this) {

			@Override
			protected void paintMapObjects(Graphics2D g) {
				renderer.renderObjects(g, mapViewPanel.createImmutableConverter(), renderFlags, selectionState.copySet());
			}

			@Override
			protected void paintPluginOverlay(Graphics2D g) {
				// the plugins
				fireOnPaintListeners(MapApiImpl.this, g);

				// the mode
				if (mode != null) {
					mode.paint(MapApiImpl.this, g);
				}
			}
			
			@Override
			public String getToolTipText(MouseEvent event) {
				StringBuilder builder = new StringBuilder();
				Rectangle rect = new Rectangle(event.getX() - 1, event.getY() - 1, 2, 2);
				List<DrawableObject> within = DatastoreRenderer.getObjectsWithinRectangle(filtered!=null? filtered.activeFiltered:null, createImmutableConverter(), rect, false);
				TLongHashSet ids =  new TLongHashSet();
				for(DrawableObject d : within){
					if(d.getGlobalRowId()!=-1){
						ids.add(d.getGlobalRowId());						
					}
				}
				
				fireTooltipListeners(MapApiImpl.this, event, ids.toArray(), builder);
				if(builder.length()==0){
					return null;
				}
				return builder.toString();
			}

		};
		mapViewPanel.addMouseListener(this);
		mapViewPanel.addMouseMotionListener(this);
		mapViewPanel.addKeyListener(this);
		mapViewPanel.setToolTipText(""); // add dummy tooltip text so getToolTipText method is called
		
		// Init the plugins before building the toolbar (so they can add to it)
		if (plugins != null) {
			for (MapPlugin factory : plugins) {
				factory.initMap(this);
			}
		}

		// Init toolbar
		toolBar = new MapToolbarImpl();
		fireBuildToolbarListeners(this, toolBar);
		containerLevel1Panel.add(toolBar.getComponent(), BorderLayout.NORTH);

		// Add all controls to the main panel
		initContainerPanel(false);

		// Init right-click menu on the map
		mapViewPanel.addMouseListener(new PopupMenuMouseAdapter() {

			@Override
			protected void launchMenu(MouseEvent me) {
				MapPopupMenuImpl menu = new MapPopupMenuImpl();
				fireBuildContextMenuListeners(MapApiImpl.this, menu);
				menu.show(me.getComponent(), me.getX(), me.getY());
			}
		});

		setObjects(beanMappedObjects,mapDatastore);

		// Zoom all by default
		addOnGeometryLoadedCallback(new Runnable() {

			@Override
			public void run() {
				// delay by one event cycle to ensure map control is initialised (sometimes it has zero dimensions
				// otherwise ... screwing up the calculation)
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						setViewToBestFit(getMapDataApi().getFilteredAllLayersTable(true));
					}
				});
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

//	public void setObjects(ODLDatastore<? extends ODLTable> newMapDatastore) {
//		
//	//	firePreObjectsChangedListener(this, mapDatastore);
//		
//		updateObjectFiltering(newMapDatastore);
//		
//
//	}

	public DisposableMapPanel getPanel() {
		return containerLevel1Panel;
	}
	
	private static class FilteredTables {
		final FindDrawableTables unfilteredTables;
	//	final Iterable<? extends DrawableObject> activeUnfiltered;
		final Iterable<? extends DrawableObject> activeFiltered;
		final LayeredDrawables allFiltered;
		final MapApiImpl api;
		final BeanMappedObjects objs;
		
		FilteredTables(BeanMappedObjects objs, ODLDatastore<? extends ODLTable> mapDatastore, MapApiImpl api,boolean isFiltered){
			this.api = api;
			this.objs = objs;
			
			api.fireStartObjectFiltering(api, mapDatastore);
			
			unfilteredTables = new FindDrawableTables(mapDatastore);
			ArrayList< DrawableObject> activeList = new ArrayList<DrawableObject>(unfilteredTables.activeTable!=null ? unfilteredTables.activeTable.getRowCount():0);
		//	activeUnfiltered = activeList;
			
			activeFiltered = filter(1,unfilteredTables.activeTable,isFiltered, activeList);
			allFiltered = new LayeredDrawables(filter(0,unfilteredTables.background,isFiltered,null), activeFiltered, filter(2,unfilteredTables.foreground,isFiltered,null));
			
			api.fireEndObjectFiltering(api);
		}
		
		private Iterable<? extends DrawableObject> filter(int tableOrder, ODLTableReadOnly table, boolean isFiltered,List< DrawableObject> saveAllToList ) {
			if (table == null) {
				return null;
			}

			int n = table.getRowCount();
			ArrayList<DrawableObject> ret = new ArrayList<DrawableObject>(n);
			for (int row = 0; row < n; row++) {
				
				// save to list if needed
				DrawableObject obj =objs.get(tableOrder, row);
				if(saveAllToList!=null && obj!=null){
					saveAllToList.add(obj);
				}
				
				if(obj!=null){
					if(saveAllToList!=null){
						saveAllToList.add(obj);						
					}
				
					boolean accept =!isFiltered || api.fireFilterObject(api, table, row);
					if(accept){
						ret.add(obj);
					}
				}

			}
			
			return ret;
		}
	}
	

	@Override
	public void updateObjectFiltering() {
		filtered = new FilteredTables(objs,mapDatastore, MapApiImpl.this,true);
		
		renderer.setObjects(filtered.allFiltered);
		
		mapViewPanel.repaint();
	}


	public void setObjects(ODLDatastore<? extends ODLTable> newMapDatastore) {
		setObjects(BeanMappedObjects.create(newMapDatastore),newMapDatastore);
	}

	private void setObjects(BeanMappedObjects objs,ODLDatastore<? extends ODLTable> newMapDatastore) {

		this.objs = objs;

		mapDatastore = newMapDatastore;
		
		updateObjectFiltering();
		
		fireObjectsChangedListeners(MapApiImpl.this);
		
		// Update selected ids. don't allow anything to be selected that's not in the active table,
		// however we do allow filtered out objects to stay selected (needed for polygon editing plugin).
		// We do this on the table instead of the drawableobjects, as the bean conversion to drawableobjects
		// filters out anything with null geometry and long lats.
		TLongHashSet newSelected = new TLongHashSet();
		ODLTableReadOnly unfilteredActive = filtered.unfilteredTables.activeTable;
		if(unfilteredActive!=null){
			int n = unfilteredActive.getRowCount();
			for(int i =0 ; i < n ; i++){
				long rowid = unfilteredActive.getRowId(i);
				if (selectionState.contains(rowid)) {
					newSelected.add(rowid);
				}
			}
		}

		if (!selectionState.equals(newSelected)) {
			setSelectedIds(newSelected.toArray());
		}



	}

	@Override
	public int getZoom() {
		return position.getZoom();
	}

	@Override
	public Point2D getWorldBitmapMapCentre() {
		return position.getCenter();
	}

	// @Override
	// public void setReuseImageOnNextPaint() {
	// // TODO Auto-generated method stub
	//
	// }

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
		TLongArrayList within = DatastoreRenderer.getWithinRectangle(filtered!=null? filtered.activeFiltered:null, createImmutableConverter(), screenCoordinatesRectangle, true);
		return within.toArray();
	}

	@Override
	public void clearSelection() {
		setSelectedIds(new long[] {});
	}

	@Override
	public void setSelectedIds(long... ids) {
		if(selectionState.set(ids)){
			fireSelectionChangedListeners(this);			
		}
		repaint(false);
	}

	@Override
	public Component getMapWindowComponent() {
		return mapViewPanel;
	}

	@Override
	public void setCursor(Cursor cursor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMapMode(MapMode newMode) {
		MapMode oldMode = mode;

		if (oldMode != null) {
			removeMouseInputListener(oldMode);
			removeObjectsChangedListener(oldMode);
			oldMode.onExitMode(this);
		}

		// turn the cursor back to normal
		mapViewPanel.setCursor(Cursor.getDefaultCursor());

		// if new mode if null then choose our default mode
		if(newMode==null){
			newMode = defaultMode;
		}
		
		fireModeChangingListener(this, oldMode, newMode);
		mode = newMode;
		fireModeChangedListener(this, oldMode, mode);

		if (newMode != null) {
			registerMouseInputListener(newMode, Integer.MIN_VALUE);
			registerObjectsChangedListener(newMode, 0);
			newMode.onEnterMode(this);

			if (newMode.getCursor() != null) {
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
		isDisposed = true;
		
		// unselect everything then tell the listeners
		setSelectedIds(new long[]{});
		fireSelectionChangedListeners(this);
		
		renderer.dispose();
		fireDisposedListeners(this);

		executorService.shutdown();
		
		for (JPanel d : sidePanels) {
			if (d != null) {
				((Disposable) d).dispose();
			}
		}

		mapViewPanel.dispose();
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

	public static void main(String[] args) {
		InitialiseStudio.initialise(false);

		final ODLDatastoreAlterable<? extends ODLTableAlterable> exampleds = MapUtils.createExampleDatastore();
		ODLDatastoreAlterable<? extends ODLTableAlterable> listener = new ListenerDecorator(ODLTableAlterable.class, exampleds);
		ODLDatastoreUndoable<? extends ODLTableAlterable> undoable = new UndoRedoDecorator<ODLTableAlterable>(ODLTableAlterable.class, listener);
		MapConfig config = new MapConfig();
		config.setUseCustomTooltips(true);

	//	exampleds.getTableAt(0).setFlags(exampleds.getTableAt(0).getFlags()  & ~TableFlags.UI_DELETE_ALLOWED);
		
		GlobalMapSelectedRowsManager gsm = new GlobalMapSelectedRowsManager() {

			@Override
			public void onMapSelectedChanged() {
				// TODO Auto-generated method stub

			}
		};

		List<MapPlugin> plugins = getPlugins(config);
		
		// Test button
		plugins.add(new MapPlugin() {
			

			@Override
			public String getId(){
				return "TEST_PLUGIN";
			}

			@Override
			public void initMap(MapApi api) {
				api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
					
					@Override
					public void onBuildToolbar(final MapApi api, MapToolbar toolBar) {
						toolBar.add(new AbstractAction("TEST") {
							
							@Override
							public void actionPerformed(ActionEvent e) {
								// test changing permissions
								
								ODLTableAlterable table = exampleds.getTableAt(0);
								long flags = table.getFlags();
								if( (flags & TableFlags.UI_DELETE_ALLOWED) !=0){
									table.setFlags(flags & ~TableFlags.UI_DELETE_ALLOWED);
								}
								else if( (flags & TableFlags.UI_INSERT_ALLOWED) !=0){
									table.setFlags(flags & ~TableFlags.UI_INSERT_ALLOWED);
								}
								else if( (flags & TableFlags.UI_SET_ALLOWED) !=0){
									table.setFlags(flags & ~TableFlags.UI_SET_ALLOWED);
								}
								((MapApiImpl)api).setObjects(BeanMappedObjects.create(exampleds),exampleds);
							}
						});
					}
				}, Integer.MIN_VALUE);
			}
		});
		

		final ODLApi api = new ODLApiImpl();
		ComponentControlLauncherApi dummyApi = new ComponentControlLauncherApi() {

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

			@Override
			public ODLDatastoreUndoable<? extends ODLTableAlterable> getGlobalDatastore() {
				return undoable;
			}

			@Override
			public MapSelectionListRegister getMapSelectionListRegister() {
				// TODO Auto-generated method stub
				return gsm;
			}

		};

		final MapApiImpl mapApi = new MapApiImpl(plugins, dummyApi, undoable,BeanMappedObjects.create(exampleds), exampleds);
		mapApi.connectToGSM(gsm);

		undoable.addListener(new ODLListener() {

			@Override
			public void tableChanged(int tableId, int firstRow, int lastRow) {
				mapApi.setObjects(BeanMappedObjects.create(exampleds),exampleds);
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

	public void connectToGSM(final MapSelectionListRegister gsm) {
		// register this selection list globally
		gsm.registerMapSelectionList(this);
		
		// unregister it when the map closes
		registerDisposedListener(new OnDisposedListener() {

			@Override
			public void onDispose(MapApi api) {
				gsm.unregisterMapSelectionList( (MapApiImpl)api);
			}
		}, 0);
		
		// and also tell the register when the selection changes
		registerSelectionChanged(new OnChangeListener() {
			
			@Override
			public void onChanged(MapApi api) {
				gsm.onMapSelectedChanged();
			}
		}, 0);
	}

	private void addOnGeometryLoadedCallback(final Runnable runnable) {

		class Handler implements ActionListener {
			Timer timer;

			Handler() {
				timer = new Timer(100, this);
				timer.start();
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				if (filtered != null) {
					for (DrawableObject pnt : filtered.allFiltered) {
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
	public void setViewToBestFit(ODLTableReadOnly drawables) {
		double defaultEdgeSizeDegrees = 0.1;
		double maxFraction = 0.975;
		Iterable<? extends DrawableObject> objs = MapUtils.getDrawables(drawables);
		LatLongBoundingBox llbb = MapUtils.getLatLongBoundingBox(objs, null);
		class Helper {
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
		ODLTileFactory tileFactory = BackgroundTileFactorySingleton.getFactory();
		TileFactoryInfo info = tileFactory.getInfo();

		Set<GeoPosition> positions = llbb.getCornerSet();
		if (positions.size() > 0) {
			if (positions.size() == 1) {
				GeoPosition pos = SetUtils.getFirst(positions);
				positions = helper.createGeopositionsSquareAroundPoint(pos.getLatitude(), pos.getLongitude(), defaultEdgeSizeDegrees);
			}

			if (info != null) {
				Rectangle viewBounds = getWorldBitmapViewport();
				int bfz = JXMapUtils.getBestFitZoom(info, positions, viewBounds.width, viewBounds.height, maxFraction).getFirst();
				Rectangle2D bounds = JXMapUtils.generateBoundingRect(info, positions, bfz);
				// GeoPosition centre = tileFactory.pixelToGeo(new
				// Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				// bfz);
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
		if (zoom > getMaxZoom()) {
			zoom = getMaxZoom();
		}
		if (zoom < getMinZoom()) {
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
		double xRatio = (double) newSize.width / oldSize.width;
		double yRatio = (double) newSize.height / oldSize.height;
		Point2D.Double newCentre = new Point2D.Double(getWorldBitmapMapCentre().getX() * xRatio, getWorldBitmapMapCentre().getY() * yRatio);
		setView(newZoom, newCentre);
	}

	@Override
	public <T extends JPanel & Disposable> void setSidePanel(T panel, PanelPosition pos) {
		int i = pos.ordinal();

		if (sidePanels[i] != panel) {
			if (sidePanels[i] != null && sidePanels[i] != panel) {
				((Disposable) sidePanels[i]).dispose();
			}

			sidePanels[i] = panel;
			initContainerPanel(true);
		}

	}
	
//	private Point getMapScreenAbsCentre(){
//		Point ret = SwingUtilities.convertPoint(mapViewPanel, mapViewPanel.getX(), mapViewPanel.getY(), containerLevel1Panel);
//		ret.x += mapViewPanel.getWidth()/2;
//		ret.y += mapViewPanel.getHeight()/2;
//		return ret;
//	}
	
	private class MeasureComponents{
		int leftWidth;
		int rightWidth;
		int bottomHeight;
		int width;
		int height;
		
		public MeasureComponents() {
			width = containerLevel2Panel.getWidth();
			height = containerLevel2Panel.getHeight();
			
			int dividerSize = new JSplitPane().getDividerSize();
			if(panel(PanelPosition.LEFT)!=null && panel(PanelPosition.LEFT).isVisible()){
				leftWidth = panel(PanelPosition.LEFT).getWidth();
				leftWidth += dividerSize;
			}
			
			if(panel(PanelPosition.RIGHT)!=null&& panel(PanelPosition.RIGHT).isVisible()){
				rightWidth  = panel(PanelPosition.RIGHT).getWidth();
				rightWidth += dividerSize;
			}
			
			if(panel(PanelPosition.BOTTOM)!=null&& panel(PanelPosition.BOTTOM).isVisible()){
				bottomHeight  = panel(PanelPosition.BOTTOM).getHeight();
				bottomHeight += dividerSize;
			}
					
		}
		
		private JPanel panel(PanelPosition p){
			return sidePanels[p.ordinal()];
		}
		
		public Point calculateMapCentre(){
			int mapWidth = width - leftWidth - rightWidth;
			int mapHeight = height - bottomHeight;
			int x= leftWidth + mapWidth/2;
			int y = mapHeight / 2;
			return new Point(x, y);
		}
	}
	
	private class InitContainerPanel implements Runnable{
		final boolean isReinit;

		public InitContainerPanel(boolean isReinit) {
			this.isReinit = isReinit;
		}

		@Override
		public void run() {
			isPendingInitContainerPanels = false;
			
			// if reinitialising, get original map centre point and remove all components 
			final Point mapPoint0;
			if(isReinit){
				mapPoint0 = (lastMeasure!=null ?lastMeasure: new MeasureComponents()).calculateMapCentre();
				setDisablePaint(true);			
				containerLevel2Panel.removeAll();
			}else{
				mapPoint0 = null;
			}

			// get things to run across several EDT cycles
			final ArrayList<Runnable> runnables = new ArrayList<Runnable>();

			Component component = mapViewPanel;
			if (sidePanels[PanelPosition.RIGHT.ordinal()] != null) {
				JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, component, sidePanels[PanelPosition.RIGHT.ordinal()]);
				splitter.setResizeWeight(0.8);
				runnables.add(new Runnable() {

					@Override
					public void run() {
						Container container = splitter.getParent();
						if (container != null) {
							int width = container.getWidth();
							splitter.setDividerLocation(width - width / 5);
						}
					}
				});
				component = splitter;
			}

			if (sidePanels[PanelPosition.LEFT.ordinal()] != null) {
				JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidePanels[PanelPosition.LEFT.ordinal()], component);
				splitter.setResizeWeight(0.2);
				runnables.add(new Runnable() {

					@Override
					public void run() {
						Container container = splitter.getParent();
						if (container != null) {
							int width = container.getWidth();
							splitter.setDividerLocation(width / 5);
						}
					}
				});
				component = splitter;
			}

			if (sidePanels[PanelPosition.BOTTOM.ordinal()] != null) {
				final JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, component, sidePanels[PanelPosition.BOTTOM.ordinal()]);
				splitter.setResizeWeight(0.8);
				runnables.add(new Runnable() {

					@Override
					public void run() {
						Container container = splitter.getParent();
						if (container != null) {
							int height = container.getHeight();
							splitter.setDividerLocation(height - height / 4);
						}
					}
				});
				component = splitter;
			}

			containerLevel2Panel.add(component, BorderLayout.CENTER);

			// fix the map centre so it doesn't change
			if (isReinit) {
				containerLevel2Panel.revalidate();

				// Add the runnable to fix the map positions, including a null runnable before hand
				// so we get an extra EDT cycle which ensures measurements are correct
				runnables.add(null);
				runnables.add(new Runnable() {
					@Override
					public void run() {
						Point mapPoint1 = new MeasureComponents().calculateMapCentre();
						Point offset = new Point(mapPoint1.x - mapPoint0.x, mapPoint1.y - mapPoint0.y);
						Point2D centre = getWorldBitmapMapCentre();
						setDisablePaint(false);
						setView(getZoom(), new Point2D.Double(centre.getX() + offset.getX(), centre.getY() + offset.getY()));
						containerLevel2Panel.repaint();
					}
				});
			}

			
			// run all runnables with a full EDT cycle between each one so we get the correct control measurements
			class RecurseRunner implements Runnable{
				final Iterator<Runnable> toRun;

				RecurseRunner(Iterator<Runnable> toRun) {
					this.toRun = toRun;
				}

				@Override
				public void run() {
					if(toRun.hasNext()){
						Runnable runnable = toRun.next();
						if(runnable!=null){
							runnable.run();
						}
						containerLevel2Panel.invalidate();
						containerLevel2Panel.revalidate();
						containerLevel2Panel.repaint();
						SwingUtilities.invokeLater(this);
					}
				}		
			}
			SwingUtilities.invokeLater(new RecurseRunner(runnables.iterator()));

		}
		
	}
	
	private void initContainerPanel(final boolean isReinit) {

		// don't allow two reinits to be posted at once
		if(!isPendingInitContainerPanels){
			isPendingInitContainerPanels = true;
			SwingUtilities.invokeLater(new InitContainerPanel(isReinit));
		}
		
		
	}

	@Override
	public Point getWorldBitmapPosition(LatLong ll, int zoom) {
		Point2D pnt = BackgroundTileFactorySingleton.getFactory().geoToPixel(new GeoPosition(ll.getLatitude(), ll.getLongitude()), zoom);
		return new Point((int) Math.round(pnt.getX()), (int) Math.round(pnt.getY()));
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
				return globalDs != null ? globalDs.getTableByImmutableId(tableId) : null;
			}

			@Override
			public void runTransactionOnGlobalDatastore(Callable<Boolean> toRun) {
				if (globalDs != null) {
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
			public ODLTable getUnfilteredActiveTable() {
				return TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES);
			}

			@Override
			public ODLTable getUnfilteredInactiveForegroundTable() {
				return TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND);
			}

			@Override
			public ODLTable getUnfilteredInactiveBackgroundTable() {
				return TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND);
			}

			@Override
			public int getLegendKeyColumn() {
				return DrawableObjectImpl.COL_LEGEND_KEY;
			}

			@Override
			public ODLTableReadOnly getFilteredAllLayersTable(boolean immutableSnapshot) {
				// At the moment we always do an immutable snapshot, but we may improve this in the future
				return toTable(filtered.allFiltered);
			}

			@Override
			public ODLDatastoreUndoable<? extends ODLTableAlterable> getGlobalDatastore() {
				return globalDs;
			}

			@Override
			public ODLTableReadOnly getUnfilteredAllLayersTable(boolean immutableSnapshot) {
				// At the moment we always do an immutable snapshot, but we may improve this in the future
				
				// Get table .. this will be a copy as it combines all input tables into one. 
				// As this recreates a table from the beanmapped objects it is quicker than actually
				// reading the original table (as this involves reading many layers of decorators),
				// this boosts performance when using the legend.
				return toTable(new FilteredTables(objs,mapDatastore, MapApiImpl.this, false).allFiltered);
			}

			@Override
			public int getTooltipColumn() {
				return DrawableObjectImpl.COL_TOOLTIP;
			}

			@Override
			public ODLTableReadOnly getActiveTableSelectedOnly() {
				long [] sel = (MapApiImpl.this).getSelectedIds();
				ODLTable active = getUnfilteredActiveTable();
				if(sel!=null && sel.length>0 && active!=null){
					ODLApi odlApi = (MapApiImpl.this).getApi();
					ODLDatastoreAlterable<? extends ODLTableAlterable> ds = odlApi.tables().createAlterableDs();
					odlApi.tables().copyTableDefinition(active, ds);
					ODLTable filtered = ds.getTableAt(0);
					for(long id : sel){
						if(active.containsRowId(id)){
							odlApi.tables().copyRowById(active, id, filtered);
						}
					}
					
					return filtered;
				}
				return null;
			}

			@Override
			public Iterable<ODLTable> getDrawableTables(ODLDatastore<? extends ODLTable> mapDatastore) {
				return new FindDrawableTables(mapDatastore);
			}

			@Override
			public ODLDatastore<? extends ODLTable> getMapDatastore() {
				return mapDatastore;
			}

			@Override
			public ODLTableReadOnly getBackgroundImagesTable() {
				return TableUtils.findTable(mapDatastore, PredefinedTags.BACKGROUND_IMAGE);
			}



		};
	}

	private ODLTableReadOnly toTable(Iterable<? extends DrawableObject> objs) {
		BeanTableMappingImpl btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
		LinkedList<BeanMappedRow> rows = new LinkedList<BeanMappedRow>();
		for (DrawableObject o : objs) {
			rows.add((BeanMappedRow) o);
		}
		return btm.writeObjectsToTable(rows.toArray(new BeanMappedRow[rows.size()]));
	}

	@Override
	public <T extends JPanel & Disposable> T getSidePanel(PanelPosition pos) {
		return (T) sidePanels[pos.ordinal()];
	}

	@Override
	public void setDefaultMapMode(MapMode mode) {
		this.defaultMode = mode;
	}

	@Override
	public MapMode getMapMode() {
		return mode;
	}

	@Override
	public MapMode getDefaultMapMode() {
		return defaultMode;
	}

	@Override
	public MapToolbar getMapToolbar() {
		return toolBar;
	}

	@Override
	public ODLApi getApi() {
		return componentControlLauncherApi.getApi();
	}
	
	private static class BeanMappedObjects{
		private List<TIntObjectHashMap<DrawableObject>> byRow;
		
		BeanMappedObjects() {
			byRow = new ArrayList<TIntObjectHashMap<DrawableObject>>(3);
			for(int i =0 ; i < 3 ; i++){
				byRow.add(new TIntObjectHashMap<DrawableObject>());
			}
		}
		
		void save(int tableOrder, int row, DrawableObject obj){
			byRow.get(tableOrder).put(row, obj);
		}
		
		DrawableObject get(int tableOrder, int row){
			return byRow.get(tableOrder).get(row);
		}
		
		public static BeanMappedObjects create(ODLDatastore<? extends ODLTable> ioDs){
			FindDrawableTables finder = new FindDrawableTables(ioDs);
			BeanMappedObjects objs = new BeanMappedObjects();
			class ToDrawable{
				void convert(int tableOrder, ODLTableReadOnly table){
					if(table==null){
						return;
					}
					
					BeanTableMappingImpl btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
					int n = table.getRowCount();
					for (int row = 0; row < n; row++) {
						DrawableObject obj = btm.readObjectFromTableByRow(table, row);
						if(obj!=null){
							objs.save(tableOrder, row, obj);
						}
					}
				
				}
			}
			ToDrawable converter = new ToDrawable();
			converter.convert(0,finder.background);
			converter.convert(1,finder.activeTable);
			converter.convert(2,finder.foreground);
			return objs;	
		}
	}
	
	public static void registerComponent( ) {
		
		
		ODLGlobalComponents.register(new AbstractMapViewerComponent() {

			@Override
			public void execute(final ComponentExecutionApi api, int mode,final Object configuration, ODLDatastore<? extends ODLTable> ioDs, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {

				// convert all to drawable objects using the non-EDT datastore on the non-EDT thread
				BeanMappedObjects objs = BeanMappedObjects.create(ioDs);

				api.submitControlLauncher(new ControlLauncherCallback() {
					
					@Override
					public void launchControls(ComponentControlLauncherApi launcherApi) {
						DisposableMapPanel panel = (DisposableMapPanel)launcherApi.getRegisteredPanel("Map");
						if(panel!=null){
							panel.getApi().setObjects(objs,ioDs);
						}
						else{
							// get all plugins
							List<MapPlugin> plugins = getPlugins((MapConfig)configuration);
							
							// create the map api
							MapApiImpl mapApi = new MapApiImpl(plugins, launcherApi, launcherApi.getGlobalDatastore(),objs, ioDs);
							mapApi.connectToGSM(launcherApi.getMapSelectionListRegister());
							
							// register the panel
							if (!launcherApi.registerPanel("Map", null, mapApi.getPanel(), true)) {
								// presumably UI is unavailable?
								mapApi.dispose();
							}	
	
						}
					}
				});		
			}
			
		});
		
		ODLGlobalComponents.register(new ViewLayerStyleComponent());
	}

	private static List<MapPlugin> getPlugins(MapConfig config) {
		List<MapPlugin> plugins = new ArrayList<MapPlugin>();
		for(MapPlugin p : GlobalMapPluginManager.getPlugins()){
			plugins.add(p);
		}
		
		if(config.isUseCustomTooltips()){
			plugins.add(new CustomTooltipPlugin());						
		}else{
			plugins.add(new SummariseFieldValuesTooltipPlugin());			
		}
		return plugins;
	}

	@Override
	public void submitWork(Runnable runnable) {
		executorService.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				runnable.run();
				return null;
			}
		});
	}

	@Override
	public boolean isDisposed() {
		return isDisposed;
	}

	@Override
	public void registerActionFactory(MapActionFactory factory, int priority, String group,  boolean needsSetPermission, boolean needsInsertPermission, boolean needsDeletePermission) {
		long flags=0;
		if(needsSetPermission){
			flags |= TableFlags.UI_SET_ALLOWED;
		}
		if(needsInsertPermission){
			flags |= TableFlags.UI_INSERT_ALLOWED;
		}
		
		if(needsDeletePermission){
			flags |= TableFlags.UI_DELETE_ALLOWED;
		}
		
		if(flags!=0){
			PluginUtils.registerActionFactory(this, factory, priority, group, flags);
		}else{
			PluginUtils.registerActionFactory(this, factory, priority, group);		
		}
	}
}
