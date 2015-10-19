/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.tables.creator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.opendoorlogistics.core.scripts.elements.ScriptElementType;
import com.opendoorlogistics.core.scripts.io.XMLConversionHandlerImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.utils.Serialization;
import com.opendoorlogistics.utils.ui.TablePanel;

final public class DatastoreEditorPanel extends JPanel {
	private final boolean showFlags;

	public DatastoreEditorPanel(DatastoreConfig config) {
		this(config,true);
	}
	
	public DatastoreEditorPanel(final DatastoreConfig config, boolean showFlags) {
		this.showFlags = showFlags;
		BorderLayout bl = new BorderLayout();
		setLayout(bl);
		bl.setVgap(10);

		TablesPanel tablesPanel = new TablesPanel(config.getTables());
		tablesPanel.setBorder(BorderFactory.createLineBorder(Color.darkGray, 1));
		tablesPanel.setPreferredSize(new Dimension(400, 200));
		add(tablesPanel, BorderLayout.CENTER);
		
	//	setTitle("Datastore editor");
	}
	
	private class TablesPanel extends TablePanel<ODLTableDefinitionImpl>{

		public TablesPanel(List<ODLTableDefinitionImpl> items) {
			super(items, "table");
			addTitleLabel("Tables");
			setConversionHandler(new XMLConversionHandlerImpl(ScriptElementType.TABLE_DEFINITION));
		}


		@Override
		protected ODLTableDefinitionImpl createNewItem() {
			return editItem(new ODLTableDefinitionImpl(-1, "New table"));
		}

		@Override
		protected ODLTableDefinitionImpl editItem(ODLTableDefinitionImpl item) {
			item = (ODLTableDefinitionImpl)Serialization.deepCopy(item);
			TableDefinitionDlg dlg = new TableDefinitionDlg(SwingUtilities.getWindowAncestor(this), item, showFlags);
			dlg.setLocationRelativeTo(this);
			if(dlg.showModal() == TableDefinitionDlg.OK_OPTION){
				// save changes
				return item;
			}
			return null;
		}
		
		@Override
		protected TableModel createTableModel() {
			return new AbstractTableModel() {
				
				@Override
				public String getColumnName(int indx){
					switch(indx){
		
					case 0:
						return "Name";
			
					case 1:
						return "Number of columns";
					}
					return "";
				}
				
				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					ODLTableDefinitionImpl config = items.get(rowIndex);
					
					switch(columnIndex){
					case 0:
						return config.getName();
						
					case 1:
						return Integer.toString(config.getColumnCount());
					}
					return "";
				}
				
				@Override
				public int getRowCount() {
					return items.size();
				}
				
				@Override
				public int getColumnCount() {
					return 2;
				}
			};
		}

	}
}
