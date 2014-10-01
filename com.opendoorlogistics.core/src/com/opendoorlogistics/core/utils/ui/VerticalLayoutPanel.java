/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * A panel that lays components out vertically with 
 * left alignment and stretching horizontally
 * @author Phil
 *
 */
public class VerticalLayoutPanel extends JPanel{
	private static final int DEFAULT_WHITESPACE=10;
	
	public VerticalLayoutPanel(){
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}
	
	@Override
	public Component add(Component component){
		return addWrapped(component, BorderLayout.NORTH);
	}
	
	public Component addWrapped(Component component, String borderLayoutPosition){
		return super.add(wrap(component,borderLayoutPosition));
	}
	
	public Component addNoWrap(Component component){
		return super.add(component);
	}

	public Component addBorderLayoutCentre(Component component ){
		return super.add(wrap(component, BorderLayout.CENTER));
	}
	
	/**
	 * Add a line of components with an indent
	 * @param indentWidth
	 * @param components
	 */
	public void addIndentedLine(int indentWidth, Component... components  ){
		Component [] arr = new Component[components.length+1];
		arr[0]=Box.createRigidArea(new Dimension(indentWidth, 1));
		System.arraycopy(components, 0, arr, 1, components.length);
		addLine(arr);
	}
	
	public Component addLine(Component ...components){
		return add(LayoutUtils.createHorizontalBoxLayout(components));		
	}

	public void addLine(String borderLayoutPosition,Component ...components){
		add(LayoutUtils.createHorizontalBoxLayout(components),borderLayoutPosition);		
	}

	
	public Component addLineNoWrap(Component ...components){
		return addNoWrap(LayoutUtils.createHorizontalBoxLayout(components));		
	}
	
	public void addWhitespace(int height){
		super.add(Box.createRigidArea(new Dimension(0,height)));
	}

	public void addHalfWhitespace(){
		addWhitespace(DEFAULT_WHITESPACE/2);
	}
	
	public void addWhitespace(){
		addWhitespace(DEFAULT_WHITESPACE);
	}
	
	public static interface CheckChangedListener {
		void checkChanged(boolean isChecked);
	}
	
	public JCheckBox addCheckBox(String text, boolean isChecked,final CheckChangedListener listener){
		final JCheckBox ret = new JCheckBox(text);
		ret.setSelected(isChecked);
		if(listener!=null){
			ret.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					listener.checkChanged(ret.isSelected());
				}
			});		
		}

		add(ret);
		return ret;
	}
	
	private static JPanel wrap(Component component, String borderLayoutPosition){
		JPanel ret = new JPanel();
		ret.setLayout(new BorderLayout());
		ret.add(component, borderLayoutPosition);
		int height = component.getPreferredSize().height;
		ret.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		return ret;
	}
	

}
