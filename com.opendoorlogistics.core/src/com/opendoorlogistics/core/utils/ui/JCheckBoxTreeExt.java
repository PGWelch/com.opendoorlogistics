/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.opendoorlogistics.codefromweb.JCheckBoxTree;
import com.opendoorlogistics.core.utils.StringIdTreeNode;
import com.opendoorlogistics.core.utils.strings.HasStringId;

/**
 * A tree with checkboxes which allows its state to be saved before
 * the treemodel is replaced and then restored as best possible - so 
 * if nodes are added or removed the existing nodes should stay the same
 * @author Phil
 *
 */
public class JCheckBoxTreeExt extends JCheckBoxTree{
	
	public static interface TreeNodeStateRetriever<TS>{
		TS getState(TreePath path);
	}
	
	private void applyExpandedState(StringIdTreeNode<Boolean> expandedState){
		for (int i = 0; i < getRowCount(); i++) {
			TreePath path = getPathForRow(i);
			
			if(path.getLastPathComponent() == getModel().getRoot()){
				// always expand the root
				expandPath(path);
			}else{
				Boolean expanded = expandedState.findLeaf(getIdPathExcludeRoot(path));
				if(expanded!=null && expanded){
					expandPath(path);
				}
			}
		}
	}
	
	public static class CheckBoxTreeState{
		private final StringIdTreeNode<Boolean> expandedState;
		private final StringIdTreeNode<Boolean> checkedState;
		
		public StringIdTreeNode<Boolean> getExpandedState() {
			return expandedState;
		}
		
		public StringIdTreeNode<Boolean> getCheckedState() {
			return checkedState;
		}
		public CheckBoxTreeState(StringIdTreeNode<Boolean> expandedState, StringIdTreeNode<Boolean> checkedState) {
			super();
			this.expandedState = expandedState;
			this.checkedState = checkedState;
		}
		
		
	}
	
	private void applyCheckedState(final StringIdTreeNode<Boolean> checkedState){

		Object root = getModel().getRoot();
		if(root==null){
			return;
		}

		class Recurse{
			void recurse(TreePath path){
				Boolean checked = checkedState.findLeaf(getIdPathExcludeRoot(path));
				if(checked!=null){
					if(isChecked(path)!=checked){
						toggleCheckState(path);
					}
				}
				
				Object node = path.getLastPathComponent();
				TreeModel model = getModel();
				int n = model.getChildCount(node);
				for(int i =0 ; i<n;i++){
					recurse(path.pathByAddingChild(model.getChild(node, i)));
				}
			}
		}
		
		new Recurse().recurse(new TreePath(root));
	}
	
	private String[] getIdPathExcludeRoot(TreePath path){
		Object[]objs = path.getPath();
		String[] ret = new String[objs.length-1];
		for(int i =0 ; i < ret.length ;i++){
			ret[i]=getId(objs[i+1]);
		}
		return ret;
	}
	
	public CheckBoxTreeState saveState(){
		return new CheckBoxTreeState(saveExpandedState(), saveCheckedState());
	}
	
	public void restoreState(CheckBoxTreeState state){
		applyExpandedState(state.getExpandedState());
		applyCheckedState(state.getCheckedState());
	}
	
	private StringIdTreeNode<Boolean> saveExpandedState(){
		Object root = getModel().getRoot();	
		if(root!=null){
			return recurseSaveState(null, new TreePath(root), new TreeNodeStateRetriever<Boolean>() {
				
				@Override
				public Boolean getState(TreePath path) {
					return isExpanded(path);
				}
			});
		}
		return null;
	}


	public StringIdTreeNode<Boolean> saveCheckedState(){
		Object root = getModel().getRoot();	
		if(root!=null){
			return recurseSaveState(null, new TreePath(root), new TreeNodeStateRetriever<Boolean>() {
				
				@Override
				public Boolean getState(TreePath path) {
					return isChecked(path);
				}
			});
		}
		return null;
	}

	private <T> StringIdTreeNode<T> recurseSaveState( StringIdTreeNode<T> parent, TreePath path,TreeNodeStateRetriever<T> stateRetriever ){
	//	TreePath path ;
		Object treeNode =path.getLastPathComponent();
		String id = getId(treeNode);
		T state = stateRetriever.getState(path);
		StringIdTreeNode<T> ret;
		if(parent!=null){
			ret = parent.add(id, state);
		}else{
			ret = new StringIdTreeNode<>(id,null);	
			ret.setLeaf(state);
		}
		
		TreeModel model = getModel();
		int n = model.getChildCount(treeNode);
		for(int i =0 ; i<n;i++){
			Object child = model.getChild(treeNode, i);
			recurseSaveState(ret, path.pathByAddingChild(child),stateRetriever);
		}
		
		return ret;
	}
	
	private String getId(Object o){
		if(HasStringId.class.isInstance(o)){
			return ((HasStringId)o).getId();
		}
		return o.toString();
	}
	
    public static void main(String args[]) {
    	class TestFrame extends JFrame {
    	    private static final long serialVersionUID = 4648172894076113183L;
    		private JCheckBoxTreeExt tree;

    	    public TestFrame() {
    	        super();
    	        setSize(500, 500);
    	        getContentPane().setLayout(new BorderLayout());
    	        tree = new JCheckBoxTreeExt();
    	        getContentPane().add(tree,BorderLayout.CENTER);     
    	        setDefaultCloseOperation(EXIT_ON_CLOSE);
    	        
    	        class Tester{
    	        	void test(boolean restore){
    	        		// save stats
    	    			StringIdTreeNode<Boolean> checkState = tree.saveCheckedState();
						StringIdTreeNode<Boolean> expanded = tree.saveExpandedState();
						
						tree.setModel(JTree.getDefaultTreeModel());

		    	        // restore state
		    	        if(restore){
			    	        tree.applyCheckedState(checkState);
			    	        tree.applyExpandedState(expanded);				    	        	
		    	        }
		    	       
		    	        tree.initCellRenderer();

    	        	}
    	        }
    	        
    	        JToolBar toolBar = new JToolBar();
    	        JButton restore = new JButton("Restore");
    	        restore.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						new Tester().test(true);
					}
				});
    	        toolBar.add(restore);

    	        
    	        JButton norestore = new JButton("No restore");
    	        norestore.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						new Tester().test(false);
					}
				});
    	        toolBar.add(norestore);
    	        getContentPane().add(toolBar,BorderLayout.SOUTH);
    	    }

    	}
    	
    	SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
		    	TestFrame m = new TestFrame();
		        m.setVisible(true);
			}
		});

    }
}
