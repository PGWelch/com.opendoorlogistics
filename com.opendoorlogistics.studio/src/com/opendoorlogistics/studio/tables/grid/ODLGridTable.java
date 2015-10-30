/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import net.sf.jasperreports.swing.JRViewer;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.codefromweb.PackTableColumn;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.SortColumn;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames.FramePlacement;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions.Permission;
import com.opendoorlogistics.studio.tables.grid.adapter.RowStyler;
import com.opendoorlogistics.studio.tables.grid.adapter.SwingAdapter;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class ODLGridTable extends GridTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4204113069098514992L;
	//private final HasInternalFrames owner;
	private final ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs;
	private final PreferredColumnWidths preferredColumnWidths;

	public ODLGridTable(ODLDatastore<? extends ODLTableReadOnly> ds, int tableId, boolean enableListeners,
			RowStyler rowStyler, ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs, GridEditPermissions permissions
			) {
		this(ds, tableId, enableListeners, rowStyler,globalDs, permissions, null);
	}

	public ODLGridTable(ODLDatastore<? extends ODLTableReadOnly> ds, int tableId, boolean enableListeners,
			RowStyler rowStyler,
			ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs, GridEditPermissions permissions,
			PreferredColumnWidths pcw) {
		super(new SwingAdapter(ds, tableId, enableListeners, permissions.get(Permission.setValues) == false,rowStyler), permissions);
		this.preferredColumnWidths = pcw;
		this.globalDs = globalDs;
		myCellRenderer = new ODLCellRenderer();
	//	this.owner = owner;

		// replace editor to disable default validation as we do it later on
		for (final ODLColumnType type : ODLColumnType.values()) {
			Class<?> cls = ColumnValueProcessor.getJavaClass(type);
			setDefaultEditor(cls, new DefaultCellEditor(new JTextField()) {
				@Override
				public Object getCellEditorValue() {
					// ensure always of correct type or null..
					return ColumnValueProcessor.convertToMe(type, super.getCellEditorValue());
				}
			});
		}

		// do best first guess widths for the columns
		ODLTableReadOnly table = getTable();
		for (int i = 0; i < getColumnCount(); i++) {
			int width = -1;
			if (preferredColumnWidths != null && table != null && i > 0) {
				width = preferredColumnWidths.get(tableId, table.getColumnImmutableId(i - 1));
			}

			if (width == -1) {
				PackTableColumn.packColumn(this, i, 2, i == 0 ? 30 : 0);
			} else {
				TableColumn col = getColumnModel().getColumn(i);
				col.setPreferredWidth(width);

			}
		}

		// add listener to save when columns are resized
		getColumnModel().addColumnModelListener(new TableColumnModelListener() {

			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {
				saveColumnSizes();
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
				saveColumnSizes();
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
				saveColumnSizes();
			}

			@Override
			public void columnAdded(TableColumnModelEvent e) {
				saveColumnSizes();
			}
		});

		updateActions();
	}

	private void saveColumnSizes() {
		saveColumnSizes(preferredColumnWidths,false);
	}
	
	
	private void saveColumnSizes(PreferredColumnWidths pcw, boolean saveCol0) {
		if(pcw!=null && getTable()!=null){
			
			if(saveCol0 && getColumnCount()>0){
				pcw.set(((SwingAdapter) getModel()).getTableId(), -1,  getColumnModel().getColumn(0).getWidth());
			}
			
			for (int i = 1; i < getColumnCount(); i++) {
				int width = getColumnModel().getColumn(i).getWidth();
				int id = getTable().getColumnImmutableId(i - 1);
				pcw.set(((SwingAdapter) getModel()).getTableId(), id, width);
			}
				
		}


	}
	
	
	public void replaceData(ODLDatastore<? extends ODLTableReadOnly> ds, int tableId, RowStyler rowStyler) {

		// update the adapter
		((SwingAdapter) getModel()).replaceData(ds, tableId, getPermissions().get(Permission.setValues) == false, rowStyler);

		// revalidate etc...
		tableChanged(new TableModelEvent(getModel(), -1, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));

		updateActions();
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		
		// save column widths before anything changed
		PreferredColumnWidths tmpWidths = new PreferredColumnWidths();
		saveColumnSizes(tmpWidths, true);
		
		super.tableChanged(e);

		// do automatic resizing of images. Get table using the model as
		// datastore reference not saved yet
		if (getModel() != null && SwingAdapter.class.isInstance(getModel())) {
			ODLTableReadOnly odlTable = ((SwingAdapter) getModel()).getTable();

			if (odlTable != null) {
				int[] minColsSizes = new int[getColumnCount()];
				int[] maxColsSizes = new int[getColumnCount()];
				Arrays.fill(maxColsSizes, Integer.MAX_VALUE);

				// Init min and max sizes based on previous sizes..
				int savedWidth = tmpWidths.get(odlTable.getImmutableId(), -1);
				if(savedWidth!=-1){
					minColsSizes[0] = savedWidth;
					maxColsSizes[0] = savedWidth;
				}
				for (int i = 0; i < odlTable.getColumnCount() && (i+1)<minColsSizes.length; i++) {
					savedWidth = tmpWidths.get(odlTable.getImmutableId(), odlTable.getColumnImmutableId(i));
					if(savedWidth!=-1){
						minColsSizes[i+1] = savedWidth;
						maxColsSizes[i+1] = savedWidth;
					}
				}
				
				// get image cols
				TIntArrayList imageCols = new TIntArrayList();
				for (int i = 0; i < odlTable.getColumnCount(); i++) {
					if (odlTable.getColumnType(i) == ODLColumnType.IMAGE) {
						imageCols.add(i);
					}
				}

				// get first and last changed row
				int minRow = 0;
				int lastRow = getRowCount() - 1;
				if (e.getFirstRow() != -1 && e.getFirstRow() != Integer.MAX_VALUE && e.getFirstRow() < odlTable.getRowCount() - 1) {
					minRow = e.getFirstRow();
				}
				if (e.getLastRow() != -1 && e.getLastRow() != Integer.MAX_VALUE && e.getLastRow() < odlTable.getRowCount() - 1) {
					lastRow = e.getLastRow();
				}

				// Make min sizes wider based on image size
				int nbImage = imageCols.size();
				if (nbImage > 0) {
					for (int row = minRow; row <= lastRow; row++) {
						int height = DEFAULT_ROW_HEIGHT;

						if (row < odlTable.getRowCount()) {
							for (int i = 0; i < nbImage; i++) {
								int col = imageCols.get(i);
								BufferedImage img = (BufferedImage) odlTable.getValueAt(row, col);
								if (img != null) {
									height = Math.max(height, img.getHeight());

									// keep a track of minimum column sizes
									int viewerCol = col + 1;
									minColsSizes[viewerCol] = Math.max(minColsSizes[viewerCol], img.getWidth());
									maxColsSizes[viewerCol] = Math.max(maxColsSizes[viewerCol], img.getWidth());
								}

							}
						}

						// change row height if needed

						if (getRowHeight(row) != height) {
							final int finalRow = row;
							final int finalHeight = height;
							EventQueue.invokeLater(new Runnable() {
								@Override
								public void run() {
									setRowHeight(finalRow, finalHeight);
								}
							});

						}
					}
				} else {
					setRowHeight(DEFAULT_ROW_HEIGHT);
				}

				for (int col = 0; col < minColsSizes.length; col++) {
					
					// resize if the current width is less than the min width
					int currentWidth =getColumnModel().getColumn(col).getWidth(); 
					if ( currentWidth< minColsSizes[col]) {
						final int finalCol = col;
						EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								getColumnModel().getColumn(finalCol).setPreferredWidth( minColsSizes[finalCol]);
							}
						});
					}
					else if(currentWidth > maxColsSizes[col]){
						final int finalCol = col;
						EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								getColumnModel().getColumn(finalCol).setPreferredWidth( maxColsSizes[finalCol]);
							}
						});
					}
				}
			}
		}

		updateActions();
	}

	protected void initSubclassActions(GridEditPermissions permissions) {
		super.initSubclassActions(permissions);

		// add separator
		if (actions.size() > 0 && actions.get(actions.size() - 1) != null) {
			actions.add(null);
		}

		actions.add(new SimpleAction("Insert column to the left", "Insert column to the left", "insert-table-column-to-left.png") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				insertCols(true);
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.alterStructure));
			}
		});

		actions.add(new SimpleAction("Insert column to the right", "Insert column to the right", "insert-table-column-to-right.png") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				insertCols(false);
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.alterStructure));
			}
		});

		actions.add(new SimpleAction("Delete column(s)", "Delete selected col(s)", "deletecolumns.png") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				deleteColumns();
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.alterStructure));
			}
		});

		actions.add(new SimpleAction("Rename column", "Rename column", "table-column-rename.png") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				final int col = getValidSingleColumnODLFrame();
				if (col != -1) {
					final String s = getColumnNameDialog(getTable().getColumnName(col));
					if (s != null) {
						runTransaction(new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								ODLTableAlterable table = getAlterableTable();
								if (!DatastoreCopier.modifyColumnWithoutTransaction(col, col, s, table.getColumnType(col), table.getColumnFlags(col), table)) {
									JOptionPane.showMessageDialog(ODLGridTable.this.getParent(), "Could not rename column; named already used.");
									return false;
								}
								return true;
							}
						});

					}
				}
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.alterStructure));
			}

		});

		actions.add(new SimpleAction("Move column left", "Move column left", "arrow-left-2.png") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				final int col = getValidSingleColumnODLFrame();
				if (col > 0) {
					runTransaction(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							ODLTableAlterable table = getAlterableTable();
							DatastoreCopier.modifyColumnWithoutTransaction(col, col - 1, table.getColumnName(col), table.getColumnType(col), table.getColumnFlags(col), table);
							return true;
						}
					});
				}
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.alterStructure));
			}
		});

		actions.add(new SimpleAction("Move column right", "Move column right", "arrow-right-2.png") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				final int col = getValidSingleColumnODLFrame();
				if (col != -1 && col < getTable().getColumnCount() - 1) {

					runTransaction(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							ODLTableAlterable table = getAlterableTable();
							DatastoreCopier.modifyColumnWithoutTransaction(col, col + 1, table.getColumnName(col), table.getColumnType(col), table.getColumnFlags(col), table);
							return true;
						}
					});

				}
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.alterStructure));
			}
		});

	}

	@Override
	public String getSelectedAsTabbedText() {

		List<List<Point>> pointsTable = selectionManager.getSelectedPoints();

		StringBuilder builder = new StringBuilder();
		if (pointsTable.size() > 0) {
			// create hashset of all selected points
			HashSet<Point> selected = new HashSet<>();
			for (List<Point> list : pointsTable) {
				selected.addAll(list);
			}

			// get correct y range
			Point min = new Point(pointsTable.get(0).get(0));
			Point max = new Point(min);
			max.y = pointsTable.get(pointsTable.size() - 1).get(0).y;

			// now update x range from all rows
			for (List<Point> row : pointsTable) {
				int x1 = row.get(0).x;
				if (x1 < min.x) {
					min.x = x1;
				}
				int x2 = row.get(row.size() - 1).x;
				if (x2 > max.x) {
					max.x = x2;
				}
			}

			// loop over all
			for (int row = min.y; row <= max.y; row++) {
				for (int col = min.x; col <= max.x; col++) {
					if (selected.contains(new Point(col, row))) {
						
						// get the actual value, remembering to -1 from the column
						String s = TableUtils.getValueAsString(getTable(), row, col - 1);
						if (s != null) {
							builder.append(s);
						}
					}

					if (col < max.x) {
						builder.append("\t");
					}
				}

				if (row < max.y) {
					builder.append(System.lineSeparator());
				}
			}
		}
		return builder.toString();
	}

	// private void showCustomReport() {
	// ODLTableReadOnly table = getTable();
	// if (table != null) {
	// try {
	// File file = ReportFileBrowsers.chooseJRXMLFile(PrefKey.LAST_GRID_VIEW_JRXML, this);
	// if(file==null){
	// return;
	// }
	// JasperPrint printable = new SingleLevelReportBuilder().buildSingleTablePrintable(table, JRXmlLoader.load(file),null);
	// boolean isPortrait = printable.getPageHeight() > printable.getPageWidth();
	// JRViewer viewer = new JRViewer(printable);
	// showReportViewer(isPortrait, viewer);
	// } catch (Throwable e) {
	// ExecutionReportImpl report = new ExecutionReportImpl();
	// report.setFailed(e);
	// report.setFailed("An error occurred when generating the report using custom jrxml file.");
	// ExecutionReportDialog.show((JFrame) SwingUtilities.getWindowAncestor(this), "Error generating report", report);
	// }
	// }
	// }

	// private void showReport(boolean portrait) {
	// ODLTableReadOnly table = getTable();
	// if (table != null) {
	// try {
	// JRViewer viewer = new SingleLevelReportBuilder().buildSingleTableViewable(table, portrait ? OrientationEnum.PORTRAIT
	// : OrientationEnum.LANDSCAPE, false,null);
	// showReportViewer(portrait, viewer);
	// } catch (Throwable e) {
	// JOptionPane.showMessageDialog(getParent(), "An error occurred when generating the report.");
	// }
	// }
	// }

