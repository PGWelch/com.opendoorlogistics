/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.componentwizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.codefromweb.BoundsPopupMenuListener;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.api.impl.scripts.ScriptInputTablesImpl;
import com.opendoorlogistics.core.api.impl.scripts.ScriptTemplatesImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.TargetIODsInterpreter;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.utils.TableId;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator.ScriptGeneratorInput;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.scripts.editor.adapters.QueryAvailableTables;

public class SetupComponentWizard extends JDialog {
	final private VerticalLayoutPanel panel = new VerticalLayoutPanel();
	final private ODLApi api;
	final private QueryAvailableTables queryAvailableTables;
	boolean userSelectedOk=false;
	private boolean mergeWithInputOption=false;
	private ComponentWizardData data = new ComponentWizardData();


	/**
	 * @param api
	 * @param queryAvailableTables
	 * @param data
	 * @return
	 */
	public static ScriptGeneratorInput getScriptGeneratorInput(ODLApi api, QueryAvailableTables queryAvailableTables, ComponentWizardData data) {
		// get the selected component from the wizard data
		ODLComponent component = getComponent(data);
		if(component==null){
			throw new RuntimeException("Invalid component wizard data. Component cannot be found: " + data.getComponent());			
		}
		
		// get the component's selected template from the wizard data 
		List<ODLWizardTemplateConfig> configs = getTemplates(api, data);
		if(configs==null || configs.size()==0 || data.getTemplateIndex()>=configs.size() || data.getTemplateIndex()<0){
			throw new RuntimeException("Invalid component wizard data. Incorrect template index.");
		}
		ODLWizardTemplateConfig config = configs.get(data.getTemplateIndex());
		
		// Construct the input tables object from the wizard data and the component's expected input		
		ScriptInputTablesImpl inputTables = new ScriptInputTablesImpl();
		ODLDatastore<? extends ODLTableDefinition> iods = config.getExpectedIods();
		for(TableId tableId : data.getTableIds()){
			ODLTableDefinition source =null;
			if(tableId!=null && tableId.isNull()==false){
				source =queryAvailableTables.getTableDefinition(tableId.getDsId(), tableId.getTableName());
				if(source==null){
					throw new RuntimeException("Invalid component wizard data. Table cannot be found: " + tableId);				
				}				
			}
			
			ODLTableDefinition target=null;
			if(iods!=null && inputTables.size() < iods.getTableCount()){
				target = iods.getTableAt(inputTables.size());
			}
			
			inputTables.add(tableId.getDsId(), source, target);
		}
		
		ScriptGeneratorInput sgi = new ScriptGeneratorInput(component, config, inputTables);
		return sgi;
	}
	
//	public Script generateScript(){
//		return generateScript(api, queryAvailableTables, data);
//	}
//	
	public SetupComponentWizard(Window parent,ODLApi api, QueryAvailableTables queryAvailableTables) {
		super(parent,ModalityType.APPLICATION_MODAL);
		this.api = api;
		this.queryAvailableTables = queryAvailableTables;
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(new JScrollPane(panel));
		//setContentPane();
		setTitle("Setup component wizard");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);
		build(true);
		if(parent!=null){
			setLocationRelativeTo(parent);
		}
	}

	// private static Border createDefaultBorder() {
	// return BorderFactory.createEmptyBorder(5, 5, 5, 5);
	// }

	void build(boolean firstBuild) {
		panel.removeAll();

		buildComponentList();
		buildSelectTemplateConfig();
		buildSelectTables();
		
		// add ok & cancel buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		Dimension buttonSize = new Dimension(80, 26);
		JButton ok = new JButton(new AbstractAction("Ok") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				userSelectedOk = true;
				dispose();
			}
		});
		setSizes(ok, buttonSize);
		buttonPanel.add(ok);
		
		JButton cancel = new JButton(new AbstractAction("Cancel"){

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
			
		});
		setSizes(cancel, buttonSize);
		buttonPanel.add(cancel);
		
		panel.add(buttonPanel);
		

		//Rectangle bounds = getBounds();
		pack();

		if (!firstBuild) {
		//	setBounds(bounds);
		}else{
			//setMinimumSize(new Dimension(300, 300));
		}		
	}

	private static void setSizes(Component component, Dimension size){
		component.setMinimumSize(size);
		component.setMaximumSize(size);
		component.setPreferredSize(size);
		component.setSize(size);
	}
	
	private void buildSelectTables() {
		// get component
		ODLComponent component = getComponent(data);
		if (component == null) {
			return;
		}

		// get template, checking its set
		List<ODLWizardTemplateConfig> templates = getTemplates(api,data);
		if (templates == null || data.getTemplateIndex() < 0 || data.getTemplateIndex() >= templates.size()) {
			return;
		}
		ODLWizardTemplateConfig template = templates.get(data.getTemplateIndex());

		// get expected iods and nb of tables
		final ODLDatastore<? extends ODLTableDefinition> iods = template.getExpectedIods();
		TargetIODsInterpreter interpreter = new TargetIODsInterpreter(api);
		Pair<Integer, Integer> nbTablesRange = interpreter.getNbTablesRange(iods);

		// validate list
		while (data.getTableIds().size() > nbTablesRange.getSecond()) {
			data.getTableIds().remove(data.getTableIds().size() - 1);
		}
		while (data.getTableIds().size() < nbTablesRange.getFirst()) {
			data.getTableIds().add(new TableId(null, null));
		}
		for(int i =0 ; i<data.getTableIds().size();i++){
			if(data.getTableIds().get(i)==null){
				data.getTableIds().set(i, new TableId(null, null));
			}
		}
		
		// replace any null with same name matches if we have them
		if(queryAvailableTables!=null){
			for(int i =0 ; i<data.getTableIds().size();i++){
				TableId current  =data.getTableIds().get(i);
				if(current.isNull() && iods!=null && i<iods.getTableCount()){
					String targetName = iods.getTableAt(i).getName();
					
					OUTER_LOOP:
					for(String datastore:queryAvailableTables.queryAvailableDatastores()){
						for(String tableName : queryAvailableTables.queryAvailableTables(datastore)){
							if(Strings.equalsStd(tableName, targetName)){
								current.setDsId(datastore);
								current.setTableName(tableName);;
								break OUTER_LOOP;
							}
						}
					}
				}
			}			
		}
		
		// return if there's nothing to set
		if (nbTablesRange.getSecond() == 0) {
			return;
		}

//		// helper class to get the 
//		class TableIdToString {
//			String toString(TableId tableId) {
//				if (tableId == null || tableId.isNull()) {
//					return "<not set>";
//				}
//				return tableId.toString();
//			}
//		}
//		final TableIdToString tableIdToString = new TableIdToString();

		// setup combo box class
		class MyComboBox extends JComboBox<TableId> {
			final int index;

			public MyComboBox(int index) {
				super(getTableIds());
				this.index = index;
				setEditable(false);
				setRenderer(new DefaultListCellRenderer() {
					@Override
					public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
						Component ret = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
						TableId tableId = (TableId)value;
						if (tableId == null || tableId.isNull()) {
							setText( "<not set>");
						}else{
							setText(tableId.toString());							
						}
						return ret;
					}
				});
				
				addPopupMenuListener(new BoundsPopupMenuListener(true, false));
				
				addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						// refill popup menu dynamically
						removeAllItems();
						for (TableId tableId : getTableIds()) {
							addItem(tableId);
						}
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						// TODO Auto-generated method stub

					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						// TODO Auto-generated method stub

					}
				});
			}

		}

		// add combo box for each table
		for (int i = 0; i < data.getTableIds().size(); i++) {
			boolean isDefined = i < iods.getTableCount();
			if (isDefined) {
				ODLTableDefinition table = iods.getTableAt(i);
				panel.add(new JLabel("<html>Input table for <em>" + table.getName() + "</em>" + (TableUtils.isTableOptional(table) ? " (optional)" : "") + ":</html>"));
			} else {
				panel.add(new JLabel("<html>Input table:</html>"));
			}
			panel.addHalfWhitespace();
			
			final MyComboBox combo = new MyComboBox(i);
			combo.setSelectedItem(data.getTableIds().get(i));
			
			combo.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent e) {
					TableId selected = (TableId)combo.getSelectedItem();
					data.getTableIds().set(combo.index, selected);
				}
			});
			
			if(isDefined){
				panel.add(combo);				
			}else{
				// wildcard tables can be removed
				JButton removeButton = new JButton(new AbstractAction("Remove") {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						data.getTableIds().remove(combo.index);
						build(false);
					}
				});
				panel.addLine(combo, removeButton);						
			}
			
			panel.addWhitespace();
		}

		// add 'add a table' button
		if(TableUtils.hasFlag(iods, TableFlags.FLAG_TABLE_WILDCARD)){
			JButton button=new JButton(new AbstractAction("Add a new table") {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					data.getTableIds().add(new TableId(null, null));
					build(false);
				}
			});
			panel.addWrapped(button, BorderLayout.WEST);
			panel.addWhitespace();
		}
		
	

	}

	/**
	 * 
	 */
	private void buildComponentList() {
		class ComponentIcon {
			final ODLComponent component;
			final Icon icon;

			public ComponentIcon(ODLComponent component) {
				this.component = component;
				this.icon = component.getIcon(api, ODLComponent.MODE_DEFAULT);
			}
		}
		ArrayList<ComponentIcon> cis = new ArrayList<>();
		for (ODLComponent component : ODLGlobalComponents.getProvider()) {
			cis.add(new ComponentIcon(component));
		}

		final JComboBox<ComponentIcon> combo = new JComboBox<>(cis.toArray(new ComponentIcon[cis.size()]));
		combo.setEditable(false);
		combo.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component ret = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				ComponentIcon ci = (ComponentIcon) value;
				setIcon(ci.icon);
				setText(ci.component.getName());
				return ret;
			}
		});
		combo.addPopupMenuListener(new BoundsPopupMenuListener(true, false));

		// select the current component...
		boolean hasSelection = false;
		for (int i = 0; i < combo.getModel().getSize(); i++) {
			ComponentIcon ci = combo.getModel().getElementAt(i);
			if (Strings.equalsStd(ci.component.getId(), data.getComponent())) {
				hasSelection = true;
				combo.setSelectedIndex(i);
			}
		}

		// set first if nothing selected
		if (hasSelection == false && combo.getModel().getSize() > 0) {
			data.setComponent(combo.getModel().getElementAt(0).component.getId());
			combo.setSelectedIndex(0);
		}

		// add a selection changed listener
		combo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				ComponentIcon ci = (ComponentIcon) combo.getSelectedItem();
				if (ci != null) {
					data.setComponent( ci.component.getId());
				} else {
					data.setComponent(null);
				}

				// rebuild dialog
				build(false);
			}
		});

		panel.add(new JLabel("<html>Select component:</html>"));
		panel.addHalfWhitespace();
		panel.add(combo);
		panel.addWhitespace();
	}

	private void buildSelectTemplateConfig() {
		List<ODLWizardTemplateConfig> list = getTemplates(api,data);
		if (list == null) {
			return;
		}

		// check if no choice is needed
		if (list.size() < 2) {
			data.setTemplateIndex(0);
			return;
		}

		// validate the index
		if (data.getTemplateIndex()<0 || data.getTemplateIndex() >= list.size()) {
			data.setTemplateIndex(0);
		}
		
		class TemplateItem {
			String s;
			int index;
		}

		TemplateItem[] items = new TemplateItem[list.size()];
		for (int i = 0; i < list.size(); i++) {
			items[i] = new TemplateItem();
			items[i].s = list.get(i).getName();
			items[i].index = i;
		}

		// create combo
		final JComboBox<TemplateItem> combo = new JComboBox<>(items);
		combo.addPopupMenuListener(new BoundsPopupMenuListener(true, false));

		// set renderer
		combo.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component ret = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				TemplateItem item = (TemplateItem) value;
				setText(item.s);
				return ret;
			}
		});

		// set selected
		for (int i = 0; i < combo.getModel().getSize(); i++) {
			TemplateItem item = combo.getModel().getElementAt(i);
			if (item.index == data.getTemplateIndex()) {
				combo.setSelectedIndex(i);
			}
		}

		// add a selection changed listener
		combo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				TemplateItem ci = (TemplateItem) combo.getSelectedItem();
				if (ci != null) {
					data.setTemplateIndex( ci.index);
				} else {
					data.setTemplateIndex( -1);
				}

				// rebuild dialog
				build(false);
			}
		});

		panel.add(new JLabel("<html>Select component configuration:</html>"));
		panel.addHalfWhitespace();
		panel.add(combo);
		panel.addWhitespace();
	}

	/**
	 * @return
	 */
	private static List<ODLWizardTemplateConfig> getTemplates(ODLApi api,ComponentWizardData data) {
		ODLComponent component = getComponent(data);
		if (component == null) {
			return null;
		}

		Iterable<ODLWizardTemplateConfig> templates = ScriptTemplatesImpl.getTemplates(api, component);
		if (templates == null) {
			return null;
		}
		List<ODLWizardTemplateConfig> list = IteratorUtils.toList(templates);
		return list;
	}

	/**
	 * @return
	 */
	private static ODLComponent getComponent(ComponentWizardData data) {
		if (Strings.isEmpty(data.getComponent())) {
			return null;
		}

		ODLComponent component = ODLGlobalComponents.getProvider().getComponent(data.getComponent());
		return component;
	}

	public static void main(String[] args) {
		InitialiseStudio.initialise(false);
		
		final ODLDatastore<? extends ODLTableDefinition> ds = ExampleData.createTerritoriesExample(4);
		QueryAvailableTables queryAvailableTables = new QueryAvailableTables() {
			
			@Override
			public String[] queryAvailableTables(String datastore) {
				List<String>ret =  TableUtils.getTableNames(ds);
				return ret.toArray(new String[ret.size()]);
			}
			
			@Override
			public String[] queryAvailableDatastores() {
				return new String[]{ScriptConstants.EXTERNAL_DS_NAME};
			}

			@Override
			public ODLTableDefinition getTableDefinition(String datastore, String tablename) {
				return TableUtils.findTable(ds, tablename);
			}

			@Override
			public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition(String datastore) {
				if(Strings.equalsStd(ScriptConstants.EXTERNAL_DS_NAME, datastore)){
					return ds;
				}
				return null;
			}
		};
		
		new SetupComponentWizard(null,new ODLApiImpl(), queryAvailableTables).showModal();
		
		//ODLInternalFrame.showInDummyDesktopPane(new ComponentWizard(new ODLApiImpl(), queryAvailableTables));
	}

	// public static void main(String[] args) {
	// String title = "Desktop Sample";
	// JFrame frame = new JFrame(title);
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//
	// JDesktopPane desktop = new JDesktopPane();
	// JInternalFrame internalFrame = new JInternalFrame("Can Do All", true, true, true, true);
	//
	// desktop.add(internalFrame);
	// internalFrame.setBounds( 25, 25, 200, 100);
	//
	// internalFrame.setVisible(true);
	//
	// Container content = frame.getContentPane();
	// content.add(desktop, BorderLayout.CENTER);
	// frame.setSize(500, 300);
	// frame.setVisible(true);
	// }

	private TableId[] getTableIds() {
		ArrayList<TableId> ret = new ArrayList<>();
		
		// add 'not set'
		ret.add(new TableId(null, null));
		
		// add other tables
		if(queryAvailableTables!=null){
			for (String datastore : queryAvailableTables.queryAvailableDatastores()){
			for(String table:queryAvailableTables.queryAvailableTables(datastore)){
				ret.add(new TableId(datastore, table));
			}
		}}
		
		return ret.toArray(new TableId[ret.size()]);
	}

	public ComponentWizardData getData() {
		return data;
	}

	public void setData(ComponentWizardData data) {
		this.data = data;
		build(false);
	}
	
	public Script showModal(){
		return (Script)showModal(null, null);
	}
	
	public Option showModal(Script script, Option parent){
		// rebuild in case data has changed
		build(false);	
		setVisible(true);	
		if(userSelectedOk){
			ScriptGeneratorInput sgi = getScriptGeneratorInput(api, queryAvailableTables, data);
			Option option = ScriptGenerator.generate(api, script, parent, sgi);
			if(mergeWithInputOption && parent!=null){
				parent.getOptions().remove(option);
				parent.mergeIntoMe(option);
			}
			return option;
		}
		return null;
	}

	public boolean isMergeWithInputOption() {
		return mergeWithInputOption;
	}

	/**
	 * Merge the new option with the input parent option
	 * @param mergeWithInputOption
	 */
	public void setMergeWithInputOption(boolean mergeWithInputOption) {
		this.mergeWithInputOption = mergeWithInputOption;
	}
	
	
}
