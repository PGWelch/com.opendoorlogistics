/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.UpdateTable;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

/**
 * Update component is a more-or-less dummy component which signals to the script executor that an update query is being done. 
 * We use a 'dummy' component as various parts of the UI use the adapter-component model.
 * 
 * @author Phil
 * 
 */
public final class UpdateQueryComponent implements ODLComponent , UpdateTable{

	@XmlRootElement(name = "UpdateQuery")
	public static class UpdateQueryConfig implements Serializable {
		private boolean isDeleteQuery;

		public boolean isDeleteQuery() {
			return isDeleteQuery;
		}

		@XmlAttribute(name = "IsDeleteQuery")
		public void setDeleteQuery(boolean isDeleteQuery) {
			this.isDeleteQuery = isDeleteQuery;
		}

	}

	@Override
	public String getId() {
		return "com.opendoorlogistics.core.components.UpdateQueryComponent";
	}

	@Override
	public String getName() {
		return "Update table";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinition> ret = ODLDatastoreImpl.alterableFactory.create();
		ret.setFlags(ret.getFlags() | TableFlags.FLAG_TABLE_WILDCARD);
		return ret;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(ComponentExecutionApi reporter,int mode,Object configuration, ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb
			) {

		// Only one input table will be provided to this component as the engine will split a multi-table
		// adapter up into single tables for the execute call.
		UpdateQueryConfig uqc = (UpdateQueryConfig) configuration;
		ODLTable table = ioDb.getTableAt(0);
		if (uqc.isDeleteQuery()) {
			TableUtils.removeAllRows(table);
		} else {
	
			UpdateTimer timer = new UpdateTimer(250);
			// The engine will have specially processed the input table so all the component needs to do is
			// read from field i and copy this value to field i+1, for i=0, 2, 4, etc.
			int rows = table.getRowCount();
			int cols = table.getColumnCount();
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols - 1; col += 2) {
					Object val = table.getValueAt(row, col);
					table.setValueAt(val, row, col + 1);
				}
				
				if(reporter.isCancelled()){
					return;
				}
				
				if(timer.isUpdate()){
					reporter.postStatusMessage("Now updating row " + (row+1) + " / " + rows);
				}
			}
		}
		
		reporter.postStatusMessage("Finished updating rows");
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return UpdateQueryConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory,int mode,Serializable config, boolean isFixedIO) {
		VerticalLayoutPanel ret = new VerticalLayoutPanel();
		final UpdateQueryConfig uqc = (UpdateQueryConfig) config;
		final JCheckBox checkBox = new JCheckBox("Delete query (deletes all selected rows)", uqc.isDeleteQuery());
		checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				uqc.setDeleteQuery(checkBox.isSelected());
			}
		});
		ret.add(checkBox);
		return ret;
	}

//	@Override
//	public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
//		return Arrays.asList(new ODLWizardTemplateConfig("Update table", "Update table",
//				"Update a table by copying values between columns or providing formulae to calculate values.", null));
//	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("Update table", "Update table",
				"Update a table by copying values between columns or providing formulae to calculate values.",
				getIODsDefinition(templatesApi.getApi(), new UpdateQueryConfig()),
				new UpdateQueryConfig());
	}
	
	@Override
	public long getFlags(ODLApi api,int mode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Icon getIcon(ODLApi api,int mode) {
		// Use own class loader to prevent problems when jar loaded by reflection
		return new ImageIcon(this.getClass().getResource("/resources/icons/update-query.png"));
	}
	
	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}

	@Override
	public Serializable createConfig(boolean isDelete) {
		UpdateQueryConfig ret = new UpdateQueryConfig();
		ret.setDeleteQuery(isDelete);
		return ret;
	}



}
