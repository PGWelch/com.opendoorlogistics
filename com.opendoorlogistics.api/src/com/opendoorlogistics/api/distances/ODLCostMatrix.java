/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.distances;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public interface ODLCostMatrix extends ODLTableReadOnly{
	public static final int COST_MATRIX_INDEX_COST=0;
	public static final int COST_MATRIX_INDEX_DISTANCE=1;
	public static final int COST_MATRIX_INDEX_TIME=2;
	
	double get(int fromIndex, int toIndex, int dim);
	int getNbCosts();
	int getIndex(String id);
	long getSizeInBytes();
	int getNbFroms();
	int getNbTos();
//	int getNbConnectedSubsets();
//	Iterable<String> getConnectedSubset(int i);
	boolean getIsConnected(int from, int to);
}
