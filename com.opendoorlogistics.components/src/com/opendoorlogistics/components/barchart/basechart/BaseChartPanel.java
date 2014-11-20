package com.opendoorlogistics.components.barchart.basechart;

import java.awt.BorderLayout;
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

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.JCheckBoxTree;
import com.opendoorlogistics.codefromweb.JCheckBoxTree.CheckChangeEvent;
import com.opendoorlogistics.core.utils.StringIdTreeNode;
import com.opendoorlogistics.core.utils.ui.JCheckBoxTreeExt;
import com.opendoorlogistics.core.utils.ui.JCheckBoxTreeExt.CheckBoxTreeState;

public abstract class BaseChartPanel extends JPanel implements Disposable, JCheckBoxTree.CheckChangeEventListener {
	private final ChartPanel chartPanel;
	private final JCheckBoxTreeExt tree;
	private ODLTableReadOnly table;
	protected BaseConfig config;
	protected final ODLApi api;

	@Override
	public void checkStateChanged(CheckChangeEvent event) {
		// create filtered chart
		int [] filtered = getFilteredRows(tree.saveCheckedState(), table);
		chartPanel.setChart(createChart(table,filtered));
	}



	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void update(BaseConfig config,ODLTableReadOnly table){
		this.config =config;
		this.table = table;
		
		// update tree
		CheckBoxTreeState state = tree.saveState();
		tree.setModel(createTreeModel());
		tree.restoreState(state);

		// refresh the chart from the checked state
		checkStateChanged(null);
		
	}

	
	public BaseChartPanel(ODLApi api, BaseConfig config,ODLTableReadOnly table ) {
		this.config = config;
		this.table = table;
		this.api = api;
		
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
	
	protected abstract JFreeChart createChart(ODLTableReadOnly table, int [] rowFilter);
	
	
	protected TreeModel createTreeModel(){
		StringIdTreeNode<Void> nodeTree = buildCategoryTree(table);
		return new DefaultTreeModel(nodeTree.exportToJTree(null));						
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
	protected String readGroupVal(ODLTableReadOnly table, int row, int groupIndx) {
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

