/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.Icon;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptEditorType;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.VLSBuilder;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.strings.Strings.DoesStringExist;

/**
 * Methods which parse the script but are tolerant if the script contains errors (unlike actual compilation).
 * 
 * @author Phil
 * 
 */
final public class ScriptUtils {
	private ScriptUtils() {
	}

	public static void deleteOption(Option root, final Option optionToDelete) {
		visitOptions(root, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				for (Option child : option.getOptions()) {
					if (child == optionToDelete) {
						option.getOptions().remove(optionToDelete);
						break;
					}
				}
				return true;
			}
		});
	}

	public static boolean hasComponentFlag(final ODLApi api, Option root, final long searchForFlags) {
		Boolean result = getScriptElement(new FindScriptElement<Boolean>() {

			@Override
			public Boolean find(Option option) {
				for (InstructionConfig instructionConfig : option.getInstructions()) {
					ODLComponent component = getComponent(instructionConfig);
					if (component == null) {
						throw new RuntimeException("Unknown component referenced in script: " + instructionConfig.getComponent());
					}
					long flags = component.getFlags(api, instructionConfig.getExecutionMode());
					if ((flags & searchForFlags) == searchForFlags) {
						return true;
					}
				}
				return null;
			}
		}, root);
		if (result != null) {
			return result;
		}
		return false;
	}

	// public static List<TableName> getTableNames(Iterable<TableFieldName> tablefields, boolean isReferencableDatastore){
	// TreeMap<Integer, TableName> tmp = new TreeMap<>();
	// for(TableFieldName o : tablefields){
	// if(isReferencableDatastore && Strings.isEmpty(o.getDatastore())){
	// continue;
	// }
	// if(tmp.containsKey(o.getTableIndx())==false){
	// tmp.put(o.getTableIndx(), o);
	// }
	// }
	// return new ArrayList<TableName>(tmp.values());
	// }

	// public static List<String> getFieldNames(Iterable<TableFieldName> tablefields, String datastore, String table){
	// TreeSet<String> ret = new TreeSet<>();
	// for(TableFieldName o : tablefields){
	// if(Strings.equalsStandardised(datastore, o.getDatastore()) && Strings.equalsStandardised(table, o.getTableName())){
	// if(Strings.isEmpty(o.fieldname)){
	// ret.add(o.fieldname);
	// }
	// }
	// }
	// return new ArrayList<String>(ret);
	// }

	// public static class AbstractGenerateListingCallback implements GenerateListingCallback {
	//
	// @Override
	// public boolean includeExternalDatastore() {
	// return true;
	// }
	//
	// @Override
	// public boolean includeInstructionInput(ComponentConfig config) {
	// return true;
	// }
	//
	// @Override
	// public boolean includeInstructionOutput(ComponentConfig config) {
	// return true;
	// }
	//
	// @Override
	// public boolean includeAdapter(AdapterConfig config) {
	// return true;
	// }
	//
	// }

	// public static List<TableFieldName> generateTableFieldListing(Script script, boolean includeInstructionInputs,
	// ODLDatastore<? extends ODLTableDefinition> external) {
	// return generateTableFieldListing(script, includeInstructionInputs, external, null);
	// }

	public static ODLDatastore<? extends ODLTableDefinition> getIODatastoreDfn(ODLApi api, Option root, InstructionConfig instruction) {
		return getIOOrOutputDatastore(api, root, instruction, true);
	}

	public static ODLDatastore<? extends ODLTableDefinition> getOutputDatastoreDfn(ODLApi api, Option root, InstructionConfig instruction) {
		return getIOOrOutputDatastore(api, root, instruction, false);
	}

	private static ODLDatastore<? extends ODLTableDefinition> getIOOrOutputDatastore(ODLApi api, Option root, InstructionConfig instruction, boolean getIO) {
		ODLComponent component = getComponent(instruction);
		if (component != null) {
			Serializable config = getComponentConfig(root, instruction);
			if (component.getConfigClass() == null) {
				config = null;
			} else if (component.getConfigClass().isInstance(config) == false) {
				config = null;
			}

			if (component.getConfigClass() == null || config != null) {
				if (getIO) {
					return component.getIODsDefinition(api, config);
				} else {
					return component.getOutputDsDefinition(api, instruction.getExecutionMode(), config);
				}
			}
		}
		return null;
	}

	public static ODLComponent getComponent(ComponentConfig instruction) {
		ODLComponent component = ODLGlobalComponents.getProvider().getComponent(instruction.getComponent());
		return component;
	}

	public static int getOptionsCount(Option option) {
		int ret = 1;
		for (Option child : option.getOptions()) {
			ret += getOptionsCount(child);
		}
		return ret;
	}

	public static Icon getComponentIcon(ODLApi api, String componentid, int mode) {
		ODLComponent comp = ODLGlobalComponents.getProvider().getComponent(componentid);
		if (comp != null) {
			return comp.getIcon(api, mode);
		}
		return null;
	}

	/**
	 * Get the component name if available
	 * 
	 * @param instruction
	 * @return
	 */
	public static String getComponentName(ComponentConfig instruction) {
		ODLComponent component = getComponent(instruction);
		if (component != null) {
			return component.getName();
		}
		return null;
	}

	/**
	 * Can the script option be run?
	 * 
	 * @param option
	 * @return
	 */
	public static boolean isRunnableOption(Option option) {
		// only leaf nodes are runnable
		return option != null && option.getOptions().size() == 0;
	}

	public static ArrayList<Option> getRunnableOptions(Script script) {
		final ArrayList<Option> ret = new ArrayList<>();
		visitOptions(script, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				if (isRunnableOption(option)) {
					ret.add(option);
				}
				return true;
			}
		});
		return ret;
	}

	public static interface OptionVisitor {
		/**
		 * @param parent
		 * @param option
		 * @param depth
		 * @return True if child options should be visited
		 */
		boolean visitOption(Option parent, Option option, int depth);
	}

	/**
	 * Return a deep copy of the script with its child options removed
	 * 
	 * @param script
	 * @return
	 */
	public static Script removeChildOptions(Script script) {
		script = ScriptIO.instance().deepCopy(script);
		script.getOptions().clear();
		return script;
	}

	public static String getOptionIdByAdapterId(Option root, final String adapterId) {
		return getScriptElement(new FindScriptElement<String>() {

			@Override
			public String find(Option option) {
				for (AdapterConfig a : option.getAdapters()) {
					if (Strings.equalsStd(adapterId, a.getId())) {
						return option.getOptionId();
					}
				}
				return null;
			}
		}, root);
	}

	public static String getOptionIdByInstructionId(Option root, String instructionId) {
		ArrayList<String> tmp = new ArrayList<>(1);
		tmp.add(instructionId);
		String[] ret = getOptionIdsByInstructionIds(root, tmp);
		if (ret.length > 0) {
			return ret[0];
		}
		return null;
	}

	public static String[] getOptionIdsByInstructionIds(Option root, Iterable<String> instructionIds) {
		final StandardisedStringSet set = new StandardisedStringSet(false);
		final StandardisedStringSet searchFor = new StandardisedStringSet(false,instructionIds);
		visitOptions(root, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				for (InstructionConfig instruct : option.getInstructions()) {
					if (searchFor.contains(instruct.getUuid())) {
						set.add(option.getOptionId());
					}
				}
				return true;
			}
		});
		return set.toArray();
	}

	public static interface InstructionVisitor {
		void visitInstruction(Option parentOption, Option option, InstructionConfig instruction);
	}

	public static void visitInstructions(Option option, final InstructionVisitor visitor) {
		visitOptions(option, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				for (InstructionConfig instruction : option.getInstructions()) {
					visitor.visitInstruction(parent, option, instruction);
				}
				return true;
			}
		});
	}

	public static interface CopyTablesVisitor {
		void visitCopyTables(Option parentOption, Option option, OutputConfig copy);
	}

	public static void visitCopyTables(Option option, final CopyTablesVisitor visitor) {
		visitOptions(option, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				for (OutputConfig copy : option.getOutputs()) {
					visitor.visitCopyTables(parent, option, copy);
				}
				return true;
			}
		});
	}
	
	public static interface AdaptersVisitor {
		void visitAdapter(Option parentOption, Option option, AdapterConfig copy);
	}

	public static void visitAdapters(Option option, final AdaptersVisitor visitor) {
		visitOptions(option, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				for (AdapterConfig adapter : option.getAdapters()) {
					visitor.visitAdapter(parent, option, adapter);
				}
				return true;
			}
		});
	}


	public static void visitOptions(Option option, final OptionVisitor visitor) {
		class Parser {
			void parse(Option parent, Option option, int depth) {
				if(visitor.visitOption(parent, option, depth)){
					for (Option child : option.getOptions()) {
						parse(option, child,depth+1);
					}					
				}
			}
		}
		new Parser().parse(null, option,0);
	}

	public static void setAllUnsynced(Option root) {
		ScriptUtils.visitOptions(root, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				option.setSynchronised(false);
				return true;
			}
		});
	}

	public static Option getOption(Script script, final String optionId) {
		List<Option> path = getOptionPath(script, optionId);
		if (path != null) {
			return path.get(path.size() - 1);
		}
		return null;
	}

	public interface OptionPredicate {
		boolean accept(Option option);
	}

	public interface FindScriptElement<T> {
		T find(Option option);
	}

	/**
	 * Gets the adapter id using a table in the adapter. The search table is checked against each adapted table in the script using the == operator
	 * (i.e. it must be the same object).
	 * 
	 * @param root
	 * @param tableInAdapter
	 * @return
	 */
	public static String getAdapterId(Option root, final AdaptedTableConfig tableInAdapter) {
		return getScriptElement(new FindScriptElement<String>() {

			@Override
			public String find(Option option) {
				for (AdapterConfig adapter : option.getAdapters()) {
					for (AdaptedTableConfig table : adapter.getTables()) {
						if (table == tableInAdapter) {
							return adapter.getId();
						}
					}
				}
				return null;
			}
		}, root);
	}

	public static <T> T getScriptElement(FindScriptElement<T> finder, Option root) {
		T ret = finder.find(root);
		if (ret != null) {
			return ret;
		}

		for (Option child : root.getOptions()) {
			ret = getScriptElement(finder, child);
			if (ret != null) {
				return ret;
			}
		}
		return null;
	}

	/**
	 * Find the path to the option, including the option as the last element, or return null if not found
	 * 
	 * @param script
	 * @param optionId
	 * @return
	 */
	public static List<Option> getOptionPath(Option script, final OptionPredicate predicate) {

		class Parser {
			List<Option> parse(Option option, List<Option> path) {
				path = new ArrayList<>(path);
				path.add(option);

				if (predicate.accept(option)) {
					return path;
				}

				for (Option child : option.getOptions()) {
					List<Option> ret = parse(child, path);
					if (ret != null) {
						return ret;
					}
				}
				return null;
			}
		}
		return new Parser().parse(script, new ArrayList<Option>());
	}

	/**
	 * Find the path to the option, including the option as the last element, or return null if not found
	 * 
	 * @param script
	 * @param optionId
	 * @return
	 */
	public static List<Option> getOptionPath(Option script, final String optionId) {
		return getOptionPath(script, new OptionPredicate() {

			@Override
			public boolean accept(Option option) {
				return Strings.equalsStd(option.getOptionId(), optionId);
			}
		});
	}

	public static List<String> getOptionIdsByInstructionExecutionMode(Script script, final int mode) {
		final ArrayList<String> ret = new ArrayList<>();
		visitOptions(script, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				boolean found = false;
				for (InstructionConfig instruction : option.getInstructions()) {
					if (instruction.getExecutionMode() == mode) {
						found = true;
						break;
					}
				}

				if (found) {
					ret.add(option.getOptionId());
				}
				return true;
			}
		});
		return ret;
	}

