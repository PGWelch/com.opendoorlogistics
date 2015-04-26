/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStateListener;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStatusObservable;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.components.geocoder.component.NominatimConfig;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModelListener;

public class InteractiveGeocoderPanel extends JPanel implements Disposable, GeocodeModelListener, ClosedStatusObservable {
//	private final InteractiveMapControl interactive;
//	private final ReadOnlyMapPanel map;
	private final NominatimMap nmap;
	private final SearchResultsPanel searchSubPanel;
	private final GeocodeItemPanel itemPanel;
	private final GeocodeToolbar toolbar;
	private final GeocodeModel model;
	private final VerticalLayoutPanel mainResultsPanel;
	private final HashSet<ClosedStateListener> closedStateListeners = new HashSet<>();

	public InteractiveGeocoderPanel(NominatimConfig config, ODLDatastore<? extends ODLTable> ds, ComponentControlLauncherApi controlLauncher) {
		this.model = new GeocodeModel(ds);

		// go to the first record
		this.model.gotoNextRecord();

		setLayout(new BorderLayout());

		// use vertical layout
		VerticalLayoutPanel vPanel = new VerticalLayoutPanel();
		add(vPanel, BorderLayout.CENTER);

		// create item panel
		itemPanel = new GeocodeItemPanel(model);
	//	Dimension itemPanelSize = new Dimension(600, 110);
	//	itemPanel.setPreferredSize(itemPanelSize);
	//	itemPanel.setMinimumSize(itemPanelSize);
		setBorder(itemPanel, "");
		vPanel.add(itemPanel);
		vPanel.addWhitespace();

		// create address results
		searchSubPanel = new SearchResultsPanel(config, model);
		Dimension searchPanelSize = new Dimension(600, 200);
		searchSubPanel.setPreferredSize(searchPanelSize);
		searchSubPanel.setMinimumSize(searchPanelSize);
		searchSubPanel.table.setPreferredScrollableViewportSize(new Dimension(600, 104));


//		// create map
//		interactive = new InteractiveMapControl(new MapConfig(),new MapModePermissions(0xFFFFFFFFFFFFFFFFL),null,null);
//		//interactive.setLegendCreator(GeocoderMapAppearance.createLegendCreator());
//		interactive.addOnMoveObjectListener(new OnClickPosition() {
//
//			@Override
//			public void onClickPosition(double latitude, double longitude) {
//				model.setGeocode(latitude, longitude);
//				map.setDrawables(GeocoderMapObjects.createDrawable(model));
//			}
//		});
//		interactive.setZoomBestFitManager(new ZoomBestFitManager() {
//
//			@Override
//			public void zoomBestFit(ReadOnlyMapControl viewer, double maxFraction) {
//				ZoomUtils.zoomToBestFit(viewer, viewer.getLatLongBoundingBox(null), maxFraction, true, 0.001);
//			}
//		});
//		map = new ReadOnlyMapPanel(interactive, true, controlLauncher) {
//
//			@Override
//			protected List<Action> createActions() {
//				List<Action> ret = super.createActions();
//				ret.add(null);
//
//				// create action to enter each of the mouse modes
//				for (final MouseMode mode : new MouseMode[] { MouseMode.NAVIGATE, MouseMode.MOVE_OBJECT }) {
//					AbstractAction action = new AbstractAction(Strings.convertEnumToDisplayFriendly(mode.toString()), mode.getButtonImageIcon()) {
//
//						@Override
//						public void actionPerformed(ActionEvent e) {
//							interactive.setMouseMode(mode);
//						}
//					};
//
//					action.putValue(Action.SHORT_DESCRIPTION, mode.getDescription());
//					action.putValue(Action.LONG_DESCRIPTION, mode.getDescription());
//					ret.add(action);
//				}
//
//				return ret;
//			}
//
//			@Override
//			protected void createLegendAction(Collection<Action> ret) {
//				// Turn off the legend...
//			}
//		};
//		map.setDrawables(GeocoderMapObjects.createDrawable(model));
//		map.setPreferredSize(new Dimension(600, 200));

		nmap = new NominatimMap(model);
		
		// put map in its own panel with a legend and border
		mainResultsPanel = new VerticalLayoutPanel();
		mainResultsPanel.add(searchSubPanel);
		mainResultsPanel.addNoWrap(nmap.getComponent());
		mainResultsPanel.add(ImageUtils.createImagePanel(GeocoderMapObjects.createLegend(), Color.WHITE));
		vPanel.addNoWrap(mainResultsPanel);
		//vPanel.addWhitespace();
		
		// create toolbar
		toolbar = new GeocodeToolbar(model){

			@Override
			protected void onExit() {
				InteractiveGeocoderPanel.this.onExit();
			}
			
		};
		add(toolbar, BorderLayout.SOUTH);

		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		modelChanged(true, true);
		model.addListener(this);
		model.fireListeners(true, true);

	}
	
	private void onExit(){
		for(ClosedStateListener listener:closedStateListeners){
			listener.onClosed();
		}
	}

