/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

final public class PasteLogic {
	private PasteLogic() {
	}

	private static Rectangle getBoundingRectangleFromRows(List<List<Point>> selectedRows) {
		if (selectedRows.size() == 0) {
			return null;
		}

		Point min = new Point(selectedRows.get(0).get(0));
		Point max = new Point(min);
		for (List<Point> row : selectedRows) {
			for (Point point : row) {
				updateMinMax(point, min, max);
			}
		}

		// turn into rectangle
		Rectangle rectangle = new Rectangle(min.x, min.y, max.x - min.x + 1, max.y - min.y + 1);
		return rectangle;
	}

	private static Rectangle getBoundingRectangle(List<Point> points) {
		if (points.size() == 0) {
			return null;
		}

		Point min = new Point(points.get(0));
		Point max = new Point(min);

		for (Point point : points) {
			updateMinMax(point, min, max);
		}

		// turn into rectangle
		Rectangle rectangle = new Rectangle(min.x, min.y, max.x - min.x + 1, max.y - min.y + 1);
		return rectangle;
	}

	private static void updateMinMax(Point point, Point min, Point max) {
		if (point.x < min.x)
			min.x = point.x;
		if (point.y < min.y)
			min.y = point.y;
		if (point.x > max.x)
			max.x = point.x;
		if (point.y > max.y)
			max.y = point.y;
	}

	private static List<PasteRegion> getPasteRegions(List<List<Point>> selectedRows) {
		Rectangle bounds = getBoundingRectangleFromRows(selectedRows);
		if (bounds == null) {
			return null;
		}

		// allocate row-column matrix
		int[][] matrix = new int[bounds.height][];
		for (int row = 0; row < bounds.height; row++) {
			matrix[row] = new int[bounds.width];
			Arrays.fill(matrix[row], Integer.MAX_VALUE);
		}

		// fill in all points with a unique id for each source point
		int counter = 0;
		for (List<Point> row : selectedRows) {
			for (Point point : row) {
				matrix[point.y - bounds.y][point.x - bounds.x] = counter++;
			}
		}

		// repeatedly scan and merge until no more merges happen
		int nbMerges;
		do {
			nbMerges = 0;
			for (int row = 0; row < bounds.height; row++) {
				for (int col = 0; col < bounds.width; col++) {
					int val = matrix[row][col];
					if (val != Integer.MAX_VALUE) {

						// get neighbouring values and our own
						ArrayList<Integer> tmp = new ArrayList<>(5);
						tmp.add(val);

						// get value to the left
						if (col > 0) {
							tmp.add(matrix[row][col - 1]);
						}

						// get value to the right
						if (col < bounds.width-1) {
							tmp.add(matrix[row][col + 1]);
						}

						// get the value above
						if (row > 0) {
							tmp.add(matrix[row - 1][col]);
						}

						// get the value below
						if (row < bounds.height - 1) {
							tmp.add(matrix[row + 1][col]);
						}

						// get min and set the cell to this
						int min = Collections.min(tmp);
						if (val != min) {
							matrix[row][col] = min;
							nbMerges++;
						}
					}
				}
			}
		} while (nbMerges > 0);

		// now scan again reading out each distinct area
		TreeMap<Integer, List<Point>> map = new TreeMap<>();
		for (int row = 0; row < bounds.height; row++) {
			for (int col = 0; col < bounds.width; col++) {
				int val = matrix[row][col];
				if (val != Integer.MAX_VALUE) {
					List<Point> list = map.get(val);
					if (list == null) {
						list = new ArrayList<>();
						map.put(val, list);
					}
					list.add(new Point(bounds.x + col,bounds.y + row ));
				}
			}
		}

		// get the bounding box for each distinct area
		ArrayList<PasteRegion> ret = new ArrayList<>();
		for (List<Point> area : map.values()) {
			 Rectangle bounding = getBoundingRectangle(area);
			 PasteRegion info = new PasteRegion(area, bounding);
			 ret.add(info);
		}

		return ret;
	}
	
	private static class PasteRegion{
		private final List<Point> points;
		private final Rectangle boundingArea;
		
		PasteRegion(List<Point> points, Rectangle boundingArea) {
			super();
			this.points = points;
			this.boundingArea = boundingArea;
		}

	}
	
	private static String[][] splitCopiedStringIntoCells(String s){
		String lines[] = s.split("\\r?\\n", -1);
		String [][] ret = new String[lines.length][];
		for (int i = 0; i < lines.length; i++) {
			ret[i] = lines[i].split("\t", -1);
		}
		return ret;
	}
	

