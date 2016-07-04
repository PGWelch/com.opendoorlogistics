package com.opendoorlogistics.core.distances.external;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opendoorlogistics.core.distances.external.InsertionOnlySpatialTree.SpatialTreeNode;
import com.opendoorlogistics.core.utils.MultiHashMap;



public class InsertionOnlySpatialTree {
	public static class DefaultSpatialTreeNode implements SpatialTreeNode{
		private ArrayList<Map.Entry<SpatialTreeCoord,Object>> entries = new ArrayList<>();
		private ArrayList<SpatialTreeNode> children = new ArrayList<>();
		private SpatialTreeCoord min;
		private SpatialTreeCoord max;
		
		@Override
		public List<SpatialTreeNode> getChildren() {
			return children;
		}
		@Override
		public List<Entry<SpatialTreeCoord, Object>> getEntries() {
			return entries;
		}
		public SpatialTreeCoord getMin() {
			return min;
		}
		public void setMin(SpatialTreeCoord min) {
			this.min = min;
		}
		public SpatialTreeCoord getMax() {
			return max;
		}
		public void setMax(SpatialTreeCoord max) {
			this.max = max;
		}
		
		
	}
	/**
	 * Interface must subclass hashcode and equals
	 * @author Phil
	 *
	 */
	public static interface SpatialTreeCoord{
		
	}
	
	public static interface SpatialTreeManager{
		Collection<SpatialTreeNode> createChildren(SpatialTreeNode parent);
		SpatialTreeNode createRoot();
		int getChildIndex(SpatialTreeNode parent,SpatialTreeCoord coord);
		int getMaxObjectsPerNode();
		boolean isContained(SpatialTreeQuery query, SpatialTreeCoord coord);
		boolean isOverlappingNode(SpatialTreeQuery query, SpatialTreeNode node);
		boolean isSplittable(SpatialTreeNode node);
	}

	public static interface SpatialTreeNode{
		List<SpatialTreeNode> getChildren();
		List<Map.Entry<SpatialTreeCoord,Object>> getEntries();

	
	}
	
	public static interface SpatialTreeQuery{
		
	}
	
	private final SpatialTreeManager mgr;
	
	
	private SpatialTreeNode root;
	
	public InsertionOnlySpatialTree(SpatialTreeManager mgr) {
		this.mgr = mgr;
		this.root = mgr.createRoot();
	}
	
	public void insert(SpatialTreeCoord coord,Object object){
		insert(root, new AbstractMap.SimpleEntry<SpatialTreeCoord,Object>(coord, object));
	}

	protected void insert(SpatialTreeNode node,Map.Entry<SpatialTreeCoord,Object> newEntry){

		// split if (a) not split already and (b) will have more than max entries and (c) is splittable
		if(node.getChildren().size() == 0 && node.getEntries().size() >= mgr.getMaxObjectsPerNode() && mgr.isSplittable(node)){
			
			// build a hashset to check the number of distinct coords
			HashSet<SpatialTreeCoord> allCoords = new HashSet<>();
			allCoords.add(newEntry.getKey());
			for(Map.Entry<SpatialTreeCoord,Object> entry:node.getEntries()){
				allCoords.add(entry.getKey());
			}
			
			// split if we don't have completely identical coords
			if(allCoords.size()>1){
				// split
				node.getChildren().clear();
				node.getChildren().addAll(mgr.createChildren(node));
				for(Map.Entry<SpatialTreeCoord,Object> entry:node.getEntries()){
					int indx = mgr.getChildIndex(node, entry.getKey());
					insert(node.getChildren().get(indx), entry);						
				}
				
				// call me again to place in correct child
				insert(node, newEntry);
				return;
			}
			

		}
		
		// are we already split?
		if(node.getChildren().size()>0){
			int indx = mgr.getChildIndex(node, newEntry.getKey());
			insert(node.getChildren().get(indx), newEntry);
		}else{
			// not split
			node.getEntries().add(newEntry);
			
		}
	}
	
	public Collection<Map.Entry<SpatialTreeCoord,Object>> query(SpatialTreeQuery query){
		LinkedList<Map.Entry<SpatialTreeCoord,Object>> results = new LinkedList<>();
		query(query,root,results);
		return results;
	}

	
	protected void query(SpatialTreeQuery query, SpatialTreeNode node,List<Map.Entry<SpatialTreeCoord,Object>> results){
		if(!mgr.isOverlappingNode(query, node)){
			return;
		}
		
		if(node.getChildren().size()>0){
			for(SpatialTreeNode child:node.getChildren()){
				query(query,child,results);
			}			
		}
		else{
			// check against coords
			for(Map.Entry<SpatialTreeCoord,Object> entry: node.getEntries()){
				if(mgr.isContained(query, entry.getKey())){
					results.add(entry);
				}
			}
		}
	}

	// TODO build knn query....
}
