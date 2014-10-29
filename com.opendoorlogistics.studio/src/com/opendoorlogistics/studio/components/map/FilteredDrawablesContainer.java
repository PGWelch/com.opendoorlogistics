/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import gnu.trove.set.hash.TLongHashSet;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.gis.map.Legend;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer;
import com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer.TileReadyListener;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckBoxItem;

public abstract class FilteredDrawablesContainer implements Disposable {
	private final TileCacheRenderer renderer = new TileCacheRenderer();
	private final RenderProperties renderFlags = new RenderProperties();
	private LayeredDrawables unfiltered = new LayeredDrawables(null,null,null);
	private LayeredDrawables filtered;
	private LegendFrame legendFilter;

	@Override
	public synchronized void dispose() {
		renderer.dispose();
		if(legendFilter!=null){
			legendFilter.dispose();
		}
	}

	/**
	 * Get the drawables which are currently visibly according to the legend filter
	 * @return
	 */
	public synchronized Iterable<? extends DrawableObject> getVisibleDrawables(boolean inactiveBackground, boolean active, boolean inactiveForeground) {
		if (filtered == null) {
			if (legendFilter != null) {
				filtered = new LayeredDrawables( 
						inactiveBackground && unfiltered.getInactiveBackground()!=null? legendFilter.filterDrawables(unfiltered.getInactiveBackground()):null,						
						active && unfiltered.getActive()!=null? legendFilter.filterDrawables(unfiltered.getActive()):null,
						inactiveForeground && unfiltered.getInactiveForeground()!=null?legendFilter.filterDrawables(unfiltered.getInactiveForeground()):null);
			} else {
				filtered = unfiltered;
			}
		}
		return filtered;
	}

	public RenderProperties getRenderFlags() {
		return renderFlags;
	}

	public void renderObjects(Graphics2D g, LatLongToScreen converter, TLongHashSet selectedObjectIds) {
		renderer.renderObjects(g, converter, renderFlags.getFlags() , selectedObjectIds);
	}

	public void setDrawables(LayeredDrawables pnts) {
		// update unfiltered
		this.unfiltered = pnts;
		
		// update legend if exists
		if(legendFilter!=null){
			updateLegend();			
		}else{
			updateFiltered();
		}
	}

	public void addTileReadyListener(TileReadyListener listener) {
		renderer.addTileReadyListener(listener);
	}


	synchronized LegendFrame openLegend() {
		checkIsEDT();

		// dispose existing
		if (legendFilter != null) {
			legendFilter.dispose();
		}

		updateLegend();

		return legendFilter;
	}

	private void updateLegend() {

		// create if needed
		if (legendFilter == null) {
			legendFilter = new LegendFrame() {

				@Override
				public void checkStateChanged() {
					updateFiltered();
				}

				@Override
				protected void disposeLegend() {
					// legend gets rid of its own reference when its closed
					legendFilter = null;
					updateFiltered();
				}

				@Override
				public void buttonClicked(CheckBoxItem item, int buttonColumn) {
					zoomOnObjects(item.getText());
				}

			};
		}

		// set legend items
		List<Map.Entry<String, BufferedImage>> items = Legend.getLegendItemImages(unfiltered, new Dimension(20, 20));
		legendFilter.updateLegend(items);

	}

	private synchronized void updateFiltered() {
		filtered = null;
		if(!renderer.isDisposed()){
			renderer.setObjects(getVisibleDrawables(true,true,true));
			repaint();			
		}
	}
	
	private void checkIsEDT() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException("Drawable objects should only be updated on the event thread.");
		}
	}

	public abstract void repaint();
	
	public abstract void zoomOnObjects(String legendKey);
}
