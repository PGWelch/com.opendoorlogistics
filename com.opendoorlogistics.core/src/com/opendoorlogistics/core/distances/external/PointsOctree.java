package com.opendoorlogistics.core.distances.external;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.opendoorlogistics.core.distances.external.InsertionOnlySpatialTree.DefaultSpatialTreeNode;
import com.opendoorlogistics.core.distances.external.InsertionOnlySpatialTree.SpatialTreeCoord;
import com.opendoorlogistics.core.distances.external.InsertionOnlySpatialTree.SpatialTreeManager;
import com.opendoorlogistics.core.distances.external.InsertionOnlySpatialTree.SpatialTreeNode;
import com.opendoorlogistics.core.distances.external.InsertionOnlySpatialTree.SpatialTreeQuery;

public class PointsOctree extends InsertionOnlySpatialTree{

	
	private PointsOctree(SpatialTreeManager mgr) {
		super(mgr);
	}

	public static class OctreeQuery implements SpatialTreeQuery {
		private ThreeDPoint pnt;
		private double radius;

		public ThreeDPoint getPnt() {
			return pnt;
		}

		public void setPnt(ThreeDPoint pnt) {
			this.pnt = pnt;
		}

		public double getRadius() {
			return radius;
		}

		public void setRadius(double radius) {
			this.radius = radius;
		}

	}

