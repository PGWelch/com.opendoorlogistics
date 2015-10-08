/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution;

import java.util.HashSet;
import java.util.Iterator;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.scripts.utils.TableId;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;

public class OptionsSubpath {
	private OptionsSubpath() {
	}

	public static Script getSubpathScript(final Script script, final TableId[] adapterTables, final String[] adapterIds, ExecutionReport report) {
		try {

			// get option ids for the tables
			HashSet<String> ids = new HashSet<>();
			for (TableId tableId : adapterTables) {
				ids.add(ScriptUtils.getOptionIdByAdapterId(script, tableId.getDsId()));
			}

			// and for the adapters
			for (String id : adapterIds) {
				String optionId = ScriptUtils.getOptionIdByAdapterId(script, id);
				if (optionId == null) {
					throw new RuntimeException("Data adapter not found in script: " + id);
				}
				ids.add(optionId);
			}

			// get the subpath
			Processor processor = new Processor(script, ids.toArray(new String[ids.size()]), report);
			processor.validateIds();
			processor.trim();
			
//			// trim any leaf option adapters not needed BEFORE collapsing the script
//			ScriptUtils.visitOptions(processor.script, new OptionVisitor() {
//
//				@Override
//				public void visitOption(Option parent, Option option) {
//					if (option.getOptions().size() > 0) {
//						// non-leaf ... must keep
//						return;
//					}
//
//					// loop over all adapters
//					Iterator<AdapterConfig> itAdapt = option.getAdapters().iterator();
//					while (itAdapt.hasNext()) {
//						boolean keepAdapter = false;
//
//						// is this adapter included in our list of adapters?
//						AdapterConfig currentAdapter = itAdapt.next();
//						for (AdapterConfig included : adapters) {
//							if (Strings.equalsStd(currentAdapter.getId(), included.getId())) {
//								keepAdapter = true;
//								break;
//							}
//						}
//
//						// should we keep some tables in the adapter?
//						if (!keepAdapter) {
//
//							// Get the individual tables used actually within this adapter.
//							// Remove any tables not needed in the adapter.
//							List<AdaptedTableConfig> tables = tablesByAdapterId.get(currentAdapter.getId());
//							if (tables != null) {
//								Iterator<AdaptedTableConfig> itTable = currentAdapter.getTables().iterator();
//								boolean foundTable = false;
//								while (itTable.hasNext()) {
//									AdaptedTableConfig currentTable = itTable.next();
//									for (AdaptedTableConfig other : tables) {
//										if (Strings.equalsStd(currentTable.getName(), other.getName())) {
//											foundTable = true;
//										}
//									}
//
//									if (!foundTable) {
//										itTable.remove();
//									}
//								}
//							} else {
//								currentAdapter.getTables().clear();
//							}
//
//							keepAdapter = currentAdapter.getTables().size() > 0;
//						}
//
//						// remove adapter if unneeded
//						if (!keepAdapter) {
//							itAdapt.remove();
//						}
//					}
//				}
//
//			});

			if(processor.finish()){
				return processor.script;
			}
			return null;

		} catch (Exception e) {
			report.setFailed("Failed to generate script including only adapted tables needed.");
			return null;
		}
	}

	/**
	 * For the input option ids or instruction ids, generate a script which executes only them by trimming the other options and placing all remaining
	 * options at the root level. If ids are null then everything is put on a single level The synchronisation of the script is always worked out.
	 * 
	 * If this method fails then the execution report is set to failed status. No exceptions are thrown.
	 * 
	 * @param script
	 * @param optionIds
	 * @return
	 */
	public static Script getSubpathScript(Script script, final String[] optionIds, ExecutionReport report) {
		try {
			Processor processor = new Processor(script, optionIds, report);
			if(!processor.processAll()){
				return null;
			}
			return processor.script;			
		} catch (Exception e) {
			if(report!=null){
				report.setFailed(e);
			}
			return null;
		}
	}

//	private static class SyncCounter {
//		int sync = 0;
//		int noSync = 0;
//
//		SyncCounter(Script script, final String[] optionIds) {
//			ScriptUtils.visitOptions(script, new OptionVisitor() {
//
//				@Override
//				public void visitOption(Option parent, Option option) {
//					// only examine leaf nodes; only these are counted as 'runnable'
//					if (option.getOptions().size() > 0) {
//						return;
//					}
//
//					// is this option include in the id list? (null list means include all)
//					boolean included = false;
//					if (optionIds == null) {
//						included = true;
//					} else {
//						for (String optionId : optionIds) {
//							if (Strings.equalsStd(optionId, option.getOptionId())) {
//								included = true;
//								break;
//							}
//						}
//					}
//					if (!included) {
//						return;
//					}
//
//					// check user was allowed to choose sync or not sync for this option
//					ArrayList<Option> tmpPath = new ArrayList<>();
//					tmpPath.add(option);
//					OutputWindowSyncLevel sync = ScriptUtils.getOutputWindowSyncLevel(tmpPath);
//					if (sync == OutputWindowSyncLevel.ERROR) {
//						throw new RuntimeException("Option with id " + option.getOptionId() + " in the script is corrupt.");
//					} else if (sync != OutputWindowSyncLevel.MANUAL) {
//						return;
//					}
//
//					// count it
//					count(option);
//				}
//
//			});
//		}
//
//		private void count(Option option) {
//			if (option.isSynchronised()) {
//				sync++;
//			} else {
//				noSync++;
//			}
//		}
//	}

