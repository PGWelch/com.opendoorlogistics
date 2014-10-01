/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.dialogs;

import java.awt.Window;

import javax.swing.JButton;

import net.sf.jasperreports.engine.type.OrientationEnum;

import com.opendoorlogistics.studio.controls.buttontable.ButtonTableDialog;
import com.opendoorlogistics.utils.ui.Icons;

final public class OrientationSelectorDialog extends ButtonTableDialog{

	static JButton [] createButtons(){
		JButton [] ret = new JButton[2];
		ret[0] = new JButton("Portrait", Icons.loadFromStandardPath("generate portrait report template.png"));
		ret[1] = new JButton("Landscape", Icons.loadFromStandardPath("generate landscape report template.png"));
		return ret;
	}
	
	public OrientationSelectorDialog(Window window){
		super(window, "Select report orientation:", createButtons());
	}
	
	public OrientationEnum getOrientation(){
		switch(getSelectedIndex()){
		case 0:
			return OrientationEnum.PORTRAIT;
			
		case 1:
			return OrientationEnum.LANDSCAPE;
			
		default:
			return null;
		}
	}
}
