/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.opendoorlogistics.core.utils.strings.HasStringId;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * A node where child nodes have unique Stringids and are also ordered.
 * @author Phil
 *
 * @param <T>
 */
public class StringIdTreeNode<T> implements HasStringId, Iterable<StringIdTreeNode<T>>{
	private final String id;
	private final ArrayList<StringIdTreeNode<T>> children = new ArrayList<>();
	private final StringIdTreeNode<T> parent;
	private T leaf;
	
	public StringIdTreeNode(String id, StringIdTreeNode<T> parent) {
		super();
		this.id = id;
		this.parent = parent;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public Iterator<StringIdTreeNode<T>> iterator() {
		return children.iterator();
	}

	public T getLeaf() {
		return leaf;
	}

	public void setLeaf(T leaf) {
		this.leaf = leaf;
	}
	
	/**
	 * Add the node. The node will not add if the string is already present.
	 * @param id
	 * @param leaf
	 * @return The node object if successfully added.
	 */
	public StringIdTreeNode<T> add(String id, T leaf){
		if(get(id)==null){
			StringIdTreeNode<T> child = new StringIdTreeNode<>(id, this);
			child.setLeaf(leaf);
			children.add(child);
			return child;
		}
		return null;
	}
	

	public StringIdTreeNode<T> get(String id){
		int index= getIndex(id);
		if(index!=-1){
			return children.get(index);
		}
		return null;
	}

	public StringIdTreeNode<T> get(int index){
		return children.get(index);
	}
	
	public int getNbChildren(){
		return children.size();
	}
	
	public  StringIdTreeNode<T>  remove(int index){
		return children.remove(index);
	}
	
	/**
	 * @param id
	 * @return
	 */
	public int getIndex(String id) {
		int n = children.size();
		for(int i =0 ; i< n ; i++){
			if(Strings.equalsStd(id, children.get(i).getId())){
				return i;
			}			
		}
		return -1;
	}
	
	public StringIdTreeNode<T> find(String [] ids){
		 StringIdTreeNode<T> ret = this;
		 for(int i =0 ; i < ids.length && ret!=null;i++){
			 ret = ret.get(ids[i]);
		 }
		 return ret;
	}
	
	public T findLeaf(String [] ids){
		StringIdTreeNode<T> node = find(ids);
		if(node!=null){
			return node.getLeaf();
		}
		return null;
	}
	
	private void toString(StringBuilder builder, int nbIndents){
		builder.append(Strings.getTabs(nbIndents) + getId() + " = " + (leaf!=null? leaf.toString() : "n/a") + System.lineSeparator());
		for(StringIdTreeNode<T> child:children){
			child.toString(builder, nbIndents+1);
		}
	}
	
	@Override
	public String toString(){
		StringBuilder b = new StringBuilder();
		toString(b, 0);
		return b.toString();
	}
	
	public StringIdTreeNode<T> getParent(){
		return parent;
	}
	
//	private static class MyTreeNode<T> implements TreeNode{
//		
//		private final StringIdTreeNode<T> sidnode;
//		
//		private MyTreeNode(StringIdTreeNode<T> sidnode) {
//			this.sidnode = sidnode;
//		}
//
//		@Override
//		public boolean isLeaf() {
//			return sidnode.getNbChildren()==0;
//		}
//		
//		@Override
//		public TreeNode getParent() {
//			return sidnode.getParent()!=null ? sidnode.getParent().createJTreeNodeWrapper() : null;
//		}
//		
//		@Override
//		public int getIndex(TreeNode node) {
//			MyTreeNode<T> mtn = (MyTreeNode<T>)node;
//			return sidnode.getIndex(mtn.sidnode.getId());
//		}
//		
//		@Override
//		public int getChildCount() {
//			return sidnode.getNbChildren();
//		}
//		
//		@Override
//		public TreeNode getChildAt(int childIndex) {
//			return sidnode.get(childIndex).createJTreeNodeWrapper();
//		}
//		
//		@Override
//		public boolean getAllowsChildren() {
//			return true;
//		}
//		
//		@Override
//		public Enumeration children() {
//			final Iterator<StringIdTreeNode<T>> it = sidnode.iterator();
//			return new Enumeration<Object>() {
//
//				@Override
//				public boolean hasMoreElements() {
//					return it.hasNext();
//				}
//
//				@Override
//				public Object nextElement() {
//					return it.next().createJTreeNodeWrapper();
//				}
//			};
//		}
//	};
	

	
	public DefaultMutableTreeNode exportToJTree(DefaultMutableTreeNode parent){
		DefaultMutableTreeNode me = new DefaultMutableTreeNode(id);
		if(parent!=null){
			parent.add(me);
		}
		
		for(StringIdTreeNode<T> child:children){
			child.exportToJTree(me);
		}
		return me;
	}
}