	private static class Processor{
		StandardisedStringSet knownOptionIds = new StandardisedStringSet(false);	
		StandardisedStringSet knownInstructionIds = new StandardisedStringSet(false);
		final Script script;
		final String []optionIds;
		final ExecutionReport report;
		
		Processor(Script script, final String[] optionIds, ExecutionReport report){
			// take a deep copy of the input script
			this.script = ScriptIO.instance().deepCopy(script);
			this.optionIds = optionIds;
			this.report = report;	

		}
		
		void validateIds(){
			// get and validate ids in the script (TO DO ... include adapters etc...)
			knownOptionIds = new StandardisedStringSet(false);
			validateOptionIds(script, knownOptionIds, true);

			knownInstructionIds = new StandardisedStringSet(false);
			validateInstructionIds(script, knownInstructionIds);

			// Check all options exist
			if (optionIds != null) {
				for (String id : optionIds) {
					if (knownOptionIds.contains(id) == false) {
						throw new RuntimeException("Unknown script option id: " + id);
					}
				}
			}
		
		}
		
		void trim(){
			// now get the trimmed script... if options is null we take the whole script...
			if (optionIds != null) {

				// now trim ... i.e. remove all unused options
				OptionsSubpath.trim(script, optionIds);
			}		
		}
		
		boolean finish(){

			// now collapse all options to one level
			collapseScriptToSingleOption(script, script);

//			if(!setSyncLevel(script, counter, report)){
//				return false;
//			}
			
			return true;
		}
		
		boolean processAll(){
			validateIds();
			trim();
			return finish();
		}
		
	}
	
//	/**
//	 * @param collapsedScript
//	 * @param counter
//	 * @param report
//	 */
//	private static boolean setSyncLevel(Script collapsedScript, SyncCounter counter, ExecutionReport report) {
//		// check the sync level
//		OutputWindowSyncLevel syncLevel = ScriptUtils.getOutputWindowSyncLevel(collapsedScript, collapsedScript.getOptionId());
//		switch (syncLevel) {
//		case ERROR:
//			report.setFailed("The script is corrupt. It may contain incompatible components - one which can only be run synchronised and another unsynchronised.");
//			return false;
//
//		case ALWAYS:
//			collapsedScript.setSynchronised(true);
//			break;
//
//		case NEVER:
//			collapsedScript.setSynchronised(false);
//			break;
//
//		case MANUAL:
//			// only sync if all set to sync...
//			collapsedScript.setSynchronised(counter.sync > 0 && counter.noSync == 0);
//			break;
//		}
//		return true;
//	}

	private static void collapseScriptToSingleOption(Script root, Option option) {
		if (option != root) {
			// add everything from this level
			root.getAdapters().addAll(option.getAdapters());
			root.getComponentConfigs().addAll(option.getComponentConfigs());
			root.getInstructions().addAll(option.getInstructions());
			root.getOutputs().addAll(option.getOutputs());
		}

		// add all its children, removing them as we go
		Iterator<Option> it = option.getOptions().iterator();
		while (it.hasNext()) {
			collapseScriptToSingleOption(root, it.next());
			it.remove();
		}

	}

	private static boolean containsId(Option option, String id) {

		if (Strings.equalsStd(option.getOptionId(), id)) {
			return true;
		}

		for (Option child : option.getOptions()) {
			if (containsId(child, id)) {
				return true;
			}
		}
		return false;

	}

	private static void trim(Option option, String[] ids) {
		Iterator<Option> it = option.getOptions().iterator();
		while (it.hasNext()) {
			Option child = it.next();

			// trim child first
			trim(child, ids);

			// then check whether to keep this node
			boolean keep = false;
			for (String id : ids) {
				if (containsId(child, id)) {
					keep = true;
					break;
				}
			}
			if (!keep) {
				it.remove();
			}
		}

	}

	private static void validateInstructionIds(Option option, StandardisedStringSet knownIds) {
		for (InstructionConfig instruction : option.getInstructions()) {
			String id = instruction.getUuid();

			if (Strings.isEmpty(id)) {
				throw new RuntimeException("Corrupt script - empty instruction id.");
			}

			if (knownIds.contains(id)) {
				throw new RuntimeException("Corrupt script - duplication option-id: " + id);
			}
			knownIds.add(id);

		}

		for (Option child : option.getOptions()) {
			validateInstructionIds(child, knownIds);
		}
	}

	private static void validateOptionIds(Option option, StandardisedStringSet knownIds, boolean emptyAllowed) {
		if (Strings.isEmpty(option.getOptionId()) && emptyAllowed == false) {
			throw new RuntimeException("Corrupt script - empty option id.");
		}

		if (knownIds.contains(option.getOptionId())) {
			throw new RuntimeException("Corrupt script - duplication option-id: " + option.getOptionId());
		}
		knownIds.add(option.getOptionId());
		for (Option child : option.getOptions()) {
			validateOptionIds(child, knownIds, false);
		}
	}

}
