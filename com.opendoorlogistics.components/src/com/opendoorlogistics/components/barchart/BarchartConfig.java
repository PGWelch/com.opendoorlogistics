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

import com.opendoorlogistics.components.barchart.basechart.BaseConfig;

@XmlRootElement(name="Barchart")
final public class BarchartConfig extends BaseConfig implements Serializable{
	private List<String> seriesNames = new ArrayList<String>();

	
	public List<String> getSeriesNames() {
		return seriesNames;
	}

	public void setSeriesNames(List<String> seriesNames) {
		this.seriesNames = seriesNames;
	}

	
	
}
