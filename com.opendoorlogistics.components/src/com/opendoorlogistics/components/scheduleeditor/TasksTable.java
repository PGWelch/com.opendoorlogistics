/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.DropMode;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.EditorTable;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.components.scheduleeditor.data.EditorData;
import com.opendoorlogistics.components.scheduleeditor.data.beans.Task;
import com.opendoorlogistics.components.scheduleeditor.data.beans.TaskOrder;
import com.opendoorlogistics.core.utils.strings.Strings;

public class TasksTable extends JTable {
	private final String vehicleId;
	private final boolean isSchedule;
	private Task[] data;
	private final TaskTransferHandler transferHandler;
	private final ODLApi api;
	private boolean tableModelInitialised = false;

	TasksTable(String vehicleId, TaskMover stopMover, ODLApi api) {
		this.vehicleId = vehicleId;
		this.transferHandler = new TaskTransferHandler(stopMover);
		this.isSchedule = Strings.equalsStd(vehicleId, ScheduleEditorConstants.UNLOADED_VEHICLE) == false;
		this.api = api;
		getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setTransferHandler(transferHandler);
		setDropMode(DropMode.INSERT_ROWS);
		setDragEnabled(true);
		setFillsViewportHeight(true);
		// setAutoCreateRowSorter(true);
		setRowSorter(null);
	}

	void setData(final EditorData editorData) {
		this.data = editorData.getTasksByResource(vehicleId);

		final DisplayFields displayFields;
		if (isSchedule) {
			displayFields = editorData.getDisplayFields(EditorTable.TASK_ORDER);
		} else {
			displayFields = editorData.getDisplayFields(EditorTable.TASKS);
		}

		final int nbStdCols = isSchedule ? 3 : 2;
		final int nd = displayFields.size();
		final int nc = nbStdCols + nd;

		final ArrayList<String> columnNames = new ArrayList<>();
		if (isSchedule) {
			columnNames.add("#");
		}
		columnNames.add("ID");
		columnNames.add("Name");
		for (int i = 0; i < nd; i++) {
			columnNames.add(displayFields.getFieldName(i));

		}
		setModel(new AbstractTableModel() {

			@Override
			public String getColumnName(int column) {
				return columnNames.get(column);
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (rowIndex < 0 || rowIndex >= data.length) {
					return null;
				}

				Task stop = data[rowIndex];

				if (isSchedule && columnIndex == 0) {
					return rowIndex + 1;
				}

				if ((isSchedule && columnIndex == 1) || (!isSchedule && columnIndex == 0)) {
					return stop.getId();
				}

				if ((isSchedule && columnIndex == 2) || (!isSchedule && columnIndex == 1)) {
					return stop.getName();
				}

				Object value = null;
				if (isSchedule) {
					// use the stop order table
					TaskOrder order = editorData.getTaskOrderById(stop.getId());
					if (order != null) {
						value = displayFields.getFieldValue(order.getGlobalRowId(), columnIndex - nbStdCols);
					}
				} else {
					// use the stop table...
					value = displayFields.getFieldValue(stop.getGlobalRowId(), columnIndex - nbStdCols);
				}
				if (value != null) {
					return api.values().convertValue(value, ODLColumnType.STRING);
				}
				return null;

			}

			@Override
			public int getRowCount() {
				return data.length;
			}

			@Override
			public int getColumnCount() {
				return nc;
			}
		});

		final Color altColour = new Color(0.95f, 0.95f, 0.95f);
		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component ret = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if(!isSelected){
					ret.setBackground(row%2==0 ? Color.WHITE: altColour);
					if(isSchedule){
						if (row >= 0 && row < data.length) {
							Task stop = data[row];
							TaskOrder order = editorData.getTaskOrderById(stop.getId());
							if(order!=null && order.getColor()!=null){
								ret.setBackground(order.getColor());
							}
						}

					}		
				}

				return ret;
			}
		});

		// make sequence number column small the first time table is shown
		if (!tableModelInitialised) {
			tableModelInitialised = true;

			if (isSchedule) {
				getColumnModel().getColumn(0).setMaxWidth(40);
			}

			setAutoCreateColumnsFromModel(false);
		}

	}

	public String getStopId(int index) {
		return data[index].getId();
	}

	public String getVehicleId() {
		return vehicleId;
	}

	/**
	 * Disable drag selection - see
	 * https://community.oracle.com/thread/1351319?start=0&tstart=0
	 */
	@Override
	protected void processMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown() && !e.isControlDown()) {
			Point pt = e.getPoint();
			int row = rowAtPoint(pt);
			int col = columnAtPoint(pt);
			if (row >= 0 && col >= 0 && !super.isCellSelected(row, col))
				changeSelection(row, col, false, false);
		}
		super.processMouseEvent(e);
	}

}
