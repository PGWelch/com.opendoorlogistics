/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.execution.OptionsSubpath;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;

public class ExecutionUtils {

	public static void showScriptFailureBox(JFrame parent,boolean compiling, String name, ExecutionReport result) {
		result = result.deepCopy();
		result.log("Could not complete operation \"" + name + "\".");
		// String message = result.getReportString(true,true);
		// if(Strings.isEmpty(message)==false){
		// message += System.lineSeparator() + "Could not complete operation \"" + name + "\".";
		// }

		ExecutionReportDialog dlg = new ExecutionReportDialog(parent, compiling ? "Compilation problem" : "Script execution problem", result, true);
		dlg.setLocationRelativeTo(parent);
		dlg.setSize(600, 300);
		dlg.setVisible(true);
	}

	/**
	 * Filters the script for the input options ids or just returns the root option on its own if no option ids provided.
	 * 
	 * @param script
	 * @param optionIds
	 * @return
	 */
	static Script getFilteredCollapsedScript(JFrame parent,Script script, String[] optionIds, String name) {

		ExecutionReportImpl report = new ExecutionReportImpl();
		Script ret = OptionsSubpath.getSubpathScript(script, optionIds, report);
		
		// uuids need to match so last parameter values are saved
		ret.setUuid(script.getUuid());
		
		if (report.isFailed() == false) {
			return ret;
		}

		report.setFailed("The script is corrupt and cannot be run.");
		ExecutionUtils.showScriptFailureBox(parent,false, name, report);
		return null;
	}

	/**
	 * Wrap the datastore to give it the correct edit / not edit flags
	 * 
	 * @param isEditable
	 * @return
	 */
	static ODLDatastoreAlterable<ODLTableAlterable> wrapDsWithEditableFlags(ODLDatastoreUndoable<ODLTableAlterable> ds) {
		return new SimpleDecorator<ODLTableAlterable>(ODLTableAlterable.class, ds) {
			@Override
			public long getFlags(int tableId) {

				long flags = super.getFlags(tableId);

				// hack .. edit permissions can sometimes be turned off by accident if we copy a table
				// to the main datastore etc.. This ensures we always have them for the external.
				flags |= TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS;

				return flags;
			}
		};

	}

	static void throwIfNotOnEDT() {
		// must be on EDT
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException();
		}
	}

	static void throwIfEDT() {
		// must be not be on EDT
		if (SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException();
		}
	}
}
