/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * An evaluated solution which can be incrementally updated.
 * @author Phil
 *
 */
final public class EvaluatedSolution extends HasCost implements Solution{
	private final Problem problem;
	private final CustomerRecord [] customers;
	private final ClusterRecord [] clusters;
	private boolean allCentresImmutable;
	
	private class CustomerRecord{
		private final int id;
		private double travelSumToSameClusterCustomers;
		private ClusterRecord cluster;
		
		private CustomerRecord(int id) {
			super();
			this.id = id;
		}
	
		private double getQuantity(){
			return problem.getLocationQuantity(id);
		}
		
		private int getInternalClusterIndx(){
			return cluster!=null? cluster.id : -1;
		}
		
		@Override
		public String toString(){
			return "id=" + id + ", travelSum=" + travelSumToSameClusterCustomers + ", " + (cluster!=null? "cluster " + cluster.id:"unassigned");
		}
		
//		public void clear(){
//			travelSumToSameClusterCustomers=0;
//			cluster=null;
//		}
	}
	
	private class ClusterRecord extends HasCost{
		private final int id;	
		private double quantity;
		private CustomerRecord centre;
		private ArrayList<CustomerRecord> assignedCustomers = new ArrayList<>();
		
		private ClusterRecord(int id) {
			super();
			this.id = id;
		}
		
//		public void clear(){
//			cost.setZero();
//			quantity=0;
//			centre = null;
//			customers.clear();
//		}
		
		@Override
		public String toString(){
			return "id=" + id + ", " + cost.toString() + ", quantity=" + quantity +"/" + problem.getClusterCapacity(id) + ", nbCustomers=" + assignedCustomers.size()
					+ (centre!=null?", centre=" + centre.id:"");
		}
		
		private void getCustomers( TIntArrayList out){
			out.clear();
			int n = assignedCustomers.size();
			out.ensureCapacity(n);;
			for(int i =0 ; i < n ; i++){
				out.add(assignedCustomers.get(i).id);
			}
		}

		private void updateAll(){
		//	assert saveCostChecker();
			
			quantity=0;
			int n = assignedCustomers.size();
			for(int i = 0 ; i < n ; i++){
				CustomerRecord ci = assignedCustomers.get(i);
				quantity += ci.getQuantity();
				ci.travelSumToSameClusterCustomers = 0;
				
				for(int j = 0 ; j < n ; j++){
					if(i!=j){
						CustomerRecord cj = assignedCustomers.get(j);	
						ci.travelSumToSameClusterCustomers += getTravelCost(ci, cj);						
					}
				}
			}
			
			updateCentreAndCost();		
		//	assert changedCentre || isCostEqualToChecker();
		}
		
		private boolean isValidState(){
			return (centre==null && assignedCustomers.size()==0) ||  (centre!=null && assignedCustomers.size()>0);
		}
		
		private boolean updateCentreAndCost(){
			
			CustomerRecord oldCentre = centre;
			if(!isImmutableCentre(id)){
				centre = null;
				int n = assignedCustomers.size();
				for(int i = 0 ; i < n ; i++){
					CustomerRecord rec = assignedCustomers.get(i);		
					if(centre == null || rec.travelSumToSameClusterCustomers < centre.travelSumToSameClusterCustomers){
						centre = rec;
					}									
				}					
			}
			cost.setZero();
						
			if(centre!=null){
				cost.setTravel(centre.travelSumToSameClusterCustomers);		
			}
			
			double capacity = problem.getClusterCapacity(id);
			if(quantity > capacity){
				cost.setCapacityViolation(quantity - capacity);
			}
			
			if(!isValidState()){
				throw new RuntimeException();
			}
			
			return oldCentre != centre;
		}
		
		private void insert(CustomerRecord customer){
			if(!isValidState()){
				throw new RuntimeException();
			}
			
			if(customer.cluster!=null){
				throw new RuntimeException();
			}
			customer.cluster= this;
			customer.travelSumToSameClusterCustomers =0;
	
			// update the distances on all customer records if not using immutable centres
			if(isImmutableCentre(id)==false){
				int n = assignedCustomers.size();
				for(int i = 0 ; i < n ; i++){
					CustomerRecord other =assignedCustomers.get(i);
					other.travelSumToSameClusterCustomers += getTravelCost(other, customer);
					customer.travelSumToSameClusterCustomers +=getTravelCost(customer, other);
				}				
			}else if(centre!=null){
				centre.travelSumToSameClusterCustomers += getTravelCost(centre, customer);		
			}
			
			// add the customer including the quantity
			assignedCustomers.add(customer);
			quantity += customer.getQuantity();
			
			updateCentreAndCost();
		}
	
		private double getTravelCost(CustomerRecord cluster, CustomerRecord beingServed){
			return problem.getCostPerUnitTravelled(beingServed.id)* problem.getTravel(cluster.id, beingServed.id);
		}
		
