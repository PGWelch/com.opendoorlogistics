/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultEditorKit;

import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.utils.SortColumn;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.studio.dialogs.SortDialog;
import com.opendoorlogistics.studio.tables.ODLTableControl;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions.Permission;
import com.opendoorlogistics.studio.tables.grid.adapter.SwingAdapter;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;

public abstract class GridTable extends ODLTableControl implements Disposable {
	protected class ColumnModel extends DefaultTableColumnModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void addColumn(TableColumn aColumn) {
			if (getColumnCount() == 0) {
				aColumn.setPreferredWidth(36);
			}
			super.addColumn(aColumn);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6615355902465336058L;
	protected boolean showFilters = false;

	protected List<SimpleAction> actions;
	protected JScrollPane scrollPane;

	protected TableCellRenderer myCellRenderer;

	protected final TableCellRenderer firstColumnRenderer = new HeaderCellRenderer(){

		@Override
		protected boolean getColumnIsItalics(int col) {
			return false;
		}
		
	};
	// protected TableCellRenderer my = new HeaderCellRenderer();

	protected final int DEFAULT_ROW_HEIGHT = 24;

	protected final GridEditPermissions defaultPermissions;

	protected final SelectionManager selectionManager = createSelectionManager();

	protected GridTable(TableModel tableModel, GridEditPermissions permissions) {
		this.defaultPermissions = permissions;
		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		// set row height before setting the model as this is overrided with
		// images...
		setRowHeight(DEFAULT_ROW_HEIGHT);

		ColumnModel colModel = new ColumnModel();
		setColumnModel(colModel);
		setSurrendersFocusOnKeystroke(true);
		setFillsViewportHeight(true);
		setColumnSelectionAllowed(true);
		setRowSelectionAllowed(true);
		setRowSorter(null);

		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// setSelectionModel(selectionManager.getSheetSelectionModel());

		// init copy / paste handler
		CopyPasteTransferHandler copyPaste = new CopyPasteTransferHandler(this);
		setTransferHandler(copyPaste);

		initTableHeader();

		initActions(copyPaste, permissions);

		// right-click popup menu
		final JPopupMenu popup = new JPopupMenu();
		for (Action action : actions) {
			if (action != null) {
				popup.add(action);
			} else {
				popup.addSeparator();
			}
		}
		addMouseListener(new PopupMenuMouseAdapter() {

			@Override
			protected void launchMenu(MouseEvent me) {
				popup.show(me.getComponent(), me.getX(), me.getY());
			}
		});

		// start editing on a key press but not for special keys
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				// ignore if control pressed
				if ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK) {
					return;
				}

