package com.opendoorlogistics.core.gis.map.background;

import java.awt.Color;

public class FadeConfig {
	Color colour;
	double greyscale;
	
	public FadeConfig(Color colour, double greyscale) {
		this.colour = colour;
		this.greyscale = greyscale;
	}
	
	public FadeConfig(){
		
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
	
}
