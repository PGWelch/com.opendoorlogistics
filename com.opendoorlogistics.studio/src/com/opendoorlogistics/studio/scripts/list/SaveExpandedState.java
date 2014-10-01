/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.list;

import java.io.File;
import java.util.HashMap;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class SaveExpandedState {
	private final HashMap<String, Boolean> map = new HashMap<>();
	
	public SaveExpandedState( ScriptsTree tree, ScriptNode scriptRoot) {
		read(tree, scriptRoot);
	}
	
	private void read(ScriptsTree tree, ScriptNode node){
		TreePath path = tree.getPath(node);
		if(path!=null){
			int row = tree.getTreeControl().getRowForPath(path);
			if(row==-1 || tree.getTreeControl().isExpanded(row)==false){
				map.put(node.getOption().getOptionId(), false);
			}else{
				map.put(node.getOption().getOptionId(), true);
			}
		}
		for(int i=0;i<node.getChildCount();i++){
			read(tree,(ScriptNode) node.getChildAt(i));
		}
	}

	@Override
	public String toString(){
		return map.toString();
	}
	
	public static HashMap<File, SaveExpandedState> save(ScriptsTree tree){
		TreeNode root =(TreeNode) tree.getTreeControl().getModel().getRoot();
		HashMap<File, SaveExpandedState>ret = new HashMap<>();
		for(int i =0 ; i<root.getChildCount();i++){
			ScriptNode node = (ScriptNode)root.getChildAt(i);
			if(node.getChildCount()>0){
				SaveExpandedState state = new SaveExpandedState(tree, node);
				ret.put(node.getFile(), state);				
			}
		}
		return ret;
	}
	
	Boolean getByOptionId(String optionId){
		return map.get(optionId);
	}
}