				if ((e.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK) {
					return;
				}

				if (e.isActionKey() || e.getKeyCode() == KeyEvent.VK_SHIFT || e.getKeyCode() == KeyEvent.VK_TAB || e.getKeyCode() == KeyEvent.VK_DELETE) {
					return;
				}

				if (selectionManager.getFocusPoint() != null) {
					Point point = selectionManager.getFocusPoint();
					if (!isEditing() && getModel().isCellEditable(point.y, point.x)) {
						clearSelection();
						selectionManager.setSelectedCell(point.y, point.x);

						// clear value first, but user the editor component to
						// clear the value
						// so the clear only enters the undo/redo buffer if the
						// user selects the new changed value
						if (editCellAt(point.y, point.x)) {
							Component component = getEditorComponent();
							if (component != null && JTextField.class.isInstance(component)) {
								((JTextField) component).setText("");
							}
						}
						transferFocus();
					}
				}
			}
		});

		addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				if (selectionManager != null) {
					selectionManager.focusLost(e);
				}
				repaint();
			}

			@Override
			public void focusGained(FocusEvent e) {
				// TODO Auto-generated method stub

			}
		});

		initMoveCursorMappings();

		// do set model last as this fires a table changed event which adjust
		// row heights for cells containing images
		setModel(tableModel);

	}

	protected SelectionManager createSelectionManager(){
		return new SelectionManager(this,false);
	}
	
	protected abstract void setHeaderRenderer();

	private void initTableHeader() {
		final JTableHeader header = getTableHeader();
		header.setReorderingAllowed(false);
		header.setFont(new Font("Dialog", Font.BOLD, 11));
		setHeaderRenderer();

		class MouseListenerImpl implements MouseListener, MouseMotionListener {
			// private boolean drag=false;

			@Override
			public void mouseReleased(MouseEvent e) {
				updateSel(e, false);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				updateSel(e, false);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// System.out.println("exit : drag=" + drag);

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// if(drag){
				// updateSel(e);
				// }
				// System.out.println("entered : drag=" + drag);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				int col = columnAtPoint(e.getPoint());
				if (col == 0) {
					selectAll();
				}
				updateSel(e, false);
				// System.out.println("clicked : drag=" + drag);

				TableCellRenderer headerRender = getTableHeader().getDefaultRenderer();
				if (headerRender != null && HeaderCellRenderer.class.isInstance(headerRender)) {
					((HeaderCellRenderer) headerRender).mouseClicked(e);
				}
			}

			private void updateSel(MouseEvent e, boolean drag) {
				int col = columnAtPoint(e.getPoint());

				selectionManager.changeSelection(-1, col, (e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0, e.isShiftDown() || drag);
				header.repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				updateSel(e, true);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		}
		MouseListenerImpl headerMouseListener = new MouseListenerImpl();
		header.addMouseListener(headerMouseListener);
		header.addMouseMotionListener(headerMouseListener);
	}

	@Override
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
		selectionManager.changeSelection(rowIndex, columnIndex, toggle, extend);
	}

	protected boolean runTransaction(Callable<Boolean> callable) {
		startTransaction();
		try {
			if (callable.call()) {
				endTransaction();
				return true;
			} else {
				rollbackTransaction();
			}
		} catch (Throwable e2) {
			rollbackTransaction();
			ExecutionReportImpl report = new ExecutionReportImpl();
			report.setFailed("An error occurred when editing the table data.");
			report.setFailed(e2);
			new ExecutionReportDialog((JFrame)SwingUtilities.getWindowAncestor(this), "Table editing error", report, false).setVisible(true);
			//JOptionPane.showMessageDialog(getRootPane(), "An error occurred." + (Strings.isEmpty(e2.getMessage()) == false ? " " + e2.getMessage() : ""));
		}
		return false;
	}

	protected void clearSelected() {
		runTransaction(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				int minRow = Integer.MAX_VALUE;
				int maxRow = Integer.MIN_VALUE;
				List<List<Point>> selected = selectionManager.getSelectedPoints();
				for (Point point : PasteLogic.toSingleList(selected)) {
					minRow = Math.min(minRow, point.y);
					maxRow = Math.max(maxRow, point.y);
					getModel().setValueAt(null, point.y, point.x);
					// System.out.println("Set null: " + point);
				}
				return true;
			}
		});

	}

	private void fillSelectedCells() {
		runTransaction(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				List<List<Point>> selected = selectionManager.getSelectedPoints();
				List<Point> points = PasteLogic.toSingleList(selected);
				if (points.size() > 0) {
					String val = JOptionPane.showInputDialog(getRootPane(), "Enter value to fill all selected cells");
					if (val != null) {
						for (Point point : points) {
							if (getModel().isCellEditable(point.y, point.x)) {
								getModel().setValueAt(val, point.y, point.x);
							}
						}
					}
				}
				return true;
			}
		});

	}

	@Override
	public void clearSelection() {
		super.clearSelection();
		if (selectionManager != null) {
			selectionManager.clearSelection();
		}
	}

	/**
	 * Creates the toolbar but doen't add it to any component as this must be done by the parent component
	 * 
	 * @return
	 */
	public JToolBar createToolbar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		for (Action action : actions) {
			if (action != null) {
				toolBar.add(action);
			} else {
				toolBar.addSeparator();
			}
		}

		toolBar.addSeparator();
		final JCheckBox checkBox = new JCheckBox("Show filters", showFilters);
		checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setShowFilters(checkBox.isSelected());
			}
		});
		// checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
		toolBar.add(checkBox);
		return toolBar;
	}

	protected abstract void deleteColumns();

	@Override
	public void dispose() {
		getModel().removeTableModelListener(this);
	};

	protected abstract void endTransaction();

	public abstract String getSelectedAsTabbedText();

	public abstract String getTableName();

	@SuppressWarnings("serial")
	protected void initActions(TransferHandler transferHandler, GridEditPermissions permissions) {
		abstract class MySimpleAction extends SimpleAction {

			private final boolean addToActionMap;

			public MySimpleAction(String name, String tooltip, String png, boolean addToActionMap) {
				super(name, tooltip, png);
				this.addToActionMap = addToActionMap;
			}

			public boolean isAddToActionMap() {
				return addToActionMap;
			}
		}

		actions = new ArrayList<>();

		class EventRedirector {
			void redirect(String longname, ActionEvent e) {
				// possible hack?
				Action original = getActionMap().get(longname.split("-")[0]);
				if (original != null) {
					ActionEvent newE = new ActionEvent(GridTable.this, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
					original.actionPerformed(newE);
				}
			}
		}
		final EventRedirector redirector = new EventRedirector();

		actions.add(new MySimpleAction("Create copy of table", "Create a copy of the table", "table-copy.png", false) {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyTable();
			}
		});

		actions.add(new MySimpleAction("Copy", "Copy selected", "edit-copy-7.png", false) {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				redirector.redirect(DefaultEditorKit.copyAction, e);
			}
		});

		actions.add(new MySimpleAction("Paste", "Paste", "edit-paste-7.png", false) {

			/**
				 * 
				 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				redirector.redirect(DefaultEditorKit.pasteAction, e);
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.setValues));
			}
		});

		actions.add(new MySimpleAction("Clear selected cells", "Clear selected cell(s)", "edit-clear-2.png", true) {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				clearSelected();
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.setValues));
			}
		});
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Clear cells");

		actions.add(new MySimpleAction("Fill selected cells", "Fill selected cell(s)", "tool-bucket-fill-16x16.png", false) {

			@Override
			public void actionPerformed(ActionEvent e) {
				fillSelectedCells();
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.setValues));
			}
		});

		actions.add(new MySimpleAction("Sort", "Sort rows", "view-sort-ascending-2.png", true) {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				SortDialog dialog = new SortDialog(getModel(), 1);

				Component parent = GridTable.this;
				if (parent.getParent() != null) {
					parent = parent.getParent();
				}

				dialog.setLocationRelativeTo(parent);
				dialog.setVisible(true);
				if (dialog.getResult() != null) {
					SortColumn[] sortCols = dialog.getResult();
					sort(sortCols);
					repaint();
				}
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.moveRows));
			}

		});

		actions.add(new MySimpleAction("Insert row", "Insert empty row(s)", "insert-table-row.png", true) {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				insertDeleteRows(true);
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.createRows));
			}
		});

		actions.add(new MySimpleAction("Delete row(s)", "Delete selected row(s)", "deleterow.png", true) {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				insertDeleteRows(false);
			}

			@Override
			public void updateEnabledState() {
				setEnabled(getPermissions().get(Permission.deleteRows));
			}
		});

		initSubclassActions(permissions);

		// put all in the action map
		for (SimpleAction action : actions) {
			if (action != null && MySimpleAction.class.isInstance(action)) {
				if (((MySimpleAction) action).isAddToActionMap()) {
					getActionMap().put(action.getValue(Action.NAME), action);
				}
			}
		}

	}

	protected void initMoveCursorMappings() {
		abstract class BaseMoveCursor extends AbstractAction {
			private final String name;
			protected final int keyCode;
			protected int modifiers;

			public BaseMoveCursor(String name, int keyCode) {
				super();
				this.name = name;
				this.keyCode = keyCode;
			}

			protected boolean isValidPoint(Point point) {
				return point.x >= 1 && point.x < getModel().getColumnCount() && point.y >= 0;
			}

			protected void selectPoint(Point point) {
				if (isValidPoint(point)) {
					if (isEditing()) {
						return;
					}
					clearSelection();
					selectionManager.setFocusPoint(point);
				}
			}
		}

		// class SelectAll extends BaseMoveCursor{
		//
		// public SelectAll() {
		// super("CTRL_A", KeyEvent.VK_A);
		// modifiers =InputEvent.CTRL_DOWN_MASK;
		// }
		//
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// // TODO Auto-generated method stub
		//
		// }
		//
		// }

		class MoveCursor extends BaseMoveCursor {
			protected final int deltaColumn;
			protected final int deltaRow;

			public MoveCursor(String name, int deltaRow, int deltaColumn, int keyCode) {
				super(name, keyCode);
				this.deltaRow = deltaRow;
				this.deltaColumn = deltaColumn;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Point point = selectionManager.getFocusPoint();
				if (point != null) {
					point.x += deltaColumn;
					point.y += deltaRow;
					selectPoint(point);
				}
			}
		}

		class ExtendMove extends MoveCursor {

			public ExtendMove(String name, int deltaRow, int deltaColumn, int keyCode) {
				super(name, deltaRow, deltaColumn, keyCode);
				modifiers = InputEvent.SHIFT_DOWN_MASK;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Point point = selectionManager.getFocusPoint();
				if (point != null) {
					point.x += deltaColumn;
					point.y += deltaRow;
					selectionManager.changeSelection(point.y, point.x, false, true);
				}
			}
		}

		class PageUpDown extends BaseMoveCursor {
			public PageUpDown(boolean up) {
				super(up ? "PAGE_UP" : "PAGE_DOWN", up ? KeyEvent.VK_PAGE_UP : KeyEvent.VK_PAGE_DOWN);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Point point = selectionManager.getFocusPoint();
				if (point != null) {

					Rectangle vr = getVisibleRect();
					int firstVisibleRow = rowAtPoint(vr.getLocation());
					vr.translate(0, vr.height);
					int visibleRows = rowAtPoint(vr.getLocation()) - firstVisibleRow;

					if (keyCode == KeyEvent.VK_PAGE_UP) {
						point.y -= visibleRows;
						point.y = Math.max(0, point.y);
					} else {
						point.y += visibleRows;
					}
					selectPoint(point);
				}
			}
		}

		class HomeEnd extends BaseMoveCursor {
			public HomeEnd(boolean home) {
				super(home ? "HOME" : "END", home ? KeyEvent.VK_HOME : KeyEvent.VK_END);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Point point = selectionManager.getFocusPoint();
				if (point != null) {
					if (keyCode == KeyEvent.VK_HOME) {
						point.x = 1;
					} else {
						point.x = getColumnCount() - 1;
					}
					selectPoint(point);
				}
			}
		}

		class ProcessTab extends BaseMoveCursor {
			public ProcessTab() {
				super("TAB", KeyEvent.VK_TAB);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Point point = selectionManager.getFocusPoint();
				if (point == null && isEditing()) {
					point = new Point(getEditingColumn(), getEditingRow());
				}

				if (point != null) {
					if (point.x < getModel().getColumnCount() - 1) {
						point.x++;
					} else {
						point.y++;
						point.x = 1;
					}

					if (isValidPoint(point)) {
						if (isEditing() && !getCellEditor().stopCellEditing()) {
							return;
						}

						clearSelection();
						selectionManager.setFocusPoint(point);
						editCellAt(point.y, point.x);
					}
				}
			}
		}

		ArrayList<BaseMoveCursor> moveActions = new ArrayList<>();
		moveActions.add(new MoveCursor("LEFT", 0, -1, KeyEvent.VK_LEFT));
		moveActions.add(new MoveCursor("RIGHT", 0, +1, KeyEvent.VK_RIGHT));
		moveActions.add(new MoveCursor("UP", -1, 0, KeyEvent.VK_UP));
		moveActions.add(new MoveCursor("DOWN", +1, 0, KeyEvent.VK_DOWN));
		moveActions.add(new ExtendMove("SHIFT_LEFT", 0, -1, KeyEvent.VK_LEFT));
		moveActions.add(new ExtendMove("SHIFT_RIGHT", 0, +1, KeyEvent.VK_RIGHT));
		moveActions.add(new ExtendMove("SHIFT_UP", -1, 0, KeyEvent.VK_UP));
		moveActions.add(new ExtendMove("SHIFT_DOWN", +1, 0, KeyEvent.VK_DOWN));
		moveActions.add(new ProcessTab());
		moveActions.add(new PageUpDown(true));
		moveActions.add(new PageUpDown(false));
		moveActions.add(new HomeEnd(true));
		moveActions.add(new HomeEnd(false));

		// put all in the action map
		for (BaseMoveCursor action : moveActions) {
			for (int imap : new int[] { JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_FOCUSED, JComponent.WHEN_IN_FOCUSED_WINDOW }) {
				getInputMap(imap).put(KeyStroke.getKeyStroke(action.keyCode, action.modifiers), action.name);
			}

			ActionMap map = getActionMap();
			map.put(action.name, action);
		}

	}

	protected void initSubclassActions(GridEditPermissions permissions) {

	}

	protected abstract void insertCols(boolean toLeft);

	protected abstract void insertDeleteRows(boolean inserting);

	@Override
	public boolean isCellSelected(int row, int column) {
		return selectionManager.isCellSelected(row, column);
	}

	@Override
	public boolean isRowSelected(int row) {
		return selectionManager.isRowSelected(row);
	}

	public abstract void pasteTabbedText(final String s);

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {

		// first column gets a header rendering instead
		if (column == 0) {
			renderer = firstColumnRenderer;
		} else {
			renderer = myCellRenderer;
		}
		Object value = getValueAt(row, column);

		// Only indicate the selection and focused cell if not printing
		boolean isSelected = isCellSelected(row, column);

		boolean hasFocus = selectionManager.isFocused(row, column) && isFocusOwner();

		return renderer.getTableCellRendererComponent(this, value, isSelected, hasFocus, row, column);
	}

	// protected abstract void redo();

	protected abstract void rollbackTransaction();

	protected abstract void sort(SortColumn[] sortCols);

	protected abstract void startTransaction();

	// protected abstract void undo();

	protected abstract void copyTable();

	/**
	 * Add the table together with toolbar and scrollpane to the container. This assumes the container is empty and replaces its layout.
	 * 
	 * @param table
	 * @param container
	 */
	public static JToolBar addToContainer(GridTable table, Container container) {
		JToolBar toolBar = table.createToolbar();
		container.setLayout(new BorderLayout());
		container.add(toolBar, BorderLayout.NORTH);
		table.scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		container.add(table.scrollPane, BorderLayout.CENTER);
		return toolBar;
	}

	abstract int getLastFilledRowNumber();

	@Override
	public void selectAll() {
		if (isEditing()) {
			removeEditor();
		}

		int nc = getColumnCount();
		LinkedList<Point> list = new LinkedList<>();
		int lastFilledRow = getLastFilledRowNumber();
		boolean allSel = true;
		for (int col = 1; col < nc; col++) {
			for (int row = 0; row <= lastFilledRow; row++) {
				list.add(new Point(col, row));
				if (selectionManager.isCellSelected(row, col) == false) {
					allSel = false;
				}
			}
		}

		if (allSel) {
			selectionManager.clearSelection();
		} else {
			selectionManager.setSelectedCells(list);
		}
		repaint();

	}

	public abstract GridEditPermissions getPermissions() ;
	
	public boolean isShowFilters() {
		return showFilters;
	}

	public void setShowFilters(boolean showFilters) {
		if (showFilters != this.showFilters) {
			this.showFilters = showFilters;
			setHeaderRenderer();

			tableHeader.invalidate();
			tableHeader.revalidate();
			tableHeader.updateUI();

		}
	}

	public void updateActions() {
		if(actions!=null){
			for (ODLAction action : actions) {
				if(action!=null){
					action.updateEnabledState();					
				}
			}			
		}
	}
}