	public static PointsOctree build(Iterable<Map.Entry<ThreeDPoint, Object>> objects, double minBoxSizeLength) {
		// get max and min
		ThreeDPoint max = new ThreeDPoint(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
		ThreeDPoint min = new ThreeDPoint(+Double.MAX_VALUE, +Double.MAX_VALUE, +Double.MAX_VALUE);
		for (Map.Entry<ThreeDPoint, Object> entry : objects) {
			for (int i = 0; i < 3; i++) {
				min.set(i, Math.min(min.get(i), entry.getKey().get(i)));
				max.set(i, Math.max(max.get(i), entry.getKey().get(i)));
			}
		}

		SpatialTreeManager mgr = new SpatialTreeManager() {

			@Override
			public boolean isOverlappingNode(SpatialTreeQuery query, SpatialTreeNode node) {
				DefaultSpatialTreeNode myNode = (DefaultSpatialTreeNode) node;
				OctreeQuery oQuery = (OctreeQuery) query;
				ThreeDPoint searchPnt = oQuery.pnt;
				double r = oQuery.getRadius();
				ThreeDPoint min = (ThreeDPoint) myNode.getMin();
				ThreeDPoint max = (ThreeDPoint) myNode.getMax();
				for (int i = 0; i < 3; i++) {
					if(searchPnt.get(i) < min.get(i) - r){
						return false;
					}
	
					if(searchPnt.get(i) > max.get(i) + r){
						return false;
					}
					
				}
				return true;
			}

			@Override
			public boolean isContained(SpatialTreeQuery query, SpatialTreeCoord coord) {
				OctreeQuery oQuery = (OctreeQuery) query;
				ThreeDPoint pnt = (ThreeDPoint) coord;
				ThreeDPoint diff = ThreeDPoint.subtract(pnt, oQuery.getPnt());
				double absSqd = ThreeDPoint.absSqd(diff);
				double radiusSqd = oQuery.getRadius() * oQuery.getRadius();
				return absSqd <= radiusSqd;
			}

			@Override
			public int getMaxObjectsPerNode() {
				return 10;
			}

			@Override
			public int getChildIndex(SpatialTreeNode parent, SpatialTreeCoord coord) {
				DefaultSpatialTreeNode node = (DefaultSpatialTreeNode) parent;
				ThreeDPoint centre = ThreeDPoint.average((ThreeDPoint) node.getMin(), (ThreeDPoint) node.getMax());
				int indexMult = 4;
				int index = 0;
				ThreeDPoint threeDPoint = (ThreeDPoint) coord;
				for (int i = 0; i < 3; i++) {
					if (threeDPoint.get(i) > centre.get(i)) {
						index += indexMult;
					}
					indexMult /= 2;
				}
				return index;
			}

			@Override
			public SpatialTreeNode createRoot() {
				DefaultSpatialTreeNode root = new DefaultSpatialTreeNode();
				root.setMin(min);
				root.setMax(max);
				return root;
			}

			@Override
			public Collection<SpatialTreeNode> createChildren(SpatialTreeNode parent) {
				DefaultSpatialTreeNode myNode = (DefaultSpatialTreeNode) parent;
				ThreeDPoint width = calcWidth(myNode);
				boolean[] toSplit = new boolean[3];
				int nbToSplit = 0;
				for (int i = 0; i < 3; i++) {
					if (width.get(i) >= minBoxSizeLength) {
						toSplit[i] = true;
						nbToSplit++;
					}
				}

				if (nbToSplit == 0) {
					throw new RuntimeException("Error building spatial lookup tree.");
				}

				ThreeDPoint min = (ThreeDPoint) myNode.getMin();
				ThreeDPoint max = (ThreeDPoint) myNode.getMax();
				ThreeDPoint centre = ThreeDPoint.average(min, max);
				ArrayList<SpatialTreeNode> ret = new ArrayList<>(8);
				for (int i = 0; i < 8; i++) {
					ret.add(null);
				}
				for (int ix = 0; ix <= 1; ix++) {
					for (int iy = 0; iy <= 1; iy++) {
						for (int iz = 0; iz <= 1; iz++) {
							int index = ix * 4 + iy * 2 + iz;
							ThreeDPoint newNodeMin = new ThreeDPoint(min);
							ThreeDPoint newNodeMax = new ThreeDPoint(max);

							if (toSplit[0]) {
								newNodeMin.x = ix == 0 ? min.x : centre.x;
								newNodeMax.x = ix == 0 ? centre.x : max.x;
							}

							if (toSplit[1]) {
								newNodeMin.y = iy == 0 ? min.y : centre.y;
								newNodeMax.y = iy == 0 ? centre.y : max.y;
							}

							if (toSplit[2]) {
								newNodeMin.z = iz == 0 ? min.z : centre.z;
								newNodeMax.z = iz == 0 ? centre.z : max.z;
							}

							DefaultSpatialTreeNode newNode = new DefaultSpatialTreeNode();
							newNode.setMin(newNodeMin);
							newNode.setMax(newNodeMax);
							ret.set(index, newNode);
						}

					}
				}

				return ret;
			}

			@Override
			public boolean isSplittable(SpatialTreeNode node) {
				DefaultSpatialTreeNode myNode = (DefaultSpatialTreeNode) node;
				ThreeDPoint width = calcWidth(myNode);
				for (int i = 0; i < 3; i++) {
					if (width.get(i) >= minBoxSizeLength) {
						return true;
					}
				}
				return false;
			}

			private ThreeDPoint calcWidth(DefaultSpatialTreeNode myNode) {
				ThreeDPoint width = ThreeDPoint.subtract((ThreeDPoint) myNode.getMax(), (ThreeDPoint) myNode.getMin());
				return width;
			}
		};

		// finally build the tree
		PointsOctree ret = new PointsOctree(mgr);
		for (Map.Entry<ThreeDPoint, Object> entry : objects) {
			ret.insert(entry.getKey(), entry.getValue());
		}
		
		return ret;
	}
	
	public Collection<Map.Entry<ThreeDPoint,Object>> query(ThreeDPoint pnt, double radius){
		OctreeQuery query = new OctreeQuery();
		query.setPnt(pnt);
		query.setRadius(radius);
		Collection<Map.Entry<SpatialTreeCoord,Object>> results =super.query(query);
		LinkedList<Map.Entry<ThreeDPoint,Object>> ret = new LinkedList<>();
		results.forEach(e->ret.add(new AbstractMap.SimpleEntry<ThreeDPoint,Object>((ThreeDPoint)e.getKey(),e.getValue())));
		return ret;
	}

}
