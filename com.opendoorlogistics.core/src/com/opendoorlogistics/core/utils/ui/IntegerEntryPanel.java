/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;



final public class IntegerEntryPanel extends TextEntryPanel{
	private static TextChangedListener createTextChangedListener(final IntChangedListener listener){
		if(listener!=null){
			return new TextChangedListener() {
				
				@Override
				public void textChange(String newText) {
					try {
						int val = Integer.parseInt(newText);
						listener.intChange(val);
					} catch (Throwable e) {
//						textField.setText("0");
//						intChangedListener.intChange(0);
					}
				}
			};
		}
		return null;
	}

	
	public IntegerEntryPanel(String label, int initialValue, String tooltip,final IntChangedListener intChangedListener) {
		super(label,Integer.toString( initialValue), tooltip, EntryType.String,createTextChangedListener(intChangedListener));

		// integer box doesn't need to be wide...
		setPreferredTextboxWidth(100);
	}
	
	

}
