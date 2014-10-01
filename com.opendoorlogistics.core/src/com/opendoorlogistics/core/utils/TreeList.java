/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A very basic binary search tree which is augmented to allow calculation of a node's index or retrieving by index in log2(n) time. Client code can
 * keep a track of node's index by retaining the reference to the node.
 * 
 * The implementation of rebalancing is very primitive - it rebuilds the entire tree after a number of modifications, where the rebuild is done in
 * random ordering to enusre on-average balancing.
 * 
 * @author Phil
 * 
 * @param <E>
 */
final public class TreeList<E> implements Iterable<TreeList<E>.TreeListNode> {
	private TreeListNode root;
	private final boolean rebuildsAllowed;
	private int nbModificationsSinceRebuild;
	private int nbGetsWithUnbalancedTree;
	private boolean debugChecks = false;

	public TreeList() {
		this(true);
	}

	public TreeList(boolean rebuildsAllowed) {
		this.rebuildsAllowed = rebuildsAllowed;
	}

	public boolean isDebugChecks() {
		return debugChecks;
	}

	public void setDebugChecks(boolean debugChecks) {
		this.debugChecks = debugChecks;
	}

	public int size() {
		if (root == null) {
			return 0;
		}
		return root.size();
	}

	public TreeListNode add(E value) {
		return insert(size(), value);
	}

	public boolean isValidReferences() {
		if (root != null) {
			return root.isValidReferences(true);
		}
		return true;
	}

	public TreeList<E> deepCopy(DeepCopier<E> valueCopier) {
		TreeList<E> ret = new TreeList<>(rebuildsAllowed);
		ret.debugChecks = debugChecks;
		ret.nbGetsWithUnbalancedTree = nbGetsWithUnbalancedTree;
		ret.nbModificationsSinceRebuild = nbModificationsSinceRebuild;
		if (root != null) {
			ret.root = root.deepCopy(valueCopier, null);
		}
		return ret;
	}

	/**
	 * Append in bulk, rebuilding whole tree at same time to ensure optimality
	 * 
	 * @param values
	 * @return
	 */
	public List<TreeListNode> bulkAppend(List<E> values) {
		int n = values.size();
		ArrayList<TreeListNode> ret = new ArrayList<>(n);

		if (values.size() > 0) {
			ArrayList<TreeListNode> nodes = rebuildStep1();

			// add the new nodes
			nodes.ensureCapacity(nodes.size() + n);
			for (E val : values) {
				TreeListNode node = new TreeListNode(val);
				ret.add(node);
				nodes.add(node);
			}

			// do rebuild...
			buildOptimalFromEmpty(nodes);
			rebuildChecks(nodes);

			// int originalSize = size();
			//
			// // insert in randomised order (should be close to optimal)
			// TIntArrayList tmpIndices = new TIntArrayList(n);
			// for(int i =0 ; i<n;i++){
			// tmpIndices.add(i);
			// ret.add(null);
			// }
			// tmpIndices.shuffle(new Random(123));
			// for(int i = 0 ; i<n ; i++){
			// int srcIndx = tmpIndices.get(i);
			// int destIndx = originalSize + srcIndx;
			// TreeListNode node = insert(destIndx, values.get(srcIndx));
			// ret.set(srcIndx, node);
			// }
			//
		}

		// return object has the same order as input object
		return ret;
	}

