/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.controls;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.core.components.ODLComponentDecorator;
import com.opendoorlogistics.core.components.ODLGlobalComponents;

final public class SelectComponentIDList extends JPanel{
	private final JComboBox <MyDecorator>comboBox;

	private class MyDecorator extends ODLComponentDecorator{

		public MyDecorator(ODLComponent decorated) {
			super(decorated);
		}
		
		@Override
		public String toString(){
			return getName() + " (" + getId() + ")";
		}
		
		ODLComponent getDecorated(){
			return decorated;
		}
	}
	
	public SelectComponentIDList(){
		this(null);
	}
	
	public SelectComponentIDList(String selected){
		ArrayList<MyDecorator> decs = new ArrayList<>();
		for(ODLComponent component: ODLGlobalComponents.getProvider()){
			decs.add(new MyDecorator(component));
		}
		
		// sort alphabetically
		Collections.sort(decs, new Comparator<MyDecorator>() {

			@Override
			public int compare(MyDecorator o1, MyDecorator o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		
		MyDecorator [] decsArray = decs.toArray(new MyDecorator[decs.size()]);
		comboBox = new JComboBox <>(decsArray);
		setLayout(new BorderLayout());
        add(comboBox);
        
        for(int i =0 ; i < comboBox.getItemCount() && selected!=null; i++){
        	if(comboBox.getItemAt(i).getId().equals(selected)){
        		comboBox.setSelectedIndex(i);
        	}
        }
	}

	public ODLComponent getSelected(){
		return ((MyDecorator)comboBox.getSelectedItem()).getDecorated();
	}
	
	public void addListSelectionListener(ActionListener listener){
		comboBox.addActionListener(listener);
	}
}
