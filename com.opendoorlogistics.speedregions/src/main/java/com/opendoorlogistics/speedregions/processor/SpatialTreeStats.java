package com.opendoorlogistics.speedregions.processor;

import java.util.HashSet;

import com.opendoorlogistics.speedregions.beans.JSONToString;
import com.opendoorlogistics.speedregions.beans.SpatialTreeNode;

public class SpatialTreeStats extends JSONToString{
	private long nodes;
	private long leafNodes;
	private long regionIds;
	
	public static long countNodes(SpatialTreeNode node){
		long ret=1;
		for(SpatialTreeNode child:node.getChildren()){
			ret += countNodes(child);
		}
		return ret;
	}

	public static SpatialTreeStats build(SpatialTreeNode node){
		SpatialTreeStats ret = new SpatialTreeStats();
		HashSet<String> regionIds =new HashSet<>();
		recurse(node,regionIds, ret);
		return ret;
		
	}
	
	private static void recurse(SpatialTreeNode node,HashSet<String> regionIds,SpatialTreeStats stats){
		if(node.getRegionId()!=null){
			if(!regionIds.contains(node.getRegionId())){
				regionIds.add(node.getRegionId());
				stats.regionIds++;
			}
		}
		stats.nodes++;
		if(node.getChildren().size()==0){
			stats.leafNodes++;
		}
		for(SpatialTreeNode child:node.getChildren()){
			recurse(child,regionIds,stats);
		}
	}

	public long getNodes() {
		return nodes;
	}

	public void setNodes(long nodes) {
		this.nodes = nodes;
	}

	public long getLeafNodes() {
		return leafNodes;
	}

	public void setLeafNodes(long leafNodes) {
		this.leafNodes = leafNodes;
	}

	public long getRegionIds() {
		return regionIds;
	}

	public void setRegionIds(long regionIds) {
		this.regionIds = regionIds;
	}
	
	
}
