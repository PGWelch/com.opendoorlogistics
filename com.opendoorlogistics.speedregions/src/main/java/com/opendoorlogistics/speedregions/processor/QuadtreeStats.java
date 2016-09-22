package com.opendoorlogistics.speedregions.processor;

import java.util.HashSet;

import com.opendoorlogistics.speedregions.beans.JSONToString;
import com.opendoorlogistics.speedregions.beans.QuadtreeNode;

public class QuadtreeStats extends JSONToString{
	private long nodes;
	private long leafNodes;
	private long regionIds;
	
	public static long countNodes(QuadtreeNode node){
		long ret=1;
		for(QuadtreeNode child:node.getChildren()){
			ret += countNodes(child);
		}
		return ret;
	}

	public static QuadtreeStats build(QuadtreeNode node){
		QuadtreeStats ret = new QuadtreeStats();
		HashSet<String> regionIds =new HashSet<>();
		recurse(node,regionIds, ret);
		return ret;
		
	}
	
	private static void recurse(QuadtreeNode node,HashSet<String> regionIds,QuadtreeStats stats){
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
		for(QuadtreeNode child:node.getChildren()){
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
