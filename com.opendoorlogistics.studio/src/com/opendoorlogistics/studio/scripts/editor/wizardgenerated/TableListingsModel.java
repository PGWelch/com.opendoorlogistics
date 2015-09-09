/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.wizardgenerated;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser.SourcedTable;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser.ScriptDataSourceType;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.scripts.editor.wizardgenerated.ScriptEditorWizardGenerated.DisplayNode;

class TableListingsModel extends AbstractTableModel {

	final private List<SourcedTable> tables;
	final private DisplayNode node;
	
	public TableListingsModel(ODLApi api,DisplayNode node, ODLDatastore<? extends ODLTableDefinition> external) {
		this.node = node;
		Option root = node.getRoot().option;
		tables = ScriptFieldsParser.getMultiLevelTables(api,root, node.option.getOptionId(), external);
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {

		case 0:
			return "From";

		case 1:
			return "Datastore id";

		case 2:
			return "Table name";
		}
		return null;
	}

	public ODLTableDefinition getTableDefinition(int row){
		return tables.get(row).getTableDefinition();
	}
	
	public boolean isCurrentOption(int row){
		return tables.get(row).getOption() == node.option;
	}
	
	public SourcedTable getTableDetails(int rowIndex){
		return tables.get(rowIndex);
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		SourcedTable tn = tables.get(rowIndex);
		Option opt = tn.getOption();
		switch (columnIndex) {
		case 0:
			if (tn.getDatastore().getSourceType() == ScriptDataSourceType.EXTERNAL) {
				return "Spreadsheet";
			}
			if(opt!=null){
				return (Strings.isEmpty(opt.getName()) ? opt.getOptionId() : opt.getName());				
			}
			return "";

		case 1:
			return tn.getDatastore()!=null ? tn.getDatastore().getDatastoreId() : "";

		case 2:
			return tn.getTableName();
		}
		return null;
	}

	@Override
	public int getRowCount() {
		return tables.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}
}
