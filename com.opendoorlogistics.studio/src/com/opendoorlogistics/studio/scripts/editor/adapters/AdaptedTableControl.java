/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.adapters;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.formulae.FmIsSelectedInMap;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.ODLDatastoreDefinitionProvider;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.controls.DynamicComboBox;
import com.opendoorlogistics.studio.controls.EditableComboBox.ValueChangedListener;
import com.opendoorlogistics.utils.ui.ODLAction;

public class AdaptedTableControl extends VerticalLayoutPanel {
	private final LabelledRadioButton noFilterCtrls;
	private final LabelledRadioButton selCtrls;
	private final CustomFormulaControls customFormulaControls;
	private final ArrayList<LabelledRadioButton> radioFilterCtrls = new ArrayList<>();
	private final AdaptedTableConfig config;
	private final QueryAvailableData queryAvailableFields;
	private final FromDatastoreCombo fromDatastore;
	private final FromTableCombo fromTable;
	private final JTextField outputTableName;
	private final AdapterTableDefinitionGrid fieldGrid;
	private final ODLApi api;
	private final long visibleTableFlags;
	private FormChangedListener formChangedListener;

	public static interface FormChangedListener{
		void formChanged(AdaptedTableControl form);
	}
	
	
	public void setFormChangedListener(FormChangedListener formChangedListener) {
		this.formChangedListener = formChangedListener;
	}

	private abstract class AutocorrectCombo extends DynamicComboBox<String> {

		public AutocorrectCombo(String initialValue) {
			super(initialValue, true,true);
			// TODO Auto-generated constructor stub
		}

