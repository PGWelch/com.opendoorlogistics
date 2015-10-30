/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringEscapeUtils;

abstract class HeaderCellRenderer implements TableCellRenderer, MouseListener {


	protected final Color disabledColour = new Color(230, 230, 230);
	protected final JLabel columnNameLabel = new JLabel();
	protected final Color selectedColour = new Color(100, 150, 200);

	HeaderCellRenderer() {
		initLabel(columnNameLabel);
	}

	/**
	 * 
	 */
	protected void initLabel(JLabel label) {
		label.setOpaque(true);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		prepareLabel(value, isSelected,getColumnIsItalics(column),columnNameLabel);
		return columnNameLabel;
	}

	protected abstract boolean getColumnIsItalics(int col);
	
	/**
	 * @param value
	 * @param isSelected
	 */
	protected void prepareLabel(Object value, boolean isSelected,boolean italics, JLabel label) {
		StringBuilder builder = new StringBuilder();
		if (value != null) {
			builder.append("<html><strong>");
			if(italics){
				builder.append("<em>");				
			}
			builder.append(StringEscapeUtils.escapeHtml4(value.toString()));
			if(italics){
				builder.append("</em>");				
			}
			builder.append("</strong></html>");
			
		}
		label.setText(builder.toString());

		if (isSelected) {
			label.setBackground(selectedColour);
		} else {
			label.setBackground(disabledColour);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
