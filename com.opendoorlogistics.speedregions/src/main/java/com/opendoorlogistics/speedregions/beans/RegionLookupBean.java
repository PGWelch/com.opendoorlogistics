package com.opendoorlogistics.speedregions.beans;

import java.util.List;

/**
 * This class allows for serialisation of the built quadtree together with the rules
 * @author Phil
 *
 */
public class RegionLookupBean extends JSONToString{
	private SpatialTreeNode quadtree;
	private List<SpeedRule> rules;
	
	public SpatialTreeNode getQuadtree() {
		return quadtree;
	}
	public void setQuadtree(SpatialTreeNode quadtree) {
		this.quadtree = quadtree;
	}
	public List<SpeedRule> getRules() {
		return rules;
	}
	public void setRules(List<SpeedRule> rules) {
		this.rules = rules;
	}
	
	
}
