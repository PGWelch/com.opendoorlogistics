/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.linegraph;

import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.LineGraph;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.barchart.basechart.BaseChartConfigPanel;
import com.opendoorlogistics.components.barchart.basechart.BaseChartPanel;
import com.opendoorlogistics.components.barchart.basechart.BaseComponent;
import com.opendoorlogistics.components.barchart.basechart.BaseConfig;
import com.opendoorlogistics.utils.ui.Icons;

final public class LineGraphComponent extends BaseComponent implements LineGraph{
	
	@Override
	public String getId() {
		return "com.opendoorlogistics.components.linegraph";
	}

	@Override
	public String getName() {
		return "Linegraph";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = api.tables().createAlterableDs();
		ODLTableDefinitionAlterable table = ret.createTable("LineData", -1);
		
		BaseConfig config = (BaseConfig) configuration;		
		addFilterGroupsToIODs(table, config);

		int keyCol=table.addColumn(-1,getColumnName(LGColumn.Key), ODLColumnType.STRING, 0);
		table.setColumnFlags(keyCol, table.getColumnFlags(keyCol) | TableFlags.FLAG_IS_OPTIONAL);
		table.addColumn(-1, getColumnName(LGColumn.X), ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, getColumnName(LGColumn.Y), ODLColumnType.DOUBLE, 0);
		

		return ret;
	}



	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		return null;
	}

	
	@Override
	public Class<? extends Serializable> getConfigClass() {
		return BaseConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory, int mode, Serializable config, boolean isFixedIO) {
		return new BaseChartConfigPanel((BaseConfig) config);
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		for(int nbFilter=0; nbFilter <=3 ; nbFilter++){
			String name = "Line graph with " + nbFilter + " filter group levels";
			BaseConfig config = new BaseConfig();
			config.setNbFilterGroupLevels(nbFilter);
			config.setXLabel("x");
			config.setYLabel("y");
			templatesApi.registerTemplate(name, name, name,getIODsDefinition(templatesApi.getApi(), config), config);
		}
	}



	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("linegraph-component.png");
	}

	@Override
	protected BaseChartPanel createPanel(ODLApi api,BaseConfig config,
			ODLTableReadOnly table) {
		return new LineGraphPanel(api,config, table); 
	}

	@Override
	public void setTitle(String title, Serializable config) {
		((BaseConfig)config).setTitle(title);
	}

	@Override
	public void setXLabel(String title, Serializable config) {
		((BaseConfig)config).setXLabel(title);
	}

	@Override
	public void setYLabel(String title, Serializable config) {
		((BaseConfig)config).setYLabel(title);
		
	}

	@Override
	public ODLTableDefinition getInputTableDefinition(ODLApi api) {
		try {
			return getIODsDefinition(api, getConfigClass().newInstance()).getTableAt(0);
		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public String getColumnName(LGColumn col) {
		switch(col){
		case Key:
			return "Key";
			
		case X:
			return "X";
			
		case Y:
			return "Y";
		}
		return null;
	}

	


}
