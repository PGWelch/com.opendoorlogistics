/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables;

import javax.swing.table.TableModel;

final public class TableModelUtils {
	public static void getMultiLineColumnNames(String[] ret){
		// count max number of lines
		int maxNbLines=1;
		for(int i =0 ;i< ret.length ; i++){
			String s= ret[i];
			
			// ensure number of spaces should equal numbers of words-1
			s= s.trim();
			s = s.replaceAll("  "," ");
			ret[i] = s;
			
			// split 
			maxNbLines = Math.max( ret[i].split(" ").length, maxNbLines);
		}
		
		// split each one
		for(int i =0 ;i< ret.length ; i++){
			String [] split =  ret[i].split(" ");
			StringBuilder builder = new StringBuilder();
			builder.append("<html>");
			for(int line =0 ; line < maxNbLines ; line++){
				if(line>0){
					builder.append("<br>");
				}
				if(line < split.length){
					builder.append(split[line]);
				}
				ret[i] = builder.toString();
			}
		}
		
	}
	
	/**
	 * Get column names properly formatted for multi-line.
	 * See http://www.javarichclient.com/multiline-column-header/ for details
	 * @param model
	 * @return
	 */
	public static String[] getMultiLineColumnNames(TableModel model){
		String [] ret = new String[model.getColumnCount()];
		
		for(int i =0 ;i< ret.length ; i++){
			ret[i] = model.getColumnName(i); 
		}

		getMultiLineColumnNames(ret);
		
		return ret;
	}
}
