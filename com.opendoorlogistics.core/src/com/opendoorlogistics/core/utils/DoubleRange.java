/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

final public class DoubleRange {
	private double min = +Double.MAX_VALUE;
	private double max = -Double.MAX_VALUE;

	public DoubleRange(){}

	public DoubleRange(double min, double max){
		this.min =min;
		this.max = max;
	}

	public DoubleRange(double[]dbls){
		for(double d:dbls){
			add(d);
		}
	}
	
	public void add(Double d){
		if(d!=null){
			add((double)d);
		}
	}
	
	public void add(double d){
		min = Math.min(d, min);
		max = Math.max(d, max);
	}
	
	public double getLength(){
		return max - min;
	}
	
	public double getCentre(){
		return 0.5*(max + min);
	}
	
	public void multiply(double d){
		if(isValid()){
			double centre = 0.5*(min + max);
			double range = max - min;
			range *= d;
			min = centre - 0.5*range;
			max = centre + 0.5*range;
		}
	}
	
	public boolean isValid(){
		return min <= max;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}
	
	
}
