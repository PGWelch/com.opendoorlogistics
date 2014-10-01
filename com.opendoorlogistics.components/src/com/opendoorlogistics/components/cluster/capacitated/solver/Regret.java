/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

final public class Regret extends Cost {
	private final int customerIndx;
	private int bestIndx;
	private int nextBestIndx;
	
	public Regret(int customerIndx) {
		super();
		this.customerIndx = customerIndx;
	}
	
	public void update(Cost []costs){
		if(costs.length<2){
			throw new RuntimeException();
		}
		
		// find best
		bestIndx = Cost.getLowestCostIndx(costs);
		nextBestIndx=-1;
		if(bestIndx==-1){
			setMax();
			return;
		}
		
		Cost best = costs[bestIndx];
		
		// find second best
		for(int i =0 ; i < costs.length ; i++){
			if(i!=bestIndx){
				if(nextBestIndx==-1 || costs[i].compareTo(costs[nextBestIndx])<0){
					nextBestIndx=i;
				}				
			}
		}
		
		if(nextBestIndx==-1){
			setMax();
			return;		
		}
		
		Cost secondBest = costs[nextBestIndx];	

		// calculate
		set(secondBest);
		subtract(best);		
	
	}


	public int getCustomerIndx() {
		return customerIndx;
	}

	public int getBestIndx() {
		return bestIndx;
	}

	public int getNextBestIndx() {
		return nextBestIndx;
	}
	
	
}
