/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashSet;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.painter.Painter;
import com.opendoorlogistics.core.gis.map.DatastoreRenderer;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;
import com.opendoorlogistics.studio.components.map.ModalMouseListener.MouseMode;
import com.opendoorlogistics.studio.components.map.ModalMouseListener.OnActionableListener;
import com.opendoorlogistics.studio.components.map.v2.SelectedIdChecker;

final public class InteractiveMapControl extends ReadOnlyMapControl implements Disposable,SelectedIdChecker {
	private final SelectionPanel selectedRowsViewer;
	private final TLongHashSet selectedGlobalRowIds = new TLongHashSet();
	private ModalMouseListener mouseListener;
	private Painter<Object> selectionPainter;
	private HashSet<OnFillListener> onFillListeners = new HashSet<>();
	private HashSet<OnClickPosition> onCreateListeners = new HashSet<>();
	private HashSet<OnClickPosition> onMoveObjectListeners = new HashSet<>();
	private final GlobalMapSelectedRowsManager gsm;
	
	public static interface OnFillListener {
		void onFill(TLongArrayList selected);
	};

	public static interface OnClickPosition {
		void onClickPosition(double latitude, double longitude);
	};

	public InteractiveMapControl(MapConfig config,MapModePermissions permissions,SelectionPanel selectedRowsViewer, GlobalMapSelectedRowsManager gsm) {
		super(config,permissions);
		this.selectedRowsViewer = selectedRowsViewer;
		this.gsm = gsm;
	}

	public void addOnFillListener(OnFillListener listener) {
		onFillListeners.add(listener);
	}

	public void addOnCreateListener(OnClickPosition listener) {
		onCreateListeners.add(listener);
	}

	public void addOnMoveObjectListener(OnClickPosition listener) {
		onMoveObjectListeners.add(listener);
	}

	public SelectionPanel getSelectionPanel() {
		return selectedRowsViewer;
	}

	public TLongHashSet getSelected() {
		return selectedGlobalRowIds;
	}

	@Override
	public void setDrawables(LayeredDrawables pnts) {
		super.setDrawables(pnts);
		updateSelectedObjects();
	}

	private void updateSelectedObjects() {
		// remove any selected which are no longer present
		TLongHashSet allPntIds = new TLongHashSet();
		for (DrawableObject pnt : getVisibleDrawables(false,true,false)) {
			if (pnt.getGlobalRowId() != -1) {
				allPntIds.add(pnt.getGlobalRowId());
			}
		}
		TLongIterator it = selectedGlobalRowIds.iterator();
		boolean removed = false;
		while (it.hasNext()) {
			long id = it.next();
			if (allPntIds.contains(id) == false) {
				it.remove();
				removed = true;
			}
		}
		if (removed) {
			// update the application-global selected rows
			if(gsm!=null){
				gsm.onMapSelectedChanged();
			}
			selectedRowsViewer.setSelectedRows(selectedGlobalRowIds.toArray());
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		selectionPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
	}

	@Override
	protected void addPainters() {
		setOverlayPainter(new Painter<Object>() {
			// long DEBUG_TEST_FLAGS = RenderFlags.LEGEND_TOP_LEFT;

			@Override
			public void paint(Graphics2D g, Object object, int width, int height) {
				drawablesContainer.renderObjects(g, createImmutableConverter(), selectedGlobalRowIds);
//				for (int i = 0; i < 100; i++) {
//					renderer.clearTiles();
	//			renderer.renderObjects(g, createImmutableConverter(), renderFlags.getFlags() | RenderProperties.RENDER_FADE, selectedGlobalRowIds);
//				}
			}
		});
	}

	@Override
	protected void initMouseListeners() {
		mouseListener = new ModalMouseListener(this, new OnActionableListener() {

			@Override
			public void onActionable(MouseMode mode, Rectangle rect, boolean ctrl) {
				TLongArrayList within = DatastoreRenderer.getWithinRectangle(getVisibleDrawables(false,true,false), createImmutableConverter(), rect,true);
				LatLongToScreen converter = createImmutableConverter();
				if (mode == MouseMode.SELECT) {
					TLongHashSet old = new TLongHashSet(selectedGlobalRowIds);
					if (!ctrl) {
						selectedGlobalRowIds.clear();
					}
					selectedGlobalRowIds.addAll(within);

					if (selectedGlobalRowIds.equals(old) == false && selectedRowsViewer != null) {
						if(gsm!=null){
							gsm.onMapSelectedChanged();
						}
						selectedRowsViewer.setSelectedRows(selectedGlobalRowIds.toArray());
					}
				} else if (mode == MouseMode.FILL) {
					for (OnFillListener listener : onFillListeners) {
						listener.onFill(within);
					}
				} else if (mode == MouseMode.CREATE) {
					LatLong pos = converter.getLongLat(rect.x, rect.y);
					for (OnClickPosition listener : onCreateListeners) {
						listener.onClickPosition(pos.getLatitude(), pos.getLongitude());
					}
				} else if (mode == MouseMode.MOVE_OBJECT) {
					LatLong pos = converter.getLongLat(rect.x, rect.y);
					for (OnClickPosition listener : onMoveObjectListeners) {
						listener.onClickPosition(pos.getLatitude(), pos.getLongitude());
					}
				}
			}
		});

		selectionPainter = mouseListener.createSelectionPainter();
		addMouseListener(mouseListener);
		addMouseMotionListener(mouseListener);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (selectedRowsViewer != null) {
			selectedRowsViewer.dispose();
		}
	}

	public void setMouseMode(MouseMode mode) {
		mouseListener.setMode(mode);
		repaint();
	}

	
	public void setSelected(long... vals) {
		selectedGlobalRowIds.clear();
		selectedGlobalRowIds.addAll(vals);
		selectedRowsViewer.setSelectedRows(selectedGlobalRowIds.toArray());
		if(gsm!=null){
			gsm.onMapSelectedChanged();
		}
	}

	public MouseMode getMouseMode() {
		return mouseListener.getMouseMode();
	}

	@Override
	public boolean isSelectedId(long rowId) {
		return selectedGlobalRowIds.contains(rowId);
	}
}
