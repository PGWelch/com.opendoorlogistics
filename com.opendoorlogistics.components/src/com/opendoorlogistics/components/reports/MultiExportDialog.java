/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

final public class MultiExportDialog extends OkCancelDialog{
	private VerticalLayoutPanel panel;
	private final FileBrowserPanel dir;
	private final TextEntryPanel prefix;
	
	public MultiExportDialog(Window parent, String initialDirectory,String initialPrefix,final List<Map.Entry<String,String>> nameDescriptionList) {
		super(parent);
		
		final JTable table = new JTable();
		
		dir = new FileBrowserPanel("Output directory ",initialDirectory, new FilenameChangeListener() {
			
			@Override
			public void filenameChanged(String newFilename) {
				if(table.getModel()!=null){
					table.tableChanged(new TableModelEvent(table.getModel()));					
				}
			}
		}, true, "OK");
		panel.addIndentedLine(4,dir);
		
		prefix = new TextEntryPanel("File prefix", initialPrefix!=null?initialPrefix:"", new TextChangedListener() {
			
			@Override
			public void textChange(String newText) {
				if(table.getModel()!=null){
					table.tableChanged(new TableModelEvent(table.getModel()));					
				}	
			}
		});
		panel.add(prefix);
		
		final Object filePrefix = new Object(){
			@Override
			public String toString(){
				String ret = dir.getFilename();
				if(ret.length()>0 && ret.charAt(ret.length()-1)!=File.separatorChar){
					ret += File.separator;
				}
				ret += prefix.getText();
				return ret;
			}
		};
		
		// add table to the panel
		panel.addWhitespace();
		panel.addIndentedLine(4,new JLabel("Exporting files:"));
		JScrollPane scrollPane = new JScrollPane(table);
		panel.addIndentedLine(4,scrollPane);
		
		// set-up table model
		table.setModel(new AbstractTableModel() {
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				Map.Entry<String,String> row = nameDescriptionList.get(rowIndex);
				if(columnIndex==0){
					return row.getValue();
				}
				
				return filePrefix.toString() + row.getKey();
			}
			
			@Override
			public int getRowCount() {
				return nameDescriptionList.size();
			}
			
			@Override
			public int getColumnCount() {
				return 2;
			}
			
			@Override
		    public String getColumnName(int column) {
				if(column==0){
					return "Description";
				}
				return "Output filename";
			}
		});
		table.setFillsViewportHeight(true);
		
	//	table.setPreferredSize(new Dimension(table.getPreferredSize().width, 100));
		scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, 100));
	//	table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredSize().width, 250));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		setTitle("Exporting multiple files");
		pack();
	}

	@Override
	protected Component createMainComponent(boolean inWindowsBuilder) {
		panel= new VerticalLayoutPanel();
		return panel;
	}
	
//	public static void main(String []args){
//		ArrayList<Map.Entry<String,String>> list = new ArrayList<>();
//		list.add(new AbstractMap.SimpleEntry<String,String>("customers.jrxml", "Customers report template"));
//		list.add(new AbstractMap.SimpleEntry<String,String>("customers.jasper", "Compiled customers report template"));
//		MultiExportDialog dlg = new MultiExportDialog(null, "c:\\temp\\", list);
//		dlg.showModal();
//	}
	
	@Override
	protected String getOkButtonText(){
		return "Export";
	}
	
	public String getExportDirectory(){
		return dir.getFilename();
	}
	
	public String getExportPrefix(){
		return prefix.getText();
	}
}
