/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

abstract class AbstractDistancesConfigBox extends OkCancelDialog{
	protected VerticalLayoutPanel panel;
	protected final long flags;
	
	public AbstractDistancesConfigBox(Window owner,String title, long flags){
		super(owner);
		this.flags = flags;
		setTitle(title);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setMinimumSize(new Dimension(400, 100));
	}
	
	@Override
	protected Component createMainComponent(boolean inWindowsBuilder) {
		panel = new VerticalLayoutPanel();
		return panel;
	}

//	protected static class HorizontalPanel extends JPanel{
//		public HorizontalPanel(Component ...components) {
//			setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
//			for(Component comp:components){
//				add(comp);
//			}
//		}
//	}

	protected boolean hasFlag(long flag){
		return (flags & flag)==flag;
	}
}
