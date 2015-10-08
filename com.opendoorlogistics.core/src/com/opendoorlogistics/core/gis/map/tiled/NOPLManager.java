/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.tiled;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.gis.map.DatastoreRenderer;
import com.opendoorlogistics.core.gis.map.ObjectRenderer;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.tiled.DrawableObjectLayer.LayerType;
import com.opendoorlogistics.core.utils.strings.StandardisedCache;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

public class NOPLManager {
	final private StandardisedStringTreeMap<LayerCache> currentLayers = new StandardisedStringTreeMap<>(false);
	final private ObjectRenderer renderer =new DatastoreRenderer(){
		
		/**
		 * Layer rendering handles poly colour itself so we need to ensure
		 * the render doesn't set it here...
		 */
		@Override
		protected Color getPolygonBorderColour(Color polyCol){
			return polyCol;
		}
	};
	
	private class LayerCache{
		final RecentlyUsedCache tileCache = new RecentlyUsedCache("nopl-layer-tile-cache",64 * 1024 * 1024);
		final DrawableObjectLayer layer;
		
		LayerCache(DrawableObjectLayer layer) {
			this.layer = layer;
		}	
	}
	
	synchronized List<DrawableObjectLayer> update(Iterable<? extends DrawableObject> newDrawables){

		// get new layers by splitting the drawables
		List<DrawableObjectLayer> newLayers = splitDrawablesIntoLayers(newDrawables);
		
		StandardisedStringSet newLayerIds = new StandardisedStringSet(false);
		
		// parse all new NOVLP layers
		for(DrawableObjectLayer newLayer:newLayers){
			if(newLayer.getType() != LayerType.NOVLPL){
				continue;
			}
			
			String groupId = Strings.std(newLayer.getNOVLPLGroupId());
			newLayerIds.add(groupId);
			
			boolean addLayer=false;
			LayerCache cachedLayer = synchronisedGetLayer(groupId);
			if(cachedLayer==null){
				// The layer is new so add it.
				addLayer = true;
			}else{
				// Check each geometry object in the new layer exists in the old one
				for(DrawableObject obj:newLayer){
					if(obj.getGeometry()!=null){
						if(cachedLayer.layer.hasGeom(obj.getGeometry())==false){
							
							// The new layer contains one or more geoms not already in the cached layer, invalidating it.
							// Remove the old cached layer and then add the new one.
							currentLayers.remove(groupId);
							addLayer = true;
							break;
						}
					}
				}
			}

			if(addLayer){
				currentLayers.put(groupId, new LayerCache(newLayer));				
			}
		}
		
		// remove any layers which are no longer used
		ArrayList<String> currentIds = new ArrayList<>(this.currentLayers.keySet());
		for(String id:currentIds){
			if(newLayerIds.contains(id)==false){
				this.currentLayers.remove(id);
			}
		}
		
		return newLayers;
	}
	

	/**
	 * Split input objects into layers based on their order and the non-overlapping polygon group key
	 * @param drawables
	 * @return
	 */
	private List<DrawableObjectLayer> splitDrawablesIntoLayers(Iterable<? extends DrawableObject> drawables){
		StandardisedCache standardiser = new StandardisedCache();

		// split by consecutive NOLP group key
		ArrayList<DrawableObjectLayer> ret = new ArrayList<>();		
		DrawableObjectLayer currentLayer = null;
		for(DrawableObject o:drawables){
			
			// create new layer if needed
			boolean novlpl = Strings.isEmpty(o.getNonOverlappingPolygonLayerGroupKey())==false;
			if(novlpl){
				String stdGroup = standardiser.std(o.getNonOverlappingPolygonLayerGroupKey());
				if(currentLayer == null || currentLayer.getType() != DrawableObjectLayer.LayerType.NOVLPL || currentLayer.getNOVLPLGroupId().equals(stdGroup)==false){
					
					// scan for matching an existing polygon layer
					currentLayer = null;
					for(DrawableObjectLayer layer:ret){
						if(layer.getType() ==DrawableObjectLayer.LayerType.NOVLPL && layer.getNOVLPLGroupId().equals(novlpl) ){
							// User has probably screwed up as same id has been used non-consecutively. 
							// Just render everything in the first layer.
							currentLayer = layer;
						}
					}
					
					// no found, create new
					if(currentLayer==null){
						currentLayer = new DrawableObjectLayer(stdGroup);
						ret.add(currentLayer);						
					}
				}
								
			}else{
				// normal case
				if(currentLayer ==null || currentLayer.getType() != DrawableObjectLayer.LayerType.NORMAL){
					currentLayer = new DrawableObjectLayer();
					ret.add(currentLayer);
				}
			}

			// add object to current layer
			currentLayer.add(o);

		}
		
		return ret;
	}
	
//	private List<DrawableObjectLayer> splitDrawablesIntoLayersV2(Iterable<? extends DrawableObject> drawables){
//		StandardisedCache standardiser = new StandardisedCache();
//
//		// split by consecutive NOLP group key
//		ArrayList<DrawableObjectLayer> ret = new ArrayList<>();		
//		DrawableObjectLayer currentLayer = null;
//		for(DrawableObject o:drawables){
//			
//			// create new layer if needed
//			boolean novlpl = Strings.isEmpty(o.getNonOverlappingPolygonLayerGroupKey())==false;
//			if(novlpl){
//				String stdGroup = standardiser.std(o.getNonOverlappingPolygonLayerGroupKey());
//				if(currentLayer == null || currentLayer.getType() != DrawableObjectLayer.LayerType.NOVLPL || currentLayer.getNOVLPLGroupId().equals(stdGroup)==false){
//					
//					// scan for matching an existing polygon layer
//					currentLayer = null;
//					for(DrawableObjectLayer layer:ret){
//						if(layer.getType() ==DrawableObjectLayer.LayerType.NOVLPL && layer.getNOVLPLGroupId().equals(novlpl) ){
//							// User has probably screwed up as same id has been used non-consecutively. 
//							// Just render everything in the first layer.
//							currentLayer = layer;
//						}
//					}
//					
//					// no found, create new
//					if(currentLayer==null){
//						currentLayer = new DrawableObjectLayer(stdGroup);
//						ret.add(currentLayer);						
//					}
//				}
//								
//			}else{
//				// normal case
//				if(currentLayer ==null || currentLayer.getType() != DrawableObjectLayer.LayerType.NORMAL){
//					currentLayer = new DrawableObjectLayer();
//					ret.add(currentLayer);
//				}
//			}
//
//			// add object to current layer
//			currentLayer.add(o);
//
//		}
//		
//		return ret;
//	}
	/**
	 * Get the tile or create it if it doesn't exist yet
	 * @param layerId
	 * @param position
	 * @param converter
	 * @param renderer
	 * @return
	 */
	NOVLPolyLayerTile getTile(String layerId, TilePosition position,LatLongToScreen converter){
		
		// get layer, synchronising so we wait for any update to have finished
		LayerCache layerCache = synchronisedGetLayer(layerId);
		if(layerCache == null){
			return null;
		}
		
		NOVLPolyLayerTile ret =(NOVLPolyLayerTile)layerCache.tileCache.get(position);
		if(ret==null){
			ret = new NOVLPolyLayerTile(converter, renderer, layerCache.layer);
			layerCache.tileCache.put(position, ret, ret.getSizeInBytes());
		}
		
		return ret;
	}


	/**
	 * Retrieve the layer in a thread-synchronised manner
	 * @param layerId
	 * @return
	 */
	synchronized private LayerCache synchronisedGetLayer(String layerId) {
		LayerCache layerCache = currentLayers.get(layerId);
		return layerCache;
	}
}
