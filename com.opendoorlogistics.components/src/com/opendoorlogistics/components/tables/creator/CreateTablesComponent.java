/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.tables.creator;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.TableCreator;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.utils.ui.Icons;

final public class CreateTablesComponent implements TableCreator{

	@Override
	public String getId() {
		return "com.opendoorlogistics.components.tables.creator.CreateTablesComponent";
	}

	@Override
	public String getName() {
		return "Create tables";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		return null;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret =api.tables().createAlterableDs();
		for(ODLTableDefinition table : ((DatastoreConfig)configuration).getTables()){
			if(TableUtils.findTable(ret, table.getName())==null){
				DatastoreCopier.copyTableDefinition(table, ret);
			}
		}
		return ret;
	}

	@Override
	public void execute(ComponentExecutionApi reporter,int mode,Object configuration, ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {
		// execute does nothing as the tables will already be made by the executor engine!! 
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return DatastoreConfig.class;
	}
	

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory,int mode,Serializable config, boolean isFixedIO) {
		return new DatastoreEditorPanel((DatastoreConfig)config,false);
	}

	@Override
	public void registerScriptTemplates(final ScriptTemplatesBuilder templatesApi) {
		class Helper{
			void create(String s, int nb){
				DatastoreConfig conf = new DatastoreConfig();
				for(int i = 1 ; i <=nb ; i++){
					ODLTableDefinitionImpl table = new ODLTableDefinitionImpl(-1, "Table" + i);
					conf.getTables().add(table);
				}
				templatesApi.registerTemplate(s, s, s,getIODsDefinition(templatesApi.getApi(), conf), conf, ScriptTemplatesBuilder.STANDARD_FLAGS & (~ScriptTemplatesBuilder.FLAG_OUTPUT_DATASTORE_IS_FIXED),ODLComponent.MODE_DEFAULT);
			}
		}
		Helper helper = new Helper();
		helper.create("Create one table", 1);
		helper.create("Create two tables", 2);
		helper.create("Create three tables", 3);
	}
	/**
	 * Create template config for creating the input datastore
	 * @param ds
	 * @return
	 */
	public static ODLWizardTemplateConfig createTemplateConfig(ODLDatastore<? extends ODLTableDefinition> ds){
		DatastoreConfig conf = new DatastoreConfig();
		for(int i = 0 ; i < ds.getTableCount() ; i++){
			ODLTableDefinition src = ds.getTableAt(i);
			ODLTableDefinitionImpl table = new ODLTableDefinitionImpl(src.getImmutableId(), src.getName());
			DatastoreCopier.copyTableDefinition(src, table);
			conf.getTables().add(table);
		}
		return new ODLWizardTemplateConfig("", "", "",null, conf, ScriptTemplatesBuilder.STANDARD_FLAGS & (~ScriptTemplatesBuilder.FLAG_OUTPUT_DATASTORE_IS_FIXED),ODLComponent.MODE_DEFAULT,null);
	}
	
	@Override
	public long getFlags(ODLApi api,int mode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return Icons.loadFromStandardPath("table-add.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}

	@Override
	public Serializable createConfiguration(ODLApi api,ODLTableDefinition ...outputTables) {
		DatastoreConfig ret = new DatastoreConfig();
		for(ODLTableDefinition outputTable : outputTables){
			ODLTableDefinitionImpl impl = new ODLTableDefinitionImpl();
			DatastoreCopier.copyTableDefinition(outputTable, impl);
			impl.setName(outputTable.getName());
			ret.getTables().add(impl);
		}
		return ret;
	}


}
