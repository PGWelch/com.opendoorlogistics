/*
 * Copyright 2016 Open Door Logistics Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opendoorlogistics.speedregions.processor;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.opendoorlogistics.speedregions.beans.Bounds;
import com.opendoorlogistics.speedregions.beans.QuadtreeNode;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

class RegionQuadtreeBuilder {
	private final GeometryFactory geomFactory;
	private final QuadtreeNodeWithGeometry root ;
	private final double minSideLengthMetres;
	private long nextPolygonPriority;
	
	public RegionQuadtreeBuilder(GeometryFactory geomFactory, double minSideLengthMetres) {
		this.geomFactory = geomFactory;
		this.root = QuadtreeNodeWithGeometry.createGlobal(geomFactory);
		this.minSideLengthMetres = minSideLengthMetres;
	}

	private static class QueryGeometry{
		final Polygon polygon;
		final String id;
		public QueryGeometry(Polygon geometry, String id) {
			this.polygon = geometry;
			this.id = id;
		}	
	}

	private boolean isSplittable(QuadtreeNodeWithGeometry node){
		if(node == root){
			// root should always be splittable (and distance calculations may not work for it anyway....)
			return true;
		}
		
		Bounds bounds = node.getBounds();
		double dLngCentre = getLngCentre(node);
		double dLatCentre = getLatCentre(node);
		double width = ProcessorUtils.greatCircleApprox(dLatCentre, bounds.getMinLng(), dLatCentre, bounds.getMaxLng());
		if(width < minSideLengthMetres){
			return false;
		}
		double height = ProcessorUtils.greatCircleApprox(bounds.getMinLat(), dLngCentre, bounds.getMaxLat(), dLngCentre);
		if(height < minSideLengthMetres){
			return false;
		}
		return true;
	}
	
	public synchronized void add(Polygon geometry, String id){
		addRecursively(root, new QueryGeometry(geometry, id));
		nextPolygonPriority++;
	}
	
	private void addRecursively(QuadtreeNodeWithGeometry node, QueryGeometry geometry){
		// check if node is already assigned, nodes are assigned to the first geometry
		// that (a) totally encloses them, or (b) if the node is split to the finest granularity level,
		// the first geometry they intersect
		if(node.getRegionId()!=null){
			return;
		}
		
		// do intersection test first (checks bounding boxes then does proper geometry tests if needed)
		boolean intersects = node.getGeometry().intersects(geometry.polygon);
		if(!intersects){
			return;
		}
		
		// If we have children then pass down to them
		if(node.getChildren().size()>0){
			for(QuadtreeNode child:node.getChildren()){
				addRecursively((QuadtreeNodeWithGeometry)child,geometry);
			}
			return;
		}
		
		// Assign and return if
		// (a) a node is totally contained by the polygon or
		// (b) we can't split the node anymore
		boolean nodeIsContainedByPolygon = geometry.polygon.contains(node.getGeometry());
		if(nodeIsContainedByPolygon || !isSplittable(node)){
			node.setRegionId(geometry.id);
			node.setAssignedPriority(nextPolygonPriority);
			return;
		}

		// split
		Bounds bounds = node.getBounds();
		double dLngCentre = getLngCentre(node);
		double dLatCentre = getLatCentre(node);
		for(int lng = 0 ; lng <=1 ; lng++){
			
			double lngMin = lng==0? bounds.getMinLng() : dLngCentre;
			double lngMax = lng==0? dLngCentre : bounds.getMaxLng();
			
			for(int lat=0 ; lat <=1 ; lat++){
				double latMin = lat==0? bounds.getMinLat() : dLatCentre;
				double latMax = lat==0? dLatCentre : bounds.getMaxLat();
				QuadtreeNodeWithGeometry child = new QuadtreeNodeWithGeometry(geomFactory, new Bounds(lngMin, lngMax, latMin, latMax));
				addRecursively(child, geometry);
				node.getChildren().add(child);
			}
		}

	}


	
	private void recurseFinaliseNode(QuadtreeNode node){
		if(node.getRegionId()!=null){
			return;
		}

		// set no 'no assigned descendents' priority
		node.setAssignedPriority(Long.MAX_VALUE);

		// loop over children getting highest priority and trimming empty
		Iterator<QuadtreeNode> itChild = node.getChildren().iterator();		
		while(itChild.hasNext()){
			QuadtreeNode child = itChild.next();
			recurseFinaliseNode(child);
			if(child.getAssignedPriority()==Long.MAX_VALUE){
				// remove empty child
				itChild.remove();
			}else{
				// get the highest (numerically minimum) priority
				node.setAssignedPriority(Math.min(node.getAssignedPriority(), child.getAssignedPriority()));
			}
		}
		
		// sort children by highest (numerically lowest) priority first
		Collections.sort(node.getChildren(), new Comparator<QuadtreeNode>() {

			public int compare(QuadtreeNode o1, QuadtreeNode o2) {
				return Long.compare(o1.getAssignedPriority(), o2.getAssignedPriority());
			}
		});
		
		// if we only have one child remaining, delete this node by replacing our content with the child's
		if(node.getChildren().size()==1){
			QuadtreeNode child = node.getChildren().get(0);
			node.setChildren(child.getChildren());
			QuadtreeNode.copyNonChildFields(child, node);
		}
	}
	
	private double getLatCentre(QuadtreeNodeWithGeometry node) {
		double dLatCentre = 0.5*(node.getBounds().getMinLat() + node.getBounds().getMaxLat());
		return dLatCentre;
	}

	private double getLngCentre(QuadtreeNodeWithGeometry node) {
		double dLngCentre = 0.5*(node.getBounds().getMinLng() + node.getBounds().getMaxLng());
		return dLngCentre;
	}

	public synchronized QuadtreeNode build(){
		// deep copy without the extra builder fields (i.e. so use the base bean class)
		QuadtreeNode ret= new QuadtreeNode(root);
		recurseFinaliseNode(ret);
		return ret;
	}
}
