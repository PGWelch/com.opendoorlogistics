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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.opendoorlogistics.speedregions.beans.Bounds;
import com.opendoorlogistics.speedregions.beans.SpatialTreeNode;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

class RegionSpatialTreeBuilder {
	private final GeometryFactory geomFactory;
	private final SpatialTreeNodeWithGeometry root;
	private final double minLengthMetres;
	private long nextPolygonPriority = 1;

	public RegionSpatialTreeBuilder(GeometryFactory geomFactory, double minSideLengthMetres) {
		this.geomFactory = geomFactory;
		this.root = SpatialTreeNodeWithGeometry.createGlobal(geomFactory);
		this.minLengthMetres = minSideLengthMetres;
	}

	private static class QueryGeometry {
		final Polygon polygon;
		final String id;

		public QueryGeometry(Polygon geometry, String id) {
			this.polygon = geometry;
			this.id = id;
		}
	}

	private boolean isVerticallySplittable(Bounds b) {

		// Distance calculation doesn't work if we have a node which covers the whole range of lat / long.
		// We therefore split anything with more than 90 degrees lat or long range...
		if (b.getMaxLng() - b.getMinLng() >= 90) {
			return true;
		}
		double dLatCentre = getLatCentre(b);
		double width = RegionProcessorUtils.greatCircleApprox(dLatCentre, b.getMinLng(), dLatCentre, b.getMaxLng());
		return (width/2) > minLengthMetres;
	}
	
	private boolean isHorizontallySplittable(Bounds b) {

		// Distance calculation doesn't work if we have a node which covers the whole range of lat / long.
		// We therefore split anything with more than 90 degrees lat or long range...
		if (b.getMaxLat() - b.getMinLat() >= 90) {
			return true;
		}
		
		double dLngCentre = getLngCentre(b);
		double height = RegionProcessorUtils.greatCircleApprox(b.getMinLat(), dLngCentre, b.getMaxLat(), dLngCentre);
		return (height/2) > minLengthMetres;
	}
	
//	private boolean isSplittable(SpatialTreeNodeWithGeometry node) {
//		// if(node == root){
//		// // root should always be splittable (and distance calculations may not work for it anyway....)
//		// return true;
//		// }
//
//		// Distance calculation doesn't work if we have a node which covers the whole range of lat / long.
//		// We therefore split anything with more than 90 degrees lat or long range...
//		Bounds b = node.getBounds();
//		if (b.getMaxLat() - b.getMinLat() >= 90) {
//			return true;
//		}
//		if (b.getMaxLng() - b.getMinLng() >= 90) {
//			return true;
//		}
//
//		// We do b-tree splitting, therefore sometimes 1 side length is twice the other.
//		// If we want to use min side length
//		// double dLngCentre = getLngCentre(node);
//		// double dLatCentre = getLatCentre(node);
//		// double width = RegionProcessorUtils.greatCircleApprox(dLatCentre, b.getMinLng(), dLatCentre, b.getMaxLng());
//		// double height = RegionProcessorUtils.greatCircleApprox(b.getMinLat(), dLngCentre, b.getMaxLat(), dLngCentre);
//		// if(width < minSideLengthMetres && height < minSideLengthMetres){
//		// return false;
//		// }
//
//		// We do b-tree splitting, therefore sometimes 1 side length is twice the other.
//		// We therefore measure the diagonal instead.
//		double diagonal = RegionProcessorUtils.greatCircleApprox(b.getMinLat(), b.getMinLng(), b.getMaxLat(), b.getMaxLng());
//		if (diagonal < minLengthMetres) {
//			return false;
//		}
//
//		return true;
//	}

	public synchronized void add(Polygon geometry, String id) {
		addRecursively(root, 0, new QueryGeometry(geometry, id));
		nextPolygonPriority++;
	}

	

