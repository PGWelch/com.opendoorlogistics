/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import com.opendoorlogistics.components.cluster.capacitated.solver.ContinueCallback.ContinueOption;
import com.opendoorlogistics.core.utils.IntUtils;

/**
 * This algorithm is a modified version of Mulvey and Beck (1984)'s first algorithm (MB1).
 * 
 * The regret for an unassigned customer is the difference in cost between the best and next best cluster it can be assigned to.
 * 
 * In the first instance p cluster centres are randomly chosen and customers are assigned to them in order of decreasing regret. Assignment never
 * breaks the capacity constraints.
 * 
 * When and if all customers are assigned, the centre of each cluster is reassigned to the candidate customer contained within it that has least
 * travel cost to all other customers in the cluster.
 * 
 * This may generate a new set of centres; the assignment and re-centering is repeated continually until no centres change. When the centres remain
 * stable a local search is then performed which swaps and moves customers between clusters.
 * 
 * When this local search terminates the whole algorithm is then repeated either from a new set of random centres (multi-start) or with a random
 * subset of centres set from the current best solution and the rest randomly chosen, so we then perform an iterated local search. This is the major
 * difference between this implementation and the MB1 algorithm, which is completely multistart.
 * 
 * The interpretation of MB1 is actually based on its description in 'A bionomic approach to the capacitated p-median problem' Maniezzo, Mingozzi and
 * Baldacci (didn't have access to the original Mulvey and Beck paper).
 * 
 * @author Phil
 * 
 */
public class Solver {
	private final Problem problem;
	private final int interchangeNNearest = 5;
	// private boolean logToConsole = false;
	private final ContinueCallback cont;
	private EvaluatedSolution best = null;
	private boolean useSwapMoves = false;
	private boolean useInsertionMoves = true;
	private int step = 0;

	public void setUseInsertionMoves(boolean useInsertionMoves) {
		this.useInsertionMoves = useInsertionMoves;
	}

	public enum HeuristicType {
		LOCAL_SEARCH, REGRET_REASSIGN, INITIAL_ASSIGN
	}

	public Solver(Problem problem, ContinueCallback cont) {
		this.problem = problem;
		this.cont = cont;
	}

	// public boolean isLogToConsole() {
	// return logToConsole;
	// }

	// public void setLogToConsole(boolean logToConsole) {
	// this.logToConsole = logToConsole;
	// }

	private int[] generateRandomPMedians(Random random) {
		int p = problem.getNbClusters();
		int[] ret = new int[p];
		for (int i = 0; i < p; i++) {
			ret[i] = problem.getFixedLocation(i);
		}

		TIntArrayList available = new TIntArrayList();
		for (int i = 0; i < problem.getNbLocations(); i++) {
			if (IntUtils.contains(ret, i) == false) {
				available.add(i);
			}
		}

		generateRandomPMedians(random, available, ret);
		return ret;
	}

	private void generateRandomPMedians(Random random, TIntArrayList available, int[] chosen) {
		if (chosen.length != problem.getNbClusters()) {
			throw new RuntimeException();
		}

		TIntArrayList unallocatedSlots = new TIntArrayList(problem.getNbClusters());
		while (true) {
			// find unallocated
			unallocatedSlots.clear();
			for (int i = 0; i < chosen.length; i++) {
				if (chosen[i] == -1) {
					unallocatedSlots.add(i);
				}
			}

			if (unallocatedSlots.size() == 0) {
				return;
			}

			// choose random unallocated slot
			int slotIndx = unallocatedSlots.get(random.nextInt(unallocatedSlots.size()));

			// choose random customer from the available customers
			if (available.size() == 0) {
				return;
			}
			int randIndx = random.nextInt(available.size());
			chosen[slotIndx] = available.get(randIndx);
			available.removeAt(randIndx);
		}

	}