//	/**
//	 * Tests if the input adapters directly read from the input table. Note that indirect reads such as can happen in a lookup formula are not
//	 * checked.
//	 * 
//	 * @param adapters
//	 * @param datastore
//	 * @param tableName
//	 * @return
//	 */
//	public static boolean getReadsDirectlyTable(Iterable<AdapterConfig> adapters, String datastore, String tableName) {
//		for (AdapterConfig adapterConfig : adapters) {
//			for (AdaptedTableConfig table : adapterConfig) {
//				if (Strings.equalsStd(datastore, table.getFromDatastore()) && Strings.equalsStd(tableName, table.getFromTable())) {
//					return true;
//				}
//			}
//		}
//
//		return false;
//	}

	/**
	 * Returns the first matching optionid for the input name
	 * @param root
	 * @param optionName
	 * @return
	 */
	public static String getOptionIdByName(Option root, String optionName){
		class Ret{
			String id;
		}
		Ret ret = new Ret();
		visitOptions(root, new OptionVisitor() {
			
			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				if(ret.id==null && Strings.equalsStd(optionName, option.getName())){
					ret.id = option.getOptionId();
				}
				// keep on parsing unless we've found the first one
				return ret.id ==null;
			}
		});
		
		return ret.id;
	}
	
	public static ComponentConfig getComponentConfig(Option root, final String configId) {
		return getScriptElement(new FindScriptElement<ComponentConfig>() {

			@Override
			public ComponentConfig find(Option option) {
				for (ComponentConfig c : option.getComponentConfigs()) {
					if (Strings.equalsStd(c.getConfigId(), configId)) {
						return c;
					}
				}
				return null;
			}
		}, root);
	}

	public static AdapterConfig getAdapterById(Option option, String id, boolean recurseChildOptions) {
		// find in current level
		for (AdapterConfig ac : option.getAdapters()) {
			if (Strings.equalsStd(ac.getId(), id)) {
				return ac;
			}
		}

		// recurse
		if (recurseChildOptions) {
			for (Option child : option.getOptions()) {
				AdapterConfig ac = getAdapterById(child, id, true);
				if (ac != null) {
					return ac;
				}
			}
		}

		return null;
	}

	public static boolean hasDatastore(Script script, String id) {
		if (Strings.equalsStd(ScriptConstants.EXTERNAL_DS_NAME, id)) {
			return true;
		}

		for (InstructionConfig instruction : script.getInstructions()) {
			if (Strings.equalsStd(instruction.getOutputDatastore(), id)) {
				return true;
			}
		}
		return false;
	}

