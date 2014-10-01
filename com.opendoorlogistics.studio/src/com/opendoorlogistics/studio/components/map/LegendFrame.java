/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckBoxItem;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckBoxItemImpl;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable.ButtonClickedListener;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable.CheckChangedListener;
import com.opendoorlogistics.utils.ui.Icons;

public abstract class LegendFrame extends JInternalFrame implements CheckChangedListener, ButtonClickedListener {
	private static final Color LEGEND_BACKGROUND_COLOUR = new Color(240, 240, 240);
	private final CheckboxTable legendFilterTable;

	public LegendFrame() {
		setTitle("Legend");
		setClosable(true);
		setResizable(true);
		setLayout(new BorderLayout());

		// init table
		legendFilterTable = new CheckboxTable(new Icon[]{Icons.loadFromStandardPath("legend-zoom-best.png")},new Dimension(20, 20), new ArrayList<CheckBoxItem>());
		legendFilterTable.addCheckChangedListener(this);
		legendFilterTable.addButtonClickedListener(this);
		add(legendFilterTable, BorderLayout.CENTER);

		// add in a scrollpane
		JScrollPane scroller = new JScrollPane(legendFilterTable);
		add(scroller, BorderLayout.CENTER);
		scroller.setPreferredSize(new Dimension(150, 150));
		setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		setBackground(LEGEND_BACKGROUND_COLOUR);
		setAlignmentX(JInternalFrame.LEFT_ALIGNMENT);
		
		JPanel showHidePanel = new JPanel();
		showHidePanel.setLayout(new GridLayout(1, 2));
		showHidePanel.add(new JButton(new AbstractAction("Show all") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				legendFilterTable.showHideAll(true);
			}
		}));
		showHidePanel.add(new JButton(new AbstractAction("Hide all") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				legendFilterTable.showHideAll(false);
			}
		}));
		add(showHidePanel , BorderLayout.SOUTH);
		pack();
	}

	@Override
	public void dispose() {
		disposeLegend();
		super.dispose();
	}

	protected abstract void disposeLegend();

	public void updateLegend(List<Map.Entry<String, BufferedImage>> items) {

		// build checkbox items
		ArrayList<CheckBoxItem> newItems = new ArrayList<>();
		for (Map.Entry<String, BufferedImage> item : items) {

			// create new checkbox item
			CheckBoxItemImpl cbItem = new CheckBoxItemImpl(item.getValue(), item.getKey());
			cbItem.setSelected(true);
			newItems.add(cbItem);

			// use old selected state if available
			if (legendFilterTable.getItems() != null) {
				for (CheckBoxItem oldItem : legendFilterTable.getItems()) {
					if (Strings.equalsStd(oldItem.getText(), cbItem.getText())) {
						cbItem.setSelected(oldItem.isSelected());
						break;
					}
				}
			}
		}

		legendFilterTable.setItems(newItems);
		checkStateChanged();
	}

	Iterable<? extends DrawableObject> filterDrawables(Iterable<? extends DrawableObject> unfiltered){
		// get set of strings to show
		StandardisedStringSet set = new StandardisedStringSet();
		for(CheckBoxItem item:legendFilterTable.getItems()){
			if(item.isSelected()){
				set.add(item.getText());						
			}
		}
		
		// filter, including anything with (a) no legend key or (b) a checked legend key
		ArrayList<DrawableObject> filteredList = new ArrayList<>();
		for(DrawableObject drawable : unfiltered){
			if(Strings.isEmpty(drawable.getLegendKey()) || set.contains(drawable.getLegendKey())){
				filteredList.add(drawable);
			}
		}	
		
		return filteredList;
	}
}
