/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.list;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.opendoorlogistics.core.scripts.ScriptsProvider;
import com.opendoorlogistics.core.scripts.ScriptsProvider.HasScriptsProvider;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptEditorType;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.scripts.editor.ScriptIcons;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;

class ScriptsTree implements HasScriptsProvider{
	// final JTable table = new JTable();
	final private JScrollPane listScrollPane;
	private ScriptNode[] items;
	final private JTree tree;
	final private TreeNode rootNode = new RootNode();
	private TreeMap<ScriptEditorType, ArrayList<File>> byType;
	final private ScriptUIManager scriptUIManager;
	
	private class RootNode implements TreeNode{
		@Override
		public boolean isLeaf() {
			return false;
		}

		@Override
		public TreeNode getParent() {
			return null;
		}

		@Override
		public int getIndex(TreeNode node) {
			if(isSingleNodeMode()){
				for(int i =0 ; i < items[0].getChildCount() ; i++){
					if(items[0].getChildAt(i) == node){
						return i;
					}
				}
			}
			else{
				for (int i = 0; items != null && i < items.length; i++) {
					if (items[i] == node) {
						return i;
					}
				}	
			}

			return -1;
		}

		@Override
		public int getChildCount() {
			if(isSingleNodeMode()){
				return items[0].getChildCount();
			}else{
				return items != null ? items.length : 0;				
			}
		}

		@Override
		public TreeNode getChildAt(int childIndex) {
			if(isSingleNodeMode()){
				return items[0].getChildAt(childIndex);
			}else{
				return items[childIndex];				
			}
		}

		@Override
		public boolean getAllowsChildren() {
			return true;
		}

		@Override
		public Enumeration children() {
			return new Enumeration<TreeNode>() {
				int index = -1;

				@Override
				public TreeNode nextElement() {
					return getChildAt(++index);
				}

				@Override
				public boolean hasMoreElements() {
					return (index + 1) < getChildCount();
				}
			};

		}
		
		private boolean isSingleNodeMode(){
			if(!scriptUIManager.getAppPermissions().isScriptDirectoryLocked()){
				return false;
			}
			
			if(items==null || items.length!=1){
				return false;
			}
			
			ScriptNode node = items[0];
			if(node.isRunnable() || node.getChildCount()==0){
				return false;
			}
			
			return true;
		}
	}
	
	ScriptsTree(final ScriptUIManager scriptUIManager,final JPopupMenu popup) {
		this.scriptUIManager = scriptUIManager;

		tree = new JTree(rootNode) {
			@Override
			public String getToolTipText(MouseEvent e) {
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path != null && path.getLastPathComponent() != null) {
					ScriptNode node = (ScriptNode) path.getLastPathComponent();
					return node.getTooltip(ScriptNode.isRunnable(node,scriptUIManager));
//					if (node.isAvailable() == false) {
//						return "The format of this script is incorrect and it cannot be loaded.";
//					} else if (ScriptNode.isRunnable(node,scriptUIManager)) {
//						return "Press the button to run " + node.getFile().getName() + " or double click on its name to edit the script.";
//					} else {
//						return "Double click on the script's name to edit it.";
//					}
				}

				return null;
			}
		};
		tree.setToolTipText(""); // set this otherwise getTooltip not called.
		tree.setRootVisible(false);
		tree.setCellRenderer(new ButtonCellRenderer(scriptUIManager));
		tree.setCellEditor(new ButtonCellEditor(scriptUIManager));
		tree.setEditable(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setRowHeight(0);
		tree.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				ensureSelected(e);
				launchPopup(e);
			}

