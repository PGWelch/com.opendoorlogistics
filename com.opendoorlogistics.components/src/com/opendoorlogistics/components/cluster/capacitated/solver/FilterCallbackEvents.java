/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

import com.opendoorlogistics.components.cluster.capacitated.solver.Solver.HeuristicType;

final public class FilterCallbackEvents {
	private int lastStep=-1;
	private Cost lastCost;
	private HeuristicType lastHeuristicType;
	
	public boolean hasStateChanged(int nbSteps, HeuristicType type,EvaluatedSolution best){
		boolean changed=false;
		if(nbSteps!=lastStep){
			lastStep = nbSteps;
			changed = true;
		}

		if(type!=lastHeuristicType){
			lastHeuristicType = type;
			changed = true;
		}
		
		if(best!=null && (lastCost == null || Cost.isApproxEqual(lastCost, best.cost)==false)){
			lastCost = new Cost(best.cost);
			changed = true;
		}
		return changed;
	}
}
