/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.kmeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.api.components.ComponentExecutionApi;

final public class KMeansAlgorithm {
	public interface CreateMean <T extends KMeanPoint>{
		Mean<T> createMean(T copyThis);
	}
	
	public <T extends KMeanPoint> List<Mean<T>> execute(int k, int randomseed,CreateMean<T>createMean, List<T> points,  ComponentExecutionApi reporter){
		if(k > points.size()){
			k = points.size();
		}
		
		ArrayList<Mean<T>> means = new ArrayList<>(k);
		if(k==0){
			return means;
		}
		
		// random choose k centres
		Random random = new Random(randomseed);
		ArrayList<T> tmp = new ArrayList<>(points);
		Collections.shuffle(tmp, random);
		for(int i=0 ;i < k ; i++){
			means.add(createMean.createMean(tmp.get(i)));
		}
		
		// reset all cluster numbers
		for(T point : points){
			point.clusterNumber = -1;
		}
		
		int nbChanges;
		int stepNb=0;
		do{
		
			// clear all assignments from the means
			for(Mean<T> mean : means){
				mean.clearAssigned();
			}
			
			// assign everything
			nbChanges=0;
			for(T pnt : points){
				int closest=-1;
				double closestDist = Double.MAX_VALUE;
				for(int i=0 ;i < k ; i++){
					Mean<T> mean = means.get(i);
					double dist = mean.getMean().distance(pnt);
					if(dist < closestDist){
						closestDist = dist;
						closest = i;
					}
				}	
				
				// count changes
				if(pnt.clusterNumber != closest){
					nbChanges++;
				}
				
				// do assignment
				pnt.clusterNumber = closest;
				means.get(closest).addAssigned(pnt);
			}
			
			// update means
			for(Mean<T> mean : means){
				if(mean.size()>0){
					mean.updateMean();
				}
			}
			
			reporter.postStatusMessage("K means step " + (stepNb+1));
			stepNb++;
		}while(nbChanges>0 && reporter.isCancelled()==false && reporter.isFinishNow()==false);

		return means;
	}
}