	public TreeListNode insert(int index, E value) {
		int initialSize = size();
		TreeListNode ret = new TreeListNode(value);
		if (root == null) {
			root = ret;
		} else {

			TreeListNode current = root;
			int currentIndx = root.getNbLeftDescendents();

			while (true) {
				if (debugChecks && currentIndx != current.getIndex()) {
					throw new RuntimeException();
				}

				if (index <= currentIndx) {
					// goes before
					if (current.leftChild == null) {
						current.setLeftChild(ret);
						break;
					} else {
						// update currentIndx
						currentIndx -= 1 + current.leftChild.getNbRightDescendents();

						// go to the left child
						current = current.leftChild;

					}
				} else {
					// goes after
					if (current.rightChild == null) {
						current.setRightChild(ret);
						break;
					} else {
						// update currentIndx
						currentIndx += 1 + current.rightChild.getNbLeftDescendents();

						// go to right child
						current = current.rightChild;
					}
				}

			}
		}

		// debug checks are cpu intensive so only done if activated
		if (debugChecks && !isValidReferences()) {
			throw new RuntimeException();
		}

		if (!rebuildIfNeeded()) {
			nbModificationsSinceRebuild++;
		}

		if (debugChecks && !isValidReferences()) {
			throw new RuntimeException();
		}

		if (debugChecks && size() != initialSize + 1) {
			throw new RuntimeException();
		}

		if (debugChecks && ret.getIndex() != index) {
			throw new RuntimeException();
		}
		return ret;
	}

	public TreeListNode removeAt(int index) {
		int initialSize = size();
		TreeListNode node = get(index);
		if (node == null) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		node.remove();

		if (!rebuildIfNeeded()) {
			nbModificationsSinceRebuild++;
		}

		if (debugChecks && !isValidReferences()) {
			throw new RuntimeException();
		}

		if (size() != initialSize - 1) {
			throw new RuntimeException();
		}
		return node;
	}

	public TreeListNode get(int index) {
		if (nbModificationsSinceRebuild > 0 && !rebuildIfNeeded()) {
			nbGetsWithUnbalancedTree++;
		}

		if (root == null) {
			throw new ArrayIndexOutOfBoundsException(index);
		} else {
			TreeListNode node = root;
			while (true) {
				int leftSize = 0;
				if (node.leftChild != null) {
					leftSize = node.leftChild.size();
				}

				// test to see if we should descend left or right or return this index
				if (index < leftSize) {
					node = node.leftChild;
				} else if (index == leftSize) {
					return node;
				} else {
					node = node.rightChild;
					index = index - leftSize - 1;
				}
			}
		}
	}

	private void setChild(TreeListNode newParent, TreeListNode child, boolean isLeftChild) {
		if (newParent == null) {
			if (root != null) {
				throw new RuntimeException();
			}
			root = child;
		} else {
			newParent.setChild(child, isLeftChild);
		}
	}

	public ArrayList<E> toArrayList() {
		ArrayList<E> ret = new ArrayList<>(size());
		for (TreeListNode node : this) {
			ret.add(node.getValue());
		}
		return ret;
	}

	/**
	 * Rebuild the binary search tree ensuring ordering of nodes is the same but nodes are inserted in random order, so the tree should be balanced on
	 * average.
	 */
	private boolean rebuildIfNeeded() {
		if (!rebuildsAllowed) {
			return false;
		}

		if (nbModificationsSinceRebuild == 0) {
			return false;
		}

		// check if we should rebuild
		int halfSize = Math.max(size() / 2, 1);
		if (nbModificationsSinceRebuild < halfSize && nbGetsWithUnbalancedTree < halfSize) {
			return false;
		}

		rebuild();
		return true;
	}

	/**
	 * Rebuild the binary tree with (more-or-less) optimal balancing
	 */
	public void rebuild() {

		// get all children
		if (size() == 0) {
			return;
		}

		List<TreeListNode> nodes = rebuildStep1();
		buildOptimalFromEmpty(nodes);

		rebuildChecks(nodes);

		// int finalHeight = 0;
		// if (root != null) {
		// finalHeight = root.getHeight();
		// }
		// if (finalHeight > initialHeight) {
		// throw new RuntimeException();
		// }
		return;
	}

	private void rebuildChecks(List<TreeListNode> nodes) {
		// do checks
		if (size() != nodes.size()) {
			throw new RuntimeException();
		}

		int n = nodes.size();
		for (int i = 0; i < n; i++) {
			if (get(i) != nodes.get(i)) {
				throw new RuntimeException();
			}
		}
	}

