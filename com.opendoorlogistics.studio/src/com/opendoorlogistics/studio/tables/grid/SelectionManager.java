/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.util.*;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;

final public class SelectionManager {
	private final GridTable table;
	private final boolean forceRowSelection;
	private Point focusPoint;
	private SelectionState selectionState = SelectionState.SHEET;
	private OneDimensionalSelection rowSelection = new OneDimensionalSelection();
	private OneDimensionalSelection colSelection = new OneDimensionalSelection();

	// store by row, col
	private TreeMap<Integer, TreeSet<Integer>> selectedCells = new TreeMap<>();
	private boolean isExt;
	private Point extAnchorPoint;
	private Point extStretchPoint;

	private enum SelectionState {
		ROW, SHEET, COL
	}

	private static class OneDimensionalSelection {
		private TreeSet<Integer> selected = new TreeSet<>();
		private boolean isExt;
		private Integer extAnchor;
		private Integer extStretch;
		private Integer focus;

		private int[] getRowRangeContainingSelectedCells() {
			int ret[] = new int[2];
			ret[0] = Integer.MAX_VALUE;
			ret[1] = Integer.MIN_VALUE;
			if (selected.size() > 0) {
				ret[0] = selected.first();
				ret[1] = selected.last();
			}

			int[] ext = getExtRange();
			if (ext != null) {
				ret[0] = Math.min(ret[0], ext[0]);
				ret[1] = Math.max(ret[1], ext[1]);
			}
			return ret;
		}

		private void focusLost(FocusEvent e) {
			focus = null;
		}

		private TreeSet<Integer> getSelected() {
			TreeSet<Integer> ret = new TreeSet<>(selected);
			int[] ext = getExtRange();
			if (ext != null) {
				for (int i = ext[0]; i <= ext[1]; i++) {
					ret.add(i);
				}
			}
			return ret;
		}

		private void add(int i) {
			selected.add(i);
		}

		private boolean isSelected(int i) {
			if (selected.contains(i)) {
				return true;
			}
			int[] ext = getExtRange();
			if (ext != null) {
				if (ext[0] <= i && i <= ext[1]) {
					return true;
				}
			}
			return false;
		}

		private int[] getExtRange() {
			if (isExt) {
				int min = Math.min(extAnchor, extStretch);
				int max = Math.max(extAnchor, extStretch);
				return new int[] { min, max };
			}
			return null;
		}

		private void clearSelection() {
			extAnchor = null;
			extStretch = null;
			selected.clear();
			isExt = false;
		}

		void changeSelection(int i, boolean toggle, boolean extend) {

			// update focus point
			Integer oldFocus = focus;
			focus = i;

			// update the stretch point if still in extend
			if (extend && isExt) {
				extStretch = focus;
			}

			// check for entering extend; only set the anchor point then
			if (!isExt && extend) {
				extAnchor = oldFocus != null ? oldFocus : focus;
				extStretch = focus;
				isExt = true;
			}

			// process if not in extend
			if (!extend) {
				if (!toggle) {
					selected.clear();
				}

				add(focus);
			}

		}

		void checkForLeavingExt(boolean toggle, boolean extend) {
			// check for leaving row extend.. add to extended region to selection if control pressed
			if (!extend && isExt) {
				if (toggle) {
					int[] ext = getExtRange();
					for (int j = ext[0]; j <= ext[1]; j++) {
						add(j);
					}
				}
				isExt = false;
				extAnchor = null;
				extStretch = null;
			}
		}
	}

	public SelectionManager(GridTable table, boolean forceRowSelection) {
		this.table = table;
		this.forceRowSelection = forceRowSelection;
		if(forceRowSelection){
			selectionState = SelectionState.ROW;
		}
	}

