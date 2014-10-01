/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

import gnu.trove.list.array.TIntArrayList;


final public class Utils {
	private Utils(){}

	public static TIntArrayList[] getClusterMembers(Solution solution) {
		TIntArrayList[] ret = new TIntArrayList[solution.getNbClusters()];
		for(int i =0 ; i < ret.length ; i++){
			ret[i] = new TIntArrayList();
		}
		
		int n = solution.getNbCustomers();
		for(int customerIndx =0 ; customerIndx < n ; customerIndx++){
			int cluster = solution.getClusterIndex(customerIndx);
			if(cluster!=-1){
				ret[cluster].add(customerIndx);
			}
		}
		return ret;
	}
	
	public static int [] getCentres(Solution solution){
		int []ret = new int[solution.getNbClusters()];
		for(int i=0 ; i < ret.length ; i++){
			ret[i] = solution.getClusterCentre(i);
		}
		return ret;
	}
	
	public static boolean numbersAreApproxEqual(double a, double b , double fractionalTolerance, double absoluteValueTolerance){
		if(Double.isNaN(a)){
			return Double.isNaN(b);
		}
		
		if(Double.isNaN(b)){
			return false;
		}
		
		if(Math.abs(a)<absoluteValueTolerance && Math.abs(b)<absoluteValueTolerance){
			return true;
		}
		return numbersAreApproxEqual(a, b, fractionalTolerance);
	}
	
	public static boolean numbersAreApproxEqual(double a, double b , double fractionalTolerance){
		double absDiff = Math.abs(a-b);
		
		if(a == 0 || b==0){
			// can't make a relative comparison.. make an absolute one
			return absDiff < fractionalTolerance;
		}
		
		if( absDiff > Math.abs(a) * fractionalTolerance){
			return false;
		}
		
		if( absDiff > Math.abs(b) * fractionalTolerance){
			return false;
		}
		return true;
	}
}
