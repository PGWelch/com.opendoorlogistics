package com.opendoorlogistics.studio.tables.custom;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.app.ui.UIAction;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.studio.tables.custom.StandardEditActionHandlers.ActionType;

/**
 * Shows a table which has a plugin popup box for editing items... Want simple controls to move items up and down, delete them and create new items. Want
 * pluggable popup controls for editing a row. The base table could have missing or misordered fields however. Could we build an adapter on-the-fly and create
 * missing fields???
 * 
 * @author Phil
 *
 * @param <T>
 */
public class CustomTableEditor<T extends BeanMappedRow> extends JPanel implements Disposable {
	private BeanMappingInfo<T> bmi;
	private final JTable jTable;
	private final List<UIAction> actions;
	private final ODLListener structureListener = new ODLListener() {

		@Override
		public void datastoreStructureChanged() {
			updateAdapter();
			((AbstractTableModel) jTable.getModel()).fireTableDataChanged();
			updateEnabledActions();
		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			((AbstractTableModel) jTable.getModel()).fireTableDataChanged();
		}
	};

	private final ODLListener dataListener = new ODLListener() {

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			((AbstractTableModel) jTable.getModel()).fireTableDataChanged();
			updateEnabledActions();

		}

		@Override
		public void datastoreStructureChanged() {
			// TODO Auto-generated method stub

		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.TABLE_CHANGED;
		}

	};

	private final StandardEditActionHandlers editHandlers;
	private final BeanEditorPanelFactory<T> editorFactory;
	private ODLTable adaptedTable;
	private ExecutionReport lastReport;

	public CustomTableEditor(ODLApi api, Class<T> cls, BeanEditorPanelFactory<T> editorFactory,ODLDatastoreUndoable<? extends ODLTableAlterable>  ds) {

		setLayout(new BorderLayout());

		this.bmi = new BeanMappingInfo<>(api, cls, ds);
		this.editorFactory = editorFactory;

		ds.addListener(structureListener);
		ds.addListener(dataListener, -1);

		// init the table
		jTable = new JTable(createTableModel());
		jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jTable.setFillsViewportHeight(true);
		jTable.getTableHeader().setReorderingAllowed(false);
		jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateEnabledActions();
			}
		});
		jTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() >= 2) {
					editHandlers.actionPerformed(null, null, ActionType.EDIT);
				}
			}
		});

		// place the table in a scrollpane
		JScrollPane scrollPane = new JScrollPane(jTable);
		add(scrollPane, BorderLayout.CENTER);

		// add a toolbar
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		add(toolBar, BorderLayout.SOUTH);

		// create right-click popup menu on the list
		final JPopupMenu popup = new JPopupMenu();
		jTable.addMouseListener(new PopupMenuMouseAdapter() {

			@Override
			protected void launchMenu(MouseEvent me) {
				// ensure the correct row is selected
				int row = jTable.rowAtPoint(me.getPoint());
				if (row != -1) {
					jTable.getSelectionModel().setSelectionInterval(row, row);
				}

				popup.show(me.getComponent(), me.getX(), me.getY());
			}
		});

		// create the actions
		editHandlers = createActionHandlers();
		actions = StandardEditActionHandlers.createActions("linked file", editHandlers);
		for (Action action : actions) {
			toolBar.add(action);
			popup.add(action);
		}

		updateAdapter();

		updateEnabledActions();
	}
	
	/**
	 * Ensure the structure expected by the bean exists..
	 */
	private void applyStructure(){
		ExecutionReportImpl report = new ExecutionReportImpl();
		
		TableUtils.runTransaction(bmi.getDs(), new Callable<Boolean>() {
			
			@Override
			public Boolean call() throws Exception {
				bmi.getApi().tables().addTableDefinition(bmi.getMapping().getTableDefinition(), bmi.getDs(), true);
				return null;
			}
		});
		
		if (report.isFailed()) {
			ExecutionReportDialog dlg = new ExecutionReportDialog((JFrame) getWindowAncestor(), "Error", report, true);
			dlg.setVisible(true);
		}
	}


	private Window getWindowAncestor() {
		return SwingUtilities.getWindowAncestor(this);
	}
	
	private StandardEditActionHandlers createActionHandlers() {
		return new StandardEditActionHandlers() {

			private void doEdit(Callable<Boolean> callable, ActionType type) {
				if (!TableUtils.runTransaction(bmi.getDs(), callable)) {
					JOptionPane.showMessageDialog(CustomTableEditor.this, "Could not complete edit action:" + type);
				}
				;
			}

			@Override
			public void actionPerformed(ActionEvent e, UIAction action, ActionType type) {
				int selRow = jTable.getSelectedRow();
				int nbRow = jTable.getRowCount();
				ODLTable rawTable = bmi.getRawTable();
				if (adaptedTable == null || rawTable == null) {
					return;
				}

				Tables tables = bmi.getApi().tables();

				switch (type) {
				case ADD:
					try {
						T item = (T) bmi.getClass().newInstance();
						runEditor(item, new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								bmi.getMapping().writeObjectToTable(item, adaptedTable);
								return true;
							}
						}, type);
					} catch (Exception e2) {
						// TODO: handle exception
					}

					break;

				case EDIT:
					ExecutionReportImpl report = new ExecutionReportImpl();
					T item = bmi.getMapping().readObjectFromTableByRow(adaptedTable, selRow, report);
					if (report.isFailed()) {
						ExecutionReportDialog dlg = new ExecutionReportDialog((JFrame) getWindowAncestor(), "Error reading object", report, true);
						dlg.setVisible(true);
					}

					runEditor(item, new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							bmi.getMapping().updateTableRow(item, adaptedTable, item.getGlobalRowId());
							return true;
						}
					}, type);

					break;

				case DELETE_ITEM:
					if (selRow != -1) {
						doEdit(new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								rawTable.deleteRow(selRow);
								return true;
							}
						}, type);
					}

					break;

				case MOVE_ITEM_UP:
					if (selRow != -1 && selRow > 0) {
						doEdit(new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								tables.copyRow(rawTable, selRow, rawTable, selRow - 1);
								rawTable.deleteRow(selRow + 1);
								jTable.getSelectionModel().setSelectionInterval(selRow - 1, selRow - 1);
								return true;
							}
						}, type);
					}
					break;

				case MOVE_ITEM_DOWN:
					if (selRow != -1 && selRow < (nbRow - 1)) {
						doEdit(new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								tables.copyRow(rawTable, selRow, rawTable, selRow + 2);
								rawTable.deleteRow(selRow);
								jTable.getSelectionModel().setSelectionInterval(selRow + 1, selRow + 1);
								return true;
							}
						}, type);
					}
					break;

				default:
					break;
				}

			}

			private void runEditor(T item, Callable<Boolean> callable, ActionType type) {
				BeanEditorPanel<T> panel = editorFactory.createEditorPanel(item);

				try {
					OkCancelDialog dlg = new OkCancelDialog(getWindowAncestor(), true, true) {

						protected Component createMainComponent(boolean inWindowsBuilder) {
							return panel;
						}

					};
					if (dlg.showModal() == OkCancelDialog.OK_OPTION) {
						doEdit(callable, type);
					}
				} catch (Exception e) {
					if (panel != null) {
						panel.dispose();
					}
				}

			}


			@Override
			public void updateEnabledState(UIAction action, ActionType type) {
				int selRow = jTable.getSelectedRow();
				int nbRow = jTable.getRowCount();
				boolean enabled = adaptedTable != null;
				if (enabled) {
					switch (type) {
					case EDIT:
					case DELETE_ITEM:
						enabled = selRow != -1;
						break;

					case MOVE_ITEM_UP:
						enabled = selRow >= 1;
						break;

					case MOVE_ITEM_DOWN:
						enabled = selRow != -1 && selRow < (nbRow - 1);
						break;

					case ADD:
						enabled = true;
						break;

					default:
						enabled = false;
						break;
					}
				}
				action.setEnabled(enabled);
			}
		};
	}

	protected TableModel createTableModel() {
		ODLTableDefinition definition = bmi.getMapping().getTableDefinition();

		return new AbstractTableModel() {

			@Override
			public int getColumnCount() {
				return definition.getColumnCount();
			}

			@Override
			public int getRowCount() {
				if (hasAdapter()) {
					return adaptedTable.getRowCount();
				}
				return 0;
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (hasAdapter()) {
					return adaptedTable.getValueAt(rowIndex, columnIndex);
				}
				return null;
			}

			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				// if (hasAdapter() && rowIndex < adaptedTable.getRowCount() && columnIndex < adaptedTable.getColumnCount()) {
				// adaptedTable.setValueAt(aValue, rowIndex, columnIndex);
				// }
			}

		};
	}

	@Override
	public void dispose() {
		bmi.getDs().removeListener(structureListener);
		bmi.getDs().removeListener(dataListener);
	}

	private boolean hasAdapter() {
		return lastReport != null && lastReport.isFailed() == false && adaptedTable != null;
	}

	private void updateAdapter() {
		lastReport = new ExecutionReportImpl();
		adaptedTable = bmi.createTableAdapter(lastReport);
	}

	private void updateEnabledActions() {
		for (UIAction action : actions) {
			action.updateEnabledState();
		}
	}

}