			private void ensureSelected(MouseEvent e) {
				// ensure correct one is selected
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path != null) {
					tree.getSelectionModel().setSelectionPath(path);
				}
			}

			private void launchPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					TreePath path = tree.getPathForLocation(e.getX(), e.getY());
					if (path != null && path.getLastPathComponent() != null) {
						popup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				ensureSelected(e);
				launchPopup(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// System.out.println("mouse clicked " + e.getSource());

			}
		});

		BasicTreeUI ui = new BasicTreeUI();
		tree.setUI(ui);
		ui.setLeftChildIndent(12);

		setFiles(new File[] {});
		listScrollPane = new JScrollPane(tree);

	}

	void addTreeSelectionListener(TreeSelectionListener listener){
		tree.getSelectionModel().addTreeSelectionListener(listener);
	}
	
	void updateAppearance() {
		if (tree != null) {
			tree.repaint();
		}
	}

	JScrollPane getScrollPane() {
		return listScrollPane;
	}

	ScriptNode getSelectedValue() {
		if (tree != null && tree.getLastSelectedPathComponent() != null) {
			return ((ScriptNode) tree.getLastSelectedPathComponent());
		}
		return null;
	}

	void setEnabled(boolean enabled) {
		tree.setEnabled(enabled);
	}

	void setSelected(ScriptNode node) {
		TreePath path = getPath(node);
		if (path != null) {
			tree.setSelectionPath(path);
		}
	}

	TreePath getPath(final ScriptNode findThis) {
		TreeNode root = (TreeNode) tree.getModel().getRoot();

		class Parser {
			TreePath parse(TreeNode current, ArrayList<TreeNode> path) {
				// copy path
				path = new ArrayList<>(path);

				// add current
				path.add(current);

				// check for match
				if (ScriptNode.class.isInstance(current)) {
					ScriptNode node = (ScriptNode) current;
					if (node.getFile().equals(findThis.getFile()) && Strings.equalsStd(node.getOption().getOptionId(), findThis.getOption().getOptionId())) {
						return new TreePath(path.toArray());
					}
				}

				// parse children
				for (int i = 0; i < current.getChildCount(); i++) {
					TreePath ret = parse((ScriptNode) current.getChildAt(i), path);
					if (ret != null) {
						return ret;
					}
				}
				return null;
			}
		}

		return new Parser().parse(root, new ArrayList<TreeNode>());
	}

	JTree getTreeControl(){
		return tree;
	}
	
	private ScriptNode buildTree(final File file, final Script script,Icon icon){
		// always build the root node
		ScriptNode ret= new ScriptNode(rootNode, file, script, script, icon);

		class Builder{
			void build(ScriptNode parent){
				if(parent.getOption()!=null){
					for(Option option:parent.getOption().getOptions()){
						ScriptNode childNode = new ScriptNode(parent, file, script, option,ScriptIcons.getIcon(scriptUIManager.getApi(),option));	
						build(childNode);
						parent.addChild(childNode);
					}					
				}
			}
		}
		
		new Builder().build(ret);
		return ret;
	}
	
	void setFiles(File[] files) {
		// save expanded state of the current trees
		HashMap<File, SaveExpandedState> state = SaveExpandedState.save(this);
	//	System.out.println("1:" + state);
		
		this.byType = new TreeMap<>();
		for (ScriptEditorType type : ScriptEditorType.values()) {
			byType.put(type, new ArrayList<File>());
		}
		
		this.items = new ScriptNode[files.length];
		ScriptIO scriptIO = ScriptIO.instance();
		for (int i = 0; i < items.length; i++) {
			final File file = files[i];

			Script script = null;
			ScriptEditorType type = null;
			try {
				script = scriptIO.fromFile(file);
				type = script.getScriptEditorUIType();
			} catch (Throwable e) {
				// to do.. show as unavailable?
			}

			// save by type
			if(type!=null){
				byType.get(type).add(file);					
			}

			Icon icon = null;
			if (script != null) {
				icon = ScriptIcons.getIcon(scriptUIManager.getApi(),script);
			}

			items[i] = buildTree(file, script, icon);

		}
		
		// set the new model and expand all rows to a certain depth
		tree.setModel(new DefaultTreeModel(rootNode));
		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			if(path == rootNode){
				tree.expandRow(i);
			}
			else{
				if(path!=null&&path.getLastPathComponent()!=null){
					ScriptNode node = (ScriptNode)path.getLastPathComponent();
					SaveExpandedState fileState = state.get(node.getFile());
					if(fileState!=null){
						Boolean expanded =node!=null&&node.getOption()!=null ?fileState.getByOptionId(node.getOption().getOptionId()):null;
						if(expanded!=null){
							if(expanded){
								tree.expandRow(i);
							}else{
								tree.collapseRow(i);
							}
						}
					
					}else if(node.getDepth()<2){
						tree.expandRow(i);
					}
				}	
			}

		//	tree.expandRow(i);
		}
		
		//System.out.println("2:" + state);
		
	}

	ScriptNode[] getScriptNodes() {
		return items;
	}

	public List<File> getScriptsByType(ScriptEditorType type) {
		return byType.get(type);
	}
	
	
	public List<ScriptNode> getLoadedScripts(){
		ArrayList<ScriptNode> nodes = new ArrayList<>();
		if(items!=null){
			for(ScriptNode item:items){
				if(item.getScript()!=null){
					nodes.add(item);
				}
			}			
		}
		return nodes;
	}

	@Override
	public ScriptsProvider getScriptsProvider() {
		final ArrayList<ScriptNode> nodes = new ArrayList<>();
		final ArrayList<Script> scripts = new ArrayList<>();
		if(items!=null){
			for(ScriptNode item:items){
				if(item.getScript()!=null){
					nodes.add(item);
					scripts.add(item.getScript());
				}
			}			
		}
		return new ScriptsProvider() {
			
			@Override
			public Iterator<Script> iterator() {
				return scripts.iterator();
			}
			
			@Override
			public int size() {
				return scripts.size();
			}
			
			@Override
			public File getFile(int i) {
				return nodes.get(i).getFile();
			}
			
			@Override
			public Script get(int i) {
				return scripts.get(i);
			}
		};
	}

}
