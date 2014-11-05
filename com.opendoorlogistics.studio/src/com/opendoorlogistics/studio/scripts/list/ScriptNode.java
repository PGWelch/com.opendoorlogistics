/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.list;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptEditorType;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;

public class ScriptNode implements MutableTreeNode{
	final private TreeNode parent;
	final private File file;
	final private Script script;
	final private Icon icon;
	final private Option option;
	final private ArrayList<ScriptNode> children = new ArrayList<>();
	final private boolean runnable;
	
	ScriptNode(TreeNode parent,File file, Script script,Option option ,Icon icon) {
		this.parent =parent;
		this.file = file;
		this.icon = icon;
		this.script = script;
		this.option = option;
		this.runnable = ScriptUtils.isRunnableOption(option);
	}

	public File getFile() {
		return file;
	}
	
	public boolean isRunnable(){
		return runnable;
	}
	
	public boolean isAvailable(){
		return script!=null;
	}

	public ScriptEditorType getType() {
		return script!=null ? script.getScriptEditorUIType() :null;
	}
	
	public Icon getIcon() {
		return icon;
	}

	public Script getScript(){
		return script;
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		return children.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return children.size();
	}

	@Override
	public TreeNode getParent() {
		return parent;
	}

	@Override
	public int getIndex(TreeNode node) {
		for(int i =0 ; i<getChildCount();i++){
			if(getChildAt(i)==node){
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public boolean isLeaf() {
		return children.size()==0;
	}

	@Override
	public Enumeration children() {
		return new Enumeration<TreeNode>() {
			int i=-1;
			
			@Override
			public boolean hasMoreElements() {
				return (i+1)<getChildCount();
			}

			@Override
			public TreeNode nextElement() {
				return getChildAt(++i);
			}
		};
	}

	@Override
	public void insert(MutableTreeNode child, int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(MutableTreeNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUserObject(Object object) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeFromParent() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParent(MutableTreeNode newParent) {
		// TODO Auto-generated method stub
		
	}
	
	public String getDisplayName(){
		if(option == script){
			return file.getName();
		}else if(Strings.isEmpty(option.getName())==false){
			return option.getName();
		}else{
			return option.getOptionId();
		}
	}
	
	public String [] getLaunchExecutorId(){
		return option!=script? new String[]{option.getOptionId()}:null;	
	}
	
	public String getLaunchEditorId(){
		return option!=script?option.getOptionId():null;
	}
	
	public static boolean isRunnable(ScriptNode node, ScriptUIManager manager){
		return node != null && manager.hasLoadedData() && node.isAvailable() && node.isRunnable();
	}
	
	Option getOption(){
		return option;
	}
	
	boolean isScriptRoot(){
		return getOption()==getScript();	
	}
	
	void addChild(ScriptNode node){
		children.add(node);
	}
	
	int getDepth(){
		int ret = 0;
		TreeNode node = this;
		while(node.getParent() != null){
			node = node.getParent();
			ret++;
		}
		return ret;
	}
	
	public String getTooltip(boolean isRunnable){
		if(option!=null && option.getEditorLabel()!=null && option.getEditorLabel().length()>0){
			return option.getEditorLabel();
		}

		String name = getDisplayName();
		if (isAvailable() == false) {
			return "The format of this script is incorrect and it cannot be loaded.";
		} else if (isRunnable) {
			return "Press the icon to run " + name + " or double click on its name to edit the option.";
		} else {
			return "Double click on the option's name to edit it.";
		}
			}
}
