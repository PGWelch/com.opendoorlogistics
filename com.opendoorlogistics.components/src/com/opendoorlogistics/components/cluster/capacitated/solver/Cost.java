/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

public class Cost implements Comparable<Cost>{
	private double travel;
	private double capacityViolation;
	
	public double getTravel() {
		return travel;
	}
	public void setTravel(double travel) {
		this.travel = travel;
	}
	public double getCapacityViolation() {
		return capacityViolation;
	}
	public void setCapacityViolation(double capacityViolation) {
		this.capacityViolation = capacityViolation;
	}
	
	public Cost(){
	}
	
	public Cost(Cost copyThis){
		this.travel = copyThis.getTravel();
		this.capacityViolation = copyThis.getCapacityViolation();
	}
	
	@Override
	public int compareTo(Cost o) {
		int diff = Double.compare(capacityViolation, o.capacityViolation);
		if(diff==0){
			diff = Double.compare(travel, o.travel);
		}
		return diff;
	}
	
	public void setZero(){
		travel = 0;
		capacityViolation = 0;
	}
	
	public void setMax(){
		capacityViolation = Double.MAX_VALUE;
		travel = Double.MAX_VALUE;
	}
	
	public boolean isMax(){
		return capacityViolation == Double.MAX_VALUE;
	}
	
	public void add(Cost cost){
		travel += cost.travel;
		capacityViolation += cost.capacityViolation;
	}
	
	public void subtract(Cost cost){
		travel -= cost.travel;
		capacityViolation -= cost.capacityViolation;	
	}
	
	public void set(Cost cost){
		travel = cost.travel;
		capacityViolation = cost.capacityViolation;
	}
	
	public void negate(){
		travel = -travel;
		capacityViolation = -capacityViolation;
	}
	
	public static int getLowestCostIndx(Cost [] costs){
		int indx=-1;
		for(int i =0 ; i < costs.length ; i++){
			if(indx==-1 || costs[i].compareTo(costs[indx])<0){
				indx=i;
			}
		}
		return indx;
	}
	
	@Override
	public String toString() {
		return "[trv=" + travel + ", overcap=" + capacityViolation + "]";
	}
	
	private final static double ROUNDOFF_FRACTION = 0.000000001;

	public static boolean isApproxEqual(Cost a, Cost b){
		return Utils.numbersAreApproxEqual(a.getCapacityViolation(), b.getCapacityViolation(), ROUNDOFF_FRACTION, ROUNDOFF_FRACTION)
			&& Utils.numbersAreApproxEqual(a.getTravel(), b.getTravel(), ROUNDOFF_FRACTION, ROUNDOFF_FRACTION);
	}
	
}
