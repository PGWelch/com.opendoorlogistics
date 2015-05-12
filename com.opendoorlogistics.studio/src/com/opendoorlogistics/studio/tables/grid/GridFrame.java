/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;

public class GridFrame extends ODLInternalFrame implements Disposable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final GridTable table;
	
	public GridFrame() {
		this(null);
	}

	/**
	 * Create the frame.
	 */
	public GridFrame(GridTable gridTable) {
		super(gridTable.getTableName());
		this.table = gridTable;
		GridTable.addToContainer(gridTable, this);
		setTitle(gridTable.getTableName());
	}

	@Override
	public void dispose(){
		super.dispose();
		table.dispose();
		
	}
	
	public String getTableName(){
		return table.getTableName();
	}
	
}