		private void remove(CustomerRecord customer){
			if(!isValidState()){
				throw new RuntimeException();
			}
			
			// check we're not removing the centre if centres are immutable
			if(isImmutableCentre(id) && customer == centre){
				throw new RuntimeException();
			}
			
			if(customer.cluster != this){
				throw new RuntimeException();
			}
			
			// find record
			int indx = assignedCustomers.indexOf(customer);
			if(indx==-1){
				throw new RuntimeException();
			}
			
			// remove quantity
			quantity-= customer.getQuantity();
			
			// remove record
			assignedCustomers.remove(indx);
			customer.cluster=null;
			
			// update distances for all if not using immutable centres
			customer.travelSumToSameClusterCustomers = 0;			
			if(isImmutableCentre(id)==false){
				int n = assignedCustomers.size();
				for(int i = 0 ; i < n ; i++){
					CustomerRecord other =assignedCustomers.get(i);
					other.travelSumToSameClusterCustomers -= getTravelCost(other, customer);
				}	
			}
			else if (centre!=null){
				centre.travelSumToSameClusterCustomers -= getTravelCost(centre, customer);
			}
				
			updateCentreAndCost();
		}
	}

	/**
	 * Copy constructor
	 * @param solution
	 */
	public EvaluatedSolution( EvaluatedSolution solution){
		this(solution.problem);

		// set customers to clusters
		int nc= getNbCustomers();
		for(int i =0 ; i< nc ; i++){
			int clusterIndx = solution.getClusterIndex(i);
			if(clusterIndx!=-1){	
				CustomerRecord customer = customers[i];
				customer.cluster = clusters[clusterIndx];
				customer.cluster.assignedCustomers.add(customer);
			}
		}
		
		// set cluster centres
		for(int i =0 ; i<clusters.length;i++){
			int customerIndx = solution.getClusterCentre(i);
			if(customerIndx!=-1){
				clusters[i].centre = customers[customerIndx];
			}
		}

		// update all counts etc
		update();
	}
	
	/**
	 * Constructor which does all object allocation
	 * @param problem
	 */
	private EvaluatedSolution(Problem problem){
		// allocate all objects
		this.problem = problem;
		this.customers = new CustomerRecord[problem.getNbLocations()];
		for(int i =0 ; i < customers.length ; i++){
			customers[i] = new CustomerRecord(i);
		}
		
		this.clusters = new ClusterRecord[problem.getNbClusters()];
		for(int i =0 ; i < clusters.length ; i++){
			clusters[i] = new ClusterRecord(i);
		}				
	}

	public EvaluatedSolution(Problem problem, int[]centres){
		this(problem);
		
		if(centres.length!=problem.getNbClusters()){
			throw new RuntimeException();
		}
		
		for(int i =0 ; i < centres.length ; i++){
			if(centres[i]!=-1){
				int customerIndx = centres[i];
				if(customers[customerIndx].cluster!=null){
					throw new RuntimeException();	
				}
				
				// assign customer to cluster and set this as its centre
				CustomerRecord customer = customers[customerIndx];
				customer.cluster = clusters[i];
				customer.cluster.assignedCustomers.add(customer);
				clusters[i].centre = customer;
			}
		}
	
		update();
	}
	


//	private void debugCheck(){
//		update();
//	}
	
//	private static int DEBUG_CALL_NB = 0;
	
	/**
	 * Get the cost of setting the customer to the cluster. If the cluster
	 * index =-1, then customer is unloaded. 
	 * @param customerIndx
	 * @param newClusterIndx
	 * @param out
	 */
	public void evaluateSet(int customerIndx , int newClusterIndx, Cost out){
		assert saveCostChecker();
		
		// set cost to minus the current cost
		out.set(cost);
		out.negate();
		
		// get the customer record
		CustomerRecord customer = customers[customerIndx];
		
		// save original cluster
		int originalClusterIndx = customer.getInternalClusterIndx();

		// set new position
		setCustomerToCluster(customerIndx, newClusterIndx);
		
		// add new cost to the return object
		out.add(cost);
		
		// revert
		setCustomerToCluster(customerIndx, originalClusterIndx);

		assert isCostEqualToChecker();
	}

	/**
	 * Evaluate the swap between 2 customers currently on clusters
	 * @param customerIndx1
	 * @param customerIndx2
	 * @param out
	 */
	public void evaluateSwap(int customerIndx1, int customerIndx2, Cost out){
		assert saveCostChecker();
		
		// set cost to minus the current cost
		out.set(cost);
		out.negate();

		// get current clusters
		int original1 = customers[customerIndx1].getInternalClusterIndx();	
		int original2 = customers[customerIndx2].getInternalClusterIndx();	
		if(original1==original2){
			out.setZero();
			return;
		}
		
		if(original1==-1 || original2==-1){
			throw new RuntimeException();
		}
		
		// swap
		setCustomerToCluster(customerIndx1, original2);
		setCustomerToCluster(customerIndx2, original1);
		
		// add new cost to the return object
		out.add(cost);
		
		// finally revert
		setCustomerToCluster(customerIndx1, original1);
		setCustomerToCluster(customerIndx2, original2);
						
		assert isCostEqualToChecker();
	}


