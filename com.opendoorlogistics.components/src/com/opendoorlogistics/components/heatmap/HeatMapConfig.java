package com.opendoorlogistics.components.heatmap;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HeatMapConfig implements Serializable {
	private String EPSG;
	private boolean simplify = true;
	private double pointRadius = 0.1;
	private int nbContourLevels = 25;
	private int resolution = 100;
	
	public String getEPSG() {
		return EPSG;
	}
	
	@XmlAttribute
	public void setEPSG(String ePSG) {
		EPSG = ePSG;
	}
	
	public boolean isSimplify() {
		return simplify;
	}
	
	@XmlAttribute
	public void setSimplify(boolean simplify) {
		this.simplify = simplify;
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