	private int[] mutatePMedians(Random random, int[] original) {

		if (original.length != problem.getNbClusters()) {
			throw new RuntimeException();
		}

		// get the subset of clusters which can be unallocated
		int p = problem.getNbClusters();
		TIntArrayList unallocatables = new TIntArrayList();
		for (int i = 0; i < p; i++) {
			if (original[i] != -1 && problem.getFixedLocation(i) == -1) {
				unallocatables.add(i);
			}
		}

		// decide how many to unallocate
		int[] mutated = Arrays.copyOf(original, original.length);
		if(unallocatables.size()>0){
			int nbToUnallocate = 1 + random.nextInt(unallocatables.size());

			// unallocate them
			unallocatables.shuffle(random);
			for (int i = 0; i < nbToUnallocate; i++) {
				if (i < unallocatables.size()) {
					int clusterIndex = unallocatables.get(i);
					mutated[clusterIndex] = -1;
				}
			}			
		}

		// get the available customers - these are any locations not used in the mutated cluster set
		TIntArrayList available = new TIntArrayList();
		for (int i = 0; i < problem.getNbLocations(); i++) {
			if (IntUtils.contains(mutated, i) == false) {
				available.add(i);
			}
		}

		// generate others randomly
		generateRandomPMedians(random, available, mutated);
		return mutated;
	}

	/**
	 * Do a regret-based assignment of all customers using the fixed centres and then reassign the centres afterwards.
	 * 
	 * @param centres
	 * @return
	 */
	private EvaluatedSolution regretBasedAssignment(int[] centres, HeuristicType currentHeuristicType) {

		// create an empty solution with the fixed centres (this assigns the customers which are centres)
		EvaluatedSolution evaluated = new EvaluatedSolution(problem, centres);
		evaluated.setAllCentresImmutable(true);

		// initialise all costs for unassigned customers (cluster centres are already assigned)
		int p = problem.getNbClusters();
		int nc = problem.getNbLocations();
		Cost[][] allCosts = new Cost[nc][];
		for (int customerIndx = 0; customerIndx < nc; customerIndx++) {
			if (evaluated.getClusterIndex(customerIndx) == -1) {
				allCosts[customerIndx] = new Cost[p];
				for (int clusterIndx = 0; clusterIndx < p; clusterIndx++) {
					Cost cost = new Cost();
					allCosts[customerIndx][clusterIndx] = cost;
					if (centres[clusterIndx] != -1) {
						evaluated.evaluateSet(customerIndx, clusterIndx, cost);
					} else {
						// cluster is unavailable
						cost.setMax();
					}
				}
			}
		}

		// initialise regret
		Regret[] regrets = new Regret[nc];
		for (int customerIndx = 0; customerIndx < nc; customerIndx++) {
			if (evaluated.getClusterIndex(customerIndx) == -1) {
				Regret regret = new Regret(customerIndx);
				regret.update(allCosts[regret.getCustomerIndx()]);
				regrets[customerIndx] = regret;
			}
		}

		// keep a binary heap of regrets
		PriorityQueue<Regret> queue = new PriorityQueue<>(regrets.length, new Comparator<Regret>() {

			@Override
			public int compare(Regret o1, Regret o2) {
				// sort highest first
				int diff = -o1.compareTo(o2);

				// also sort by id so everything is unique
				if (diff == 0) {
					diff = Integer.compare(o1.getCustomerIndx(), o2.getCustomerIndx());
				}
				return diff;
			}
		});
		for (Regret regret : regrets) {
			if (regret != null) {
				queue.add(regret);
			}
		}

		// keep on popping the queue
		while (queue.size() > 0) {

			// check for quitting (only break loop if user actually quit)
			if (getContinue(currentHeuristicType) == ContinueOption.USER_CANCELLED) {
				return null;
			}

			Regret top = queue.poll();

			int customerIndx = top.getCustomerIndx();
			if (evaluated.getClusterIndex(customerIndx) != -1) {
				throw new RuntimeException();
			}
			int clusterIndx = Cost.getLowestCostIndx(allCosts[customerIndx]);
			if (clusterIndx == -1 || allCosts[customerIndx][clusterIndx].isMax()) {
				// can't assign
				continue;
			}

			// do assignment and blank its cost records
			evaluated.setCustomerToCluster(customerIndx, clusterIndx);
			regrets[customerIndx] = null;
			allCosts[customerIndx] = null;

			// update all costs for this cluster over all unassigned customers
			for (int i = 0; i < nc; i++) {
				Cost[] costs = allCosts[i];

				// check customer is still unassigned
				if (costs != null) {
					if (evaluated.getClusterIndex(i) != -1) {
						throw new RuntimeException();
					}

					// get new cost for this cluster index
					evaluated.evaluateSet(i, clusterIndx, costs[clusterIndx]);

					// Update regret if the changed cluster corresponds to the first or second
					// best cost or the changed cluster has a better cost than the second best.
					Regret regret = regrets[i];
					if (regret.getBestIndx() == clusterIndx || regret.getNextBestIndx() == clusterIndx
							|| costs[clusterIndx].compareTo(costs[regret.getNextBestIndx()]) < 0) {

						// remove, update and re-add
						queue.remove(regret);
						regret.update(costs);
						queue.add(regret);
					}
				}
			}

		}

		// // test costs OK
		// Cost testCost = new Cost(evaluated.getCost());
		// evaluated.update();
		// if(Cost.isApproxEqual(evaluated.getCost(), testCost)==false){
		// throw new RuntimeException();
		// }

		// update to reassign centres and refresh (help prevent rounding error)
		evaluated.setAllCentresImmutable(false);
		evaluated.update();

		return evaluated;
	}

