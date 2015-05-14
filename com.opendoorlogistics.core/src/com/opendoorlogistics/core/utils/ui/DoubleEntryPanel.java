/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;



final public class DoubleEntryPanel extends TextEntryPanel{
	private static TextChangedListener createTextChangedListener(final DoubleChangedListener listener){
		if(listener!=null){
			return new TextChangedListener() {
				
				@Override
				public void textChange(String newText) {
					try {
						double val = Double.parseDouble(newText);
						listener.doubleChange(val);
					} catch (Throwable e) {
//						textField.setText("0");
//						intChangedListener.intChange(0);
					}
				}
			};
		}
		return null;
	}
	
	public DoubleEntryPanel(String label, double initialValue, String tooltip,final DoubleChangedListener doubleChangedListener) {
		super(label,Double.toString( initialValue), tooltip, EntryType.DoubleNumber,createTextChangedListener(doubleChangedListener));
			
		setPreferredTextboxWidth(100);
	}
		
//	public static interface DoubleChangedListener {
//		void doubleChange(double newDbl);
//	}
//	

}
