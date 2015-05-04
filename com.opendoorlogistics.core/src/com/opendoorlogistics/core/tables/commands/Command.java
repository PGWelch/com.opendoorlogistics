/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.commands;

import java.awt.image.BufferedImage;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;

public abstract class Command {

	protected final int tableId;

	protected Command(int tableId) {
		super();
		this.tableId = tableId;
		
	}

	/**
	 * Perform the command and return the command that undoes it or null if the command was not done
	 * @param database
	 * @return
	 */
	public abstract Command doCommand(ODLDatastore<? extends ODLTableDefinition> database);

	public int getTableId(){
		return tableId;
	}
	
	public abstract long calculateEstimateSizeBytes();
	
	protected long getEstimatedObjectMemoryFootprintBytes(Object o){
		long ret =8; // for the pointer
		if(o!=null){
			if(String.class.isInstance(o)){
				ret+= ((String)o).length() * 8;
			}
			else if (BufferedImage.class.isInstance(o)){
				BufferedImage img = (BufferedImage)o;
				ret += (long)img.getWidth() * img.getHeight() * 4;
			}
			else if(ODLTime.class.isInstance(o)){
				ret += 12;
			}
			else if (ODLLoadedGeometry.class.isInstance(o)){
				// Only worry about loaded geometry as this is created in the polygon editor.
				// Other sorts of geometry are probably cached elsewhere and hence we just include the pointer
				ret += ((ODLLoadedGeometry)o).getEstimatedSizeInBytes();
			}
			else{
				ret += 8;
			}
		}
		return ret;
	}
	

}
