/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.tables;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.standardcomponents.TableViewer;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.panels.TableViewerPanel;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions;
import com.opendoorlogistics.studio.tables.grid.adapter.RowStyler;
import com.opendoorlogistics.utils.ui.Icons;

public class TableControlComponent implements TableViewer {

	@XmlRootElement
	public static class QueryTableConfig implements Serializable {
		private boolean showTableControl = true;

		public boolean isShowTableControl() {
			return showTableControl;
		}

		@XmlAttribute
		public void setShowTableControl(boolean showTableControl) {
			this.showTableControl = showTableControl;
		}

	}

	// public static String ID = "com.opendoorlogistics.studio.uicomponents.showtable";
	@Override
	public String getName() {
		return "Show table";
	}

	protected String panelName() {
		return "TableGrid";
	}

	@Override
	public String getId() {
		return "com.opendoorlogistics.studio.uicomponents.showtable";
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED | ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		ODLTableDefinitionAlterable table = ret.createTable("Table",-1);
		table.setFlags(table.getFlags() | TableFlags.FLAG_COLUMN_WILDCARD | TableFlags.FLAG_TABLE_NAME_WILDCARD | TableFlags.FLAG_TABLE_NAME_USE_SOURCE);
		return ret;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = api.tables().createDefinitionDs();
		ret.setFlags(ret.getFlags() | TableFlags.FLAG_TABLE_WILDCARD);
		return ret;
	}

	@Override
	public void execute(final ComponentExecutionApi reporter, int mode, final Object configuration, final ODLDatastore<? extends ODLTable> ioDb, final ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {
		//System.out.println(ioDb.getTableAt(0).getName());
		/**
		 * Show each table provided...
		 */
		if (configuration==null || ((QueryTableConfig) configuration).isShowTableControl()) {
			reporter.submitControlLauncher(new ControlLauncherCallback() {

				@Override
				public void launchControls(ComponentControlLauncherApi launcherApi) {
						final ODLTableReadOnly table = ioDb.getTableAt(0);
						if (table != null) {
							// check for row-colour column ?
							final int colourColIndx = TableUtils.findColumnIndx(table, PredefinedTags.ROW_FONT_COLOUR);
							if (colourColIndx != -1) {
								// create an adapted datastore without the column
								Tables tableApis = launcherApi.getApi().tables();
								ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> tmpDs = tableApis.createDefinitionDs();
								ODLTableDefinitionAlterable tmpTable = tableApis.copyTableDefinition(table, tmpDs);
								tmpTable.deleteColumn(colourColIndx);

								AdapterMapping mapping = AdapterMapping.createUnassignedMapping(tmpDs);
								mapping.setTableSourceId(tmpTable.getImmutableId(), 0, table.getImmutableId());
								for (int col = 0; col < tmpTable.getColumnCount(); col++) {
									int srcCol = col < colourColIndx ? col : col + 1;
									mapping.setFieldSourceIndx(tmpTable.getImmutableId(), col, srcCol);
								}

								ArrayList<ODLDatastore<? extends ODLTable>> list = new ArrayList<>();
								list.add(ioDb);
								AdaptedDecorator<ODLTable> decorator = new AdaptedDecorator<>(mapping, list);

								// create row style object
								RowStyler styler = new RowStyler() {

									@Override
									public Color getRowFontColour(long rowId) {
										Object value = table.getValueById(rowId, colourColIndx);
										if (value != null) {
											return Colours.toColour(value);
										}
										return null;
									}
								};

								launchControls(decorator, launcherApi, decorator.getTableAt(0), styler);

							} else {
								launchControls(ioDb, launcherApi, table, null);
							}

						}
				//	}
				}

				/**
				 * @param ioDb
				 * @param launcherApi
				 * @param table
				 * @param styler
				 */
				private void launchControls(final ODLDatastore<? extends ODLTable> ioDb, ComponentControlLauncherApi launcherApi, final ODLTableReadOnly table, RowStyler styler) {
					// get panel if already registered...
					String panelName = panelName() + " - " + table.getName();
					JPanel panel = launcherApi.getRegisteredPanel(panelName);

					boolean addTable = true;
					if (panel != null && TableViewerPanel.class.isInstance(panel)) {
						TableViewerPanel viewer = (TableViewerPanel) panel;
						viewer.replaceData(ioDb, table.getImmutableId(), styler);
						addTable = false;
					}

					if (addTable) {
						// don't bother using listeners as they don't work for adapted tables..
						TableViewerPanel viewer = new TableViewerPanel(ioDb, table.getImmutableId(), false, styler, launcherApi.getGlobalDatastore(), getPermissions(table));
						launcherApi.registerPanel(panelName, table.getName(), viewer, true);
					}
				}
			});
		}
	}


	protected GridEditPermissions getPermissions(ODLTableDefinition table) {
		return GridEditPermissions.create(table,false);
	}
	
	//protected abstract ODLDatastoreUndoable<ODLTableAlterable> getGlobalDatastore();

//	protected abstract HasInternalFrames getOwner();

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return QueryTableConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory, int mode, final Serializable config, boolean isFixedIO) {
		final JCheckBox checkBox = new JCheckBox("Show table control?" , ((QueryTableConfig)config).isShowTableControl());
		checkBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				((QueryTableConfig)config).setShowTableControl(checkBox.isSelected());
			}
		} );
		JPanel panel = new JPanel(); 
		panel.setLayout(new BorderLayout());
		panel.add(checkBox,BorderLayout.NORTH);
		return panel;

		// new TextEntryPanel("Show table with name" ,((ShowTableConfig)config).getTable() ,
		// "Enter the table name to show. If this is empty, the first table is shown.",
		// new TextEntryPanel.TextChangedListener(){
		//
		// @Override
		// public void textChange(String newText) {
		// ((ShowTableConfig)config).setTable(newText);
		// }
		//
		// });
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("Show table view", "Show table view",
				"Show view of the table which can include filtering, calculated fields etc.",
				getIODsDefinition(templatesApi.getApi(), null),
				new BuildScriptCallback(){

					@Override
					public void buildScript(ScriptOption builder) {
						// build adapter first
						ScriptAdapter adapter = builder.addDataAdapter("QueryInput");
						
						// add table to adapter
						String outputName="";
						ScriptInputTables input = builder.getInputTables();
						if(input.size()>0 && input.getSourceTable(0)!=null){
							outputName = "Query " + input.getSourceTable(0).getName();
							adapter.addSourcedTableToAdapter(input.getSourceDatastoreId(0), input.getSourceTable(0), input.getSourceTable(0));
						}else{
							outputName = "Query table";
							adapter.addEmptyTable(outputName);
						}
						
						// create show instruction
						builder.addInstruction(adapter.getAdapterId(), getId(), ODLComponent.MODE_DEFAULT,new QueryTableConfig());
						
						// create output
						builder.addCopyTable(adapter.getAdapterId(), adapter.getTable(0).getTableDefinition().getName()	, OutputType.DO_NOT_OUTPUT, outputName);
					}
			
		});
	}
	
	
	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return Icons.loadFromStandardPath("show-table-component.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}

}
