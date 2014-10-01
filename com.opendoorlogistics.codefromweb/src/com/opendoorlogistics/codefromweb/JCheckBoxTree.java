/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.codefromweb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * See http://stackoverflow.com/questions/21847411/java-swing-need-a-good-quality-developed-jtree-with-checkboxes
 * 
 */
public class JCheckBoxTree extends JTree {

	private static final long serialVersionUID = -4194122328392241790L;

	JCheckBoxTree selfPointer = this;

	// Defining data structure that will enable to fast check-indicate the state of each node
	// It totally replaces the "selection" mechanism of the JTree
	private class CheckedNode {
		boolean isSelected;
		boolean hasChildren;
		boolean allChildrenSelected;

		public CheckedNode(boolean isSelected_, boolean hasChildren_, boolean allChildrenSelected_) {
			isSelected = isSelected_;
			hasChildren = hasChildren_;
			allChildrenSelected = allChildrenSelected_;
		}
	}

	HashMap<TreePath, CheckedNode> nodesCheckingState;
	HashSet<TreePath> checkedPaths = new HashSet<TreePath>();

	// Defining a new event type for the checking mechanism and preparing event-handling mechanism
	protected EventListenerList listenerList = new EventListenerList();

	public class CheckChangeEvent extends EventObject {
		private static final long serialVersionUID = -8100230309044193368L;

		public CheckChangeEvent(Object source) {
			super(source);
		}
	}

	public interface CheckChangeEventListener extends EventListener {
		public void checkStateChanged(CheckChangeEvent event);
	}

	public void addCheckChangeEventListener(CheckChangeEventListener listener) {
		listenerList.add(CheckChangeEventListener.class, listener);
	}

	public void removeCheckChangeEventListener(CheckChangeEventListener listener) {
		listenerList.remove(CheckChangeEventListener.class, listener);
	}

