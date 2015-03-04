/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.kmeans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

@XmlRootElement
final public class KMeansConfig implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int k=10;
	private int randomSeed=1;

	public int getK() {
		return k;
	}

	@XmlAttribute
	public void setK(int k) {
		this.k = k;
	}

	public int getRandomSeed() {
		return randomSeed;
	}

	@XmlAttribute
	public void setRandomSeed(int randomSeed) {
		this.randomSeed = randomSeed;
	}
	
	public static JPanel createConfigEditorPanel(final KMeansConfig config) {
		VerticalLayoutPanel panel = new VerticalLayoutPanel();

		panel.add(new JLabel("Number of clusters k:"));
		final JFormattedTextField kField = new JFormattedTextField();
		kField.setValue(new Integer(config.getK()));
		kField.setColumns(10);
		kField.addPropertyChangeListener("value", new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
			     int k = ((Number)kField.getValue()).intValue();
			     config.setK(k);
			}
		});
		panel.add(kField);
		panel.addWhitespace();
		
		panel.add(new JLabel("Random seed:"));
		final JFormattedTextField seedField = new JFormattedTextField();
		seedField.setValue(new Integer(config.getRandomSeed()));
		seedField.setColumns(10);
		seedField.addPropertyChangeListener("value", new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
			     int seed = ((Number)seedField.getValue()).intValue();
			     config.setRandomSeed(seed);
			}
		});
		panel.add(seedField);
		panel.addWhitespace();
		
		return panel;
	}

}