//	public static boolean hasAdapterReadingTable(Script script, String datastore, String table) {
//		for (AdapterConfig adapterConfig : script.getAdapters()) {
//			for (AdaptedTableConfig tableConfig : adapterConfig.getTables()) {
//				if (Strings.equalsStd(datastore, tableConfig.getFromDatastore())) {
//					if (Strings.equalsStd(table, tableConfig.getFromTable())) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}

	/**
	 * Find the instruction's configuration. This could be internal (inside the component object) or external - an id referencing a data object
	 * earlier in the script.
	 * 
	 * @param root
	 * @param instruction
	 * @return
	 */
	public static Serializable getComponentConfig(Option root, final InstructionConfig instruction) {
		Serializable ret = null;
		if (Strings.isEmpty(instruction.getConfigId())) {
			ret = instruction.getComponentConfig();
		} else {
			// search for config
			List<Option> available = getOptionPath(root, new OptionPredicate() {

				@Override
				public boolean accept(Option option) {
					for (InstructionConfig test : option.getInstructions()) {
						if (test == instruction) {
							return true;
						}
					}
					return false;
				}
			});

			if (available == null || available.size() == 0) {
				throw new RuntimeException("Corrupt script - cannot find instruction in script.");
			}

			// find the config
			int n = available.size();
			boolean found = false;
			for (int i = n - 1; found == false && i >= 0; i--) {
				for (ComponentConfig conf : available.get(i).getComponentConfigs()) {
					if (Strings.equalsStd(conf.getConfigId(), instruction.getConfigId())) {
						ret = conf.getComponentConfig();
						found = true;
						break;
					}
				}
			}

			if (!found) {
				throw new RuntimeException("Corrupt script - cannot find component configuration with id: " + instruction.getConfigId());
			}
		}

		// return default object if we have none
		if (ret == null) {
			ODLComponent component = getComponent(instruction);
			if(component!=null){
				if(component.getConfigClass()!=null){
					try {
						ret = component.getConfigClass().newInstance();
					} catch (Exception e) {
						throw new RuntimeException(e);
					} 
				}
			}
		}
		
		return ret;
	}

	/**
	 * Create a new component configuration object if the instruction doesn't already have one or its of the wrong class
	 * 
	 * @param component
	 * @param componentConfig
	 * @return
	 */
	public static void validateComponentConfigClass(ODLComponent component, ComponentConfig componentConfig) {

		if (component.getConfigClass() == null) {
			// no config used
			componentConfig.setComponentConfig(null);
			return;
		}

		boolean isInstruction = InstructionConfig.class.isInstance(componentConfig);
		if (isInstruction && Strings.isEmpty(componentConfig.getConfigId()) == false) {
			// its an instruction which uses external config
			componentConfig.setComponentConfig(null);
			return;
		}

		// check correct class
		Serializable obj = componentConfig.getComponentConfig();
		if (obj != null && component.getConfigClass() != null && component.getConfigClass().isInstance(obj) == false) {
			obj = null;
		}

		// create new object if missing one
		if (obj == null) {
			try {
				obj = component.getConfigClass().newInstance();
			} catch (Throwable e) {
				obj = null;
			}
		}
		componentConfig.setComponentConfig(obj);

	}

	/**
	 * Gets the icon from a script if has a single instruction with an Icon
	 * 
	 * @return
	 */
	public static Icon getIconFromMasterComponent(ODLApi api, Option option) {
		Pair<ODLComponent, Integer> pair = getMasterComponent(option);
		if (pair != null && pair.getFirst() != null && pair.getSecond() != null) {
			return pair.getFirst().getIcon(api, pair.getSecond());
		}
		return null;
	}

	// public static String getShortDisplayName(Script script) {
	// // if (script.getScriptEditorUIType() == ScriptEditorType.GENERAL_EDITOR) {
	// // return "General";
	// // }
	//
	// Pair<ODLComponent, Integer> master = getMasterComponent(script);
	// if (master != null) {
	// ODLComponent component = master.getFirst();
	// if (component != null) {
	// return component.getName();
	//
	// }
	// }
	//
	// return "";
	// }

	// public static Pair<String,Integer> getMasterComponentId(Option option) {
	// if(Script.class.isInstance(option)){
	// Script script = (Script)option;
	// if(!Strings.isEmpty(script.getCreatedByComponentId())){
	// return new Pair<String, Integer>(script.getCreatedByComponentId(), ODLComponent.MODE_DEFAULT);
	// }
	// }
	//
	// InstructionConfig config = option.getLastInstruction();
	// if (config != null) {
	// return new Pair<String,Integer>( config.getComponent(), config.getExecutionMode());
	// }
	//
	// return null;
	// }

	private static Pair<ODLComponent, Integer> getMasterComponent(Option option) {
		Pair<String, Integer> id = null;
		if (Script.class.isInstance(option)) {
			Script script = (Script) option;
			if (!Strings.isEmpty(script.getCreatedByComponentId())) {
				id = new Pair<String, Integer>(script.getCreatedByComponentId(), ODLComponent.MODE_DEFAULT);
			}
		}

		if (id == null) {
			InstructionConfig config = option.getLastInstruction();
			if (config != null) {
				id = new Pair<String, Integer>(config.getComponent(), config.getExecutionMode());
			}
		}

		if (id != null && id.getFirst() != null) {
			return new Pair<ODLComponent, Integer>(ODLGlobalComponents.getProvider().getComponent(id.getFirst()), id.getSecond());
		}
		return null;
	}

	/**
	 * Validates a script's synchronisation settings, returning false if script is invalid and settings cannot be fixed
	 */
	public static boolean validateSynchonisation(final ODLApi api, final Script script) {
		class RetValue {
			boolean ret = true;
		}
		final RetValue retValue = new RetValue();
		visitOptions(script, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				OutputWindowSyncLevel syncLevel = getOutputWindowSyncLevel(api, script, option.getOptionId());

				switch (syncLevel) {
				case NEVER:
					option.setSynchronised(false);
					break;

				case ALWAYS:
					option.setSynchronised(true);
					break;

				case MANUAL:
					break;

				case ERROR:
					retValue.ret = false;
					break;
				}
				return true;
			}
		});
		return retValue.ret;
	}

	public enum OutputWindowSyncLevel {
		NEVER, MANUAL, ALWAYS, ERROR;
	}

	private enum IdType {
		INSTRUCTION, ADAPTER, INSTRUCTION_CONFIGURATION, OPTION
	}

	public static void validateIds(final Script script) {
		final StandardisedStringSet[] sets = new StandardisedStringSet[IdType.values().length];
		for (int i = 0; i < sets.length; i++) {
			sets[i] = new StandardisedStringSet(false);
		}

		visitOptions(script, new OptionVisitor() {

			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				validate(option.getOptionId(), IdType.OPTION);

				for (AdapterConfig adapterConfig : option.getAdapters()) {
					validate(adapterConfig.getId(), IdType.ADAPTER);
				}

				for (InstructionConfig instructionConfig : option.getInstructions()) {
					validate(instructionConfig.getUuid(), IdType.INSTRUCTION);
				}

				for (ComponentConfig componentConfig : option.getComponentConfigs()) {
					validate(componentConfig.getConfigId(), IdType.INSTRUCTION_CONFIGURATION);
				}
				return true;
			}

			void validate(String id, IdType type) {
				if (Strings.isEmpty(id)) {
					throw new RuntimeException("Script contains an empty id for an " + Strings.convertEnumToDisplayFriendly(type) + ".");
				}
				int i = type.ordinal();
				if (sets[i].contains(id)) {
					throw new RuntimeException("Script contains a duplicate id " + id + " for an " + Strings.convertEnumToDisplayFriendly(type) + ".");
				}
				sets[i].add(id);
			}
		});
	}

	public static InstructionConfig getInstructionByUUID(Option root, final String uuid) {
		return getScriptElement(new FindScriptElement<InstructionConfig>() {

			@Override
			public InstructionConfig find(Option option) {
				for (InstructionConfig instruction : option.getInstructions()) {
					if (Strings.equalsStd(instruction.getUuid(), uuid)) {
						return instruction;
					}
				}
				return null;
			}
		}, root);
	}

	public static String createUniqueInstructionId(Option root) {
		while (true) {
			String id = UUID.randomUUID().toString();
			if (getInstructionByUUID(root, id) == null) {
				return id;
			}
		}
	}

	/**
	 * Create a unique datastore id. This is checked against (a) the external datastore name, (b) all adapters in the root option and below and (c)
	 * all instruction outputs in the root option and below
	 * 
	 * @param root
	 * @param baseId
	 * @return
	 */
	public static String createUniqueDatastoreId(final Option root, String baseId) {
		return Strings.makeUnique(baseId, new DoesStringExist() {

			@Override
			public boolean isExisting(final String s) {
				if (Strings.equalsStd(s, ScriptConstants.EXTERNAL_DS_NAME)) {
					return true;
				}
				if (getAdapterById(root, s, true) != null) {
					return true;
				}

				if (getInstructionByOutputDs(root, s) != null) {
					return true;
				}

				return false;
			}
		});
	}
	
	public static class ScriptIds{
		final public Set<String> adapters;
		final public Set<String> outputdatastores;		
		final public Set<String> options;
		final public Set<String> datastoresAndAdapters;
		
		ScriptIds(Set<String> adapters, Set<String> outputdatastores, Set<String> options, Set<String> datastoresAndAdapters) {
			this.adapters = adapters;
			this.outputdatastores = outputdatastores;
			this.options = options;
			this.datastoresAndAdapters = datastoresAndAdapters;
		}
		
		public boolean containsAdapterOrDatastore(String s){
			return adapters.contains(s) || outputdatastores.contains(s);
		}
		
		public static Set<String> getCommonStrings(ODLApi api,Set<String> setA, Set<String> setB){
			 Set<String> ret = api.stringConventions().createStandardisedSet();
			 for(String s : setA){
				 if(setB.contains(s)){
					 ret.add(s);
				 }
			 }
			 
			 for(String s : setB){
				 if(setA.contains(s)){
					 ret.add(s);
				 }
			 }
			 
			 return ret;
		}
		
	}

	/**
	 * Fetch all ids in the script
	 * @param api
	 * @param option
	 * @return
	 */
	public static ScriptIds getIds(ODLApi api,Option option){
		StringConventions strings = api.stringConventions();
		ScriptIds ret = new ScriptIds(strings.createStandardisedSet(), strings.createStandardisedSet(), strings.createStandardisedSet(), strings.createStandardisedSet());
		visitOptions(option, new OptionVisitor() {
			
			@Override
			public boolean visitOption(Option parent, Option option, int depth) {
				ret.options.add(option.getOptionId());
				for(InstructionConfig instruction : option.getInstructions()){
					if(instruction.getOutputDatastore()!=null){
						ret.outputdatastores.add(instruction.getOutputDatastore());
					}
				}
				
				for(AdapterConfig adapter : option.getAdapters()){
					if(adapter.getId()!=null){
						ret.adapters.add(adapter.getId());						
					}
				}
				return true;
			}
		});
		
		ret.datastoresAndAdapters.addAll(ret.outputdatastores);
		ret.datastoresAndAdapters.addAll(ret.adapters);
		return ret;
	}
	
	/**
	 * For the input script find out what synchronisation can be done of output windows
	 * 
	 * @param script
	 * @param optionId
	 * @return
	 */
	public static OutputWindowSyncLevel getOutputWindowSyncLevel(ODLApi api, Script script, String optionId) {
		List<Option> path = getOptionPath(script, optionId);
		return getOutputWindowSyncLevel(api, path);
	}

	/**
	 * @param path
	 * @return
	 */
	public static OutputWindowSyncLevel getOutputWindowSyncLevel(ODLApi api, List<Option> path) {
		if (path == null) {
			return OutputWindowSyncLevel.ERROR;
		}

		int alwaysCount = 0;
		int neverCount = 0;
		int canBe = 0;
		for (Option option : path) {
			for (InstructionConfig instruction : option.getInstructions()) {
				ODLComponent component = getComponent(instruction);
				if (component == null) {
					return OutputWindowSyncLevel.ERROR;
				}

				long flags = component.getFlags(api, instruction.getExecutionMode());
				if ((flags & ODLComponent.FLAG_OUTPUT_WINDOWS_ALWAYS_SYNCHRONISED) == ODLComponent.FLAG_OUTPUT_WINDOWS_ALWAYS_SYNCHRONISED) {
					alwaysCount++;
				}

				else if ((flags & ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED) != ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED) {
					neverCount++;
				}

				else {
					canBe++;
				}
			}
		}

		// 8 possiblities exist

		if (alwaysCount == 0 && neverCount == 0 && canBe == 0) {
			return OutputWindowSyncLevel.NEVER;
		}

		if (alwaysCount == 0 && neverCount == 0 && canBe >= 1) {
			return OutputWindowSyncLevel.MANUAL;
		}

		if (alwaysCount == 0 && neverCount >= 1 && canBe >= 0) {
			return OutputWindowSyncLevel.NEVER;
		}

		if (alwaysCount == 0 && neverCount >= 1 && canBe >= 1) {
			return OutputWindowSyncLevel.NEVER;
		}

		if (alwaysCount >= 1 && neverCount == 0 && canBe == 0) {
			return OutputWindowSyncLevel.ALWAYS;
		}

		if (alwaysCount >= 1 && neverCount == 0 && canBe >= 1) {
			return OutputWindowSyncLevel.ALWAYS;
		}

		if (alwaysCount >= 1 && neverCount >= 1 && canBe >= 0) {
			return OutputWindowSyncLevel.ERROR;
		}

		if (alwaysCount >= 1 && neverCount >= 1 && canBe >= 1) {
			return OutputWindowSyncLevel.ERROR;
		}

		// should never get to here...
		throw new UnsupportedOperationException();
	}

	public static <T extends ODLTableDefinition> Iterable<T> tableIterator(final ODLDatastore<T> ds) {
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					int currentIndex = -1;

					@Override
					public boolean hasNext() {
						return (currentIndex + 1) < ds.getTableCount();
					}

					@Override
					public T next() {
						return ds.getTableAt(++currentIndex);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public static String getDefaultScriptName(Script script) {
		Pair<ODLComponent, Integer> component = getMasterComponent(script);

		if (component != null && component.getFirst() != null) {
			return component.getFirst().getName();
		}

		return "script";
	}

//	public static boolean getReadsExternalDatastore(final ODLApi api, Script script) {
//		for (AdapterConfig adapterConfig : script.getAdapters()) {
//			for (AdaptedTableConfig table : adapterConfig.getTables()) {
//				if (ScriptConstants.isExternalDs(table.getFromDatastore())) {
//					return true;
//				}
//			}
//		}
//
//		for (InstructionConfig instruction : script.getInstructions()) {
//			try {
//				ODLComponent component = ScriptUtils.getComponent(instruction);
//				if (component != null) {
//					Serializable componentConfig = ScriptUtils.getComponentConfig(script, instruction);
//					if (component.getIODsDefinition(api, componentConfig) != null && ScriptConstants.isExternalDs(instruction.getDatastore())) {
//						return true;
//					}
//				}
//
//			} catch (Throwable e) {
//				// TODO: handle exception
//			}
//		}
//		return false;
//	}

	/**
	 * Create a provider for the adapter's expected structure, based on whether its a VLS adapter or
	 * the component which uses it. The expected structure is recreated dynamically on-the-fly,
	 * so should always be up-to-date.
	 * @param api
	 * @param root
	 * @param optionContainingAdapter
	 * @param adapterId
	 * @return
	 */
	public static AdapterExpectedStructureProvider createAdapterExpectedStructure(final ODLApi api, final Option root, final Option optionContainingAdapter, final String adapterId) {
		// find the adapter
		return new AdapterExpectedStructureProvider() {

			@Override
			public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition() {
				final ODLDatastoreAlterable<? extends ODLTableAlterable> ret = ODLFactory.createAlterable();

				// Check if the adapter is drawable
				AdapterConfig adapterConfig = getAdapterById(optionContainingAdapter, adapterId, false);
				if(adapterConfig.getAdapterType() == ScriptAdapterType.VLS){
					// Add view-layer-style tables
					api.tables().addTableDefinitions(VLSBuilder.getVLSTableDefinitions(), ret, false);
					
					ODLTableDefinition src = VLSBuilder.getSourceTableDefinition();
					api.tables().addTableDefinition(src, ret, false);
					return ret;
				}
				
				else if(adapterConfig.getAdapterType() == ScriptAdapterType.PARAMETER){
					return api.scripts().parameters().dsDefinition(false);
				}
				
				if ((adapterConfig.getFlags() & TableFlags.FLAG_IS_DRAWABLES) == TableFlags.FLAG_IS_DRAWABLES) {
					ODLDatastore<? extends ODLTableDefinition> drawables = api.standardComponents().map().getLayeredDrawablesDefinition();
					for (int i = 0; i < drawables.getTableCount(); i++) {
						api.tables().copyTableDefinition(drawables.getTableAt(i), ret);
					}
				}

				// Find all instructions which are children of the input option and use the adapter id.
				visitInstructions(optionContainingAdapter, new InstructionVisitor() {

					@Override
					public void visitInstruction(Option parentOption, Option option, InstructionConfig instruction) {

						// does the instruction use the adapter id?
						if (Strings.equalsStd(instruction.getDatastore(), adapterId)) {
							ODLDatastore<? extends ODLTableDefinition> iods = getIODatastoreDfn(api, root, instruction);
							if (iods != null) {

								// add any additional flags we find (e.g. wildcard flag, is reporter flag)
								ret.setFlags(ret.getFlags() | iods.getFlags());

								for (ODLTableDefinition targetTable : tableIterator(iods)) {

									// find the table in the return datastore and create it if not existing
									ODLTableDefinitionAlterable retTable = TableUtils.findTable(ret, targetTable.getName());
									if (retTable == null) {
										retTable = DatastoreCopier.copyTableDefinition(targetTable, ret);
									}

									// add column wildcard if we have it...
									if (TableUtils.hasFlag(targetTable, TableFlags.FLAG_COLUMN_WILDCARD)) {
										retTable.setFlags(retTable.getFlags() | TableFlags.FLAG_COLUMN_WILDCARD);
									}

									// now copy any missing columns over (happens when more than 1 instruction uses the same adapter)
									int nc = targetTable.getColumnCount();
									for (int col = 0; col < nc; col++) {
										String colName = targetTable.getColumnName(col);
										if (TableUtils.findColumnIndx(retTable, colName) == -1) {
											DatastoreCopier.copyColumnDefinition(targetTable, retTable, col, false);
										}
									}
								}

							}
						}

						// what about adapters which take this as input???
					}
				});
				return ret;
			}
		};
	}

	public static String createUniqueTableName(final AdapterConfig adapter, String base) {
		return Strings.makeUnique(base, new DoesStringExist() {

			@Override
			public boolean isExisting(String s) {
				for (AdaptedTableConfig table : adapter) {
					if (Strings.equalsStd(table.getName(), s)) {
						return true;
					}
				}
				return false;
			}
		});
	}

	/**
	 * @param root
	 * @param dsId
	 * @return
	 */
	private static InstructionConfig getInstructionByOutputDs(final Option root, final String dsId) {
		return getScriptElement(new FindScriptElement<InstructionConfig>() {

			@Override
			public InstructionConfig find(Option option) {
				for (InstructionConfig instructionConfig : option.getInstructions()) {
					if (Strings.equalsStd(instructionConfig.getOutputDatastore(), dsId)) {
						return instructionConfig;
					}
				}
				return null;
			}
		}, root);
	}
}