	private ArrayList<TreeListNode> rebuildStep1() {
		nbModificationsSinceRebuild = 0;
		nbGetsWithUnbalancedTree = 0;

		// save all nodes in correct order
		ArrayList<TreeListNode> nodes = new ArrayList<>(size());
		for (TreeListNode node : this) {
			nodes.add(node);
		}

		// clear tree
		root = null;
		for (TreeListNode node : nodes) {
			node.clearReferences();
		}
		return nodes;
	}

	private void buildOptimalFromEmpty(List<TreeListNode> nodes) {
		if (root != null) {
			throw new UnsupportedOperationException();
		}

		// optimally split
		class BuildHelper {
			TreeListNode split(List<TreeListNode> list) {
				// assign node for the centre
				int centre = list.size() / 2;
				TreeListNode centreNode = list.get(centre);

				List<TreeListNode> left = list.subList(0, centre);
				if (left.size() > 0) {
					centreNode.setLeftChild(split(left));
				}

				if (centre + 1 < list.size()) {
					List<TreeListNode> right = list.subList(centre + 1, list.size());
					if (right.size() > 0) {
						centreNode.setRightChild(split(right));
					}
				}
				return centreNode;
			}
		}
		BuildHelper helper = new BuildHelper();

		if (nodes.size() > 0) {
			root = helper.split(nodes);
		}
	}

	// private static int DEBUG_CALL_NB=0;

	public class TreeListNode {
		private final E value;
		private TreeListNode leftChild;
		private TreeListNode rightChild;
		private TreeListNode parent;
		private int nbChildren;

		public TreeListNode(E value) {
			this.value = value;
		}

		public TreeListNode deepCopy(DeepCopier<E> valueCopier, TreeListNode copiedParent) {
			TreeListNode ret = new TreeListNode(value != null ? valueCopier.deepCopy(value) : null);
			if (leftChild != null) {
				ret.leftChild = leftChild.deepCopy(valueCopier, ret);
			}
			if (rightChild != null) {
				ret.rightChild = rightChild.deepCopy(valueCopier, ret);
			}
			ret.parent = copiedParent;
			ret.nbChildren = nbChildren;
			return ret;
		}

		public boolean isDescendedFrom(TreeListNode node) {
			TreeListNode p = parent;
			while (p != null) {
				if (p == node) {
					return true;
				}
				p = p.parent;
			}
			return false;
		}

		public int getNbLeftDescendents() {
			return leftChild != null ? leftChild.size() : 0;
		}

		public int getNbRightDescendents() {
			return rightChild != null ? rightChild.size() : 0;
		}

