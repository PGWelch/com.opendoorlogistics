/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.components.barchart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.JCheckBoxTree;
import com.opendoorlogistics.codefromweb.JCheckBoxTree.CheckChangeEvent;
import com.opendoorlogistics.core.utils.StringIdTreeNode;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.JCheckBoxTreeExt;
import com.opendoorlogistics.core.utils.ui.JCheckBoxTreeExt.CheckBoxTreeState;

class BarchartPanel extends JPanel implements Disposable, JCheckBoxTree.CheckChangeEventListener {
	private final ChartPanel chartPanel;
	private final JCheckBoxTreeExt tree;
	private BarchartConfig config;
	private ODLTableReadOnly table;
	
	BarchartPanel(BarchartConfig config,ODLTableReadOnly table ) {
		this.config = config;
		this.table = table;
		
		JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		setLayout(new BorderLayout());
		add(splitter,BorderLayout.CENTER);
		setPreferredSize(new Dimension(500, 270));
		
		chartPanel = new ChartPanel(createChart(table,null));
		chartPanel.setFillZoomRectangle(true);
		chartPanel.setMouseWheelEnabled(true);
		splitter.setRightComponent(chartPanel);
		
		JPanel westPanel = new JPanel();
		westPanel.setLayout(new BorderLayout());
		JLabel label = new JLabel("Categories");
		label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		westPanel.add(label,BorderLayout.NORTH);
		add(westPanel,BorderLayout.WEST);
		
		tree = new JCheckBoxTreeExt();
		JScrollPane scrollPane = new JScrollPane(tree);
		westPanel.add(scrollPane,BorderLayout.CENTER);
		westPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		tree.setModel(createTreeModel());
		tree.checkAll(true);
		tree.setRootVisible(false);
		tree.addCheckChangeEventListener(this);
		tree.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 5));
		
		JPanel showHidePanel = new JPanel();
		showHidePanel.setLayout(new GridLayout(1, 2));						
		showHidePanel.add(new JButton(new AbstractAction("Show all") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				tree.checkAll(true);
			}
		}));
		showHidePanel.add(new JButton(new AbstractAction("Hide all") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				tree.checkAll(false);
			}
		}));
		westPanel.add(showHidePanel,BorderLayout.SOUTH);
		
		splitter.setLeftComponent(westPanel);
	}

	@Override
	public void dispose() {

	}

	@Override
	public void checkStateChanged(CheckChangeEvent event) {
		// create filtered chart
		int [] filtered = getFilteredRows(tree.saveCheckedState(), table);
		chartPanel.setChart(createChart(table,filtered));
	}


	void update(BarchartConfig config,ODLTableReadOnly table){
		this.config = config;
		this.table = table;
		
		// update tree
		CheckBoxTreeState state = tree.saveState();
		tree.setModel(createTreeModel());
		tree.restoreState(state);

		// refresh the chart from the checked state
		checkStateChanged(null);
		
	}
	
	private TreeModel createTreeModel(){
		StringIdTreeNode<Void> nodeTree = buildCategoryTree(table);
		return new DefaultTreeModel(nodeTree.exportToJTree(null));						
	}
	
//	/**
//	 * @param table
//	 * @param row
//	 * @return
//	 */
//	private String readCategory(ODLTableReadOnly table, int row) {
//		String category = (String) table.getValueAt(row, config.getNbFilterGroupLevels());
//		if (category == null) {
//			// treat null as blank
//			category = "";
//		}
//		return category;
//	}
	
	private JFreeChart createChart(ODLTableReadOnly table, int [] rowFilter) {
		DefaultCategoryDataset dataset = createDataset(table,rowFilter);

		// create the chart...
		JFreeChart chart = ChartFactory.createBarChart(Strings.isEmpty(config.getTitle()) ? null : config.getTitle(),// chart title
				config.getXLabel(), // domain axis label
				config.getYLabel(), // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips?
				false // URLs?
				);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.WHITE);

		// don't show legend for just one series
		if (config.getSeriesNames().size() <= 1) {
			chart.removeLegend();
		}
		return chart;
	}
	
	private DefaultCategoryDataset createDataset(ODLTableReadOnly table,int [] rowFilter) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		if(rowFilter!=null){
			for(int row:rowFilter){
				createRowData( table, dataset, row);				
			}
		}else{
			int n = table.getRowCount();
			for (int row = 0; row < n; row++) {
				createRowData(table, dataset, row);
			}			
		}
		return dataset;
	}
	

	/**
	 * @param bc
	 * @param table
	 * @param dataset
	 * @param row
	 */
	private void createRowData( ODLTableReadOnly table, DefaultCategoryDataset dataset, int row) {
		String category = readGroupVal(table, row, config.getNbFilterGroupLevels());
		for (int series = 0; series < config.getSeriesNames().size(); series++) {
			Double val = (Double) table.getValueAt(row,config.getNbFilterGroupLevels() + 1 + series);
			if (val == null) {
				throw new RuntimeException("Empty numeric value passed into barcode component input table.");
			}

			dataset.addValue(val, config.getSeriesNames().get(series), category);
		}
	}
	
	private StringIdTreeNode<Void> buildCategoryTree(ODLTableReadOnly table){
		// build single level tree right now
		int n = table.getRowCount();
		StringIdTreeNode<Void> ret = new StringIdTreeNode<>("Visible categories", null);
		for (int row = 0; row < n; row++) {
			
			// loop over nb filter groups + 1 (as the last one is the category)
			StringIdTreeNode<Void> parent = ret;
			for(int groupIndx =0 ; groupIndx <= config.getNbFilterGroupLevels() ; groupIndx++){
				String groupVal = readGroupVal(table, row, groupIndx);
					
				StringIdTreeNode<Void> newParentTreeNode = parent.get(groupVal);
				if(newParentTreeNode==null){
					newParentTreeNode = parent.add(groupVal, null);
				}
				
				parent = newParentTreeNode;
			}
		}
		return ret;
	}

	/**
	 * @param table
	 * @param row
	 * @param groupIndx
	 * @return
	 */
	private String readGroupVal(ODLTableReadOnly table, int row, int groupIndx) {
		String groupVal = (String) table.getValueAt(row, groupIndx);
		if (groupVal == null) {
			// treat null as blank
			groupVal = "";
		}
		return groupVal;
	}
	
	private int[] getFilteredRows(StringIdTreeNode<Boolean> filter, ODLTableReadOnly table){
		int n = table.getRowCount();
		int [] ret = new int[n];
		
		if(filter==null){
			for(int row =0 ; row<n;row++){
				ret[row] = row;
			}
			return ret;
		}else{
			int nf=0;
			for(int row =0 ; row<n;row++){
	
				// loop over nb filter groups + 1 (as the last one is the category)
				boolean pass = true;
				StringIdTreeNode<Boolean> parent = filter;
				for(int groupIndx =0 ; groupIndx <= config.getNbFilterGroupLevels() && pass; groupIndx++){
					String groupVal = readGroupVal(table, row, groupIndx);
					StringIdTreeNode<Boolean> node = parent.get(groupVal);
					
					if(node==null){
						// Node unknown so show the category. This means the tree is out-of-date
						// with the category data which should hopefully never happen
						break;
					}
					
					Boolean val = node.getLeaf();
					if(val == null){
						// Should hopefully never happen
						break;
					}else{
						pass = val;
					}
					
					parent = node;
				}
				
				if(pass){
					ret[nf++] =row;
				}
			}
			
			return Arrays.copyOf(ret, nf);
		}

	}

}