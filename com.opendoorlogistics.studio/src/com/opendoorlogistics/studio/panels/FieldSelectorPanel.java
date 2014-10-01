/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.panels;

import gnu.trove.set.hash.TIntHashSet;

import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ShowPanel;

final public class FieldSelectorPanel extends JPanel {
	private final JTree tree;
	private final DefaultMutableTreeNode topNode = new DefaultMutableTreeNode();

	public FieldSelectorPanel(ODLDatastore<? extends ODLTableDefinition> ds) {
		this.tree = new JTree(topNode);
		tree.setRootVisible(false);
		tree.setSelectionModel(new MyTreeSelectionModel());
		setLayout(new BorderLayout());
		add(new JScrollPane(tree), BorderLayout.CENTER);
		update(ds,null);
	}

	public void update(ODLDatastore<? extends ODLTableDefinition> ds, TIntHashSet availableTableIds) {
		String [] current = getSelected();
		boolean hasSelected = current!=null && current.length==2;
		
		topNode.removeAllChildren();
		DefaultMutableTreeNode selNode=null;
		for (ODLTableDefinition table : TableUtils.getAlphabeticallySortedTables(ds)) {
			if(availableTableIds!=null && availableTableIds.contains(table.getImmutableId())==false){
				continue;
			}
			
			DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(table.getName());
			topNode.add(tableNode);
		
			// is this the previously selected table?
			boolean selTable = hasSelected && Strings.equalsStd(table.getName(), current[0]);
			
			int nc = table.getColumnCount();
			for (int i = 0; i < nc; i++) {
				DefaultMutableTreeNode fieldNode = new DefaultMutableTreeNode(table.getColumnName(i));

				if(selTable && Strings.equalsStd(table.getColumnName(i), current[1])){
					selNode = fieldNode;
				}
					
				tableNode.add(fieldNode);
			}
		}

		// hack otherwise update doesn't work...
		DefaultTreeModel model = new DefaultTreeModel(topNode);
		tree.setModel(model);
		
		// ensure path is expanded otherwise child nodes will be invisible if top node is invisible
		tree.expandPath(new TreePath(topNode.getPath()));
		
		// also expand all tables
		Enumeration<?>e= topNode.children();
		while(e.hasMoreElements()){
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
			tree.expandPath(new TreePath(node.getPath()));
		}
		
		// reselect if possible
		if(selNode!=null){
			tree.setSelectionPath(new TreePath(selNode.getPath()));
		}

	}
	
	public String [] getSelected(){
		TreePath path = tree.getSelectionPath();
		if(path!=null){
			Object [] objs = path.getPath();
			return new String[]{objs[1].toString(), objs[2].toString()};
		}
		return null;
	}

	public static void main(String[] args) {
		ShowPanel.showPanel(new FieldSelectorPanel(ExampleData.createExampleDatastore(true)));
	}

	/**
	 * See http://stackoverflow.com/questions/12629309/create-non-selectable-defaulttreemodel-node-with-children
	 *
	 */
	private class MyTreeSelectionModel implements TreeSelectionModel {

		TreeSelectionModel selectionModel = new DefaultTreeSelectionModel();

		private boolean canPathBeAdded(TreePath treePath) {
			return treePath.getPathCount() > 2;
		}

		private TreePath[] getFilteredPaths(TreePath[] paths) {
			List<TreePath> returnedPaths = new ArrayList<TreePath>(paths.length);
			for (TreePath treePath : paths) {
				if (canPathBeAdded(treePath)) {
					returnedPaths.add(treePath);
				}
			}
			return returnedPaths.toArray(new TreePath[returnedPaths.size()]);
		}

		@Override
		public void setSelectionMode(int mode) {
			selectionModel.setSelectionMode(mode);
		}

		@Override
		public int getSelectionMode() {
			return selectionModel.getSelectionMode();
		}

		@Override
		public void setSelectionPath(TreePath path) {
			if (canPathBeAdded(path)) {
				selectionModel.setSelectionPath(path);
			}
		}

		@Override
		public void setSelectionPaths(TreePath[] paths) {
			paths = getFilteredPaths(paths);
			selectionModel.setSelectionPaths(paths);
		}

		@Override
		public void addSelectionPath(TreePath path) {
			if (canPathBeAdded(path)) {
				selectionModel.addSelectionPath(path);
			}
		}

		@Override
		public void addSelectionPaths(TreePath[] paths) {
			paths = getFilteredPaths(paths);
			selectionModel.addSelectionPaths(paths);
		}

		@Override
		public void removeSelectionPath(TreePath path) {
			selectionModel.removeSelectionPath(path);
		}

		@Override
		public void removeSelectionPaths(TreePath[] paths) {
			selectionModel.removeSelectionPaths(paths);
		}

		@Override
		public TreePath getSelectionPath() {
			return selectionModel.getSelectionPath();
		}

		@Override
		public TreePath[] getSelectionPaths() {
			return selectionModel.getSelectionPaths();
		}

		@Override
		public int getSelectionCount() {
			return selectionModel.getSelectionCount();
		}

		@Override
		public boolean isPathSelected(TreePath path) {
			return selectionModel.isPathSelected(path);
		}

		@Override
		public boolean isSelectionEmpty() {
			return selectionModel.isSelectionEmpty();
		}

		@Override
		public void clearSelection() {
			selectionModel.clearSelection();
		}

		@Override
		public void setRowMapper(RowMapper newMapper) {
			selectionModel.setRowMapper(newMapper);
		}

		@Override
		public RowMapper getRowMapper() {
			return selectionModel.getRowMapper();
		}

		@Override
		public int[] getSelectionRows() {
			return selectionModel.getSelectionRows();
		}

		@Override
		public int getMinSelectionRow() {
			return selectionModel.getMinSelectionRow();
		}

		@Override
		public int getMaxSelectionRow() {
			return selectionModel.getMaxSelectionRow();
		}

		@Override
		public boolean isRowSelected(int row) {
			return selectionModel.isRowSelected(row);
		}

		@Override
		public void resetRowSelection() {
			selectionModel.resetRowSelection();
		}

		@Override
		public int getLeadSelectionRow() {
			return selectionModel.getLeadSelectionRow();
		}

		@Override
		public TreePath getLeadSelectionPath() {
			return selectionModel.getLeadSelectionPath();
		}

		@Override
		public void addPropertyChangeListener(PropertyChangeListener listener) {
			selectionModel.addPropertyChangeListener(listener);
		}

		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener) {
			selectionModel.removePropertyChangeListener(listener);
		}

		@Override
		public void addTreeSelectionListener(TreeSelectionListener x) {
			selectionModel.addTreeSelectionListener(x);
		}

		@Override
		public void removeTreeSelectionListener(TreeSelectionListener x) {
			selectionModel.removeTreeSelectionListener(x);
		}

	}

}
