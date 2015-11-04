/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.controls.checkboxtable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.controls.CustomTableItemRenderer;
import com.opendoorlogistics.utils.ui.Icons;

public class CheckboxTable  extends JScrollPane{
	private final HashSet<CheckChangedListener> checkChangedListeners = new HashSet<>();
	private final HashSet<ButtonClickedListener> buttonClickedListeners = new HashSet<>();
	private final JTable table;
	private final Dimension checkboxSize;
	private List<? extends CheckBoxItem> items;
	private boolean disableListeners=false;
	private final Icon []buttonIcons;
	private JCheckBox []boxes;
	private JButton [][]buttons;
	
	public static interface CheckChangedListener{
		void checkStateChanged();
	}

	public static interface ButtonClickedListener{
		void buttonClicked(CheckBoxItem item, int buttonColumn);
	}
	
//	public CheckboxTable( Dimension checkboxSize, List<? extends CheckBoxItem> items){
//		this(null, checkboxSize, items);
//	}
	
	public CheckboxTable(Icon[] buttonIcons, Dimension checkboxSize, List<? extends CheckBoxItem> items){
		this.checkboxSize = checkboxSize;
		this.buttonIcons = buttonIcons;
		
		// create table
		table = new JTable();
		table.setTableHeader(null);	
		setViewportView(table);
		
		setItems(items);
	}

	private int getNbButtonCols(){
		return buttonIcons!=null? buttonIcons.length:0;
	}