	/**
	 * For the selected copied string, which is assumed to be tab-separated
	 * with line delimiters, and for the input selected points which should 
	 * be ordered by row first then column, paste into the table.
	 * @param tabSeparatedCopyString
	 * @param selectedRows
	 * @param out
	 * @return
	 */
	public static TableModelEvent paste(String tabSeparatedCopyString,List<List<Point>> selectedRows,int lastSettableRow, TableModel out){
		// get copied cells
		String[][] copiedCells = splitCopiedStringIntoCells(tabSeparatedCopyString);
		
		// get max copy width
		int maxCopyWidth =0;
		for(String[] srcRow : copiedCells){
			maxCopyWidth = Math.max(srcRow.length, maxCopyWidth);
		}
		if(maxCopyWidth==0){
			return null;
		}
		
		// get distinct paste regions
		List<PasteRegion> pasteRegions = getPasteRegions(selectedRows);
		if(pasteRegions==null || pasteRegions.size()==0){
			return null;
		}
		
		int minModifiedRow = Integer.MAX_VALUE;
		int maxModifiedRow= Integer.MIN_VALUE;
		
		// paste into each distinct region
		for(PasteRegion region : pasteRegions){
			int row0 = region.boundingArea.y;
			int col0 = region.boundingArea.x;
			
			// paste *at least* the size of the copied strings
			for(int rowIndx =0 ; rowIndx < copiedCells.length ; rowIndx++){
				int row = row0 + rowIndx;
				if(row <= lastSettableRow){
					String[] srcRowArray = copiedCells[rowIndx];
					for(int colIndx = 0 ; colIndx < srcRowArray.length ; colIndx++){
						int col = col0 + colIndx ;
						out.setValueAt(srcRowArray[colIndx], row, col);
						
						// update min and max modified rows
						if(row < minModifiedRow){
							minModifiedRow = row;
						}
						if(row > maxModifiedRow){
							maxModifiedRow = row;
						}
					}		
				}

			}
			
			// now go over the selected points; paste these as well and wrap
			// for any larger than the copied strings
			for(Point point : region.points){
				
				// get source row and column using the wrapping logic
				int srcRow = point.y - row0;
				srcRow %= copiedCells.length;
				
				int srcCol = point.x - col0;
				srcCol %= maxCopyWidth;
				
				// see if we have a source cell, paste if so
				String[] srcRowArray = copiedCells[srcRow];
				if(srcCol < srcRowArray.length && point.y<=lastSettableRow){
					out.setValueAt(srcRowArray[srcCol],point.y, point.x);	
					
					// update min and max modified rows
					if(point.y < minModifiedRow){
						minModifiedRow = point.y;
					}
					if(point.y > maxModifiedRow){
						maxModifiedRow = point.y;
					}
				}
			}
		}
		
		return new TableModelEvent(out, minModifiedRow,maxModifiedRow);
	}
	
	public static List<Point> toSingleList(List<List<Point>> points){
		ArrayList<Point> ret = new ArrayList<>();
		for(List<Point> list : points){
			ret.addAll(list);
		}
		return ret;
	}
	
	/**
	 * Get distinct columns in ascending order
	 * @param points
	 * @return
	 */
	public static List<Integer> getOrderedDistinctColumns(List<Point> points){
		// distinct columns
		TreeSet<Integer> cols = new TreeSet<>();
		for(Point point : points){
			cols.add(point.x);
		}
		
		// get array
		ArrayList<Integer> list = new ArrayList<>(cols);
		return list;
	}
	
	public static List<List<Integer>> getContiguousGroups(List<Integer> numbers){
		ArrayList<Integer> tmp = new ArrayList<>(numbers);
		Collections.sort(tmp);
		ArrayList<List<Integer>> ret = new ArrayList<>();
		ret.add(new ArrayList<Integer>());
		for(int i =0 ; i < tmp.size(); i++){
			// start a new list?
			if(i>0){
				List<Integer> current = ret.get(ret.size()-1);
				if(current.get(current.size()-1)!= tmp.get(i)-1){
					ret.add(new ArrayList<Integer>());	
				}
			}
			
			ret.get(ret.size()-1).add(tmp.get(i));
		}
		return ret;
	}
	
	public static ListSelectionEvent mergeSelectionEvents(ListSelectionEvent a, ListSelectionEvent b) {

		if (a!=null && b==null){
			return a;
		}
		
		if(a==null && b!=null){
			return b;
		}
		
		if(a==null && b==null){
			return null;
		}
		
		return new ListSelectionEvent(a.getSource(), Math.min(a.getFirstIndex(), b.getFirstIndex()), Math.max(a.getLastIndex(), b.getLastIndex()), false);
	}
}
