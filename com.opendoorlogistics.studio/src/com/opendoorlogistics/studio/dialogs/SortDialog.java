/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.table.TableModel;

import com.opendoorlogistics.core.tables.utils.SortColumn;
import com.opendoorlogistics.studio.controls.ColumnSelectorComboBox;
import com.opendoorlogistics.studio.controls.ColumnSelectorComboBox.SelectedColumn;

final public class SortDialog extends JDialog{
	private ArrayList<ColumnSelectorComboBox > boxes = new ArrayList<>();
	private ArrayList<JCheckBox> checkBoxes = new ArrayList<>();
	private SortColumn [] result;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			SortDialog dialog = new SortDialog(null,0);
			dialog.setVisible(true);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public SortDialog(TableModel sheet, int skipFirstNColumns) {
		super((JFrame)null, true);
		setLayout(null);
		
		// create rows of boxes
		int sep = 40;
		int y = 10;
		for(int i =0 ; i < 3 ; i++){
			final ColumnSelectorComboBox comboBox = new ColumnSelectorComboBox(sheet,skipFirstNColumns);
			comboBox.setBounds(7, y, 169, 20);
			add(comboBox);
			boxes.add(comboBox);
			
			final JCheckBox chckbxAscending = new JCheckBox("Ascending");
			
			chckbxAscending.setBounds(182, y, 100, 23);
			chckbxAscending.setSelected(true);
			add(chckbxAscending);	
			checkBoxes.add(chckbxAscending);

			y+= sep;
		}
		
		int buttonHeight = 26;
		JButton okButton = new JButton("OK");
		okButton.setActionCommand("OK");
		okButton.setBounds(116, y, 60, buttonHeight);
		add(okButton);
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// read result out
				ArrayList<SortColumn> res = new ArrayList<>();
				for(int i =0 ; i < boxes.size() ; i++){
					ColumnSelectorComboBox box = boxes.get(i);
					if(box.getSelectedItem()!=null && ((SelectedColumn)box.getSelectedItem()).getColIndx()>=0){
						res.add(new SortColumn(((SelectedColumn)box.getSelectedItem()).getColIndx(), checkBoxes.get(i).isSelected()) );
					}
				}
				
				if(res.size()>0){
					result = res.toArray(new SortColumn[res.size()]);					
				}
				dispose();
			}
		});
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("Cancel");
		cancelButton.setBounds(184, y, 80, buttonHeight);
		add(cancelButton);
		cancelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		setSize(280, 200);
		setTitle("Sort by columns...");
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	public SortColumn[] getResult(){
		return result;
	}
	
//	public static boolean sort(TableModel sheet){
//		SortDialog dialog = new SortDialog(sheet);
//		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//		dialog.setVisible(true);
//		if(dialog.getResult()!=null){
//			RowSorter sorter = new RowSorter(sheet,dialog.getResult());
//			PoiUtils.sort(sheet, sorter);
//			return true;
//		}
//		return false;
//	}
	
}
