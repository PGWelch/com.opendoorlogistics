/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.utils;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * Utility class to parse the fields generates in a script
 * 
 * @author Phil
 *
 */
public class ScriptFieldsParser {
	public enum ScriptDataSourceType {
		DATA_ADAPTER, EXTERNAL, INSTRUCTION_IO, INSTRUCTION_OUTPUT, INTERNAL_DATASOURCE;

		@Override
		public String toString() {
			return Strings.convertEnumToDisplayFriendly(super.toString());
		}
	}

	public static class Source {
		private final String datastoreId;
		private final int instructionIndx;
		private final Option option;
		private final ScriptDataSourceType sourceType;

		public Source(String datastoreId, ScriptDataSourceType sourceType, int instructionIndx, Option option) {
			this.datastoreId = datastoreId;
			this.sourceType = sourceType;
			this.instructionIndx = instructionIndx;
			this.option = option;
		}

		public String getDatastoreId() {
			return datastoreId;
		}

		public int getInstructionIndx() {
			return instructionIndx;
		}

		public Option getOption() {
			return option;
		}

		public ScriptDataSourceType getSourceType() {
			return sourceType;
		}

	}

	public static class SourcedColumn {
		private final int fieldIndex;
		private final SourcedTable table;

		public SourcedColumn(SourcedTable table, int fieldIndx) {
			this.table = table;
			this.fieldIndex = fieldIndx;
		}

		public String getDatastoreId() {
			return table.getDatastoreId();
		}

		public String getFieldname() {
			return table.getTableDefinition().getColumnName(fieldIndex);
		}

		public SourcedTable getTable() {
			return table;
		}

		public String getTableName() {
			return table.getTableName();
		}

		public ODLColumnType getType() {
			return table.getTableDefinition().getColumnType(fieldIndex);
		}
	}

	public static class SourcedDatastore extends Source {
		private final ODLDatastore<? extends ODLTableDefinition> definition;

		public SourcedDatastore(String id, ODLDatastore<? extends ODLTableDefinition> definition, ScriptDataSourceType sourceType, int instructionIndx, Option option) {
			super(id, sourceType, instructionIndx, option);
			this.definition = definition;
		}

		public ODLDatastore<? extends ODLTableDefinition> getDefinition() {
			return definition;
		}

	}

	public static class SourcedTable {
		private final SourcedDatastore datastore;
		private final ODLTableDefinition table;
		private int tableIndx;

		public SourcedTable(SourcedDatastore datastore, int tableIndx, ODLTableDefinition table) {
			super();
			this.datastore = datastore;
			this.tableIndx = tableIndx;
			this.table = table;
		}

		public SourcedDatastore getDatastore() {
			return datastore;
		}

		public String getDatastoreId() {
			return datastore.getDatastoreId();
		}

		public Option getOption() {
			return datastore.getOption();
		}

		public ODLTableDefinition getTableDefinition() {
			return table;
		}

		public int getTableIndx() {
			return tableIndx;
		}

		// public ScriptDataSourceType getSourceType() {
		// return sourceType;
		// }
		//
		// public String getTableName() {
		// return tableName;
		// }
		//
		// public int getInstructionIndx() {
		// return instructionIndx;
		// }

		public String getTableName() {
			return table.getName();
		}

		public void setTableIndex(int index) {
			this.tableIndx = index;
		}

		// public Option getOption() {
		// return option;
		// }

		@Override
		public String toString() {
			return datastore + "." + table.getName();
		}
	}

	/**
	 * Get all fields available at the input option level defined by the option id. This includes fields at its own level and fields inherited from
	 * its ancestors
	 * 
	 * @param root
	 * @param optionId
	 * @param includeInstructionInputs
	 * @param external
	 * @param cb
	 * @return
	 */
	public static List<SourcedColumn> getMultiLevelColumns(final ODLApi api, Option root, String optionId, ODLDatastore<? extends ODLTableDefinition> external) {
		return toColumns(getMultiLevelTables(api, root, optionId, external));
	}