//	private void showReportViewer(boolean portrait, JRViewer viewer) {
//		ODLInternalFrame internalFrame = new ODLInternalFrame("Report of " + getTableName());
//		internalFrame.setContentPane(viewer);
//		internalFrame.setPreferredSize(portrait ? new Dimension(600, 800) : new Dimension(800, 600));
//		internalFrame.pack();
//		internalFrame.setVisible(true);
//		owner.addInternalFrame(internalFrame, FramePlacement.AUTOMATIC);
//	}

	private class ImageRenderer extends JPanel {
		private BufferedImage image;

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}

	}

	protected class ODLCellRenderer extends CellRenderer {
		private final Color afterTable = new Color(230, 230, 230);
		private final ImageRenderer imageRenderer = new ImageRenderer();
		private final JLabel colourLabel = createLabel();

		public ODLCellRenderer() {
			super(false);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			JLabel ret = prepareLabel(value, isSelected, hasFocus, row, column);

			ODLTableReadOnly odlTable = getTable();
			if (odlTable != null && column > 0) {

				if (odlTable.getColumnType(column - 1) == ODLColumnType.IMAGE && value != null) {
					// display image differently
					if (isSelected) {
						ret.setText("");
					} else {
						imageRenderer.image = (BufferedImage) value;
						return imageRenderer;
					}
				} else if (odlTable.getColumnType(column - 1) == ODLColumnType.COLOUR && !isSelected && colourLabel != null && value != null) {
					// display colour differently
					ret = colourLabel;
					ret.setText(value.toString());
					Color col = (Color) getTable().getValueAt(row, column - 1);
					// ret.setForeground(col);
					if (col.getRed() + col.getGreen() + col.getBlue() < 382) {
						ret.setForeground(Color.WHITE);
					} else {
						ret.setForeground(Color.BLACK);
					}
					ret.setBackground(col);
				}
				else if(!isSelected){
					// check for different colour
					Color rowColor = ((SwingAdapter)getModel()).getRowColour(row);
					if(rowColor!=null){
						ret.setForeground(rowColor);
					}
				}
			}

			// linked rows appear in italics
			if(odlTable!=null && ret.getFont()!=null){
				if((odlTable.getRowFlags(odlTable.getRowId(row))& TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA) == TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA  ){
					ret.setFont(ret.getFont().deriveFont(Font.ITALIC));					
				}
			}
			
			// set anything after the last row to be grey
			if (isSelected == false && row >= getTable().getRowCount()) {
				ret.setBackground(afterTable);
			}
			return ret;
		}

	}

	int getLastFilledRowNumber() {
		ODLTableReadOnly table = getTable();
		if (table != null) {
			return table.getRowCount() - 1;
		}
		return 0;
	}

	private ODLTableReadOnly getTable() {
		if(getModel()!=null && SwingAdapter.class.isInstance(getModel())){			
			return ((SwingAdapter) getModel()).getTable();
		}
		return null;
	}

	private ODLTable getWritableTable() {
		return (ODLTable) getTable();
	}

	private ODLTableAlterable getAlterableTable() {
		return (ODLTableAlterable) getTable();
	}

	@Override
	protected void startTransaction() {
		if (globalDs != null) {
			globalDs.startTransaction();
		}
	}

	@Override
	protected void endTransaction() {
		if (globalDs != null) {
			if (globalDs.isInTransaction()) {
				globalDs.endTransaction();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void rollbackTransaction() {
		if (globalDs != null) {
			globalDs.rollbackTransaction();
		}
	}

	// @SuppressWarnings("rawtypes")
	// @Override
	// protected void undo() {
	// if (globalDs != null) {
	// globalDs.undo();
	// }
	// }
	//
	// @SuppressWarnings("rawtypes")
	// @Override
	// protected void redo() {
	// if (globalDs != null) {
	// globalDs.redo();
	// }
	// }

	@Override
	public void pasteTabbedText(final String s) {
		runTransaction(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				int lastRow = Integer.MAX_VALUE;
				if (getPermissions().get(Permission.createRows) == false) {
					lastRow = getTable().getRowCount() - 1;
				}
				PasteLogic.paste(s, selectionManager.getSelectedPoints(), lastRow, getModel());
				return true;
			}
		});
	}

	@Override
	protected void deleteColumns() {

		final List<Integer> list = selectionManager.getSelectedColumns();
		if (list.size() == 0) {
			return;
		}

		runTransaction(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				ODLTableAlterable table = getAlterableTable();
				for (int i = list.size() - 1; i >= 0; i--) {
					int col = list.get(i);
					table.deleteColumn(col - 1);
				}
				return true;
			}
		});
	}

	// @Override
	// protected List<List<Point>> getSelectedPoints() {
	//
	// List<List<Point>> pointsTable = new ArrayList<>();
	// switch (selectionState) {
	// case ROW:
	// case ROW_EXT:
	// int nbCols = getModel().getColumnCount();
	// for (int row : rowHeaderSel.getSelectedRows()) {
	// ArrayList<Point> pointsRow = new ArrayList<>();
	//
	// // ignore first column as its the row number
	// for (int i = 1; i < nbCols; i++) {
	// pointsRow.add(new Point(i, row));
	// }
	// if (pointsRow.size() > 0) {
	// pointsTable.add(pointsRow);
	// }
	// }
	// break;
	//
	// case SHEET:
	// case SHEET_EXT:
	// pointsTable = sheetSel.getSelectedPoints();
	// break;
	//
	// default:
	// throw new RuntimeException();
	// }
	//
	// return pointsTable;
	// }

	@Override
	protected void insertCols(boolean toLeft) {
		List<Integer> list = PasteLogic.getOrderedDistinctColumns(PasteLogic.toSingleList(selectionManager.getSelectedPoints()));
		if (list.size() > 1) {
			showMoreThanOneSelectedColumnWarning();
			return;
		}

		final ODLTableAlterable table = getAlterableTable();
		int col = table.getColumnCount();
		if (list.size() == 0) {
			toLeft = false;
		} else {
			col = list.get(0);
		}

		final String s = getColumnNameDialog("New column");
		if (s == null) {
			return;
		}

		final boolean finalToleft = toLeft;
		final int finalCol = col;
		boolean inserted = runTransaction(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return table.insertColumn(-1, finalToleft ? finalCol - 1 : finalCol, s, ODLColumnType.STRING, 0, false);
			}
		});

		if (!inserted) {
			JOptionPane.showMessageDialog(getParent(), "Could not insert column; named already used.");
		}
	}

	/**
	 * Get the single selected column index in the ODL frame. Report error if incorrect columns selected.
	 * 
	 * @return
	 */
	private int getValidSingleColumnODLFrame() {
		List<Integer> list = selectionManager.getSelectedColumns();
		if (list.size() == 0) {
			JOptionPane.showMessageDialog(getParent(), "No column selected");
			return -1;
		} else if (list.size() > 1) {
			showMoreThanOneSelectedColumnWarning();
			return -1;
		}

		int ret = list.get(0);
		ret--;
		if (ret < 0 || ret >= getTable().getColumnCount()) {
			JOptionPane.showMessageDialog(getParent(), "Invalid column selected");
			return -1;
		}

		return ret;
	}

	private void showMoreThanOneSelectedColumnWarning() {
		JOptionPane.showMessageDialog(getParent(), "Only one column should be selected");
	}

	private String getColumnNameDialog(String current) {
		String s = JOptionPane.showInputDialog(getParent(), "Enter new column name", current);
		return s;
	}

	@Override
	protected void insertDeleteRows(final boolean inserting) {
		final List<List<Point>> selected = selectionManager.getSelectedPoints();
		if (selected == null || selected.size() == 0) {
			return;
		}

		runTransaction(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				ODLTable table = getWritableTable();
				for (int i = selected.size() - 1; i >= 0; i--) {
					List<Point> rowList = selected.get(i);
					int row = rowList.get(0).y;
					if (inserting) {
						table.insertEmptyRow(row, -1);
					} else {
						if (row < table.getRowCount()) {
							table.deleteRow(row);
						}
					}
				}
				return true;
			}
		});

	}

	@Override
	public String getTableName() {
		return ((SwingAdapter) getModel()).getTableName();
	}

	@Override
	protected void sort(final SortColumn[] sortCols) {
		runTransaction(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				ODLTable table = getWritableTable();
				SortColumn[] conv = new SortColumn[sortCols.length];
				for (int i = 0; i < conv.length; i++) {
					conv[i] = new SortColumn(sortCols[i].getIndx() - 1, sortCols[i].isAscending());
				}
				TableUtils.sort(table, conv);
				return true;
			}
		});
	}

	// public void setBestFit(){
	// // do columns
	// int colSum=0;
	// for(int i =0 ;i<getColumnCount() ; i++){
	// PackTableColumn.packColumn(this,i, 6);
	// colSum += getColumnModel().getColumn(i).getPreferredWidth();
	// }
	//
	// // how many rows are there really? (Swing adapter adds 'virtual rows').
	// int nbRows=0;
	// ODLTableReadOnly table = getTable();
	// if(table!=null){
	// nbRows = table.getRowCount();
	// }
	//
	// // will never want more than 100 on-screen anyway
	// nbRows = Math.min(100, nbRows);
	//
	// // count the height
	// long rowSum = getTableHeader().getHeight();
	// for(int i =0 ; i< nbRows; i++){
	// rowSum += getRowHeight(i);
	// }
	// // include extra one for a bit of padding..
	// rowSum += getRowHeight();
	//
	// // never do more than 600?
	// rowSum = Math.min(rowSum, 600);
	//
	// setPreferredSize(new Dimension(colSum,(int) rowSum));
	// repaint();
	// }

	@Override
	protected void copyTable() {
		if (getTable() != null) {
			String name = getTable().getName();
			final String s = JOptionPane.showInputDialog(getParent(), "Enter new table name", "Copy of " + name);
			if (s != null) {
				runTransaction(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						if (DatastoreCopier.copyTable(getTable(), globalDs, s) == null) {
							throw new RuntimeException("Could not copy the table.");
						}
						return true;
					}
				});

				// ODLTableReadOnly copy =DatastoreCopier.copyTable(copyThis,
				// copyInto) DatastoreCopier.copyTableIntoSameDatastore(ds,
				// table.getImmutableId(), name);
				// if (copy != null) {
				// launcher.launchTableGrid(copy.getImmutableId());
				// } else {
				// showTableNameError();
				// }

			}

		}

	}

	@Override
	public GridEditPermissions getPermissions() {
		if(defaultPermissions!=null && getTable()!=null){			
			GridEditPermissions ret= GridEditPermissions.and(defaultPermissions,((SwingAdapter)getModel()).getPermissions());
			return ret;
		}
		return new GridEditPermissions();
	}
	
	public long [] getRowIds(boolean selectedOnly){
		ODLTableReadOnly table = getTable();
		int n = table.getRowCount();
		TLongArrayList tmp = new TLongArrayList();
		
		for(int row = 0 ; row < n ; row++){
			if(!selectedOnly || selectionManager.isRowSelected(row)){
				tmp.add(table.getRowId(row));				
			}
		}
		
		return tmp.toArray();
	}
	
	@Override
	protected void setHeaderRenderer() {
		class IsItalic{
			boolean isItalic(int col){
				ODLTableDefinition table = getTable();
				col--;
				if(col>=0 && col < table.getColumnCount()){
					return (table.getColumnFlags(col)& TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA) == TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA;
				}
				return false;
			}
		}
		IsItalic isItalic = new IsItalic();
		
		JTableHeader header = getTableHeader();
		if (showFilters) {
			header.setDefaultRenderer(new FilterHeaderRender() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					return super.getTableCellRendererComponent(table, value, selectionManager != null ? selectionManager.isCellSelected(row, column) : isSelected, hasFocus, row, column);
				}

				@Override
				protected boolean getColumnIsItalics(int col) {
					return isItalic.isItalic(col);
				}
			});
		} else {
			header.setDefaultRenderer(new HeaderCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					return super.getTableCellRendererComponent(table, value, selectionManager != null ? selectionManager.isCellSelected(row, column) : isSelected, hasFocus, row, column);
				}

				@Override
				protected boolean getColumnIsItalics(int col) {
					return isItalic.isItalic(col);
				}
			});
		}

	}

}
