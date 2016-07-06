/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.graphhopper;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.shapes.GHPoint;
import com.opendoorlogistics.graphhopper.CHMatrixGeneration;
import com.opendoorlogistics.graphhopper.MatrixResult;
import com.opendoorlogistics.graphhopper.geocodes4profiling.UKGeocodes;

public class TestMatrixCalculation {
	private CHMatrixGeneration dijsktra;
	private GHPoint[] points;
	private MatrixResult oneByOne;
	private MatrixResult combined;

	@Before
	public void setUp() throws Exception {
		
		String graphFolder = "C:\\temp\\TestGH0.5\\great-britain-latest.osm-gh";
		dijsktra = new CHMatrixGeneration(graphFolder);

		int n = 25;
		GHPoint[] pnts = UKGeocodes.createUKGeocodes();
		if (pnts.length < n) {
			n = pnts.length;
		}

		points = new GHPoint[n];
		for (int i = 0; i < n; i++) {
			points[i] = pnts[i];
		}

		
		System.out.println("Calculating combined");
		combined = dijsktra.calculateMatrix(points,null);

		System.out.println("Calculating one-by-one");
		long startNano = System.nanoTime();		
		oneByOne = dijsktra.calculateMatrixOneByOne(points);
		long endNano = System.nanoTime();
		double averageNanos =(double)(endNano - startNano)/(n*n);
		double averageMs = averageNanos / 1000000;
		System.out.print("Average milliseconds per one-by-one call: "+ averageMs );

	}

	@After
	public void tearDown() throws Exception {
		dijsktra.dispose();
	}

	@Test
	public void test() {
		assertEquals(oneByOne.getPointsCount(), combined.getPointsCount());
		assertEquals(points.length, combined.getPointsCount());

		int n = oneByOne.getPointsCount();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double da = oneByOne.getDistanceMetres(i,j);
				double db = combined.getDistanceMetres(i,j);
				assertEquals(da,db, 0.00001 * da);
				
				double ta = oneByOne.getTimeMilliseconds(i,j);
				double tb = combined.getTimeMilliseconds(i,j);
				assertEquals(ta,tb, 0.00001 * ta);
				
			}
		}
	}

	
}
