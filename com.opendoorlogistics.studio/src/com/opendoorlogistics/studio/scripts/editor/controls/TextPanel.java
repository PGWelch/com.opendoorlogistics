/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.controls;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

final public class TextPanel extends JPanel{
	private final JTextPane text = new JTextPane();

	public TextPanel(){
		setLayout(new BorderLayout());	
		
		text.setText("XML");
		JScrollPane areaScrollPane = new JScrollPane(text);
		add(areaScrollPane,BorderLayout.CENTER);	
	}
	
	public void setText(String s){
		text.setText(s);
	}
	
	public void setEditable(boolean editable){
		text.setEditable(editable);
	}
}
