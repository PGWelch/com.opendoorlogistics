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
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.reports.ReporterComponent;
import com.opendoorlogistics.components.reports.ReporterConfig;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.utils.ui.Icons;

public abstract class AbstractMapViewerComponent implements Maps {
	public static final String COMPONENT_ID ="com.opendoorlogistics.studio.uicomponents.map"; 
	@Override
	public String getId() {
		return COMPONENT_ID;
	}

	@Override
	public String getName() {
		return "Show map";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		return getIODsDefinition(false);		
	}

	private ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(boolean activeOnly) {
		if(activeOnly){
			ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = ODLFactory.createDefinition();
			ODLTableDefinition dfn = DrawableObjectImpl.getBeanMapping().getDefinition().getTableAt(0);
			
			DatastoreCopier.copyTableDefinition(dfn, ret);
			return ret;		
			
		}
		else{
			return DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS;
		}

	}
	
	private static void makeOptional(ODLTableDefinitionAlterable alterable){
		alterable.setFlags(alterable.getFlags() | TableFlags.FLAG_IS_OPTIONAL);
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
		return ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED| ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING
				|ODLComponent.FLAG_DISABLE_FRAMEWORK_DATA_READ_FOR_DEPENDENCIES;
	}


	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		//templatesApi.registerTemplate("Show map", "Show map of table", "Show map of table.", getIODsDefinition(true),(Serializable) new MapConfig());

		// make script template not including background / foreground
		templatesApi.registerTemplate("Show map", "Show map", "Show map of table",getIODsDefinition(false), new BuildScriptCallback() {

			@Override
			public void buildScript(ScriptOption builder) {
				ScriptInputTables inputTables = builder.getInputTables();
				
				// add only the drawables table to the adapter
				ScriptAdapter mapAdapter = builder.addDataAdapter("Mapinput");
				for(int i =0 ; i< inputTables.size() ; i++){
					ODLTableDefinition src = inputTables.getSourceTable(i);
					ODLTableDefinition dest = inputTables.getTargetTable(i);
					String dsid = inputTables.getSourceDatastoreId(i);
					if(!Strings.equalsStd(dest.getName(), PredefinedTags.DRAWABLES)
					&& !Strings.equalsStd(dest.getName(), PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND)
					&& !Strings.equalsStd(dest.getName(), PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND)
					){
						continue;
					}
					
					
					
					if(src!=null){
						mapAdapter.addSourcedTableToAdapter(dsid, src, dest);
					}else if(Strings.equalsStd(dest.getName(), PredefinedTags.DRAWABLES)){
						mapAdapter.addSourcelessTable(dest);
					}
				}
				
				// now add the instruction
				builder.addInstruction(mapAdapter.getAdapterId(), getId(), ODLComponent.MODE_DEFAULT,new MapConfig());
				
			}
			
		});
		
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
		
//		templatesApi.registerTemplate("Reports", "Reports",  "Create reports, exporting to pdf, word, etc...", getIODsDefinition(templatesApi.getApi(), new ReporterConfig()),new BuildScriptCallback() {
//			
//			@Override
//			public void buildScript(ScriptOption builder) {
//				// build map adapter and add to top level option
//				ScriptAdapter mapAdapter = builder.addDataAdapter("Map");
//				mapAdapter.setName("Image data per row");
//				mapAdapter.setFlags(mapAdapter.getFlags() | TableFlags.FLAG_IS_DRAWABLES);
//				
//				String htmlHeader = "<html><body style='width: 300 px'>";
//				builder.setEditorLabel(htmlHeader + "The reporter component lets you generate reports containing text and map images and export them to pdf, html etc. See the online tutorials for more details.");
//				
//				// build report input adapter and add to top level option
//				ScriptAdapter adapter = builder.addDataAdapter("Report content data");
//				adapter.setName("Report content data");
//				final String reportInputId = adapter.getAdapterId();
//				
//				ScriptInputTables inputTables = builder.getInputTables();
//				for(int i=0 ; i<inputTables.size();i++){
//					if(inputTables.getSourceTable(i)!=null){
//						if(Strings.equalsStd(templatesApi.getApi().standardComponents().reporter().getHeaderMapTableName(), inputTables.getTargetTable(i).getName())){
//							// destination for header map is the drawables table
//							adapter.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),inputTables.getSourceTable(i), inputTables.getTargetTable(i));							
//						}else{
//							// destination for report is just the same as the source
//							adapter.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),inputTables.getSourceTable(i), inputTables.getSourceTable(i));
//						}
//					}
//				}
//			
//				ScriptInstruction instruction = builder.addInstruction(reportInputId, getId(), ReporterComponent.MODE_GENERATE_REPORTS,  new ReporterConfig());
//				instruction.setName("Export & processing options");
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
	public void setCustomTooltips(boolean customTooltips, Serializable config){
		if(config!=null && MapConfig.class.isInstance(config)){
			((MapConfig)config).setUseCustomTooltips(customTooltips);
		}
	}


	@Override
	public ODLDatastore<? extends ODLTableDefinition> getLayeredDrawablesDefinition(){
		return getIODsDefinition(false);
	}


	@Override
	public ODLTableDefinition getDrawableTableDefinition(){
		return getIODsDefinition(true).getTableAt(0);
	}
	
	@Override
	public boolean isBackgroundMapRenderedOffline(){
		return BackgroundTileFactorySingleton.getFactory().isRenderedOffline();
	}
}
