/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.kmeans.latlng;

import com.opendoorlogistics.components.cluster.kmeans.Mean;

final public class MeanLngLat extends Mean<KMeanPointLngLat>{


	@Override
	public void updateMean() {
		if(assigned.size()==0){
			throw new RuntimeException();
		}
		
		// get a mean in cartesian space
		double [] tmp = new double[3];
		for(KMeanPointLngLat pnt : assigned){
			double [] xyz = pnt.toUnitCartesian();
			for(int i = 0 ; i < 3 ; i++){
				tmp[i] += xyz[i];
			}
		}
		
		for(int i = 0 ; i < 3 ; i++){
			tmp[i] /= assigned.size();
		}
		
		// convert back to angles
		mean.latitude = 90 -Math.toDegrees(Math.acos(tmp[2]) );
		mean.longitude =Math.toDegrees (Math.atan2(tmp[1], tmp[0]));
	}

	@Override
	protected KMeanPointLngLat createObj() {
		return new KMeanPointLngLat();
	}

//	public static void main(String[] args) {
//		for(Annotation annotation : KMeanPointLngLat.class.getAnnotations()){
//			System.out.println(annotation);			
//		}
//		
//		MeanLngLat mll = new MeanLngLat();
//		KMeanPointLngLat a = new KMeanPointLngLat(-4.012, 52.008);
//		mll.addAssigned(a) ;
//		
//		KMeanPointLngLat b = new KMeanPointLngLat(-4.316,51.856);
//		mll.addAssigned(b);
//		mll.updateMean();
//		double earthRadiusKm = 6371 ;
//		System.out.println(mll.getMean() + " separation " + earthRadiusKm* a.distance(b) + " " +earthRadiusKm* b.distance(a));
//	}
}
