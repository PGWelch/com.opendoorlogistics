/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.barchart;

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
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.barchart.basechart.BaseChartPanel;
import com.opendoorlogistics.components.barchart.basechart.BaseComponent;
import com.opendoorlogistics.components.barchart.basechart.BaseConfig;
import com.opendoorlogistics.utils.ui.Icons;

final public class BarchartComponent extends BaseComponent {
	
	@Override
	public String getId() {
		return "com.opendoorlogistics.components.barchart";
	}

	@Override
	public String getName() {
		return "Barchart";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = api.tables().createAlterableDs();
		ODLTableDefinitionAlterable table = ret.createTable("ChartData", -1);
		
		BarchartConfig config = (BarchartConfig) configuration;		
		addFilterGroupsToIODs(table, config);
		
		table.addColumn(-1, "Category", ODLColumnType.STRING, 0);
		
		for (int i = 1; i <= config.getSeriesNames().size(); i++) {
			table.addColumn(-1, "Series" + i, ODLColumnType.DOUBLE, 0);
		}
		return ret;
	}

	
	
	@Override
	public Class<? extends Serializable> getConfigClass() {
		return BarchartConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory, int mode, Serializable config, boolean isFixedIO) {
		return new BarchartConfigPanel((BarchartConfig) config);
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		for (int i = 1; i <= 4; i++) {
			String name = "Barchart with " + i + " series";
			BarchartConfig barchartConfig = new BarchartConfig();
			ArrayList<String> names = new ArrayList<>();
			for (int j = 1; j <= i; j++) {
				names.add("Series" + j);
			}
			barchartConfig.setSeriesNames(names);
			templatesApi.registerTemplate(name, name, name,getIODsDefinition(templatesApi.getApi(), barchartConfig), barchartConfig);
		}
	}



	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("office-chart-bar.png");
	}

	@Override
	protected BaseChartPanel createPanel(ODLApi api,BaseConfig config,
			ODLTableReadOnly table) {
		return new BarchartPanel(api,(BarchartConfig)config, table);
	}



}