		protected void updateAppearance() {
			Component component = getEditor().getEditorComponent();
			if (JComponent.class.isInstance(component)) {
				JComponent j = (JComponent) component;
				if (isUnknown() && !isImportLink()) {
					j.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 1),
							BorderFactory.createEmptyBorder(0, 4, 0, 0)));
				} else {
					j.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
				}
			}

		}

		protected abstract boolean isUnknown();
		
		protected abstract boolean isImportLink();
	}

	private class FromDatastoreCombo extends AutocorrectCombo {

		public FromDatastoreCombo(String initialValue) {
			super(initialValue);
		}

		@Override
		protected List<String> getAvailableItems() {
			ArrayList<String> ret = new ArrayList<>();
			if (queryAvailableFields != null) {
				String[] raw = queryAvailableFields.queryAvailableDatastores();
				if (raw != null) {

					// to do .. filter these

					for (String ds : raw) {
						ret.add(ds);
					}
				}
			}
			return ret;
		}

		@Override
		protected boolean isUnknown() {
			if (queryAvailableFields != null) {
				return Strings.containsStandardised(getValue(), Arrays.asList(queryAvailableFields.queryAvailableDatastores())) == false;
			}
			return false;
		}
		
		@Override
		public boolean isImportLink(){
			if(getValue()!=null){
				return getValue().contains(ScriptConstants.IMPORT_LINK_POSTFIX);
			}
			return false;
		}
	}

	private class FromTableCombo extends AutocorrectCombo {

		public FromTableCombo(String initialValue) {
			super(initialValue);
		}

		@Override
		protected List<String> getAvailableItems() {
			if (queryAvailableFields != null) {
				return asList(queryAvailableFields.queryAvailableTables(config.getFromDatastore()));
			}
			return new ArrayList<>();
		}

		@Override
		protected boolean isUnknown() {
			if (queryAvailableFields != null) {
				if (fromDatastore.isUnknown() == false) {
					if(queryAvailableFields.getDatastoreDefinition(fromDatastore.getValue())==null){
						// datastore may be known as its the external one (which is assumed to always exist),
						// however tables are not known
						return false;
					}
					
					if (queryAvailableFields.getTableDefinition(fromDatastore.getValue(), getValue()) == null) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		protected boolean isImportLink() {
			return false;
		}
	}

	private class CustomFormulaControls extends LabelledRadioButton{
		final JTextField text = new JTextField();

		@Override
		JPanel createLine() {
			return LayoutUtils.createHorizontalBoxLayout(createIndent(), radio, createHSpace(), label, createHSpace(), text);
		}

		CustomFormulaControls(AdaptedTableConfig tableConfig) {
			super("Filter using formula", "Filter the rows using a custom formula", RadioOption.CUSTOM);
			text.setText(tableConfig.getFilterFormula());
			// text.addKeyListener(createKeyListener());
			text.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					readFromForm();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					readFromForm();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					readFromForm();
				}
			});
			setMaxHeight(text, 30);
		}

		@Override
		void setEnabled(boolean enabled) {
			label.setEnabled(enabled);
			text.setEnabled(enabled);
		}
	}

	private class LabelledRadioButton{
		final JRadioButton radio = new JRadioButton();
		final JLabel label;
		final RadioOption type;
		
		LabelledRadioButton(String name, String tooltip, RadioOption type) {
			label = new JLabel(name);
			radio.setToolTipText(tooltip);
			label.setToolTipText(tooltip);
			this.type = type;
		}
		
		void updateEnabled(){
			setEnabled(radio.isSelected());
		}
		
		void setEnabled(boolean enabled) {
			label.setEnabled(enabled);
		}

		JPanel createLine() {
			return LayoutUtils.createHorizontalBoxLayout(createIndent(), radio, createHSpace(), label);
		}
	}

	
	private void setMaxHeight(Component component, int height) {
		component.setMaximumSize(new Dimension(component.getMaximumSize().width, height));
	}

	protected void readFromForm() {
		if (outputTableName != null) {
			config.setName(outputTableName.getText());
		}

		if (fromDatastore != null) {
			config.setFromDatastore(fromDatastore.getValue());
		}

		if (fromTable != null) {
			config.setFromTable(fromTable.getValue());
		}

		if (noFilterCtrls.radio.isSelected()) {
			config.setFilterFormula("");
		}
		else if(selCtrls.radio.isSelected()){
			config.setFilterFormula(new FmIsSelectedInMap().toString());
		}
		else if (customFormulaControls.radio.isSelected()) {
			config.setFilterFormula(customFormulaControls.text.getText());
		}

		updateAppearance();
		
		if(formChangedListener!=null){
			formChangedListener.formChanged(this);
		}
	}

	@SuppressWarnings("incomplete-switch")
	private void writeToForm() {
		// if(Strings.isEmpty(config.getFilterFormula())){
		// noFilterCtrls.radio.setSelected(true);
		// }else {
		// customFormulaControls.radio.setSelected(true);
		// }

		if (outputTableName != null) {
			outputTableName.setText(config.getName());
		}

		String text="";
		switch(getOption()){
				
			case SEL:
				text = new FmIsSelectedInMap().toString();
				break;
				
			case CUSTOM:
				text = config.getFilterFormula();
				break;
		};
		customFormulaControls.text.setText(text);
	}

	private Component createIndent() {
		return createHSpace(16);
	}

	private static Component createHSpace() {
		return createHSpace(5);
	}

	private static Component createHSpace(int width) {
		return Box.createRigidArea(new Dimension(width, 1));
	}

	private RadioOption getOption(){
		String s = config.getFilterFormula();
		if(Strings.isEmpty(config.getFilterFormula())){
			return RadioOption.ALL;
		}
		else if (Strings.equalsStd(new FmIsSelectedInMap().toString(), s)){
			return RadioOption.SEL;
		}else{
			return RadioOption.CUSTOM;
		}
	}
	
	@SuppressWarnings("serial")
	public AdaptedTableControl(ODLApi api,AdaptedTableConfig config, boolean includeTableName, boolean includeSourceControl,long visibleTableFlags, long visibleColumnFlags,
			QueryAvailableData availableOptionsQuery) {
		this.config = config;
		this.api = api;
		this.queryAvailableFields = availableOptionsQuery;
		this.visibleTableFlags = visibleTableFlags;

		// allow table name to be changed if flagged
		if (includeTableName) {
			outputTableName = new JTextField(config.getName());
			outputTableName.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					readFromForm();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					readFromForm();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					readFromForm();
				}
			});

			outputTableName.setMaximumSize(new Dimension(200, 26));
			addLine(new JLabel("Generate table with name  "), outputTableName);
			addWhitespace();
		} else {
			outputTableName = null;
		}

		// create select header...
		if (includeSourceControl) {
			// with source datastore and table
			fromDatastore = new FromDatastoreCombo(config.getFromDatastore());
			fromTable = new FromTableCombo(config.getFromTable());
			ValueChangedListener<String> listener = new ValueChangedListener<String>() {

				@Override
				public void comboValueChanged(String newValue) {
					readFromForm();
				}
			};
			fromDatastore.addValueChangedListener(listener);
			fromTable.addValueChangedListener(listener);
			Dimension size = new Dimension(160, 26);
			fromDatastore.setPreferredSize(size);
			fromDatastore.setMaximumSize(size);
			fromTable.setPreferredSize(size);
			fromTable.setMaximumSize(size);
			addLine(new JLabel("Select rows from datastore  "), fromDatastore, new JLabel("  table  "), fromTable);
		} else {
			// without source
			fromDatastore = null;
			fromTable = null;
			add(new JLabel("Select rows from:"));
			addHalfWhitespace();
		}

		// create filters line
		noFilterCtrls = new LabelledRadioButton("All rows", "Select all rows from the table.", RadioOption.ALL);
		selCtrls = new LabelledRadioButton("Selected in map", "Select the rows from the table which are selected in the map.",RadioOption.SEL);
		
		// create custom filter
		customFormulaControls = new CustomFormulaControls(config);
		radioFilterCtrls.add(noFilterCtrls);
		radioFilterCtrls.add(selCtrls);
		radioFilterCtrls.add(customFormulaControls);
		addLine(noFilterCtrls.createLine(),selCtrls.createLine(), customFormulaControls.createLine());

		initRadioGroupings(getOption());

		// gap between source and fields
		addWhitespace();

		// create fields header and grid
		this.fieldGrid = new AdapterTableDefinitionGrid(api,config, visibleColumnFlags, availableOptionsQuery) {
			IntegerEntryPanel limitNb;
			
			@Override
			protected List<ODLAction> createActions() {
				ArrayList<ODLAction> ret = new ArrayList<>();
				ret.addAll(super.createActions());
				ret.addAll(AdaptedTableControl.this.createActions());
				return ret;
			}
			
			@Override
			protected void fillToolbar(JToolBar toolBar) {
				super.fillToolbar(toolBar);
				
				AdaptedTableControl.this.fillToolbar(toolBar);
				
				// add limit results box
				final JCheckBox limitBox = new JCheckBox("Limit results", AdaptedTableControl.this.config.isLimitResults());
				limitBox.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						AdaptedTableControl.this.config.setLimitResults(limitBox.isSelected());
						updateAppearance();
					}
				});
				limitNb = new IntegerEntryPanel("", AdaptedTableControl.this.config.getMaxNumberRows(), "Maximum number of rows", new IntChangedListener() {
					
					@Override
					public void intChange(int newInt) {
						AdaptedTableControl.this.config.setMaxNumberRows(newInt);
					}
				});
				
				toolBar.addSeparator();
				toolBar.add(limitBox);
				toolBar.add(limitNb);
			}			

			@Override
			public void updateAppearance() {
				super.updateAppearance();
				if(limitNb!=null){
					limitNb.setEnabled(AdaptedTableControl.this.config.isLimitResults());
				}
			}
			
		};
		fieldGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
		fieldGrid.setPreferredSize(new Dimension(650, 234));
		Box box = new Box(BoxLayout.X_AXIS); // box gives correct alignment (otherwise grid is right-aligned)
		box.add(fieldGrid);
		addNoWrap(box);

		writeToForm();
	}

	/**
	 * Overridden in other classes...
	 * @param toolBar
	 */
	protected void fillToolbar(JToolBar toolBar) {
	
		// get checkboxes to add
//		ArrayList<JCheckBox> checkBoxes = new ArrayList<>();
		
//		if(DefinedFlags.hasFlag(visibleTableFlags, DefinedFlags.TABLE_IS_REPORT_HEADER_MAP)){
//			final JCheckBox box = new JCheckBox("Report header?",DefinedFlags.hasFlag(config.getFlags(), DefinedFlags.TABLE_IS_REPORT_HEADER_MAP));
//			box.addActionListener(new ActionListener() {
//				
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					config.setFlags(DefinedFlags.setFlag(config.getFlags(), DefinedFlags.TABLE_IS_REPORT_HEADER_MAP, box.isSelected()));
//					updateAppearance();
//				}
//			});
//			checkBoxes.add(box);
//		}
		
//		if(checkBoxes.size()>0){
//			toolBar.addSeparator();
//			for(JCheckBox box:checkBoxes){
//				toolBar.add(box);
//			}
//		}
	}
	
	/**
	 * Default does-nothing implementation; overridden in subclasses
	 * 
	 * @return
	 */
	protected List<ODLAction> createActions() {
		ArrayList<ODLAction> ret = new ArrayList<>();
		return ret;
	}

	private enum RadioOption{
		ALL,
		SEL,
		CUSTOM
	}
	
	private void initRadioGroupings(RadioOption option) {
		ButtonGroup group = new ButtonGroup();
		group.add(noFilterCtrls.radio);
		group.add(selCtrls.radio);
		group.add(customFormulaControls.radio);
		ActionListener radioListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for(LabelledRadioButton lrb:radioFilterCtrls){
					lrb.updateEnabled();
				}
				readFromForm();
				writeToForm();
			}
		};
		
		for(LabelledRadioButton lrb:radioFilterCtrls){
			lrb.radio.addActionListener(radioListener);
			lrb.radio.setSelected(option == lrb.type);
		}

		radioListener.actionPerformed(null);
	}

