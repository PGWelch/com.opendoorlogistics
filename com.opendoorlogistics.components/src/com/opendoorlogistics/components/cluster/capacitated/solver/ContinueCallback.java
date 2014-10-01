/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

import com.opendoorlogistics.components.cluster.capacitated.solver.Solver.HeuristicType;

public interface ContinueCallback {
	enum ContinueOption{
		KEEP_GOING,
		FINISH_NOW,
		USER_CANCELLED
	}
	
	ContinueOption continueOptimisation(int nbSteps,HeuristicType currentHeuristic, EvaluatedSolution best);
}