	public static List<SourcedDatastore> getMultiLevelDatastores(ODLApi api, Option root, String optionId, ODLDatastore<? extends ODLTableDefinition> external) {
		ArrayList<SourcedDatastore> ret = new ArrayList<>();
		if (root != null) {
			List<Option> path = ScriptUtils.getOptionPath(root, optionId);
			if (path != null) {
				for (int i = 0; i < path.size(); i++) {
					ret.addAll(getSingleLevelDatastores(api, root, path.get(i), i == 0 ? external : null));
				}
			}
		} else {
			ret.addAll(getSingleLevelDatastores(api, null, null, external));
		}
		return ret;
	}

	public static List<SourcedTable> getMultiLevelTables(ODLApi api, Option root, String optionId, ODLDatastore<? extends ODLTableDefinition> external) {
		return toTables(getMultiLevelDatastores(api, root, optionId, external));
	}

	/**
	 * Generate list of all available tables from the single option without recursion
	 * 
	 * @param option
	 * @param includeInstructionInputs
	 * @param external
	 * @param cb
	 * @return
	 */
	public static List<SourcedColumn> getSingleLevelColumns(final ODLApi api, Option root, final Option option, ODLDatastore<? extends ODLTableDefinition> external) {
		return toColumns(toTables(getSingleLevelDatastores(api, root, option, external)));
	}

	public static List<SourcedDatastore> getSingleLevelDatastores(final ODLApi api, Option root, Option option, ODLDatastore<? extends ODLTableDefinition> external) {
		ArrayList<SourcedDatastore> ret = new ArrayList<>();

		// add external datastore if we have it
		if (external != null) {
			ret.add(new SourcedDatastore(ScriptConstants.EXTERNAL_DS_NAME, external, ScriptDataSourceType.EXTERNAL, -1, null));
		}

		// we should always have the internal parameters datastore
		Parameters parameters = api.scripts().parameters();
		ret.add(new SourcedDatastore(parameters.getDSId(), parameters.dsDefinition(true), ScriptDataSourceType.INTERNAL_DATASOURCE, -1, null));
		
		// add adapters
		if (option != null) {
			for (AdapterConfig adapter : option.getAdapters()) {
				try {
					ret.add(new SourcedDatastore(adapter.getId(), adapter.createOutputDefinition(), ScriptDataSourceType.DATA_ADAPTER, -1, option));
				} catch (Throwable e) {
				}
			}
		}

		// add instruction outputs
		if (option != null && root != null) {
			for (int i = 0; i < option.getInstructions().size(); i++) {
				InstructionConfig instruction = option.getInstructions().get(i);
				ODLDatastore<? extends ODLTableDefinition> output = ScriptUtils.getOutputDatastoreDfn(api, root, instruction);
				if (output != null) {
					ret.add(new SourcedDatastore(instruction.getOutputDatastore(), output, ScriptDataSourceType.INSTRUCTION_OUTPUT, i, option));
				}
			}
		}

		return ret;
	}

	public static List<SourcedColumn> toColumns(List<SourcedTable> tables) {
		ArrayList<SourcedColumn> ret = new ArrayList<>();
		for (SourcedTable table : tables) {
			ODLTableDefinition tableDefinition = table.getTableDefinition();
			for (int i = 0; i < tableDefinition.getColumnCount(); i++) {
				ret.add(new SourcedColumn(table, i));
			}
		}
		return ret;
	}

	public static List<SourcedTable> toTables(List<SourcedDatastore> datastores) {
		ArrayList<SourcedTable> ret = new ArrayList<>();
		for (SourcedDatastore sd : datastores) {
			ret.addAll(toTables(sd));
		}
		return ret;
	}

	public static List<SourcedTable> toTables(SourcedDatastore sd) {
		ArrayList<SourcedTable> ret = new ArrayList<>();
		for (int i = 0; i < sd.getDefinition().getTableCount(); i++) {
			ODLTableDefinition table = sd.getDefinition().getTableAt(i);
			ret.add(new SourcedTable(sd, i, table));
		}
		return ret;
	}
}
