/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

public class HasCost {
	protected final Cost cost = new Cost();
	private final Cost COST_CHECKER = new Cost();
	
	protected boolean saveCostChecker(){
		COST_CHECKER.set(cost);
		return true;
	}
	
	protected boolean isCostEqualToChecker(){
		return Cost.isApproxEqual(cost, COST_CHECKER);
	}
	
}