	@Override
	public int getClusterIndex(int customerIndx) {
		return customers[customerIndx].getInternalClusterIndx();
//		if(clusterIndx!=-1){
//			return clusters[clusterIndx].centre.id;
//		}
//		return -1;
	}
	
	public int getClusterSize(int clusterIndx){
		return clusters[clusterIndx].assignedCustomers.size();
	}
	
	public int getCustomer(int clusterIndx, int indx){
		return clusters[clusterIndx].assignedCustomers.get(indx).id;
	}
	
	public void getCustomers(int clusterIndx, TIntArrayList out){
		clusters[clusterIndx].getCustomers(out);
	}

	@Override
	public void setCustomerToCluster(int customerIndx, int cluster) {
		//debugCheck();
		
		// get customer record and original and destination records
		CustomerRecord customer = customers[customerIndx];
		ClusterRecord original = customer.cluster;
		ClusterRecord destination = cluster==-1 ? null : clusters[cluster];
		
		if(original==destination){
			// do nothing
			return;
		}
		
		// check we're not moving a fixed centre to a different cluster
		int fixedClusterIndx = problem.getFixedClusterIndexByLocationIndex(customerIndx);
		if(fixedClusterIndx!=-1 && fixedClusterIndx!=cluster){
			throw new RuntimeException();
		}
		
		// remove costs of involved clusters
		if(original!=null){
			cost.subtract(original.cost);
		}
		if(destination!=null){
			cost.subtract(destination.cost);
		}
		
		// remove if needed
		if(original!=null){
			original.remove(customer);
		}
		
		// insert if needed
		if(destination!=null){
			destination.insert(customer);
		}
		
		// re-add costs of involved clusters
		if(original!=null){
			cost.add(original.cost);
		}
		if(destination!=null){
			cost.add(destination.cost);
		}
		
		//debugCheck();
		
	}
	
	public Cost getCost(){
		return cost;
	}
	
	public Cost getClusterCost(int clusterIndx){
		return clusters[clusterIndx].cost;
	}
	
	public double getClusterQuantity(int clusterIndex){
		return clusters[clusterIndex].quantity;
	}
	
	public int getClusterLocationCount(int clusterIndex){
		return clusters[clusterIndex].assignedCustomers.size();
	}
	
//	public ClusterSummary [] createSummary(Problem problem){
//		ClusterSummary []ret = new ClusterSummary[clusters.length];
//		for(int i =0 ; i<ret.length ; i++){
//			ret[i] = new ClusterSummary();
//			ret[i].setClusterId(problem.getClusterId(i));
//			ret[i].setOvercapacity(clusters[i].cost.getCapacityViolation());
//			ret[i].setTravel(clusters[i].cost.getTravel());
//			ret[i].setQuantity(clusters[i].quantity);
//			ret[i].setCustomers(clusters[i].assignedCustomers.size());
//			//ret[i].setPositionId(getc)
//		}
//		return ret;
//	}
	
//	public Cost getCost(int clusterIndx){
//		return clusters[clusterIndx].cost;
//	}
//
//	public double getQuantity(int clusterIndx){
//		return clusters[]
//	}
	
	@Override
	public int getNbCustomers() {
		return customers.length;
	}

	@Override
	public int getClusterCentre(int clusterIndx) {
		if(clusters[clusterIndx].centre!=null){
			return clusters[clusterIndx].centre.id;
		}
		return -1;
	}

	@Override
	public void setClusterCentre(int clusterIndx, int customerIndx) {
		throw new RuntimeException();
	}

	@Override
	public int getNbClusters() {
		return clusters.length;
	}
	
	

	/**
	 * Update all internal variables accept assignment
	 * of customers to clusters. Cluster centres can change
	 * and hence cost can change.
	 */
	public void update(){
		//assert countImmutableCentres()==0 || saveCostChecker();			
	
		cost.setZero();
		for(ClusterRecord cluster : clusters){
			cluster.updateAll();
			cost.add(cluster.cost);
		}
		
	//	assert countImmutableCentres()==0 ||isCostEqualToChecker();
	}
	
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append(cost.toString() + System.lineSeparator());
		builder.append(System.lineSeparator());
		for(ClusterRecord cluster: clusters){
			builder.append(cluster + System.lineSeparator());
			builder.append("Centre:" + cluster.centre + System.lineSeparator());
			for(CustomerRecord customer : cluster.assignedCustomers){
				builder.append("\t" + customer + System.lineSeparator());
			}
			builder.append(System.lineSeparator());
		}
		return builder.toString();
	}

	public void setAllCentresImmutable(boolean immutable) {
		this.allCentresImmutable = immutable;
	}

	private boolean isImmutableCentre(int clusterIndx){
		return allCentresImmutable || problem.getFixedLocation(clusterIndx)!=-1;
	}
	
	private int countImmutableCentres(){
		int p = problem.getNbClusters();
		if(allCentresImmutable){
			return p;
		}
		int ret=0;
		for(int i=0 ; i<p; i++){
			if(isImmutableCentre(i)){
				ret++;
			}
		}
		return ret;
	}
}
