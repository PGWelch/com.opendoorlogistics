package com.opendoorlogistics.components.jsprit;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.graphhopper.jsprit.core.algorithm.box.Jsprit.Strategy;

public class AlgorithmConfig implements Serializable {
	private boolean constructionRegret;
	private boolean vehicleSwitch;
	private double radialBestWeight;
	private double radialRegretWeight;
	private double randomBestWeight;
	private double randomRegretWeight;
	private double worstBestWeight;
	private double worstRegretWeight;
	private double clusterBestWeight;
	private double clusterRegretWeight;
	private double fractionFixedVehicleCostUsedDuringInsertion;
	
	public AlgorithmConfig(){
		resetToDefaults();
	}
	
	public boolean isConstructionRegret() {
		return constructionRegret;
	}
	public void setConstructionRegret(boolean constructionRegret) {
		this.constructionRegret = constructionRegret;
	}
	public boolean isVehicleSwitch() {
		return vehicleSwitch;
	}
	public void setVehicleSwitch(boolean vehicleSwitch) {
		this.vehicleSwitch = vehicleSwitch;
	}
	
	@JSpritStrategyWeight(Strategy.RADIAL_BEST)
	public double getRadialBestWeight() {
		return radialBestWeight;
	}
	public void setRadialBestWeight(double radialBestWeight) {
		this.radialBestWeight = radialBestWeight;
	}
	
	@JSpritStrategyWeight(Strategy.RADIAL_REGRET)
	public double getRadialRegretWeight() {
		return radialRegretWeight;
	}
	public void setRadialRegretWeight(double radialRegretWeight) {
		this.radialRegretWeight = radialRegretWeight;
	}
	
	@JSpritStrategyWeight(Strategy.RANDOM_BEST)
	public double getRandomBestWeight() {
		return randomBestWeight;
	}
	public void setRandomBestWeight(double randomBestWeight) {
		this.randomBestWeight = randomBestWeight;
	}
	
	@JSpritStrategyWeight(Strategy.RANDOM_REGRET)
	public double getRandomRegretWeight() {
		return randomRegretWeight;
	}
	public void setRandomRegretWeight(double randomRegretWeight) {
		this.randomRegretWeight = randomRegretWeight;
	}
	
	@JSpritStrategyWeight(Strategy.WORST_BEST)
	public double getWorstBestWeight() {
		return worstBestWeight;
	}
	public void setWorstBestWeight(double worstBestWeight) {
		this.worstBestWeight = worstBestWeight;
	}
	
	@JSpritStrategyWeight(Strategy.WORST_REGRET)
	public double getWorstRegretWeight() {
		return worstRegretWeight;
	}
	public void setWorstRegretWeight(double worstRegretWeight) {
		this.worstRegretWeight = worstRegretWeight;
	}
	
	@JSpritStrategyWeight(Strategy.CLUSTER_BEST)
	public double getClusterBestWeight() {
		return clusterBestWeight;
	}
	public void setClusterBestWeight(double clusterBestWeight) {
		this.clusterBestWeight = clusterBestWeight;
	}
	
	@JSpritStrategyWeight(Strategy.CLUSTER_REGRET)
	public double getClusterRegretWeight() {
		return clusterRegretWeight;
	}
	public void setClusterRegretWeight(double clusterRegretWeight) {
		this.clusterRegretWeight = clusterRegretWeight;
	}
	
	public void resetToDefaults(){
		this.setVehicleSwitch(false);
		this.setConstructionRegret(true);
		this.setFractionFixedVehicleCostUsedDuringInsertion(0.5);
		this.setClusterBestWeight(1);
		this.setClusterRegretWeight(0);
		this.setRadialBestWeight(1);
		this.setRadialRegretWeight(0);
		this.setRandomBestWeight(1);
		this.setRandomRegretWeight(0);
		this.setWorstBestWeight(1);
		this.setWorstRegretWeight(0);
	}
	
	public static AlgorithmConfig createDefaults(){
		AlgorithmConfig ret = new AlgorithmConfig();
		ret.resetToDefaults();
		return ret;
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface JSpritStrategyWeight {
		Strategy value();
	}

	public double getFractionFixedVehicleCostUsedDuringInsertion() {
		return fractionFixedVehicleCostUsedDuringInsertion;
	}

	public void setFractionFixedVehicleCostUsedDuringInsertion(double fractionFixedVehicleCostUsedDuringInsertion) {
		this.fractionFixedVehicleCostUsedDuringInsertion = fractionFixedVehicleCostUsedDuringInsertion;
	}

	
}