	public void setItems(final List<? extends CheckBoxItem> items) {
		this.items = items;
		// check for images and get the standard sizes
		Dimension imageSize=null;
		for(CheckBoxItem item: items){
			BufferedImage image = item.getImage();
			if(image!=null){
				Dimension size = ImageUtils.getSize(image);
				if(imageSize==null){
					imageSize = size;
				}else{
					imageSize = new Dimension(Math.max(imageSize.width, size.width), Math.max(imageSize.height, size.height));
				}
			}
		}
	//	final Dimension stdImageSize = imageSize;
		final int imageCol = imageSize==null? -1: getNbButtonCols() + 1;
		final int checkboxCol = getNbButtonCols();
		final int textCol = imageSize==null? getNbButtonCols() + 1: getNbButtonCols() + 2;
		final int nbCols = textCol+1;

		table.setModel(new AbstractTableModel() {

			@Override
			public Object getValueAt(int rowIndex, int col) {
				CheckBoxItem item = items.get(rowIndex);
				if(col==imageCol){
					return item.getImage();
				}
				else if(col == textCol){
					return item.getText();
				}
				return null;
			}


			@Override
			public Class<?> getColumnClass(int col) {
				if(col < getNbButtonCols()){
					return JButton.class;
				}
				else if(checkboxCol==0){
					return JCheckBox.class;
				}else if(col == imageCol){
					return BufferedImage.class;
				}
				return String.class;
			}

			@Override
			public int getRowCount() {
				return items.size();
			}

			@Override
			public int getColumnCount() {
				return nbCols;
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return true;
			}
		});
		//table.getTableHeader().sev

		if(imageCol!=-1){
			TableCellRenderer imageRenderer = new TableCellRenderer(){

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					final BufferedImage img = items.get(row).getImage();
					final int w = table.getColumnModel().getColumn(column).getWidth();
					final int h = table.getRowHeight(row);
					return new JPanel(){
						@Override
						protected void paintComponent(Graphics g) {
							super.paintComponent(g);
							if(img!=null){
								// do white fill
								g.setColor(Color.WHITE);
								g.fillRect(0, 0, w, h);
								
								// draw centred
								int sw = Math.max(w - img.getWidth(),0);
								int sh = Math.max(h - img.getHeight(),0);
								g.drawImage(img, sw/2, sh/2, null);								
							}
						}	
					};
				}
				
			};
			
			TableColumn col=table.getColumnModel().getColumn(imageCol);
			col.setCellRenderer(imageRenderer);
			col.setMaxWidth(imageSize.width);
			
			// need to set row height later, presumably after table shown?
			final int rowHeight = Math.max(table.getRowHeight(),imageSize.height);
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					table.setRowHeight(rowHeight);
				}
			});
			
		}
		
		// create buttons
		initButtons(items);
		
		initCheckboxes(items, checkboxCol);
		
		table.setRowHeight(checkboxSize.height);
		table.setFillsViewportHeight(true);
		
		//PackTableColumn.packAll(table, 4);
	}

	private void initButtons(final List<? extends CheckBoxItem> items) {
		if(getNbButtonCols()==0){
			buttons = null;
			return;
		}
		
		buttons = new JButton[getNbButtonCols()][];
		for(int i =0 ; i<getNbButtonCols() ; i++){
			buttons[i] = new JButton[items.size()];
			for(int j =0 ; j<buttons[i].length;j++){
				final Image img;
				final Icon icon = buttonIcons[i];
				if(icon!=null){
					img = Icons.iconToImage(buttonIcons[i]);
				}else{
					img = null;
				}
				
				final JButton button = new JButton(){
					
					  public void paint(Graphics g) {
						  super.paint(g);
						  if(img!=null){
							  
						  }
					        boolean shouldClearPaintFlags = false;

					        if ((getWidth() <= 0) || (getHeight() <= 0)) {
					            return;
					        }

					        Graphics componentGraphics = getComponentGraphics(g);
					        Graphics co = componentGraphics.create();
					        try {
						        Rectangle clipRect = co.getClipBounds();
						     //   Dimension imgSize =icon new Dimension(3*clipRect.width/2, 3*clipRect.height/2);
						        Dimension imgSize = new Dimension(icon.getIconWidth(), icon.getIconHeight());
						        int x = (clipRect.width - imgSize.width)/2;
						        int y =(clipRect.height-imgSize.height)/2;
						        co.drawImage(img,x,y, imgSize.width, imgSize.height	, null);
								
							} catch (Exception e) {
								// TODO: handle exception
							}finally{
								co.dispose();
							}
					        
					    }

					
				};
				
				final int colIndx=i;
				final CheckBoxItem item = items.get(j);
				buttons[i][j]=button;
				button.setHorizontalAlignment(SwingConstants.LEFT);
				button.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						for(ButtonClickedListener listener : buttonClickedListeners){
							listener.buttonClicked(item, colIndx);
						}
					}
				});
			}
			
			// set customer item renderer for the checkboxes
			CustomTableItemRenderer<JCheckBox> renderer = new CustomTableItemRenderer(buttons[i]);
			TableColumn checkBoxCol = table.getColumnModel().getColumn(i);
			checkBoxCol.setCellEditor(renderer);
			checkBoxCol.setCellRenderer(renderer);
			checkBoxCol.setMaxWidth(checkboxSize.width);
		}
		

	}
	
	/**
	 * @param items
	 * @param checkboxCol
	 */
	private void initCheckboxes(final List<? extends CheckBoxItem> items, final int checkboxCol) {
		// set all to left align... looks better
		boxes=new JCheckBox[items.size()];
		for(int i =0 ; i<boxes.length;i++){
			final JCheckBox box = new JCheckBox();
			final CheckBoxItem item = items.get(i);
			boxes[i] = box;
			box.setHorizontalAlignment(SwingConstants.LEFT);
			box.setSelected(item.isSelected());
			box.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent e) {
					item.setSelected(box.isSelected());
					fireListeners();
				}

			});
		}

		
		// set customer item renderer for the checkboxes
		CustomTableItemRenderer<JCheckBox> renderer = new CustomTableItemRenderer(boxes);
		TableColumn checkBoxCol = table.getColumnModel().getColumn(checkboxCol);
		checkBoxCol.setCellEditor(renderer);
		checkBoxCol.setCellRenderer(renderer);
		checkBoxCol.setMaxWidth(checkboxSize.width);
	}

	public synchronized void showHideAll(boolean showAll){
		if(boxes!=null){
			disableListeners = true;
			for(JCheckBox box:boxes){
				box.setSelected(showAll);
			}
			disableListeners = false;
			fireListeners();
		}
		repaint();
	}

	public void addCheckChangedListener(CheckChangedListener listener){
		checkChangedListeners.add(listener);
	}
	
	public void removeCheckChangedListener(CheckChangedListener listener){
		checkChangedListeners.remove(listener);
	}
	
	public void addButtonClickedListener(ButtonClickedListener listener){
		buttonClickedListeners.add(listener);
	}
	
	public void removeButtonClickedListener(ButtonClickedListener listener){
		buttonClickedListeners.remove(listener);
	}
	
	
	public static void main(String[]args){
		InitialiseStudio.initialise(false);
		
	//	for(boolean showImage : new boolean[]{true,false}){
		JPanel panel = new JPanel();
		ArrayList<CheckBoxItem> items = new ArrayList<>();
		items.add(new CheckBoxItemImpl(ImageUtils.createBlankImage(20, 20, Color.RED),"one"));
		items.add(new CheckBoxItemImpl(ImageUtils.createBlankImage(20, 20, Color.GREEN),"two"));
		items.add(new CheckBoxItemImpl(ImageUtils.createBlankImage(20, 20, Color.BLUE),"three"));
		CheckboxTable table = new CheckboxTable(new Icon[]{Icons.loadFromStandardPath("legend-zoom-best.png")},new Dimension(20, 20), items);
		panel.setLayout(new BorderLayout());
		panel.add(table, BorderLayout.CENTER);
		ShowPanel.showPanel(panel,false);
	//	}

	}
	
	public List<? extends CheckBoxItem> getItems(){
		return items;
	}
	
	private void fireListeners() {
		if(!disableListeners){
			for(CheckChangedListener listener : checkChangedListeners){
				listener.checkStateChanged();
			}						
		}
	}
}
