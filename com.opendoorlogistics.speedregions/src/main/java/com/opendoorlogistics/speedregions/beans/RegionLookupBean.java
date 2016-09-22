package com.opendoorlogistics.speedregions.beans;

import java.util.List;

/**
 * This class allows for serialisation of the built quadtree together with the rules
 * @author Phil
 *
 */
public class RegionLookupBean {
	private QuadtreeNode quadtree;
	private List<SpeedRule> rules;
	
	public QuadtreeNode getQuadtree() {
		return quadtree;
	}
	public void setQuadtree(QuadtreeNode quadtree) {
		this.quadtree = quadtree;
	}
	public List<SpeedRule> getRules() {
		return rules;
	}
	public void setRules(List<SpeedRule> rules) {
		this.rules = rules;
	}
	
	
}
