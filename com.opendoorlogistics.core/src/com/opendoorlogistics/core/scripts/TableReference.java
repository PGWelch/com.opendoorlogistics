/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutionBlackboardImpl;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * Inputs references are of the form"external,clusters" "script.odlx, map"
 * 
 * 1. Use commas to separate values
 * 
 * 2. We only have one set of speech marks around the entire thing (so tables shouldn't have commas)
 * 
 * 3. If something ends is .odlx its another script (also if we have 3 comma separated values)
 * 
 * 4a. One value means a table from the input datastore
 * 
 * 4b. Two values could be datastore, table OR script table (if have .odlx extension)
 * 
 * 4c. Three values must be script, datastore, table
 * 
 * @author Phil
 * 
 */
final public class TableReference {
	private String scriptFilename;
	private String datastoreName;
	private String tableName;

	public TableReference(){
		
	}
	
	public TableReference(String scriptFilename, String datastoreName, String tableName) {
		this.scriptFilename = scriptFilename;
		this.datastoreName = datastoreName;
		this.tableName = tableName;
	}
	
	public TableReference(String datastoreName, String tableName) {
		this(null,datastoreName,tableName);
	}


	/**
	 * Create the reference from the input string or returns null if unidentified
	 * 
	 * @param s
	 * @return
	 */
	public static TableReference create(String s, ExecutionReport result) {
		boolean ok = true;
		TableReference ret = new TableReference();
		ArrayList<String> failureReasons = new ArrayList<>();

		// split by commas
		String[] split = s.split(",");
		if (split.length > 3) {
			failureReasons.add("Found more than two commas in the table reference.");
			ok = false;
		}

		if (split.length == 1) {
			int lastIndex = s.lastIndexOf('.');
			if (lastIndex != -1) {
				if (endsInCorrectExtension(split[0], failureReasons)) {
					ret.scriptFilename = split[0];
				} else {
					ok = false;
				}
			} else {
				// no extension, must be datastore
				ret.tableName = split[0];
			}
			
		} else if (split.length == 2) {
			ret.tableName = split[1];

			// determine if split[0] is datastore or script
			int lastIndex = s.lastIndexOf('.');
			if (lastIndex != -1) {
				if (endsInCorrectExtension(split[0], failureReasons)) {
					ret.scriptFilename = split[0];
				} else {
					ok = false;
				}
			} else {
				// no extension, must be datastore
				ret.datastoreName = split[0];
			}
		} else if (split.length == 3) {
			ret.tableName = split[2];
			ret.datastoreName = split[1];
			ret.scriptFilename = split[0];

			if (!endsInCorrectExtension(ret.scriptFilename, failureReasons)) {
				ok = false;
			}
		}

		if (!ok) {
			for (String failure : failureReasons) {
				result.log(failure);
			}
			if (result != null) {
				result.setFailed("Failed to parse table reference \"" + s + "\"");
			}
			return null;
		}

		// standardise
		if (ret.tableName != null) {
			ret.tableName = Strings.std(ret.tableName);
		}

		if (ret.datastoreName != null) {
			ret.datastoreName = Strings.std(ret.datastoreName);
		}

		if (ret.scriptFilename != null) {
			ret.scriptFilename = Strings.std(ret.scriptFilename);
		}
		return ret;
	}

	/**
	 * Check the input string either (a) has no file extension or (b) ends in the correct one
	 * 
	 * @param s
	 * @param failureReasons
	 * @return
	 */
	private static boolean endsInCorrectExtension(String s, List<String> failureReasons) {
		int lastIndex = s.lastIndexOf('.');
		if (lastIndex != -1) {
			String after = s.substring(lastIndex + 1);
			if (Strings.equalsStd(after, ScriptConstants.FILE_EXT) == false) {
				failureReasons.add("Found script file reference \"" + s + "\" not ending in ." + ScriptConstants.FILE_EXT);
				return false;
			}
		}
		return true;
	}

	public String getScriptFilename() {
		return scriptFilename;
	}

	public String getDatastoreName() {
		return datastoreName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean includeLabels) {
		StringBuilder builder = new StringBuilder();
		if (scriptFilename != null) {
			if(includeLabels){
				builder.append("script=");
			}
			builder.append( scriptFilename );
		}

		if (datastoreName != null) {
			if(builder.length()>0){
				builder.append(",");
			}
			if(includeLabels){
				builder.append("datastore=");
			}
			builder.append( datastoreName );
		}

		if (tableName != null) {
			if(builder.length()>0){
				builder.append(",");
			}
			if(includeLabels){
				builder.append("table=");
			}
			builder.append( tableName );
		}

		return builder.toString().trim();
	}

	public String getDescription(){
		StringBuilder builder = new StringBuilder();
		if (scriptFilename != null) {
			builder.append(" script=\"" + scriptFilename + "\"");
		}

		if (datastoreName != null) {
			builder.append(" datastore=\"" + datastoreName + "\"");
		}

		if (tableName != null) {
			builder.append(" table=\"" + tableName + "\"");
		}

		return builder.toString().trim();	
	}
	
//	public static void main(String[] args) {
//		for (String s : new String[] { "table", "external ,  table", "script.doc, mytable", "script.odlx, mytable", "script.ODLX, mytable",
//				"external, mytable", "script, groupresult, mytable", "script.odlx, groupresult, mytable", "script.doc, groupresult, mytable",
//				"script,odlx, groupresult, mytable", "script.doc" ,"script.odlx",
//				"datastore, table"}) {
//			
//			ScriptExecutionBlackboard result = new ScriptExecutionBlackboard(false);
//			TableReference ref = create(s, result);
//
//			if (ref != null) {
//				System.out.println("\"" + s + "\" -> " + ref.toString(true));
//			} else {
//				System.out.println("\"" + s + "\" -> FAILED");
//				System.out.println(Strings.getTabIndented(result.getReportString(true,true), 1));
//			}
//
//			System.out.println();
//		}
//	}
	
	public interface FetchDatastore{
		ODLDatastore<? extends ODLTable> fetchDatastore(TableReference reference);
	}
}
