package com.opendoorlogistics.components.barchart.basechart;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Chart")
public class BaseConfig implements Serializable{
	private int nbFilterGroupLevels;
	private String yLabel = "Value";
	private String xLabel = "Category";
	private String title = "";
	
	public int getNbFilterGroupLevels() {
		return nbFilterGroupLevels;
	}

	public void setNbFilterGroupLevels(int nbFilterGroupLevels) {
		this.nbFilterGroupLevels = nbFilterGroupLevels;
	}
	

	public String getYLabel() {
		return yLabel;
	}

	public void setYLabel(String ylabel) {
		this.yLabel = ylabel;
	}

	public String getXLabel() {
		return xLabel;
	}

	public void setXLabel(String xlabel) {
		this.xLabel = xlabel;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	
}
