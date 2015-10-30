package com.opendoorlogistics.studio.tables.custom;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.app.ui.BeanEditorFactory;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.studio.LoadedState.HasLoadedDatastore;

/**
 * Shows a table which has a plugin popup box for editing items... Want simple controls to move items up and down, delete them and create new items. Want
 * pluggable popup controls for editing a row. The base table could have missing or misordered fields however. Could we build an adapter on-the-fly and create
 * missing fields???
 * 
 * @author Phil
 *
 * @param <T>
 */
public abstract class CustomTableEditor<T extends BeanMappedRow> extends JPanel implements Disposable {
	private final BeanMappingInfo<T> bmi;
	private final BeanEditorFactory<T> editorFactory;
	private final Component parent;
	private final ODLListener structureListener = new ODLListener() {

		@Override
		public void datastoreStructureChanged() {
			if(tableActivePanel!=null){
				tableActivePanel.datastoreStructureChanged();
			}
			
			updateMode();
		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			if(tableActivePanel!=null){
				tableActivePanel.tableChanged(tableId, firstRow, lastRow);;
			}
		}
	};

	private final ODLListener dataListener = new ODLListener() {

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			if(tableActivePanel!=null){
				tableActivePanel.tableChanged(tableId, firstRow, lastRow);;
			}
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

	private TableActivePanel<T> tableActivePanel;
	
	private enum Mode{
		TABLE_IS_PRESENT_IN_DATASTORE,
		TABLE_OR_FIELDS_MISSING
	}
	
	private Mode mode;
	

	private void fixDatastoreNoPrompt(){

		ExecutionReport report = new ExecutionReportImpl();

		if(bmi.getDs()==null){
			report.setFailed("No datastore is open.");
		}
		else{
			TableUtils.runTransaction(bmi.getDs(), new Callable<Boolean>() {
				
				@Override
				public Boolean call() throws Exception {
					bmi.getApi().tables().addTableDefinition(bmi.getMapping().getTableDefinition(), bmi.getDs(), true);
					return true;
				}
			});		
		}

		
		if (report.isFailed()) {
			ExecutionReportDialog dlg = new ExecutionReportDialog((JFrame) SwingUtilities.getWindowAncestor(parent), "Error", report, true);
			dlg.setVisible(true);
		}

		updateMode();
	}
	
	public CustomTableEditor(Component parent,ODLApi api, Class<T> cls, BeanEditorFactory<T> editorFactory,HasLoadedDatastore hasDs) {

		this.parent = parent;
		this.bmi = new BeanMappingInfo<>(api, cls, hasDs);
		this.editorFactory = editorFactory;
		
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		bmi.getDs().addListener(structureListener);
		bmi.getDs().addListener(dataListener, -1);

		updateMode();
	}
	


	@Override
	public void dispose() {
		bmi.getDs().removeListener(structureListener);
		bmi.getDs().removeListener(dataListener);
	}

	private void updateMode(){
		Tables tables = bmi.getApi().tables();
		ExecutionReportImpl report = new ExecutionReportImpl();
		ODLTableDefinition dfn = bmi.getMapping().getTableDefinition();
		
		Mode newMode = Mode.TABLE_IS_PRESENT_IN_DATASTORE;
		if(bmi.getDs() == null || !tables.getTableDefinitionExists(dfn, bmi.getDs(), true, report)){
			newMode = Mode.TABLE_OR_FIELDS_MISSING;
		}
		
		if(newMode!=mode){
			// remove all existing components
			removeAll();
			tableActivePanel = null;
			
			if(newMode == Mode.TABLE_IS_PRESENT_IN_DATASTORE){
				tableActivePanel = new TableActivePanel<>(bmi, editorFactory);
				add(tableActivePanel,BorderLayout.CENTER);
				setPreferredSize(new Dimension(800, 600));
			}else{
				initInactiveMode(report);
				setPreferredSize(null);
			}
			mode = newMode;

			onModeChange();
			validate();
			repaint();
		}
	}
	
	private void initInactiveMode(ExecutionReportImpl report){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		
		StringBuilder builder = new StringBuilder();
		builder.append("<html>The table " + bmi.getMapping().getTableDefinition().getName() + " and its fields must be present in your datastore.");
		builder.append("<br>Do you want to correct the issue(s) found below?<br>");
		for(String line : report.getLines(false)){
			builder.append("<br>-<i>");
			builder.append(line);
			builder.append("</i>");
		}
		builder.append("</html>");
		
		JLabel label = new JLabel(builder.toString());
		//JScrollPane pane = new JScrollPane(textArea);
		//textArea.setMinimumSize(new Dimension(600, 200));
		
		panel.add(label);
		
		panel.add(Box.createRigidArea(new Dimension(1, 10)));
		
		JButton button = new JButton("Click to fix datastore issues");
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fixDatastoreNoPrompt();
			}
		});
		
		panel.add(button);
		
		add(new JScrollPane(panel),BorderLayout.CENTER);
	}
	
	protected abstract void onModeChange();
}