	private boolean localSearchSingleStep(Random random, EvaluatedSolution solution) {

		// calculate a cluster to cluster distance matrix using the minimum distance to
		// a point in another cluster
		int p = problem.getNbClusters();
		final double[][] matrix = new double[p][p];
		for (int i = 0; i < p; i++) {
			matrix[i] = new double[p];
			Arrays.fill(matrix[i], Double.MAX_VALUE);
		}

		int nc = problem.getNbLocations();
		for (int i = 0; i < nc; i++) {
			int ci = solution.getClusterIndex(i);
			if (ci != -1) {
				for (int j = 0; j < nc; j++) {
					int cj = solution.getClusterIndex(j);
					if (cj != -1) {
						double distance = problem.getTravel(i, j);
						matrix[ci][cj] = Math.min(matrix[ci][cj], distance);
					}
				}
			}
		}

		// get a sorted list of nearest clusters for each cluster
		ArrayList<ArrayList<Integer>> nearestLists = new ArrayList<>();
		for (int i = 0; i < p; i++) {
			ArrayList<Integer> nearest = new ArrayList<>();
			nearestLists.add(nearest);
			for (int j = 0; j < p; j++) {
				if (i != j) {
					nearest.add(j);
				}
			}

			final int from = i;
			Collections.sort(nearest, new Comparator<Integer>() {

				@Override
				public int compare(Integer o1, Integer o2) {
					double d1 = matrix[from][o1];
					double d2 = matrix[from][o2];
					return Double.compare(d1, d2);
				}
			});

			while (nearest.size() > interchangeNNearest) {
				nearest.remove(nearest.size() - 1);
			}
		}

		// get a randomly ordered list of cluster indices
		TIntArrayList list = new TIntArrayList(p);
		for (int i = 0; i < p; i++) {
			list.add(i);
		}
		list.shuffle(random);

		// record starting cost
		solution.update();
		Cost initial = new Cost();
		initial.set(solution.getCost());

		// loop over each cluster taking first improving moves
		for (int i = 0; i < p; i++) {
			int cli = list.get(i);

			// shuffle its nearest clusters
			List<Integer> nearest = nearestLists.get(cli);
			Collections.shuffle(nearest, random);

			for (int j = 0; j < nearest.size(); j++) {
				int clj = nearest.get(j);
				if (cli == clj) {
					throw new RuntimeException();
				}

				if (random.nextBoolean()) {
					if (useInsertionMoves) {
						interclusterMoves(random,cli, clj, solution);
					}

					if (useSwapMoves) {
						interclusterSwaps(random,cli, clj, solution);
					}

				} else {
					if (useSwapMoves) {
						interclusterSwaps(random,cli, clj, solution);
					}

					if (useInsertionMoves) {
						interclusterMoves(random,cli, clj, solution);
					}
				}
			}

			// refresh solution after processing each cluster to help prevent round-off
			solution.update();

			// check for quitting
			updateGlobalBest(solution);
			if (getContinue(HeuristicType.LOCAL_SEARCH) != ContinueOption.KEEP_GOING) {
				break;
			}
		}

		// see if we've improved by more than the round-off limit
		Cost newCost = solution.getCost();
		if (!Cost.isApproxEqual(initial, newCost)) {
			return newCost.compareTo(initial) < 0;
		}

		return false;
	}

