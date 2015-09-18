package com.opendoorlogistics.core.scripts.formulae.rules;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.strings.StandardisedCache;

public class RuleNode {
	final public int ruleNb;
	final public int colIndx;
	final public Object value;
	private final StandardisedCache standardisedCache;
	ArrayList<RuleNode> children;

	RuleNode(int ruleNb, int colIndx, Object value, boolean isRootNode) {
		this.ruleNb = ruleNb;
		this.colIndx = colIndx;
		this.value = value;
		this.standardisedCache = isRootNode ? new StandardisedCache():null;
	}
	
	boolean isRootNode(){
		return standardisedCache!=null;
	}

	// @Override
	// public String toString(){
	// StringBuilder b = new StringBuilder();
	// toString(b);
	// return b.toString();
	// }

	// private void toString(StringBuilder b){
	// if(colIndx>=0){
	// b.append(Strings.repeat("\t", colIndx) + selectorFieldNames[colIndx] +" = " + value + (colIndx == nc-1? " (rule #" + ruleNb + ")":"") + System.lineSeparator());
	// }
	// if(children!=null){
	// for(RuleNode n:children){
	// n.toString(b);
	// }
	// }
	// }

	public static RuleNode buildTree(List<List<Object>> selectorsMatrix) {
		return buildTree(selectorsMatrix, false);
	}
	
	/**
	 * Build the tree given the input matrix of rules x select values
	 * @param selectorsMatrix
	 * @return
	 */
	public static RuleNode buildTree(List<List<Object>> selectorsMatrix, boolean buildNodePerRule) {
		StandardisedCache standardisedCache = new StandardisedCache();
		int n = selectorsMatrix.size();
		
		RuleNode baseNode= new RuleNode(-1,-1,null, true);
		baseNode.children = new ArrayList<RuleNode>();
		for (int rule = 0; rule < n; rule++) {

			RuleNode parent = baseNode;
			int nc = selectorsMatrix.get(rule).size();
			for (int col = 0; col < nc; col++) {

				// get selector value and ensure zero length is treated as null
				Object s =selectorsMatrix.get(rule).get(col);
				if (s != null && s.toString().length() == 0) {
					s = null;
				}

				// try to find the pre-existing node with this value
				int nchildren = parent.children != null ? parent.children.size() : 0;
				RuleNode nextParent = null;
				for (int k = 0; k < nchildren; k++) {
					RuleNode child = parent.children.get(k);
					if (ColumnValueProcessor.isEqual(s, child.value, standardisedCache)) {
						nextParent = child;
						break;
					}
				}

				// make a node if none exists, marking the rule number
				boolean isLastColumn= col==nc-1;
				if (nextParent == null || (isLastColumn && buildNodePerRule)) {
					nextParent = new RuleNode(rule, col, s, false);
					
					// add to parent
					if (parent.children == null) {
						parent.children = new ArrayList<RuleNode>(3);
					}
					parent.children.add(nextParent);
				}

				parent = nextParent;
			}

		}
		return baseNode;
	}
	
	/**
	 * Find matching rule number or -1 if not found
	 * @param values
	 * @return
	 */
	public int findRuleNumber(Object[] values,RuleFilter ruleFilter, Object filterData) {
		if(!isRootNode()){
			throw new RuntimeException("Only call this method on the root node of the tree");
		}
		// Find all matching rules
		int[] lowestRuleNumber = new int[1];
		lowestRuleNumber[0] = Integer.MAX_VALUE;

		// Find the lowest matching rule number
		recurseMatch(values, this, lowestRuleNumber,ruleFilter,filterData,standardisedCache);
		int ruleNb =lowestRuleNumber[0] ;
		
		if(ruleNb == Integer.MAX_VALUE){
			return -1;
		}
		return ruleNb;
	}
	
	public int findRuleNumber(Object[] values) {
		return findRuleNumber(values, null, null);
	}
	
	public static interface RuleFilter{
		boolean filter(Object filterData,Object[] values, int ruleNb);
	}
	
	private static void recurseMatch(Object[] values, RuleNode node, int[] lowestRuleNumber, RuleFilter ruleFilter, Object filterData,StandardisedCache standardisedCache) {

		// record the rule if we're on the last one and its lower than the
		// current lowest
		int nc = values.length;
		if (node.colIndx == nc - 1) {
			if(ruleFilter==null || ruleFilter.filter(filterData, values, node.ruleNb)){
				lowestRuleNumber[0] = Math.min(lowestRuleNumber[0], node.ruleNb);				
			}
		} else {
			// recurse to the next level if we find a null selector (which
			// matches all) or we have a match
			if (node.children != null) {
				int nchildren = node.children.size();
				for (int i = 0; i < nchildren; i++) {
					RuleNode childNode = node.children.get(i);
					boolean recurse = childNode.value == null;
					if (!recurse) {
						recurse = ColumnValueProcessor.isEqual(values[childNode.colIndx], childNode.value, standardisedCache);
					}

					if (recurse) {
						recurseMatch(values,  childNode, lowestRuleNumber, ruleFilter, filterData,standardisedCache);
					}
				}
			}
		}
	}
}