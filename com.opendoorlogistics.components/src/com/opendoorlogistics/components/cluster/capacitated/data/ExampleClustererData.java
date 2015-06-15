/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.data;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.components.cluster.capacitated.solver.ContinueCallback;
import com.opendoorlogistics.components.cluster.capacitated.solver.EvaluatedSolution;
import com.opendoorlogistics.components.cluster.capacitated.solver.FilterCallbackEvents;
import com.opendoorlogistics.components.cluster.capacitated.solver.Problem;
import com.opendoorlogistics.components.cluster.capacitated.solver.Solver;
import com.opendoorlogistics.components.cluster.capacitated.solver.Solver.HeuristicType;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ExampleClustererData {
	private boolean logToConsole;
	private boolean heterogeneousCostPerUnitTravel = false;
	private boolean hetereogenousClusterCapacity = false;
	private boolean fixedClusterSubset=false;

	private static final double POSITION_RANGE = 100;

	public ExampleClustererData(boolean logToConsole) {
		this.logToConsole = logToConsole;
	}

	private static double[] randomAllocation(int nbAssignments, double total, Random random) {

		// divide used capacity
		double[] randoms = new double[nbAssignments - 1];
		for (int i = 0; i < randoms.length; i++) {
			randoms[i] = total * random.nextDouble();
		}
		Arrays.sort(randoms);

		double sum = 0;
		double[] ret = new double[nbAssignments];
		for (int i = 0; i < nbAssignments; i++) {

			// get quantity
			double lastValue = 0;
			if (i > 0) {
				lastValue = randoms[i - 1];
			}

			double value = total;
			if (i < nbAssignments - 1) {
				value = randoms[i];
			}
			ret[i] = value - lastValue;
			sum += ret[i];
			// if (logToConsole) {
			// System.out.println("Assignment " + i + ": " + ret[i] + ", total allocated=" + sum);
			// }
		}

		// if (logToConsole) {
		// System.out.println("Check total quantity: " + sum + " should be approximately equal to " + total);
		// }

		return ret;
	}

	public Problem createProblem(ODLApi api,int nbLocations, int nbClusters, double percentageUsedCapacity) {
		double totalCapacity = 1000;
		double usedCapacity = totalCapacity * percentageUsedCapacity / 100.0;
		double capacityPerCluster = totalCapacity / nbClusters;
		if (logToConsole) {
			System.out.println("Total capacity: " + totalCapacity);
			System.out.println("Used capacity: " + usedCapacity);
		}

		// divide used capacity up by nbCustomers
		Random random = new Random(1);
		double[] randoms = randomAllocation(nbLocations, usedCapacity, random);

		// create customers
		ArrayList<Point2D.Double> pnts = new ArrayList<>(nbLocations);
		ArrayList<Location> locations = new ArrayList<Location>(nbLocations);
		for (int i = 0; i < nbLocations; i++) {
			// get position
			pnts.add(new Point2D.Double(random.nextDouble() * POSITION_RANGE, random.nextDouble() * POSITION_RANGE));

			Location loc = new Location();
			locations.add(loc);
			loc.setQuantity(randoms[i]);
			loc.setId("Location" + Integer.toString(i));

			// if testing cost per unit travel, set the cost arbitrarily high on first p customers
			// so they will always be chosen as cluster centres
			if (heterogeneousCostPerUnitTravel && i < nbClusters) {
				loc.setCostPerUnitTravel(1000.0 * nbLocations);
			}
		}

		// create clusters
		Cluster[] clusters = new Cluster[nbClusters];
		for (int i = 0; i < nbClusters; i++) {
			clusters[i] = new Cluster();
			clusters[i].setClusterId(Integer.toString(i + 1));
			clusters[i].setCapacity(capacityPerCluster);
		}
		
		if (hetereogenousClusterCapacity) {
			double[] capacities = randomAllocation(nbClusters, totalCapacity, random);
			for (int i = 0; i < nbClusters; i++) {
				clusters[i].setCapacity(capacities[i]);
			}
		}
		
		if(fixedClusterSubset){
			for (int i = 0; i < nbClusters; i++) {
				if(i%2==0){
					
					// create dummy location
					double factor = (double)POSITION_RANGE * ((double)i /(nbClusters-1) );
					pnts.add(new Point2D.Double(factor, factor));
					Location location = new Location();
					location.setId("Cluster" + i);
					location.setQuantity(0);
					locations.add(location);
					
					// set cluster to use it
					clusters[i].setFixedLocation(1);
					clusters[i].setLocationKey(location.getId());
				}
			}
		}
		
		// create travel
		nbLocations = locations.size();
		Travel[] travel = new Travel[nbLocations * nbLocations];
		int indx = 0;
		for (int i = 0; i < nbLocations; i++) {
			Point2D.Double from = pnts.get(i);
			for (int j = 0; j < nbLocations; j++) {
				Point2D.Double to = pnts.get(j);
				double cost = from.distance(to);
				Travel trv = new Travel();
				trv.setFromLocation(locations.get(i).getId());
				trv.setToLocation(locations.get(j).getId());
				trv.setCost(cost);
				travel[indx++] = trv;
			}
		}
		return new Problem(api,locations, Arrays.asList(clusters),Arrays.asList(travel));

	}

	
	public boolean isHeterogeneousCostPerUnitTravel() {
		return heterogeneousCostPerUnitTravel;
	}

	public void setHeterogeneousCostPerUnitTravel(boolean heterogeneousCostPerUnitTravel) {
		this.heterogeneousCostPerUnitTravel = heterogeneousCostPerUnitTravel;
	}

	public boolean isHetereogenousClusterCapacity() {
		return hetereogenousClusterCapacity;
	}

	public void setHetereogenousClusterCapacity(boolean hetereogenousClusterCapacity) {
		this.hetereogenousClusterCapacity = hetereogenousClusterCapacity;
	}

	public boolean isFixedClusterSubset() {
		return fixedClusterSubset;
	}

	public void setFixedClusterSubset(boolean fixedClusterSubset) {
		this.fixedClusterSubset = fixedClusterSubset;
	}

	public static void main(String[] args) {
		ExampleClustererData exampleClustererData = new ExampleClustererData(true);
		// exampleClustererData.testLimitedCandidates = true;
		exampleClustererData.setFixedClusterSubset(true);
		Problem problem = exampleClustererData.createProblem(new ODLApiImpl(),100, 10, 90);

		final FilterCallbackEvents filter = new FilterCallbackEvents();

		System.out.println("Starting solver");
		Solver solver = new Solver(problem, new ContinueCallback() {

			@Override
			public ContinueOption continueOptimisation(int nbSteps, HeuristicType type, EvaluatedSolution best) {
				if (nbSteps > 10) {
					return ContinueOption.FINISH_NOW;
				}
				if (filter.hasStateChanged(nbSteps, type, best) && best != null) {
					System.out.println(new Date().toString() + " - Step: " + nbSteps + " (" + Strings.convertEnumToDisplayFriendly(type.name()) + ")"
							+ " Best: " + best.getCost());

				}
				return ContinueOption.KEEP_GOING;
			}
		});
		solver.setUseSwapMoves(true);
		EvaluatedSolution sol = solver.run();
		System.out.println(sol);
	}
}
