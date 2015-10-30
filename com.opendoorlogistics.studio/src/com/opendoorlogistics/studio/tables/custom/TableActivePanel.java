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
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.app.ui.BeanEditorFactory;
import com.opendoorlogistics.api.app.ui.BeanEditorPanel;
import com.opendoorlogistics.api.app.ui.UIAction;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.codefromweb.PackTableColumn;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.studio.tables.custom.StandardEditActionHandlers.ActionType;

/**
 * The panel to show when the table is ok
 * @author Phil
 *
 */
public class TableActivePanel<T extends BeanMappedRow> extends JPanel implements ODLListener{
	private BeanMappingInfo<T> bmi;
	private final JTable jTable;
	private final List<UIAction> actions;
	private final StandardEditActionHandlers editHandlers;
	private final BeanEditorFactory<T> editorFactory;
	private ODLTable adaptedTable;
	private ExecutionReport lastReport;
	
	public TableActivePanel(BeanMappingInfo<T> bmi, BeanEditorFactory<T> editorFactory) {

		setLayout(new BorderLayout());
		
		this.bmi = bmi;
		this.editorFactory = editorFactory;

		// update the adapter first of all so we have a full table for the column packer to work on.. 
		updateAdapter();

		Component header = editorFactory.createTableHeader();
		if(header!=null){
			add(header, BorderLayout.NORTH);
		}
		
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
		
		PackTableColumn.packAll(jTable, 6);


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
		
		updateEnabledActions();
	}
	
	@Override
	public void tableChanged(int tableId, int firstRow, int lastRow) {
		fireTableDataChanged();
		updateEnabledActions();

	}

	@Override
	public void datastoreStructureChanged() {
		updateAdapter();
		fireTableDataChanged();
		updateEnabledActions();		
	}

	public void fireTableDataChanged() {
		// save table selection
		int selRow=jTable.getSelectedRow();
		AbstractTableModel model = (AbstractTableModel) jTable.getModel();
		model.fireTableDataChanged();
		if(selRow!=-1){
			jTable.getSelectionModel().setSelectionInterval(selRow, selRow);
		}
	}

	@Override
	public ODLListenerType getType() {
		// TODO Auto-generated method stub
		return null;
	}
	

	private Window getWindowAncestor() {
		return SwingUtilities.getWindowAncestor(this);
	}
	
	private StandardEditActionHandlers createActionHandlers() {
		return new StandardEditActionHandlers() {

			private void doEdit(Callable<Boolean> callable, ActionType type) {
				if (bmi.getDs()==null || !TableUtils.runTransaction(bmi.getDs(), callable)) {
					JOptionPane.showMessageDialog(TableActivePanel.this, "Could not complete edit action:" + type);
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
						T item = (T) bmi.getMapping().getBeanClass().newInstance();
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
					dlg.setResizable(panel.isResizable());
					
					switch(type){
					case ADD:
						dlg.setTitle("Add new record");
						break;
					case EDIT:
						dlg.setTitle("Edit record");
						break;
					default:
						break;
					}
					
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
		    public String getColumnName(int column) {
				return definition.getColumnName(column);
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
