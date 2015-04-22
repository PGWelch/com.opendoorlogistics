/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.tables;

import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.studio.AppFrame;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;

final public class EditableTableComponent extends QueryReadOnlyTableComponent{
	private final AppFrame appFrame;

	public EditableTableComponent(AppFrame appFrame) {
		this.appFrame = appFrame;
	}

//	@Override
//	protected HasInternalFrames getOwner() {
//		return appFrame;
//	}

	@Override
	protected ODLDatastoreUndoable<ODLTableAlterable> getGlobalDatastore() {
		return appFrame.getLoaded().getDs();
	}


//	@Override
//	public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
//		return Arrays.asList(new ODLWizardTemplateConfig("Show table view", "Show table view",
//				"Show view of the table which can include filtering, calculated fields etc.", null));
//	}



}
