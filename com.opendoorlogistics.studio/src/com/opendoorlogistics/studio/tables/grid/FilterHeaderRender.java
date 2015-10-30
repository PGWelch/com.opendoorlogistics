/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import com.opendoorlogistics.codefromweb.JScrollPopupMenu;
import com.opendoorlogistics.codefromweb.MenuArrowIcon;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.studio.tables.grid.adapter.SwingAdapter;

public abstract class FilterHeaderRender extends HeaderCellRenderer{
	private final JPanel panel;
	private final JPanel filterPanel;
	private final JLabel firstColumnLabel;
	//private final JComboBox<String> filter;
	private final JLabel filterValueLabel;
	private final JLabel dummyButtonLabel;
//	private final JTable table;
	
	private static final String UNFILTERED = "<unfiltered>";
	
	public FilterHeaderRender() {
		firstColumnLabel = new JLabel();
		initLabel(firstColumnLabel);

		// take border off label
		columnNameLabel.setBorder(null);
		
		// give label its own panel to ensure its centred
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BorderLayout());
		labelPanel.add(columnNameLabel, BorderLayout.CENTER);
		
		panel = new JPanel();
//		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setLayout(new BorderLayout());
		panel.add(labelPanel, BorderLayout.CENTER);
		panel.setOpaque(true);
		panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
		filterPanel = new JPanel();
		filterPanel.setLayout(new BorderLayout());
		filterPanel.setOpaque(true);
		panel.add(filterPanel, BorderLayout.SOUTH);

		filterValueLabel = new JLabel("test");
		filterValueLabel.setBackground(Color.WHITE);
		filterValueLabel.setOpaque(true);
		filterValueLabel.setBorder(BorderFactory.createEtchedBorder());		
		filterPanel.add(filterValueLabel,BorderLayout.CENTER);
		dummyButtonLabel = new JLabel(new MenuArrowIcon());
		filterPanel.add(dummyButtonLabel,BorderLayout.EAST);
		//filterButton.setPreferredSize(new Dimension(20, 26));
	
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

		if(column==0){
			prepareLabel(value, isSelected,false,firstColumnLabel);			
			return firstColumnLabel;
		}
		prepareLabel(value, isSelected,getColumnIsItalics(column),columnNameLabel);	
		
		if (isSelected) {
			panel.setBackground(selectedColour);
			filterPanel.setBackground(selectedColour);
		} else {
			panel.setBackground(disabledColour);
			filterPanel.setBackground(disabledColour);
		}
	
		String filterText = ((SwingAdapter)table.getModel()).getColumnFilter(table.getColumnName(column));
		filterValueLabel.setText(filterText!=null ?filterText :UNFILTERED);
		return panel;
	}

//	public static void main(String[]args){
//		JTable table = new JTable();
//		table.setModel(new AbstractTableModel() {
//			
//			@Override
//			public Object getValueAt(int rowIndex, int columnIndex) {
//				return "hello";
//			}
//			
//			@Override
//			public int getRowCount() {
//				return 3;
//			}
//			
//			@Override
//			public int getColumnCount() {
//				return 3;
//			}
//		});
//		
//		JTableHeader tableHeader = table.getTableHeader();
//		tableHeader.setDefaultRenderer(new FilterHeaderRender());
//		tableHeader.invalidate();
//		tableHeader.revalidate();
//		tableHeader.updateUI();
//		
//		JScrollPane scrollPane = new JScrollPane(table);
//		JPanel panel = new JPanel();
//		panel.setLayout(new BorderLayout());
//		panel.add(scrollPane,BorderLayout.CENTER);
//		ShowPanel.showPanel(panel);
//	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		JTableHeader header = (JTableHeader) e.getSource();
		final JTable table = header.getTable();
		TableColumnModel columnModel = table.getColumnModel();
		final int col = columnModel.getColumnIndexAtX(e.getX());
		if(col==-1){
			return;
		}
	
		// Get the column header rectangle - a rectangle defining the position and extent of the individual column's header
		Rectangle chr = header.getHeaderRect(col);
		
		// Get the button label's size
		Dimension bls = dummyButtonLabel.getSize();
		
		// Get the rectangle for the button, assuming it exactly fills the bottom right corner of the header rectangle
		Rectangle buttonRectangle = new Rectangle(chr.x + chr.width - bls.width, chr.y + chr.height - bls.height, bls.width, bls.height);
		
		if(buttonRectangle.contains(e.getPoint())){
			//System.out.println("hello world");
			JScrollPopupMenu popupMenu = new JScrollPopupMenu();
			popupMenu.setMaximumVisibleRows(20);
			StandardisedStringSet set = ((SwingAdapter)table.getModel()).getUniqueUnfilteredColumnValues(col);
			
			popupMenu.add(new JMenuItem(new AbstractAction(UNFILTERED) {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					setFilter(table, col, null);
				}
			}));
			
			for(final String s : set){
				popupMenu.add(new JMenuItem(new AbstractAction(s) {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						setFilter(table, col, s);
						
					}


				}));
			}
			popupMenu.show(header, buttonRectangle.x, buttonRectangle.y + bls.height);
		}

		
	}
	

	private void setFilter(final JTable table, final int col, final String filter) {
		((SwingAdapter)table.getModel()).setColumnFilter(table.getColumnName(col), filter);
		TableModelEvent tme= new TableModelEvent(table.getModel(), -1, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
		table.tableChanged(tme);
	}
}