	/**
	 * Try moving each location in cluster i to cluster j; take any improving moves
	 * 
	 * @param clusteri
	 * @param clusterj
	 * @param solution
	 */
	private void interclusterMoves(Random random,int clusteri, int clusterj, EvaluatedSolution solution) {

		Cost cost = new Cost();
		TIntArrayList customersi = new TIntArrayList();
		solution.getCustomers(clusteri, customersi);
		int n = customersi.size();
		customersi.shuffle(random);
		int fixedCentre = problem.getFixedLocation(clusteri);

		// loop over each customer in cluster i
		for (int i = 0; i < n; i++) {
			// check we're not moving the fixed centre
			int customeri = customersi.get(i);
			if (customeri != fixedCentre) {
				solution.evaluateSet(customeri, clusterj, cost);
				if (cost.getCapacityViolation() <= 0 && cost.getTravel() <= 0) {
					solution.setCustomerToCluster(customeri, clusterj);
				}
			}
		}
	}

//	private void mutateLocationAssignment(Random random,int clusteri, int clusterj,double mutatefraction, EvaluatedSolution solution) {
//
//		TIntArrayList customersi = new TIntArrayList();
//		solution.getCustomers(clusteri, customersi);
//		int n = customersi.size();
//		customersi.shuffle(random);
//		int fixedCentre = problem.getFixedLocation(clusteri);
//
//		// loop over each customer in cluster i
//		for (int i = 0; i < n; i++) {
//			// check we're not moving the fixed centre
//			int customeri = customersi.get(i);
//			if (customeri != fixedCentre) {
//				boolean mutate = random.nextDouble() < mutatefraction;
//				if(mutate){
//					solution.setCustomerToCluster(customeri, clusterj);					
//				}
//			}
//		}
//	}

	private void interclusterSwaps(Random random,int clusteri, int clusterj, EvaluatedSolution solution) {

		TIntArrayList customersi = new TIntArrayList();
		solution.getCustomers(clusteri, customersi);
		customersi.shuffle(random);

		TIntArrayList customersj = new TIntArrayList();
		solution.getCustomers(clusterj, customersj);
		customersj.shuffle(random);

		Cost cost = new Cost();
		Cost bestSwap = new Cost();

		int fixedCentrei = problem.getFixedLocation(clusteri);
		int fixedCentrej = problem.getFixedLocation(clusterj);

		int ni = customersi.size();
		int nj = customersj.size();
		for (int i = 0; i < ni; i++) {

			int customeri = customersi.get(i);
			if (customeri != fixedCentrei) {
				bestSwap.setMax();
				int bestSwapCustomerIndx = -1;
				for (int j = 0; j < nj; j++) {
					// check still assigned to j before evaluating swap
					int customerj = customersj.get(j);
					if (customerj != fixedCentrej) {
						if (solution.getClusterIndex(customerj) == clusterj) {

							solution.evaluateSwap(customeri, customerj, cost);
							if (cost.compareTo(bestSwap) < 0) {
								bestSwap.set(cost);
								bestSwapCustomerIndx = customerj;
							}
						}
					}
				}

				// do swap if profitable
				if (bestSwap.getCapacityViolation() <= 0 && bestSwap.getTravel() <= 0) {
					solution.setCustomerToCluster(customeri, clusterj);
					solution.setCustomerToCluster(bestSwapCustomerIndx, clusteri);
				}
			}

		}
	}