		public boolean isValidReferences(boolean recurse) {
			// check either have a parent or I'm the root
			if (parent == null) {
				if (root != this) {
					return false;
				}
			}

			// check parent points to me
			if (parent != null && (parent.leftChild != this && parent.rightChild != this)) {
				return false;
			}

			// check left child points to me
			if (leftChild != null && leftChild.parent != this) {
				return false;
			}

			// check right child points to me
			if (rightChild != null && rightChild.parent != this) {
				return false;
			}

			// check children are different
			if (leftChild != null && leftChild == rightChild) {
				return false;
			}

			// recurse
			if (recurse && leftChild != null && leftChild.isValidReferences(true) == false) {
				return false;
			}

			if (recurse && rightChild != null && rightChild.isValidReferences(true) == false) {
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			return value != null ? value.toString() : "";
		}

		public E getValue() {
			return value;
		}

		// private int getHeight() {
		// int ret = 1;
		// if (leftChild != null) {
		// ret = Math.max(ret, 1 + leftChild.getHeight());
		// }
		// if (rightChild != null) {
		// ret = Math.max(ret, 1 + rightChild.getHeight());
		// }
		// return ret;
		// }

		private void remove() {
			// DEBUG_CALL_NB++;

			int initialSize = TreeList.this.size();

			// save references
			TreeListNode oldLeftChild = leftChild;
			TreeListNode oldRightChild = rightChild;
			TreeListNode oldParent = parent;
			TreeListNode oldSuccessor = getNext();

			// find if I'm on left or right of parent
			boolean isLeft = false;
			if (parent != null) {
				isLeft = parent.leftChild == this;
				if (!isLeft && parent.rightChild != this) {
					throw new RuntimeException();
				}
			}

			// test if I'm the root
			boolean isRoot = parent == null;
			if (isRoot && root != this) {
				throw new RuntimeException();
			}

			// Remove all first, keeping the tree intact and numbers correct.
			// We remove in order of deepest down the tree first to keep everything intact.

			// Remove the successor if needed
			boolean removedSuccessor = false;
			boolean removedRightChildSuccessor = false;
			if (oldLeftChild != null && oldRightChild != null) {

				// As we have a right child we should:
				// (a) always have a successor
				// (b) the successor should be the right child or a child of the right child
				// (c) the successor cannot have a left child
				// (d) the successor cannot be the root
				// (e) the successor must have a parent
				if (oldSuccessor == null || oldSuccessor.leftChild != null
						|| (oldSuccessor != oldRightChild && oldSuccessor.isDescendedFrom(oldRightChild) == false) || oldSuccessor == root
						|| oldSuccessor.parent == null) {
					throw new RuntimeException();
				}

				removedSuccessor = true;
				if (oldRightChild == oldSuccessor) {
					removedRightChildSuccessor = true;
				}

				// remove the successor
				boolean successorWasLeftChild = oldSuccessor == oldSuccessor.parent.leftChild;
				TreeListNode osparent = oldSuccessor.parent;
				if (successorWasLeftChild) {
					osparent.removeLeftChild();
				} else {
					if (oldSuccessor != osparent.rightChild) {
						throw new RuntimeException();
					}
					osparent.removeRightChild();
				}

				// then if the successor is not the right child, splice it out so its right child
				// belongs to its parent
				if (oldSuccessor != oldRightChild && oldSuccessor.rightChild != null) {
					TreeListNode osrch = oldSuccessor.rightChild;
					oldSuccessor.removeRightChild();
					osparent.setChild(osrch, successorWasLeftChild);
				}

			}

			// Remove left child
			if (oldLeftChild != null) {
				removeLeftChild();
			}

			// Remove right child
			if (oldRightChild != null && removedRightChildSuccessor == false) {
				removeRightChild();
			}

			// Finally remove myself. As we only remove the successor if we have both left
			// and right children, I must be higher than the successor.
			if (isRoot) {
				root = null;
			} else if (isLeft) {
				parent.removeLeftChild();
			} else {
				parent.removeRightChild();
			}

			// Now re-add
			if (oldLeftChild != null && oldRightChild == null) {
				// splice in left child
				TreeList.this.setChild(oldParent, oldLeftChild, isLeft);
				if (removedSuccessor) {
					throw new RuntimeException();
				}
			} else if (oldLeftChild == null && oldRightChild != null) {
				// splice in right child
				TreeList.this.setChild(oldParent, oldRightChild, isLeft);
				if (removedSuccessor || removedRightChildSuccessor) {
					throw new RuntimeException();
				}
			} else if (oldLeftChild != null && oldRightChild != null) {

				if (!removedSuccessor) {
					throw new RuntimeException();
				}

				// successor takes my old position
				TreeList.this.setChild(oldParent, oldSuccessor, isLeft);

				// and gets old children
				oldSuccessor.setLeftChild(oldLeftChild);

				if (oldRightChild != oldSuccessor) {
					oldSuccessor.setRightChild(oldRightChild);
				}
			}

			// I should be empty now
			if (this.size() > 1) {
				throw new RuntimeException();
			}

			// finally clear my references
			clearReferences();

			// do checks
			if (oldLeftChild != null && oldLeftChild.isValidReferences(false) == false) {
				throw new RuntimeException();
			}
			if (oldRightChild != null && oldRightChild.isValidReferences(false) == false) {
				throw new RuntimeException();
			}
			if (TreeList.this.size() != initialSize - 1) {
				throw new RuntimeException();
			}
		}

		private void clearReferences() {
			parent = null;
			leftChild = null;
			rightChild = null;
			nbChildren = 0;
		}

		/**
		 * Get the index of this node
		 * 
		 * @return
		 */
		public int getIndex() {
			// add my left children
			int ret = 0;
			if (leftChild != null) {
				ret += leftChild.size();
			}

			// keep on going to parent and add left side + parent if came from the right
			TreeListNode node = this;
			while (node.parent != null) {
				if (node == node.parent.rightChild) {
					if (node.parent.leftChild != null) {
						ret += node.parent.leftChild.size();
					}

					// include parent
					ret++;
				} else if (node != node.parent.leftChild) {
					throw new RuntimeException();
				}

				node = node.parent;
			}
			return ret;
		}

		private TreeListNode getLeftMost() {
			TreeListNode ret = this;
			while (ret.leftChild != null) {
				ret = ret.leftChild;
			}
			return ret;
		}

		private TreeListNode getNext() {
			if (rightChild != null) {
				// get left-most node of right child
				return rightChild.getLeftMost();
			} else {
				TreeListNode x = this;
				TreeListNode y = parent;
				while (y != null && x == y.rightChild) {
					x = y;
					y = y.parent;
				}
				return y;
			}
		}

		private void setChild(TreeListNode node, boolean isLeftNode) {
			if (isLeftNode) {
				setLeftChild(node);
			} else {
				setRightChild(node);
			}
		}

		private void setLeftChild(TreeListNode node) {
			if (leftChild != null) {
				throw new RuntimeException();
			}
			if (node.parent != null) {
				throw new RuntimeException();
			}

			leftChild = node;
			node.parent = this;
			addToChildCount(node.size());
		}

		private void setRightChild(TreeListNode node) {
			if (rightChild != null) {
				throw new RuntimeException();
			}
			if (node.parent != null) {
				throw new RuntimeException();
			}

			rightChild = node;
			node.parent = this;
			addToChildCount(node.size());
		}

		/**
		 * Get the size of the subtree (number of children + 1).
		 * 
		 * @return
		 */
		private int size() {
			return nbChildren + 1;
		}

		private TreeListNode removeLeftChild() {
			if (leftChild == null) {
				throw new RuntimeException();
			}
			if (leftChild.parent != this) {
				throw new RuntimeException();
			}
			addToChildCount(-leftChild.size());
			leftChild.parent = null;
			TreeListNode ret = leftChild;
			leftChild = null;
			return ret;
		}

		private TreeListNode removeRightChild() {
			if (rightChild == null) {
				throw new RuntimeException();
			}
			if (rightChild.parent != this) {
				throw new RuntimeException();
			}
			addToChildCount(-rightChild.size());
			rightChild.parent = null;
			TreeListNode ret = rightChild;
			rightChild = null;
			return ret;
		}

		private void addToChildCount(int nbNewChildren) {
			TreeListNode addTo = this;
			while (addTo != null) {
				addTo.nbChildren += nbNewChildren;
				addTo = addTo.parent;
			}
		}
	}

	@Override
	public Iterator<TreeListNode> iterator() {
		class It implements Iterator<TreeListNode> {
			private TreeListNode next;

			public It(TreeListNode next) {
				super();
				this.next = next;
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public TreeListNode next() {
				TreeListNode ret = next;
				next = next.getNext();
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		if (root != null) {
			return new It(root.getLeftMost());
		}
		return new It(null);
	}

	@Override
	public String toString() {
		int size = size();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append("[");
			builder.append(get(i).toString());
			builder.append("]");
		}
		return builder.toString();
	}
}
