/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.swing.Action;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.api.impl.scripts.ScriptInputTablesImpl;
import com.opendoorlogistics.core.api.impl.scripts.ScriptTemplatesImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator.ScriptGeneratorInput;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.scripts.componentwizard.SetupComponentWizard;
import com.opendoorlogistics.studio.scripts.editor.adapters.QueryAvailableTables;
import com.opendoorlogistics.utils.ui.ODLAction;

final public class ScriptWizardActions {
	private final ODLApi api;
	private final Window parent;
	private final QueryAvailableTables queryAvailableTables;
	
	public static interface WizardActionsCallback {
		void onNewScript(Script script);
		// void onChooseNextLevelAction(List<ODLAction> actions);
	}

	public ScriptWizardActions(ODLApi api, Window parent, QueryAvailableTables queryAvailableTables) {
		this.api = api;
		this.parent = parent;
		this.queryAvailableTables = queryAvailableTables;
	}

//	protected List<ODLAction> createComponentConfigActions(final ODLApi api, final Window parent, final ODLComponent component, final Iterable<ODLWizardTemplateConfig> configs, final ODLDatastore<? extends ODLTableDefinition> externalDs,
//			final int selectedTableId, final WizardActionsCallback cb) {
//		ArrayList<ODLAction> ret = new ArrayList<>();
//		for (final ODLWizardTemplateConfig config : configs) {
//			ODLAction action = new ODLAction(config.getName()) {
//
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					createScript(component, config, externalDs, selectedTableId, cb);
//				}
//
//			};
//
//			// add icon to action
//			if (component.getIcon(api, config.getExecutionMode()) != null) {
//				action.putValue(Action.SMALL_ICON, component.getIcon(api, config.getExecutionMode()));
//			}
//
//			if (config.getDescription() != null) {
//				action.putValue(Action.SHORT_DESCRIPTION, config.getDescription());
//				action.putValue(Action.LONG_DESCRIPTION, config.getDescription());
//			}
//			ret.add(action);
//		}
//		return ret;
//	}

	public List<ODLAction> createComponentActions( final WizardActionsCallback cb) {
		ArrayList<ODLAction> ret = new ArrayList<>();

		// add actions for each component
		for (final ODLComponent component : ODLGlobalComponents.getProvider()) {
			final Iterable<ODLWizardTemplateConfig> templates = ScriptTemplatesImpl.getTemplates(api, component);
			if (templates == null) {
				continue;

			}
			final List<ODLWizardTemplateConfig> list = IteratorUtils.toList(templates);
			if (list.size() > 0) {

				// create action
				ODLAction action = new ODLAction(component.getName()) {

					@Override
					public void actionPerformed(ActionEvent e) {
						createScript(component, cb);
						
//						if (list.size() > 1) {
//							ButtonTableDialog btd = new ButtonTableDialog(parent, "Select configuration", createComponentConfigActions(api, parent, component, list, externalDs, selectedTableId, cb));
//							btd.showModal();
//						} else {
//						}
					}
				};

				// add icon to action
				if (component.getIcon(api, ODLComponent.MODE_DEFAULT) != null) {
					action.putValue(Action.SMALL_ICON, component.getIcon(api, ODLComponent.MODE_DEFAULT));
				}

				// add to list
				ret.add(action);
			}
		}

		// sort actions by their name
		Collections.sort(ret, new Comparator<Action>() {

			private String name(Action action) {
				return action.getValue(Action.NAME).toString().toLowerCase();
			}

			@Override
			public int compare(Action o1, Action o2) {
				return name(o1).compareTo(name(o2));
			}

		});

		return ret;
	}

	protected void createScript(final ODLComponent component,final WizardActionsCallback cb) {
		SetupComponentWizard wizard= new SetupComponentWizard(parent, api, queryAvailableTables);
		wizard.getData().setComponent(component.getId());
		Script script = wizard.showModal(); // createScriptFromMasterComponent(api, parent, component, config, externalDs, selectedTableId != -1 ? new int[] { selectedTableId } : new int[0]);

		if (script != null) {
			cb.onNewScript(script);
		}
	}

//	public Script promptUser() {
//		class Ret {
//			Script ret;
//		}
//		final Ret ret = new Ret();
//		WizardActionsCallback cb = new WizardActionsCallback() {
//
//			// @Override
//			// public void onChooseNextLevelAction(List<ODLAction> actions) {
//			// ButtonTableDialog btd = new ButtonTableDialog(parent, "Select configuration",actions);
//			// btd.showModal();
//			// }
//
//			@Override
//			public void onNewScript(Script script) {
//				ret.ret = script;
//			}
//		};
//
//		ButtonTableDialog btd = new ButtonTableDialog(parent, "Select script wizard type:", createComponentActions(null, -1, cb));
//		btd.showModal();
//		return ret.ret;
//	}