	/**
	 * Return true if and only if both are assigned and assigned to the same region
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean isEqualNonNullAssignment(SpatialTreeNode a, SpatialTreeNode b) {
		if (a.getRegionId() == null || b.getRegionId() == null) {
			return false;
		}
		return a.getRegionId().equals(b.getRegionId());
	}

	/**
	 * Recombine a node's children if they are all assigned to the same region *and* they don't have children themselves
	 * (we recursively call this anyway so we should still recursively recombine).
	 * 
	 * @param node
	 */
	private void recombineChildrenIfPossible(SpatialTreeNode node) {

		int nc = node.getChildren().size();
		if (nc == 0) {
			// Must have children
			return;
		}

		for (int i = 0; i < nc; i++) {
			SpatialTreeNode child = node.getChildren().get(i);
			if (child.getChildren().size() > 0) {
				// Children cannot have children
				return;
			}

			if (child.getRegionId() == null) {
				// Must all be assigned
				return;
			}

			if (i > 0) {
				if (!isEqualNonNullAssignment(child, node.getChildren().get(0))) {
					// Must all be have same assigned id
					return;
				}

			}
		}

		// recombine
		SpatialTreeNode child0 = node.getChildren().get(0);
		node.getChildren().clear();
		node.setRegionId(child0.getRegionId());
		node.setAssignedPriority(child0.getAssignedPriority());

	}

	private void addRecursively(SpatialTreeNodeWithGeometry node, int depth, QueryGeometry geometry) {
		// check if node is already assigned, nodes are assigned to the first geometry
		// that (a) totally encloses them, or (b) if the node is split to the finest granularity level,
		// the first geometry they intersect
		if (node.getRegionId() != null) {
			return;
		}

		// do intersection test first (checks bounding boxes then does proper geometry tests if needed)
		if (!isIntersecting(node, geometry)) {
			return;
		}

		// If already we have children then pass down to them
		if (node.getChildren().size() > 0) {
			for (SpatialTreeNode child : node.getChildren()) {
				addRecursively((SpatialTreeNodeWithGeometry) child, depth + 1, geometry);
			}
			recombineChildrenIfPossible(node);
			return;
		}

		// No children already. Assign and return if:
		// (a) a node is totally contained by the polygon or
		// (b) we can't split the node anymore
		Bounds b = node.getBounds();		
		boolean nodeIsContainedByPolygon = geometry.polygon.contains(node.getGeometry());
		if (nodeIsContainedByPolygon || (!isHorizontallySplittable(b) && !isVerticallySplittable(b))) {
			node.setRegionId(geometry.id);
			node.setAssignedPriority(nextPolygonPriority);
			return;
		}

		if(!isHorizontallySplittable(b)){
			node.getChildren().addAll(getVerticalSplit(b));	
		}else if(!isVerticallySplittable(b)){
			node.getChildren().addAll(getHorizontalSplit(b));				
		}
		else{
			// General case. Test both splits. If one gives a non-intersecting side, take that
			// horizontal split along line of constant latitude
			List<SpatialTreeNodeWithGeometry> hSplitList = getHorizontalSplit(b);
			boolean hGood = isGoodSplit(geometry, hSplitList);
			
			// vertical split along line of constant longitude
			List<SpatialTreeNodeWithGeometry> vSplitList = getVerticalSplit(b);
			boolean vGood = isGoodSplit(geometry, vSplitList);

			List<SpatialTreeNodeWithGeometry> chosen =null;
			if(hGood && !vGood){
				chosen=hSplitList;
			}
			else if(!hGood && vGood){
				chosen=vSplitList;			
			}
			else if(depth%2==0){
				chosen=hSplitList;			
			}else{
				chosen=vSplitList;
			}
			node.getChildren().addAll(chosen);
			
		}

		// Bounds b = node.getBounds();
		// if(depth%2==0){
		// // horizontal split along line of constant latitude
		// double dLatCentre = getLatCentre(node);
		// for(int i =0 ; i<=1 ; i++){
		// double latMin = i==0? b.getMinLat() : dLatCentre;
		// double latMax = i==0? dLatCentre : b.getMaxLat();
		// node.getChildren().add(new SpatialTreeNodeWithGeometry(geomFactory, new Bounds(b.getMinLng(), b.getMaxLng(),
		// latMin, latMax)));
		// }
		// }else{
		// // vertical split along line of constant longitude
		// double dLngCentre = getLngCentre(node);
		// for(int i =0 ; i<=1 ; i++){
		// double lngMin = i==0? b.getMinLng() : dLngCentre;
		// double lngMax = i==0? dLngCentre : b.getMaxLng();
		// node.getChildren().add(new SpatialTreeNodeWithGeometry(geomFactory, new Bounds(lngMin,lngMax, b.getMinLat(),
		// b.getMaxLat())));
		// }
		//
		// }
		//

		// add to the child geometries after split
		for (SpatialTreeNode child : node.getChildren()) {
			addRecursively((SpatialTreeNodeWithGeometry) child, depth + 1, geometry);
		}

		recombineChildrenIfPossible(node);
	}