	private ContinueOption getContinue(HeuristicType currentHeuristic) {

		// always call the callback even on step 0 as it also reports cost
		ContinueOption ret = cont.continueOptimisation(step, currentHeuristic, best);

		// always ensure we have a solution if the user hasn't cancelled
		if (best == null && ret == ContinueOption.FINISH_NOW) {
			return ContinueOption.KEEP_GOING;
		}

		return ret;
	}

	public synchronized EvaluatedSolution run() {
		Random random = new Random(123);

		// reset
		best = null;
		step = 0;

		while (true) {
			// get new centres from either mutation or random restart
			int[] centres;
			if (random.nextInt(3) == 0 || best == null) {
				centres = generateRandomPMedians(random);
			} else {
				centres = mutatePMedians(random, Utils.getCentres(best));
			}

			// create an initial assigned solution using regret
			EvaluatedSolution assigned = regretBasedAssignment(centres, HeuristicType.INITIAL_ASSIGN);
			if(assigned!=null){				
				updateGlobalBest(assigned);
			}
			if (getContinue(HeuristicType.INITIAL_ASSIGN) != ContinueOption.KEEP_GOING) {
				break;
			}

			// then loop through cycles of choose centre, do regret based assignment
			EvaluatedSolution localBest = regretReassignLoop(assigned);
			if (getContinue(HeuristicType.REGRET_REASSIGN) != ContinueOption.KEEP_GOING) {
				break;
			}

			// now optimise using swaps and moves until the local search stagnates
			if (useInsertionMoves || useSwapMoves) {
				while (localSearchSingleStep(random, localBest)) {
					updateGlobalBest(localBest);
					if (getContinue(HeuristicType.LOCAL_SEARCH) != ContinueOption.KEEP_GOING) {
						break;
					}
				}
			}

			// check if we've beaten the best
			updateGlobalBest(localBest);
			step++;
		}

		return best;
	}

	private boolean updateGlobalBest(EvaluatedSolution sol) {
		if (best == null || (Cost.isApproxEqual(sol.getCost(), best.getCost()) == false && sol.getCost().compareTo(best.getCost()) <= 0)) {

			// deep copy
			best = new EvaluatedSolution(sol);
			if(Cost.isApproxEqual(best.getCost(), sol.getCost())==false){
				throw new RuntimeException();
			}
			return true;
		}
		return false;
	}

	private EvaluatedSolution regretReassignLoop(EvaluatedSolution initial) {

		// continue looping until no improvement
		EvaluatedSolution localBest = new EvaluatedSolution(initial);

		// int nbRegretLoops=0;
		while (true) {

			// get the current centres and do a regret-based assignment using them
			int[] centres = Utils.getCentres(localBest);
			EvaluatedSolution assigned = regretBasedAssignment(centres, HeuristicType.REGRET_REASSIGN);

			// if(logToConsole){
			// System.out.println("After regret: " + assigned.getCost() );
			// }

			// update local best (assigned can be null if user cancelled)
			if (assigned!=null && Cost.isApproxEqual(assigned.getCost(), localBest.getCost()) == false && assigned.getCost().compareTo(localBest.getCost()) < 0) {
				localBest = assigned;
			} else {
				break;
			}

			// update global best and check for quitting
			updateGlobalBest(localBest);
			if (getContinue(HeuristicType.REGRET_REASSIGN) != ContinueOption.KEEP_GOING) {
				break;
			}

			// nbRegretLoops++;
		}
		return localBest;
	}

	public void setUseSwapMoves(boolean doSwaps) {
		this.useSwapMoves = doSwaps;
	}

}
