package com.opendoorlogistics.components.heatmap;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HeatMapConfig implements Serializable {
	private String EPSG;
	private double pointRadius = 0.2;
	private int nbContourLevels = 15;
	private int resolution = 500;
	
	public String getEPSG() {
		return EPSG;
	}
	
	@XmlAttribute
	public void setEPSG(String ePSG) {
		EPSG = ePSG;
	}
	
	
	public double getPointRadius() {
		return pointRadius;
	}
	
	@XmlAttribute
	public void setPointRadius(double pointRadius) {
		this.pointRadius = pointRadius;
	}
	
	public int getNbContourLevels() {
		return nbContourLevels;
	}
	
	@XmlAttribute
	public void setNbContourLevels(int nbContourLevels) {
		this.nbContourLevels = nbContourLevels;
	}
	
	public int getResolution() {
		return resolution;
	}
	
	@XmlAttribute
	public void setResolution(int resolution) {
		this.resolution = resolution;
	}
	
	
	
}
