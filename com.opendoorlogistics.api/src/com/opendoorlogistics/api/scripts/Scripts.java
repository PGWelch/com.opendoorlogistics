package com.opendoorlogistics.api.scripts;

import java.io.File;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;

/**
 * Class to handle scripts
 * @author Phil
 *
 */
public interface Scripts {
	/**
	 * Load the script. Can throw a runtime exception if something fails.
	 * @param file
	 * @return
	 */
	ScriptOption loadScript(File file);
	
	/**
	 * Uses the name to find and return the option id, for any descendents or the script
	 * (i.e. any number of levels down the hierarchy).
	 * @param option
	 * @return
	 */
	String findOptionIdByName(ScriptOption option, String optionName);
	
	ExecutionReport executeScript( ScriptOption option, String optionId,ODLDatastoreAlterable<? extends ODLTableAlterable> ds);
	
	/**
	 * Get interface for dealing with script parameters
	 * @return
	 */
	Parameters parameters();
}