	void fireCheckChangeEvent(CheckChangeEvent evt) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = 0; i < listeners.length; i++) {
			if (listeners[i] == CheckChangeEventListener.class) {
				((CheckChangeEventListener) listeners[i + 1]).checkStateChanged(evt);
			}
		}
	}

	// Override
	public void setModel(TreeModel newModel) {
		super.setModel(newModel);
		resetCheckingState();
		
		// Added by Phil W. Reinit cell renderer as getting rendering problems
		initCellRenderer();
	}

	// New method that returns only the checked paths (totally ignores original "selection" mechanism)
	public TreePath[] getCheckedPaths() {
		return checkedPaths.toArray(new TreePath[checkedPaths.size()]);
	}

	// Returns true in case that the node is selected, has children but not all of them are selected
	public boolean isSelectedPartially(TreePath path) {
		CheckedNode cn = nodesCheckingState.get(path);
		return cn.isSelected && cn.hasChildren && !cn.allChildrenSelected;
	}

	private void resetCheckingState() {
		nodesCheckingState = new HashMap<TreePath, CheckedNode>();
		checkedPaths = new HashSet<TreePath>();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) getModel().getRoot();
		if (node == null) {
			return;
		}
		addSubtreeToCheckingStateTracking(node);
	}

	// Creating data structure of the current model for the checking mechanism
	private void addSubtreeToCheckingStateTracking(DefaultMutableTreeNode node) {
		TreeNode[] path = node.getPath();
		TreePath tp = new TreePath(path);
		CheckedNode cn = new CheckedNode(false, node.getChildCount() > 0, false);
		nodesCheckingState.put(tp, cn);
		for (int i = 0; i < node.getChildCount(); i++) {
			addSubtreeToCheckingStateTracking((DefaultMutableTreeNode) tp.pathByAddingChild(node.getChildAt(i)).getLastPathComponent());
		}
	}

	// Overriding cell renderer by a class that ignores the original "selection" mechanism
	// It decides how to show the nodes due to the checking-mechanism
	private class CheckBoxCellRenderer extends JPanel implements TreeCellRenderer {
		private static final long serialVersionUID = -7341833835878991719L;
		JCheckBox checkBox;

		public CheckBoxCellRenderer() {
			super();
			this.setLayout(new BorderLayout());
			checkBox = new JCheckBox();
			add(checkBox, BorderLayout.CENTER);
			setOpaque(false);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object obj = node.getUserObject();
			TreePath tp = new TreePath(node.getPath());
			CheckedNode cn = nodesCheckingState.get(tp);
			if (cn == null) {
				return this;
			}
			checkBox.setSelected(cn.isSelected);
			checkBox.setText(obj.toString());
			checkBox.setOpaque(cn.isSelected && cn.hasChildren && !cn.allChildrenSelected);
			
			return this;
		}

	}

	public JCheckBoxTree() {
		super();
		// Disabling toggling by double-click
		this.setToggleClickCount(0);
		// Overriding cell renderer by new one defined above
		initCellRenderer();

		// Overriding selection model by an empty one
		DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel() {
			private static final long serialVersionUID = -8190634240451667286L;

			// Totally disabling the selection mechanism
			public void setSelectionPath(TreePath path) {
			}

			public void addSelectionPath(TreePath path) {
			}

			public void removeSelectionPath(TreePath path) {
			}

			public void setSelectionPaths(TreePath[] pPaths) {
			}
		};
		// Calling checking mechanism on mouse click
		this.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent arg0) {
				TreePath tp = selfPointer.getPathForLocation(arg0.getX(), arg0.getY());
				if (tp == null) {
					return;
				}
				toggleCheckState(tp);
			}

			public void mouseEntered(MouseEvent arg0) {
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent arg0) {
			}

			public void mouseReleased(MouseEvent arg0) {
			}
		});
		this.setSelectionModel(dtsm);
	}

	/**
	 * 
	 */
	protected void initCellRenderer() {
		CheckBoxCellRenderer cellRenderer = new CheckBoxCellRenderer();
		this.setCellRenderer(cellRenderer);
	}

	// When a node is checked/unchecked, updating the states of the predecessors
	protected void updatePredecessorsWithCheckMode(TreePath tp, boolean check) {
		TreePath parentPath = tp.getParentPath();
		// If it is the root, stop the recursive calls and return
		if (parentPath == null) {
			return;
		}
		CheckedNode parentCheckedNode = nodesCheckingState.get(parentPath);
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
		parentCheckedNode.allChildrenSelected = true;
		parentCheckedNode.isSelected = false;
		for (int i = 0; i < parentNode.getChildCount(); i++) {
			TreePath childPath = parentPath.pathByAddingChild(parentNode.getChildAt(i));
			CheckedNode childCheckedNode = nodesCheckingState.get(childPath);
			// It is enough that even one subtree is not fully selected
			// to determine that the parent is not fully selected
			if (!childCheckedNode.allChildrenSelected) {
				parentCheckedNode.allChildrenSelected = false;
			}
			// If at least one child is selected, selecting also the parent
			if (childCheckedNode.isSelected) {
				parentCheckedNode.isSelected = true;
			}
		}
		if (parentCheckedNode.isSelected) {
			checkedPaths.add(parentPath);
		} else {
			checkedPaths.remove(parentPath);
		}
		// Go to upper predecessor
		updatePredecessorsWithCheckMode(parentPath, check);
	}

	// Recursively checks/unchecks a subtree
	protected void checkSubTree(TreePath tp, boolean check) {
		CheckedNode cn = nodesCheckingState.get(tp);
		cn.isSelected = check;
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
		for (int i = 0; i < node.getChildCount(); i++) {
			checkSubTree(tp.pathByAddingChild(node.getChildAt(i)), check);
		}
		cn.allChildrenSelected = check;
		if (check) {
			checkedPaths.add(tp);
		} else {
			checkedPaths.remove(tp);
		}
	}

	/**
	 * Check or uncheck the entire tree
	 * @param check
	 */
	public void checkAll(boolean check){
		TreePath path = new TreePath(getModel().getRoot());
		checkSubTree(path, check);
		afterSubtreeCheck(path, check);
	}
	
	public static void main(String args[]) {
		class TestFrame extends JFrame {

			private static final long serialVersionUID = 4648172894076113183L;

			public TestFrame() {
				super();
				setSize(500, 500);
				this.getContentPane().setLayout(new BorderLayout());
				final JCheckBoxTree cbt = new JCheckBoxTree();
				this.getContentPane().add(cbt);
				cbt.addCheckChangeEventListener(new JCheckBoxTree.CheckChangeEventListener() {
					public void checkStateChanged(JCheckBoxTree.CheckChangeEvent event) {
						System.out.println("event");
						TreePath[] paths = cbt.getCheckedPaths();
						for (TreePath tp : paths) {
							for (Object pathPart : tp.getPath()) {
								System.out.print(pathPart + ",");
							}
							System.out.println();
						}
					}
				});
				this.setDefaultCloseOperation(EXIT_ON_CLOSE);
			}

		}
		TestFrame m = new TestFrame();
		m.setVisible(true);
	}

	/**
	 * Retrieve check state for a tree path. Added by Phil W
	 * 
	 * @param path
	 * @return
	 */
	public boolean isChecked(TreePath path) {
		CheckedNode cn = nodesCheckingState.get(path);
		if (cn != null) {
			return cn.isSelected;
		}
		return false;
	}

	/**
	 * Method moved from the MouseListener by Phil W to enable access outside the class
	 * 
	 * @param tp
	 */
	public void toggleCheckState(TreePath tp) {
		boolean checkMode = !nodesCheckingState.get(tp).isSelected;
		checkSubTree(tp, checkMode);
		afterSubtreeCheck(tp, checkMode);
	}

	/**
	 * @param tp
	 * @param checkMode
	 */
	private void afterSubtreeCheck(TreePath tp, boolean checkMode) {
		updatePredecessorsWithCheckMode(tp, checkMode);
		// Firing the check change event
		fireCheckChangeEvent(new CheckChangeEvent(new Object()));
		// Repainting tree after the data structures were updated
		selfPointer.repaint();
	}

}