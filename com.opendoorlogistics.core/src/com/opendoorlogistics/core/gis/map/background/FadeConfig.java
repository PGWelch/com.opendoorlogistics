package com.opendoorlogistics.core.gis.map.background;

import java.awt.Color;

public class FadeConfig {
	private Color colour;
	private double greyscale;
	
	public FadeConfig(Color colour, double greyscale) {
		this.colour = colour;
		this.greyscale = greyscale;
	}
	
	public FadeConfig(){
		
	}
	
	public FadeConfig(FadeConfig fc){
		this.colour = fc.getColour();
		this.greyscale = fc.getGreyscale();
	}
	
	public Color getColour() {
		return colour;
	}
	public void setColour(Color colour) {
		this.colour = colour;
	}
	public double getGreyscale() {
		return greyscale;
	}
	public void setGreyscale(double greyscale) {
		this.greyscale = greyscale;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((colour == null) ? 0 : colour.hashCode());
		long temp;
		temp = Double.doubleToLongBits(greyscale);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		FadeConfig other = (FadeConfig) obj;
		if (colour == null) {
			if (other.colour != null)
				return false;
		} else if (!colour.equals(other.colour))
			return false;
		if (Double.doubleToLongBits(greyscale) != Double.doubleToLongBits(other.greyscale))
			return false;
		return true;
	}
	
}