//	public static void main(String[] args) {
//		InitialiseStudio.initialise();
//		final ODLDatastoreAlterable<ODLTableAlterable> ds = ExampleData.createTerritoriesExample(3);
//		ODLTable table = ds.getTableAt(0);
//		AdaptedTableConfig tableConfig = WizardUtils.createAdaptedTableConfig(table, table.getName());
//		tableConfig.setFilterFormula("true");
//		QueryAvailableDataImpl options = new QueryAvailableDataImpl() {
//
//			@Override
//			protected ODLDatastore<? extends ODLTableDefinition> getDs() {
//				return ds;
//			}
//
//			@Override
//			protected String getDsName() {
//				return ScriptConstants.EXTERNAL_DS_NAME;
//			}
//
//		};
//
//		ShowPanel.showPanel(new AdaptedTableControl(tableConfig, true, true,0, TableFlags.FLAG_IS_OPTIONAL, options));
//	}

	public void updateAppearance() {
		if (fieldGrid != null) {
			fieldGrid.updateAppearance();
		}

		if (fromDatastore != null) {
			fromDatastore.updateAppearance();
		}

		if (fromTable != null) {
			fromTable.updateAppearance();

		}
	}

	public void setTargetDatastoreDefinitionProvider(ODLDatastoreDefinitionProvider targetDatastoreDefinitionProvider) {
		fieldGrid.setTargetDatastoreDefinitionProvider(targetDatastoreDefinitionProvider);
	}

}
