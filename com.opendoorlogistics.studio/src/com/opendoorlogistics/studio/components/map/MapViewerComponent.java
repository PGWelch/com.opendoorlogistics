/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.utils.ui.Icons;

public abstract class MapViewerComponent implements Maps {
	//public static final String ID = "com.opendoorlogistics.studio.uicomponents.map";

	@Override
	public String getId() {
		return "com.opendoorlogistics.studio.uicomponents.map";
	}

	@Override
	public String getName() {
		return "Show map";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		return MapUtils.createEmptyDatastore();
	}

	@Override
	public ODLDatastore<ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return MapConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory, int mode, final Serializable config, boolean isFixedIO) {
		VerticalLayoutPanel ret = new VerticalLayoutPanel();
		final JCheckBox checkBox = new JCheckBox("Populate tooltip from adapter's tooltip field instead of object's field values", ((MapConfig)config).isUseCustomTooltips());
		checkBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				((MapConfig)config).setUseCustomTooltips(checkBox.isSelected());
			}
		});
		ret.add(checkBox);
		return ret;
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED| ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
	}


	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("Show map", "Show map of table", "Show map of table.", getIODsDefinition(templatesApi.getApi(), null),(Serializable) new MapConfig());

//		String name = "Show vehicle routes";
//		templatesApi.registerTemplate(name, name, name,getIODsDefinition(templatesApi.getApi(), null), new BuildScriptCallback() {
//
//			@Override
//			public void buildScript(ScriptOption builder) {
//				builder.setSynced(true);
//
//				String inputTableDatastore = builder.getApi().conventions().getSpreadsheetAdapterId();
//				String inputTableName = "stop-details";
//				if (builder.getInputTables().size()> 0) {
//					inputTableDatastore = builder.getInputTables().getSourceDatastoreId(0);
//					inputTableName = builder.getInputTables().getSourceTable(0).getName();
//				}
//
//				ScriptAdapter adapter = builder.addDataAdapter("MapInput");
//				final String id =adapter.getAdapterId();
//				
//				ODLDatastore<? extends ODLTableDefinition> ds = MapUtils.createEmptyDatastore();
//
//				// init lines
//				adapter.addSourcelessTable(ds.getTableAt(0));
//				adapter.setSourceTable(0, inputTableDatastore, inputTableName);
//
//				adapter.setSourceColumns(0, new String[][] { new String[] { PredefinedTags.LATITUDE, null }, new String[] { PredefinedTags.LONGITUDE, null }, new String[] { "geometry", PredefinedTags.INCOMING_PATH },
//						new String[] { "legendKey", PredefinedTags.VEHICLE_ID }, new String[] { "imageFormulaKey", PredefinedTags.VEHICLE_ID }, });
//
//				adapter.setFormulae(0, new String[][] { new String[] { "colour", "\"#787878\"" }, new String[] { "pixelWidth", "2" }, new String[] { "legendColour", "randcolour(\"" + PredefinedTags.VEHICLE_ID + "\")" }, });
//
//				adapter.setTableFilterFormula(0, "len(\"" + PredefinedTags.INCOMING_PATH + "\")>0");
//
//				// init points
//				adapter.addSourcelessTable(ds.getTableAt(0));
//				adapter.setSourceTable(1, inputTableName);
//				adapter.setSourceColumns(1, new String[][] { new String[] { PredefinedTags.LATITUDE, PredefinedTags.STOP_LATITUDE }, new String[] { PredefinedTags.LONGITUDE, PredefinedTags.STOP_LONGITUDE },
//						new String[] { "colourKey", null }, new String[] { "legendKey", PredefinedTags.VEHICLE_ID }, new String[] { "imageFormulaKey", PredefinedTags.VEHICLE_ID }, });
//
//				adapter.setFormulae(1, new String[][] { new String[] { "label", "if(\"is-depot\",\"depot\",\"stop-number\")" }, new String[] { "colour", "randcolour(\"" + PredefinedTags.VEHICLE_ID + "\")" },
//						new String[] { "pixelWidth", "10" }, new String[] { "drawOutline", "true" }, });
//
//				builder.addInstruction(id, getId(), ODLComponent.MODE_DEFAULT, null);
//			}
//		});
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("map-viewer-component.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}


	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition() {
		return getIODsDefinition(null, null);
	}

	@Override
	public void setCustomTooltips(boolean customTooltips, Serializable config){
		if(config!=null && MapConfig.class.isInstance(config)){
			((MapConfig)config).setUseCustomTooltips(customTooltips);
		}
	}

}