	private void setBorder(JPanel panel, String title) {
		// panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// panel.setBorder(BorderFactory.createTitledBorder(title));

		panel.setBorder(LayoutUtils.createInsetTitledBorder(title));
		// panel.setBorder(BorderFactory.createTitledBorder(
		// BorderFactory.createEmptyBorder(10,10,10, 10), title));
	}


	protected long createPoint(DrawableObjectImpl newPoint) {
		throw new UnsupportedOperationException();
	}

	protected boolean movePoint(long globalId, LatLongImpl newPosition) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispose() {
		nmap.dispose();
		// globalDs.removeListener(fieldSelectorPanel);
	}

//	public static void main(String[] args) {
//		SwingUtilities.invokeLater(new Runnable() {
//
//			@Override
//			public void run() {
//				InteractiveGeocoderPanel panel = createWithDummyData();
//				ShowPanel.showPanel(panel);
//			}
//
//		});
//	}

//	public static InteractiveGeocoderPanel createWithDummyData() {
//
//		ODLDatastoreAlterable<ODLTableAlterable> ds = GeocodeModel.createEmptyDs();
//		ODLTable table = ds.getTableAt(0);
//
//		for (String address : new String[] { "Union Square, Aberdeen, AB11 5PN", "Queen Anne’s Walk, Basingstoke, RG21 7BE",
//				"Southgate Place, Bath, BA1 1AP", "Victoria Square, Belfast, BT1 4QG", "Churchill Square, Brighton, BN1 2TE",
//				"11 Philadelphia Street, Quakers Friars, Bristol, BS1 3BZ", "263-66 Grand Arcade, Cardiff, CF10 2EL",
//				"24 Princess hay, Exeter, EX1 1GE", "147 Buchanan Street, Glasgow, G1 2JX", "Eastgate Road, Eastville, BS5 6XX",
//				"Valley Park, Purley Way, Croydon, CR0 4UZ", "Straiton Road, Loanhead,Midlothian, EH20 9PW ",
//				"Metro Park West, Gateshead, NE11 9XS, United Kingdom", "Heron Way, West Thurrock, Essex, RM20 3WJ, United Kingdom",
//				"Wellington Road ,Ashton Under Lyne, OL6 7TE ,United Kingdom",
//				"Goslington , Off Bletcham Way , Milton Keynes, MK1 1QB , United Kingdom", "910 Europa Blvd , Warrington, WA5 7TY, United Kingdom",
//				"Park Lane, Wednesbury, West Midlands, WS10 9SF, United Kingdom", "2 Drury Way, North Circular Rd, London, NW10 0TH" }) {
//			int row = table.createEmptyRow(-1);
//			table.setValueAt(address, row, 0);
//		}
//
//		System.out.println(ds);
//
//
//		UndoRedoDecorator<ODLTableAlterable> undoRedo = new UndoRedoDecorator<>(ODLTableAlterable.class, ds);
//
//		NominatimConfig config = new NominatimConfig();
//		config.setEmail("me@me.com");
//		InteractiveGeocoderPanel panel = new InteractiveGeocoderPanel(config, undoRedo);
//		return panel;
//	}

	@Override
	public void modelChanged(boolean recordChanged, boolean searchResultsChanged) {
		// always repaint the map with a fresh drawable datastore
		nmap.update();
		
		// rezoom if search results have changed
		if (searchResultsChanged) {
			// do zooming after current event as it won't work if controls are
			// not already visible and sized
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					nmap.zoomBestFit();
					// final double maxZoomFraction = 0.95;
					// if(model.getSelectedResultsCount()>1){
					// interactive.zoomBestFit(maxZoomFraction);
					// }
					// else if(model.getSelectedResultsCount()==1){
					// // create dummy positions around the point so we can get
					// a sensible zoom...
					// HashSet<GeoPosition>dummies = new HashSet<>();
					// double d = 0.005;
					// for(int lat =-1 ; lat<=1 ; lat+=2){
					// for(int lng =-1 ; lng<=1 ; lng+=2){
					// dummies.add(new
					// GeoPosition(model.getSearchResults().get(0).getLatitude()
					// + lat *d,
					// model.getSearchResults().get(0).getLongitude() + lat
					// *d));
					// }
					// }
					// ReadOnlyMapViewer.zoomToBestFit(interactive, dummies,
					// maxZoomFraction);
					// }else{
					// interactive.setCenter(new Point2D.Double(0, 0));
					// interactive.setZoom(interactive.getTileFactory().getInfo().getMaximumZoomLevel());
					// }
					// map.repaint();
				}
			});
		}

		// update title if record changed
		if (recordChanged) {
			setBorder(itemPanel, "Current record (" + (model.getRow() + 1) + "/" + model.getRowCount() + ")");
		}

		// set results border if searched results have changed
		if (searchResultsChanged) {
			// setBorder(searchResultsPanel, "Search results for \"" +
			// model.getAddress() + "\"");
			setBorder(mainResultsPanel, "Search results for \"" + model.getAddress() + "\"");
		}
	}

	@Override
	public void addClosedStatusListener(ClosedStateListener listener) {
		closedStateListeners.add(listener);
	}

	@Override
	public void removeClosedStatusListener(ClosedStateListener listener) {
		closedStateListeners.remove(listener);
		
	}

}
