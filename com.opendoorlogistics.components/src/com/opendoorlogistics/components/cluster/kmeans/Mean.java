/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.kmeans;

import java.util.ArrayList;

public abstract class Mean<T extends KMeanPoint> {
	protected final ArrayList<T> assigned = new ArrayList<>();
	protected final T mean = createObj();
	
	public void clearAssigned(){
		assigned.clear();
	}
	
	public void addAssigned(T p){
		assigned.add(p);
	}
	
	public abstract void updateMean();
	
	public int size(){
		return assigned.size();
	}
	
	protected abstract T createObj();
	
	public T getMean(){
		return mean;
	}
}
