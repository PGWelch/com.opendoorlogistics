/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.distances;

import java.io.Serializable;

public final class DistancesConfiguration implements Serializable {
	private CalculationMethod method = CalculationMethod.GREAT_CIRCLE;
	private DistancesOutputConfiguration outputConfig = new DistancesOutputConfiguration();
	private GreatCircleConfiguration greatCircleConfig = new GreatCircleConfiguration();
	private GraphhopperConfiguration graphhopperConfig = new GraphhopperConfiguration();
	private ExternalMatrixFileConfiguration externalConfig = new ExternalMatrixFileConfiguration();
	public enum CalculationMethod {
		GREAT_CIRCLE("Crow fly distance (along the surface of the Earth) from one lat/long pair to another"), 
		ROAD_NETWORK("Use a road network, calculated using the Graphhopper library"),
		EXTERNAL_MATRIX("Use an external text file contain a matrix of distance and time values");

		private final String description;

		private CalculationMethod(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
	
	public DistancesConfiguration deepCopy(){
		DistancesConfiguration ret = new DistancesConfiguration();
		ret.method = method;
		ret.outputConfig = outputConfig.deepCopy();
		if(greatCircleConfig!=null){
			ret.greatCircleConfig = greatCircleConfig.deepCopy();
		}
		if(graphhopperConfig!=null){
			ret.graphhopperConfig = graphhopperConfig.deepCopy();
		}
		
		if(externalConfig!=null){
			ret.externalConfig = externalConfig.deepCopy();
		}
		return ret;
	}

	public CalculationMethod getMethod() {
		return method;
	}

	public void setMethod(CalculationMethod method) {
		this.method = method;
	}

	public DistancesOutputConfiguration getOutputConfig() {
		return outputConfig;
	}

	public void setOutputConfig(DistancesOutputConfiguration outputConfig) {
		this.outputConfig = outputConfig;
	}

	public GreatCircleConfiguration getGreatCircleConfig() {
		return greatCircleConfig;
	}

	public void setGreatCircleConfig(GreatCircleConfiguration config) {
		this.greatCircleConfig = config;

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + method.hashCode();
		switch(method){
		case GREAT_CIRCLE:
			result = prime * result + greatCircleConfig.hashCode();
			break;
			
		case ROAD_NETWORK:
			result = prime * result + graphhopperConfig.hashCode();
			break;
			
		case EXTERNAL_MATRIX:
			result = prime * result + externalConfig.hashCode();
			break;
			
		default:
			throw new RuntimeException();
		}
		result = prime * result + outputConfig.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DistancesConfiguration other = (DistancesConfiguration) obj;

		if(other.method!= method){
			return false;
		}
		
		if(other.outputConfig.equals(outputConfig)==false){
			return false;
		}
		
		switch(method){
		case GREAT_CIRCLE:
			if(other.greatCircleConfig.equals(greatCircleConfig)==false){
				return false;
			}
			break;
			
		case ROAD_NETWORK:
			if(other.graphhopperConfig.equals(graphhopperConfig)==false){
				return false;
			}
			break;
			
		case EXTERNAL_MATRIX:
			if(!other.externalConfig.equals(externalConfig)){
				return false;
			}
			break;
		default:
			throw new RuntimeException();
		}
		
		return true;
	}

	public GraphhopperConfiguration getGraphhopperConfig() {
		return graphhopperConfig;
	}

	public void setGraphhopperConfig(GraphhopperConfiguration graphhopperConfig) {
		this.graphhopperConfig = graphhopperConfig;
	}

	public ExternalMatrixFileConfiguration getExternalConfig() {
		return externalConfig;
	}

	public void setExternalConfig(ExternalMatrixFileConfiguration externalConfig) {
		this.externalConfig = externalConfig;
	}
	
	
}
