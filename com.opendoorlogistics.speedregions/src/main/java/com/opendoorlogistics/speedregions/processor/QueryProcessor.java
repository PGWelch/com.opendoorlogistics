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

import com.opendoorlogistics.speedregions.beans.SpatialTreeNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class QueryProcessor {
	private final SpatialTreeNodeWithGeometry root;

	public QueryProcessor(GeometryFactory factory, SpatialTreeNode root) {
		// add geometry objects to the tree
		this.root = new SpatialTreeNodeWithGeometry(factory, root);
	}

	public String query(Geometry edge) {
		QueryObj queryObj = new QueryObj();
		queryObj.geom = edge;
		queryRecurse(root, queryObj);
		return queryObj.currentBestId;
	}

	private static class QueryObj {
		Geometry geom;
		String currentBestId;

		// Remember priority is higher for numerically lower values!!
		long currentBestPriority = Long.MAX_VALUE;

	}

	/**
	 * Recursively query, checking higher priority nodes first for intersection
	 * with the edge and pruning node subtrees from the search whose highest
	 * priority is lower (numerically higher) than the best found so-far.
	 * @param node
	 * @param obj
	 */
	private void queryRecurse(SpatialTreeNodeWithGeometry node, QueryObj obj) {

		if (node.getAssignedPriority() >= obj.currentBestPriority) {
			// Can't beat the highest priority found so far so exclude the subtree
			return;
		}

		if(obj.geom instanceof Point){
			// Version of JTS used appears not to support optimised point-in-rectangle tests yet, so we do it explicitly here
			Coordinate coordinate = ((Point)obj.geom).getCoordinate();
			Envelope envelope = node.getEnvelope();
			if(!envelope.contains(coordinate)){
				return;
			}
		}
		else{
			// General case (e.g. for a linestring). This still does a bounding box test first before doing the expensive one
			if (!node.getGeometry().intersects(obj.geom)) {
				return;
			}			
		}


		// Check for leaf node
		if (node.getRegionId() != null) {
			// If we got to this point we must have already checked for the node being a higher
			// (numerically lower) priority than already found, so take the node
			// is the node higher priority than the current result?
			obj.currentBestPriority = node.getAssignedPriority();
			obj.currentBestId = node.getRegionId();

		} else {
			// Children are sorted by priority so we do highest (numerically lowest) first.
			// This should help terminate the search sooner.

			// Do for loop rather than iterator so we don't allocate memory..
			int n = node.getChildren().size();
			for (int i = 0; i < n; i++) {
				queryRecurse((SpatialTreeNodeWithGeometry) node.getChildren().get(i), obj);
			}
		}

	}
}
