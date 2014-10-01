/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

class CellRenderer implements TableCellRenderer {
	private final Color alternateColour = new Color(230, 230, 250);
	private final Font boldFont;
	private final boolean firstRowIsHeader;
	private final Border headerBorder = new LineBorder(new Color(100, 100, 100), 1) {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			if ((this.thickness > 0) && (g instanceof Graphics2D)) {
				Graphics2D g2d = (Graphics2D) g;

				Color oldColor = g2d.getColor();
				g2d.setColor(this.lineColor);

				Shape outer = new Rectangle2D.Float(x, y + height - thickness, width, thickness);
				Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
				path.append(outer, false);
				g2d.fill(path);
				g2d.setColor(oldColor);
			}

		}

	};
	private final JLabel label = createLabel();
	private final Color mainColour = new Color(250, 250, 230);
	private final Font normalFont;
	private final Color selectedColour = new Color(100, 150, 200);
	private final NumberFormat nf = NumberFormat.getInstance();	//new DecimalFormat("####0.000");
	private final Border focusedBorder = BorderFactory.createLineBorder(Color.BLACK, 2);
	
	public CellRenderer(boolean firstRowIsHeader) {
		normalFont = label.getFont();
		boldFont = new Font(normalFont.getFontName(), Font.BOLD, normalFont.getSize());
		this.firstRowIsHeader = firstRowIsHeader;
	}

	protected static JLabel createLabel(){
		JLabel label = new JLabel();
		label.setOpaque(true);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		return label;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		return prepareLabel(value, isSelected, hasFocus, row, column);
	}

	protected JLabel prepareLabel(Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value != null) {
			if (Number.class.isInstance(value)) {
				label.setText(nf.format(value));
			} else {
				label.setText(value.toString());
			}
		} else {
			label.setText("");
		}

		label.setForeground(Color.BLACK);
		
		if (isSelected) {
			label.setBackground(selectedColour);
		} else if (row % 2 == (firstRowIsHeader ? 1 : 0)) {
			label.setBackground(mainColour);
		} else {
			label.setBackground(alternateColour);
		}

		Border border = null;
		Font font = normalFont;
		if (firstRowIsHeader && row == 0) {
			font = boldFont;
			border = headerBorder;
		}
		else if(hasFocus){
			border = focusedBorder;
		}
		
		if (label.getFont() != font) {
			label.setFont(font);
		}

		if (label.getBorder() != border) {
			label.setBorder(border);
		}

		return label;
	}

}