	public static Script createScriptFromMasterComponent(ODLApi api, final Window parent, ODLComponent component, ODLWizardTemplateConfig config, ODLDatastore<? extends ODLTableDefinition> externalDs, int[] selectedTableIds) {

//		// create temporary datastore with selected tables
//		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> selected = api.tables().createDefinitionDs();
//		if (selectedTableIds != null && externalDs != null) {
//			for (int i : selectedTableIds) {
//				ODLTableDefinition srcTable = externalDs.getTableByImmutableId(i);
//				if (srcTable != null) {
//					api.tables().copyTableDefinition(srcTable, selected);
//				}
//			}
//		}

		// turn selected tables into input data structure
		ODLDatastore<? extends ODLTableDefinition> expected = config.getExpectedIods();
		ScriptInputTablesImpl inputTables = new ScriptInputTablesImpl();		
		if (expected != null) {
			HashSet<Integer> usedTableIds = new HashSet<>();			
			if (expected.getTableCount() == 1 && externalDs != null && selectedTableIds.length == 1) {
				// only 1 possibility so match up 
				inputTables.add(ScriptConstants.EXTERNAL_DS_NAME, externalDs.getTableByImmutableId(selectedTableIds[0]), expected.getTableAt(0));
				usedTableIds.add(selectedTableIds[0]);
			} else {
				// match up based on names
				ArrayList<Integer> matches = new ArrayList<>();
				int matchCount=0;
				for (int i = 0; i < expected.getTableCount(); i++) {
					int foundId=-1;
					if (externalDs != null && selectedTableIds != null) {
						for (int id : selectedTableIds) {
							if (Strings.equalsStd(expected.getTableAt(i).getName(), externalDs.getTableByImmutableId(id).getName())) {
								foundId=id;
								matchCount++;
								break;
							}
						}
					}
					matches.add(foundId);
				}
				
				if(matchCount>0){
					// use matches if we found them
					for (int i = 0; i < expected.getTableCount(); i++) {
						int matchedId = matches.get(i);
						ODLTableDefinition source = null;
						if(matchedId!=-1){
							source = externalDs.getTableByImmutableId(matchedId);	
							usedTableIds.add(matchedId);								
						}
						inputTables.add(ScriptConstants.EXTERNAL_DS_NAME, source, expected.getTableAt(i));
					}
				}else{
					// if no matches then just fill in order... this gives sensible results in some circumstances
					int nbSel = selectedTableIds!=null?selectedTableIds.length:0;
					for (int i = 0; i < expected.getTableCount(); i++) {
						ODLTableDefinition source =null;
						if(i<nbSel){
							source = externalDs.getTableByImmutableId(selectedTableIds[i]);
							usedTableIds.add(selectedTableIds[i]);
						}
						inputTables.add(ScriptConstants.EXTERNAL_DS_NAME, source, expected.getTableAt(i));
					}
				}
			

			}
			
			// add any non-used ids if we allow wildcards
			if(TableUtils.hasFlag(expected, TableFlags.FLAG_TABLE_WILDCARD) && externalDs!=null && selectedTableIds!=null){
				for(int id:selectedTableIds){
					if(usedTableIds.contains(id)==false){
						ODLTableDefinition src = externalDs.getTableByImmutableId(id);
						inputTables.add(ScriptConstants.EXTERNAL_DS_NAME,src,src);						
					}
				}
			}
		}

		return ScriptGenerator.generate(api,new ScriptGeneratorInput(component, config, inputTables));
	}

	// protected WizardOptionChooseCallback createOptionsCB(final Window parent) {
	// return new WizardOptionChooseCallback() {
	//
	// @Override
	// public int selectOption(String message,String []options, String [] tooltips) {
	// ButtonTableDialog dlg = new ButtonTableDialog(parent,message!=null?message: "Select wizard option:", options);
	// if(dlg.showModal() == ButtonTableDialog.OK_OPTION){
	// return dlg.getSelectedIndex();
	// }
	// return -1;
	// }
	// };
	// }
}