	private int[] getRowRangeContainingSelectedCells() {
		int ret[] = new int[2];
		ret[0] = Integer.MAX_VALUE;
		ret[1] = Integer.MIN_VALUE;
		switch (selectionState) {
		case SHEET:
			if (selectedCells.size() > 0) {
				ret[0] = selectedCells.firstKey();
				ret[1] = selectedCells.lastKey();
			}
			Rectangle ext = getExtendRectangle();
			if (ext != null) {
				ret[0] = Math.min(ret[0], ext.y);
				ret[1] = Math.max(ret[1], ext.y + ext.height - 1);
			}
			break;

		case ROW:
			return rowSelection.getRowRangeContainingSelectedCells();

		case COL:
			int lastRow = table.getLastFilledRowNumber();
			if (lastRow >= 0) {
				ret[0] = 0;
				ret[1] = lastRow;
			}
			break;

		default:
			break;
		}
		return ret;
	}

	void focusLost(FocusEvent e) {
		focusPoint = null;
		rowSelection.focusLost(e);
	}

	boolean isFocused(int row, int col) {
		return focusPoint != null && focusPoint.y == row && focusPoint.x == col;
	}

	private static List<Point> getPoints(Rectangle rectangle) {
		ArrayList<Point> ret = new ArrayList<>();
		for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
			for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
				ret.add(new Point(x, y));
			}
		}
		return ret;
	}

	private static List<Point> getPoints(TreeMap<Integer, TreeSet<Integer>> map) {
		ArrayList<Point> ret = new ArrayList<>();
		for (Map.Entry<Integer, TreeSet<Integer>> entry : map.entrySet()) {
			int row = entry.getKey();
			for (int col : entry.getValue()) {
				ret.add(new Point(col, row));
			}
		}
		return ret;
	}

	/**
	 * Get columns with one or more selected cells or which are selected
	 * themselves in the column header. Columns are returned in ascending order.
	 * @return
	 */
	List<Integer> getSelectedColumns(){
		TreeSet<Integer> tmp = new TreeSet<>();
		
		if(selectionState == SelectionState.COL){
			tmp = colSelection.getSelected();
		}else{
			for(Point point : PasteLogic.toSingleList(getSelectedPoints()) ){
				tmp.add(point.x);
			}
		}
		return new ArrayList<>(tmp);
	}
	
	List<List<Point>> getSelectedPoints() {
		ArrayList<List<Point>> ret = new ArrayList<>();
		switch (selectionState) {
		case ROW:
			int nbCols = table.getModel().getColumnCount();
			for (int row : rowSelection.getSelected()) {
				ArrayList<Point> pointsRow = new ArrayList<>();
				for (int i = 1; i < nbCols; i++) {
					pointsRow.add(new Point(i, row));
				}
				if (pointsRow.size() > 0) {
					ret.add(pointsRow);
				}
			}
			break;

		case COL:
			int lastRow = table.getLastFilledRowNumber();
			for (int row = 0; row <= lastRow; row++) {
				ArrayList<Point> pointsRow = new ArrayList<>();
				for (int col : colSelection.getSelected()) {
					pointsRow.add(new Point(col, row));
				}
				if (pointsRow.size() > 0) {
					ret.add(pointsRow);
				}
			}
			break;

		case SHEET:
			// copy selected
			TreeMap<Integer, TreeSet<Integer>> tmpMap = new TreeMap<>();
			addPoints(getPoints(selectedCells), tmpMap);

			// add the extended rectangle
			Rectangle rect = getExtendRectangle();
			if (rect != null) {
				addPoints(getPoints(rect), tmpMap);
			}

			// read out by row
			for (Map.Entry<Integer, TreeSet<Integer>> row : tmpMap.entrySet()) {
				ArrayList<Point> rowList = new ArrayList<>();
				ret.add(rowList);
				for (int col : row.getValue()) {
					rowList.add(new Point(col, row.getKey()));
				}
			}
			break;

		default:
			break;
		}

		return ret;
	}

	private Rectangle getExtendRectangle() {
		if (!isExt) {
			return null;
		}
		Point min = new Point(Math.min(extAnchorPoint.x, extStretchPoint.x), Math.min(extAnchorPoint.y, extStretchPoint.y));
		Point max = new Point(Math.max(extAnchorPoint.x, extStretchPoint.x), Math.max(extAnchorPoint.y, extStretchPoint.y));

		return new Rectangle(min.x, min.y, max.x - min.x + 1, max.y - min.y + 1);
	}

	private static void addPointMatrix(List<List<Point>> points, TreeMap<Integer, TreeSet<Integer>> selected) {
		for (List<Point> row : points) {
			addPoints(row, selected);
		}
	}

	private static void addPoints(Iterable<Point> points, TreeMap<Integer, TreeSet<Integer>> selected) {
		for (Point point : points) {
			addPoint(point, selected);
		}
	}

	private static void addPoint(Point point, TreeMap<Integer, TreeSet<Integer>> selected) {
		TreeSet<Integer> row = selected.get(point.y);
		if (row == null) {
			row = new TreeSet<>();
			selected.put(point.y, row);
		}
		row.add(point.x);
	}

	void setSelectedCell(int row, int col) {
		clearSelection();
		addPoint(new Point(col, row), selectedCells);
	}

	void setSelectedCells(Iterable<Point> points) {
		clearSelection();
		addPoints(points, selectedCells);
	}

	private void changeState(SelectionState newState, boolean toggle) {

		
		if (toggle) {
			// keep everything currently selected
			addPointMatrix(getSelectedPoints(), selectedCells);
		} else {
			// clear everything
			clearSelection();
		}
		if(forceRowSelection){
			selectionState = SelectionState.ROW;
		}
		else{
			selectionState = newState;			
		}
	}

	void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
		// if we're in force row selection mode then ignore column selection events
		if(forceRowSelection && (rowIndex==-1 && columnIndex!=-1)){
			return;
		}
		
		// get selected row range
		int[] oldSelRowRange = getRowRangeContainingSelectedCells();

		// check for leaving extension state first of all as we don't enter other states if still extending
		switch (selectionState) {
		case SHEET:
			checkForLeavingExt(toggle, extend);
			break;

		case ROW:
			rowSelection.checkForLeavingExt(toggle, extend);
			break;

		case COL:
			colSelection.checkForLeavingExt(toggle, extend);
			break;
		}

		// leave sheet state?
		if (selectionState == SelectionState.SHEET && !isExt) {

			if (columnIndex == 0 && rowIndex >= 0) {
				// enter row state from sheet state
				changeState(SelectionState.ROW, toggle);
			}

			if (rowIndex == -1 && columnIndex > 0) {
				// enter column state from sheet state
				changeState(SelectionState.COL, toggle);
			}
		}

		// leave row state?
		if (selectionState == SelectionState.ROW && !rowSelection.isExt) {

			if (columnIndex > 0) {
				if (rowIndex >= 0) {
					// enter sheet state from row state
					changeState(SelectionState.SHEET, toggle);
				} else {
					// enter col state from row state
					changeState(SelectionState.COL, toggle);
				}
				rowSelection.clearSelection();
			}

		}

		// leave col state?
		boolean leftHeader=false;
		if (selectionState == SelectionState.COL && !colSelection.isExt) {

			if (rowIndex >= 0) {
				if (columnIndex > 0) {
					// enter sheet state from col state
					changeState(SelectionState.SHEET, toggle);
				} else {
					// enter row state from col state
					changeState(SelectionState.ROW, toggle);
				}
				colSelection.clearSelection();
				leftHeader= true;
			}
		}

		// update focus point
		Point oldFocus = focusPoint;
		focusPoint = new Point(Math.max(columnIndex, 1),Math.max(rowIndex,0));

		if (selectionState == SelectionState.SHEET) {

			// update the stretch point if still in extend
			if (isExt) {
				extStretchPoint = new Point(focusPoint);
			}

			// check for entering extend; only set the anchor point then
			if (!isExt && extend && columnIndex > 0 && rowIndex >= 0) {
				extAnchorPoint = oldFocus != null ? new Point(oldFocus) : new Point(focusPoint);
				extStretchPoint = focusPoint;
				isExt = true;
			}

			// process if not in extend
			if (!extend && columnIndex > 0 && rowIndex >= 0) {
				if (!toggle) {
					selectedCells.clear();
				}

				addPoint(focusPoint, selectedCells);
			}
		} else if (selectionState == SelectionState.ROW) {
			rowSelection.changeSelection(Math.max(rowIndex,0), toggle, extend);
		}
		else{
			colSelection.changeSelection(Math.max(columnIndex,1), toggle, extend);
		}

		// get new range of selected rows and then flag to repaint anything within the union of old and new range
		int[] newSelRowRange = getRowRangeContainingSelectedCells();
		int[] potentialChangeRange = new int[] { Math.min(oldSelRowRange[0], newSelRowRange[0]), Math.max(oldSelRowRange[1], newSelRowRange[1]) };
		if (potentialChangeRange[0] <= potentialChangeRange[1]) {
			// its a valid interval
			table.valueChanged(new ListSelectionEvent(this, potentialChangeRange[0], potentialChangeRange[1], false));
		}

		// also check for focus change
		if (oldFocus != null && oldFocus.equals(focusPoint) == false) {
			table.tableChanged(new TableModelEvent(table.getModel(), oldFocus.y, oldFocus.y));
		}

		// and for header needing repainting
		if(leftHeader){
			table.getTableHeader().repaint();
		}
	}

	private void checkForLeavingExt(boolean toggle, boolean extend) {
		if (!extend && isExt) {
			if (toggle) {
				addPoints(getPoints(getExtendRectangle()), selectedCells);
			}
			isExt = false;
			extAnchorPoint = null;
			extStretchPoint = null;
		}
	}

	boolean isRowSelected(int row) {
		switch (selectionState) {
		case SHEET:
			return false;

		case ROW:
			return rowSelection.isSelected(row);

		case COL:
			return row <= table.getLastFilledRowNumber();
		}
		return false;
	}

	boolean isCellSelected(int row, int column) {
		switch (selectionState) {
		case ROW:
			return rowSelection.isSelected(row);

		case SHEET:
			TreeSet<Integer> rowSet = selectedCells.get(row);
			if (rowSet != null && rowSet.contains(column)) {
				return true;
			}

			Rectangle ext = getExtendRectangle();
			if (ext != null && ext.contains(column, row)) {
				return true;
			}

			break;

		case COL:
			return row <= table.getLastFilledRowNumber() && colSelection.isSelected(column);
		}
		return false;
	}

	void clearSelection() {
		selectionState =forceRowSelection? SelectionState.ROW: SelectionState.SHEET;
		isExt = false;
		extAnchorPoint = null;
		extStretchPoint = null;
		selectedCells.clear();
		rowSelection.clearSelection();
		colSelection.clearSelection();
		table.repaint();
		table.getTableHeader().repaint();
	}

	Point getFocusPoint() {
		if (focusPoint != null) {
			return new Point(focusPoint);
		}
		return null;
	}

	void setFocusPoint(Point newPoint) {
		// don't allow the dummy column to be selected
		if (newPoint.x == 0) {
			newPoint.x = 1;
		}
		Point oldFocusPoint = focusPoint;
		focusPoint = newPoint;

		int minChangedRow = newPoint.y;
		int maxChangedRow = newPoint.y;

		if (oldFocusPoint != null && focusPoint.equals(oldFocusPoint) == false) {
			// focus point has changed
			minChangedRow = Math.min(minChangedRow, oldFocusPoint.y);
			maxChangedRow = Math.max(maxChangedRow, oldFocusPoint.y);
		}

		// fire listeners *after* focus state has changed
		table.tableChanged(new TableModelEvent(table.getModel(), minChangedRow, maxChangedRow));

		scrollToVisible(table, focusPoint.y, focusPoint.x);
	}

	/**
	 * See http://stackoverflow.com/questions/853020/jtable-scrolling-to-a-specified-row-index
	 * 
	 * @param table
	 * @param rowIndex
	 * @param vColIndex
	 */
	private static void scrollToVisible(JTable table, int rowIndex, int vColIndex) {
		if (!(table.getParent() instanceof JViewport)) {
			return;
		}
		JViewport viewport = (JViewport) table.getParent();

		// This rectangle is relative to the table where the
		// northwest corner of cell (0,0) is always (0,0).
		Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);

		// The location of the viewport relative to the table
		Point pt = viewport.getViewPosition();

		// Translate the cell location so that it is relative
		// to the view, assuming the northwest corner of the view is (0,0)
		rect.setLocation(rect.x - pt.x, rect.y - pt.y);

		// Scroll the area into view
		viewport.scrollRectToVisible(rect);
	}

}