	private boolean isGoodSplit(QueryGeometry geometry, List<SpatialTreeNodeWithGeometry> hSplitList) {
		return isIntersecting(hSplitList.get(0), geometry)!=isIntersecting(hSplitList.get(1), geometry);
	}

	private List<SpatialTreeNodeWithGeometry> getVerticalSplit(Bounds b) {
		List<SpatialTreeNodeWithGeometry> vSplitList = new ArrayList<>(2);
		double dLngCentre = getLngCentre(b);
		for (int i = 0; i <= 1; i++) {
			double lngMin = i == 0 ? b.getMinLng() : dLngCentre;
			double lngMax = i == 0 ? dLngCentre : b.getMaxLng();
			vSplitList.add(new SpatialTreeNodeWithGeometry(geomFactory, new Bounds(lngMin, lngMax, b.getMinLat(), b.getMaxLat())));
		}
		return vSplitList;
	}

	private List<SpatialTreeNodeWithGeometry> getHorizontalSplit(Bounds b) {
		List<SpatialTreeNodeWithGeometry> hSplitList = new ArrayList<>(2);
		double dLatCentre = getLatCentre(b);
		for (int i = 0; i <= 1; i++) {
			double latMin = i == 0 ? b.getMinLat() : dLatCentre;
			double latMax = i == 0 ? dLatCentre : b.getMaxLat();
			hSplitList.add(new SpatialTreeNodeWithGeometry(geomFactory, new Bounds(b.getMinLng(), b.getMaxLng(), latMin, latMax)));
		}
		return hSplitList;
	}

	private boolean isIntersecting(SpatialTreeNodeWithGeometry node, QueryGeometry geometry) {
		return node.getGeometry().intersects(geometry.polygon);
	}

	private void recurseFinaliseNode(SpatialTreeNode node) {
		if (node.getRegionId() != null) {
			return;
		}

		// // if we have a horizontal or vertical which are assigned to the same, combine them
		// if(node.getChildren().size()==4){
		//
		// }

		// set no 'no assigned descendents' priority
		node.setAssignedPriority(Long.MAX_VALUE);

		// loop over children getting highest priority and trimming empty
		Iterator<SpatialTreeNode> itChild = node.getChildren().iterator();
		while (itChild.hasNext()) {
			SpatialTreeNode child = itChild.next();
			recurseFinaliseNode(child);
			if (child.getAssignedPriority() == Long.MAX_VALUE) {
				// remove empty child
				itChild.remove();
			} else {
				// get the highest (numerically minimum) priority
				node.setAssignedPriority(Math.min(node.getAssignedPriority(), child.getAssignedPriority()));
			}
		}

		// sort children by highest (numerically lowest) priority first
		Collections.sort(node.getChildren(), new Comparator<SpatialTreeNode>() {

			public int compare(SpatialTreeNode o1, SpatialTreeNode o2) {
				return Long.compare(o1.getAssignedPriority(), o2.getAssignedPriority());
			}
		});

		// if we only have one child remaining, delete this node by replacing our content with the child's
		if (node.getChildren().size() == 1) {
			SpatialTreeNode child = node.getChildren().get(0);
			node.setChildren(child.getChildren());
			SpatialTreeNode.copyNonChildFields(child, node);
		}

	}

	private double getLatCentre(Bounds b) {
		double dLatCentre = 0.5 * (b.getMinLat() + b.getMaxLat());
		return dLatCentre;
	}

	private double getLngCentre(Bounds b) {
		double dLngCentre = 0.5 * (b.getMinLng() +b.getMaxLng());
		return dLngCentre;
	}

	public synchronized SpatialTreeNode build() {
		// deep copy without the extra builder fields (i.e. so use the base bean class)
		SpatialTreeNode ret = new SpatialTreeNode(root);
		recurseFinaliseNode(ret);
		return ret;
	}
}
