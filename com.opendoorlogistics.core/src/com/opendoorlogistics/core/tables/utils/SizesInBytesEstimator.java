/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

import java.awt.image.RenderedImage;
import java.time.LocalDate;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;

public class SizesInBytesEstimator {

	public static long estimateBytes(ODLDatastore<? extends ODLTableReadOnly> ds) {
		long ret = 0;
		for(int i=0; i<ds.getTableCount();i++){
			ret += estimateBytes(ds.getTableAt(i));
		}
		return ret;
	}
	
	public static long estimateBytes(ODLTableReadOnly table) {
		long ret = 0;
		int nrows = table.getRowCount();
		int ncols = table.getColumnCount();
		for (int row = 0; row < nrows; row++) {
			for (int col = 0; col < ncols; col++) {
				Object value = table.getValueAt(row, col);
				
				switch (table.getColumnType(col)) {
				case STRING:
					if(value!=null){
						ret += value.toString().length()*2;
					}
					break;

				case DOUBLE:
					ret += 8;
					break;

				case LONG:
					ret += 8;
					break;

				case TIME:
					ret += 8;
					break;

				case COLOUR:
					ret += 4*4;
					break;
					
				case IMAGE:
					RenderedImage img =(RenderedImage)value;
					if(img!=null){
						ret +=(long) img.getWidth() *img.getHeight() *4;
					}
					break;
					
				case GEOM:
					ODLGeom geom = (ODLGeom)value;
					if(geom!=null){
						ret += ((ODLGeomImpl)geom).getEstimatedSizeInBytes();
					}
					break;
					
				case DATE:
					ret += 12;
					break;
					
				default:
					break;
				}
				
				// add something for the pointer
				ret += 8;
			}
			
			// add something for the row holder object
			ret +=20;
		}

		return ret;
	}
}
