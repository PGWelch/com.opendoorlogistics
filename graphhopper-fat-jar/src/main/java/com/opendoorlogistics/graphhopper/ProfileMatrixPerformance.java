package com.opendoorlogistics.graphhopper;

import java.util.Arrays;

import com.graphhopper.GHResponse;
import com.graphhopper.util.shapes.GHPoint;
import com.opendoorlogistics.graphhopper.geocodes4profiling.UKGeocodes;
import com.opendoorlogistics.graphhopper.geocodes4profiling.USAGeocodes;

public class ProfileMatrixPerformance {
	public static void main(String []args){
	//	String graphFolder = "C:\\temp\\TestGH0.5\\great-britain-latest.osm-gh";
		String graphFolder = "C:\\temp\\TestGH0.5\\north-america-latest.osm-gh";
		int n = 250;

		//GHPoint [] pnts = UKGeocodes.createUKGeocodes();
		GHPoint [] pnts = USAGeocodes.createUSAGeocodes();

		pnts = Arrays.copyOf(pnts, Math.min(n, pnts.length));					


		
		System.out.println("Starting matrix profiling for " + pnts.length + " points");
		for(int i =0 ; i <= 10 ; i++){
			for(boolean memoryMapped : new boolean[]{
					false,
					true
					}){
				
				
				
				CHMatrixGeneration ch = new CHMatrixGeneration(graphFolder, memoryMapped);

				// test problem points first
//				GHPoint to = pnts[168];
//				GHPoint from = pnts[1];
//				GHResponse response = ch.getResponse(from, to);
				
				long startNano = System.nanoTime();		
				MatrixResult result = ch.calculateMatrix(pnts,null);
				long endNano = System.nanoTime();

				
				System.out.println("Time in millis: "+ ((endNano-startNano) / 1000000.0) +" for memorymapped=" + memoryMapped + " and " + pnts.length + " points");
				System.out.println("Allocated memory: " + (Runtime.getRuntime().totalMemory() / 1024) + " KB");
			}
	
		}

	}
	

}
