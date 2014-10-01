/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.barchart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Barchart")
final public class BarchartConfig implements Serializable{
	private List<String> seriesNames = new ArrayList<String>();
	private String yLabel = "Value";
	private String xLabel = "Category";
	private String title = "";
	private int nbFilterGroupLevels;
	
	public List<String> getSeriesNames() {
		return seriesNames;
	}

	public void setSeriesNames(List<String> seriesNames) {
		this.seriesNames = seriesNames;
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

	public int getNbFilterGroupLevels() {
		return nbFilterGroupLevels;
	}

	public void setNbFilterGroupLevels(int nbFilterGroupLevels) {
		this.nbFilterGroupLevels = nbFilterGroupLevels;
	}

//	public int getSeriesCount() {
//		return seriesCount;
//	}
//
//	@XmlAttribute(name="SeriesCount")
//	public void setSeriesCount(int seriesCount) {
//		this.seriesCount = seriesCount;
//	}

	
	
}
