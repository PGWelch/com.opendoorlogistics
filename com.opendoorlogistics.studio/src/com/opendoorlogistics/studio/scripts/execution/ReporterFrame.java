/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.Parameters.TableType;
import com.opendoorlogistics.api.scripts.parameters.ParametersControlFactory;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.tables.decorators.datastores.ListenerDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.core.utils.LoggerUtils;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager.GlobalSelectionChangedCB;
import com.opendoorlogistics.studio.controls.ODLScrollableToolbar;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;
import com.opendoorlogistics.utils.ui.Icons;

final public class ReporterFrame<T extends JPanel & Disposable> extends ODLInternalFrame implements GlobalSelectionChangedCB {
	private final static Logger LOGGER = Logger.getLogger(ReporterFrame.class.getName());
	
	private final GlobalMapSelectedRowsManager gsm;
	private final ReporterFrameIdentifier id;
	private final Border defaultBorder;
	private final Border outOfDateBorder = BorderFactory.createLineBorder(Color.RED, 2);
	private final RefreshMode refreshMode;
	private final ODLComponent callingComponent;
	// private final JPanel parametersPanel;
	private final SmartSouthPanel southPanel = new SmartSouthPanel();
	// private JCheckBox autorefreshBox;
	private JButton manualRefreshButton;
	private JLabel refreshLabel;
	private T userPanel;
	private volatile boolean isDirty = false;
	private OnRefreshReport refreshCB;
	private ODLDatastore<? extends ODLTableReadOnly> externalDs;
	private DataDependencies dependencies;
	private Script unfilteredScript;
	private ODLDatastoreAlterable<? extends ODLTableAlterable> parametersDs;
	private HashSet<ODLListener> listeners = new HashSet<>();
	private String title;
	private boolean showLastRefreshedTime;
	private LocalDateTime lastRefreshedTime;
	private JLabel lastRefreshedTimeLabel;
	private JLabel topLabel;

	public enum RefreshMode {
		AUTOMATIC, MANUAL_AUTO_DISABLE,MANUAL_ALWAYS_AVAILABLE, NEVER
	}

	public ReporterFrame(T userPanel, ReporterFrameIdentifier id, String title, ODLComponent component, RefreshMode refreshMode,boolean showLastRefreshedTime, GlobalMapSelectedRowsManager gmsrm) {
		super(id.getCombinedId());
		this.id = id;
		this.userPanel = userPanel;
		this.title = title;
		this.defaultBorder = getBorder();
		this.refreshMode = refreshMode;
		this.gsm = gmsrm;
		this.callingComponent = component;
		this.showLastRefreshedTime = showLastRefreshedTime;
		// this.parametersPanel = new JPanel();
		// this.parametersPanel.setLayout(new BorderLayout());

		// parametersPanel.setBorder(BorderFactory.createEmptyBorder());
		gsm.registerListener(this);

		setLayout(new BorderLayout());
		add(userPanel, BorderLayout.CENTER);
		saveLastRefreshedTime();

		if (refreshMode == RefreshMode.MANUAL_AUTO_DISABLE || refreshMode == RefreshMode.MANUAL_ALWAYS_AVAILABLE) {
			southPanel.addRefreshPanel(createRefreshControl());
		}
		updateAppearance();
	}

	private void saveLastRefreshedTime() {
		lastRefreshedTime = LocalDateTime.now();
	}

	public void setRefresherCB(OnRefreshReport cb) {
		this.refreshCB = cb;
		updateAppearance();
	}

	public ODLComponent getComponent() {
		return callingComponent;
	}

	/**
	 * South panel which adds / hides itself
	 * 
	 * @author Phil
	 *
	 */
	private class SmartSouthPanel extends ODLScrollableToolbar {
		private JPanel refreshPanel;
		private JPanel parametersPanel;

		SmartSouthPanel() {
			// setLayout(new FlowLayout(FlowLayout.LEFT));
			// setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			// setBorder(BorderFactory.createEmptyBorder());
			// setMinimumSize(new Dimension());
			getToolBar().setBorder(BorderFactory.createEmptyBorder());
		}

		void addRefreshPanel(JPanel panel) {
			this.refreshPanel = panel;
			update();
		}

		void addParametersPanel(JPanel panel) {
			this.parametersPanel = panel;
			update();
		}

		void update() {
			ReporterFrame.this.remove(this);
			getToolBar().removeAll();

			int count = 0;
			if (parametersPanel != null && parametersPanel.getComponentCount() > 0) {
				parametersPanel.setBorder(BorderFactory.createEmptyBorder());
				getToolBar().add(parametersPanel);
				count++;
			}

			if (refreshPanel != null) {
				getToolBar().add(refreshPanel);
				count++;
			}

			if (count > 0) {
				ReporterFrame.this.add(this, BorderLayout.SOUTH);
			}
		}
	}

	private JPanel createRefreshControl() {
		// instantiate and configure the toolbar obejct
		JPanel refreshBar = new JPanel();
		refreshBar.setLayout(new BoxLayout(refreshBar, BoxLayout.X_AXIS));
		// refreshBar.setFloatable(false);

		// add the manual refresh label
		refreshLabel = new JLabel(" Manual refresh ");
		refreshBar.add(refreshLabel);

		// add the manual refresh button
		manualRefreshButton = new JButton(Icons.loadFromStandardPath("view-refresh-6.png"));
		manualRefreshButton.setToolTipText("Manually refresh the report.");
		manualRefreshButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (refreshCB != null && unfilteredScript != null) {
					refreshCB.postReportRefreshRequest(unfilteredScript, id, false, parametersDs);
				}
			}
		});
		refreshBar.add(manualRefreshButton);

		if(showLastRefreshedTime){
			lastRefreshedTimeLabel = new JLabel("");
			refreshBar.add(lastRefreshedTimeLabel);			
		}
		
		// refreshBar.addSeparator();

		return refreshBar;
	}

	private void updateAppearance() {
		if (refreshMode == RefreshMode.MANUAL_AUTO_DISABLE || refreshMode == RefreshMode.MANUAL_ALWAYS_AVAILABLE) {
			boolean manualEnabled = isDirty && refreshCB != null;
			boolean alwaysEnabled = refreshCB!=null && refreshMode== RefreshMode.MANUAL_ALWAYS_AVAILABLE;
			manualRefreshButton.setEnabled(manualEnabled || alwaysEnabled);
			refreshLabel.setEnabled(manualEnabled|| alwaysEnabled);

			// boolean autoenabled = refreshCB != null && (dependencies == null
			// || dependencies.isWritten() == false);
			// autorefreshBox.setEnabled(autoenabled);

			setTitle(title + (isDirty ? " (OUT-OF-DATE)" : ""));

			// change border
			Border border;
			if (isDirty) {
				border = outOfDateBorder;
				// }else if(defaultBorder!=null){
				// border = defaultBorder;
			} else {
				border = defaultBorder;
			}

			if (getBorder() != border) {
				setBorder(border);
			}
			
		} else {
			setTitle(title);
		}
		
		if(lastRefreshedTimeLabel!=null && lastRefreshedTime!=null){
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
			lastRefreshedTimeLabel.setText("  Last refreshed " +formatter.format(lastRefreshedTime));
		}
	}

	public ReporterFrameIdentifier getId() {
		return id;
	}

	public JPanel getUserPanel() {
		return userPanel;
	}

	public void setUserPanel(T panel) {
		// changing panel?
		if (this.userPanel != panel) {

			// remove old panel
			if (this.userPanel != null) {
				remove(this.userPanel);
				this.userPanel.dispose();
			}

			// add new panel
			this.userPanel = panel;
			add(userPanel, BorderLayout.CENTER);
		}

		isDirty = false;
		saveLastRefreshedTime();
		updateAppearance();

		// need revalidate here or it doesn't always repaint (repaint doesn't
		// work)
		revalidate();
	}

	/**
	 * Set the reporter frame to be dirty. This can be called from other
	 * threads.
	 */
	public synchronized void setDirty() {
		if (isDirty == false) {
			isDirty = true;

			LOGGER.info(LoggerUtils.prefix() + " - reporter frame " + id.getCombinedId() + " set dirty. Title="+getTitle());
			
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateAppearance();
				}
			});

			if (refreshMode == RefreshMode.AUTOMATIC) {
				runAutorefresh();
			}
		}
	}

	public RefreshMode getRefreshMode() {
		return refreshMode;
	}

	private void runAutorefresh() {
		if (refreshCB != null && unfilteredScript != null && isDirty) {
			refreshCB.postReportRefreshRequest(unfilteredScript, id, true, parametersDs);
		}

	}

	public void setDependencies(ODLDatastore<? extends ODLTableReadOnly> ds, Script unfilteredScript, DataDependencies dependencies, ODLDatastore<? extends ODLTable> parametersDs,
			ExecutionReport report) {
		this.unfilteredScript = unfilteredScript;

		// remove any listeners from the saved datastore
		removeListeners();

		if (ds != null && dependencies != null) {
			this.externalDs = ds;
			this.dependencies = dependencies;

			// Add listener for table structure
			if (this.dependencies.isReadTableSet()) {
				ODLListener listener = new ODLListener() {

					@Override
					public void datastoreStructureChanged() {
						LOGGER.info(LoggerUtils.prefix() + " - reporter frame " + id.getCombinedId() + ", datastore structure changed. Title=" + getTitle());						
						setDirty();
					}

					@Override
					public void tableChanged(int tableId, int firstRow, int lastRow) {
						// TODO Auto-generated method stub

					}

					@Override
					public ODLListenerType getType() {
						return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
					}
				};

				listeners.add(listener);
				ds.addListener(listener);
			}

			// Add listeners for read table data
			int[] tableids = this.dependencies.getReadTableIds();
			if (tableids.length > 0) {
				ODLListener listener = new ODLListener() {

					@Override
					public void datastoreStructureChanged() {
					}

					@Override
					public void tableChanged(int tableId, int firstRow, int lastRow) {
						LOGGER.info(LoggerUtils.prefix() + " - reporter frame " + id.getCombinedId() + ", source table " + tableId + " is dirty. Title="+getTitle());
						setDirty();
					}

					@Override
					public ODLListenerType getType() {
						return ODLListenerType.TABLE_CHANGED;
					}
				};

				listeners.add(listener);
				ds.addListener(listener, tableids);
			}
		} else {
			this.externalDs = null;
			this.dependencies = null;
		}

		// handle parameters and show / update the panel as needed
		ODLApi api = new ODLApiImpl();
		this.parametersDs = api.tables().copyDs(parametersDs);
		Parameters parameters = api.scripts().parameters();
		ParametersControlFactory pcf = parameters.getControlFactory();

		// handle the case of overriding the visible parameters; replace with filtered table
		if (id != null && id.getInstructionId() != null && parametersDs != null) {
			String myOptionId = ScriptUtils.getOptionIdByInstructionId(unfilteredScript, id.getInstructionId());
			Option myOption = myOptionId != null ? ScriptUtils.getOption(unfilteredScript, myOptionId) : unfilteredScript;
			if (myOption != null && myOption.isOverrideVisibleParameters()) {
				ODLTable parametersTable = parameters.findTable(this.parametersDs, TableType.PARAMETERS);
				if (parametersTable != null) {
					ODLTable filteredParamsTable = ((ParametersImpl) parameters).applyVisibleOverrides(myOption.getVisibleParametersOverride(), parametersTable, report);
					if (!report.isFailed()) {
						api.tables().clearTable(parametersTable);
						for (int i = 0; i < filteredParamsTable.getRowCount(); i++) {
							api.tables().copyRow(filteredParamsTable, i, parametersTable);
							;
						}
					}
				}
			}
			// }
		}

		// create the parameters table
		JPanel parametersPanel = null;
		if (parametersDs != null && pcf != null && !report.isFailed()) {
			ListenerDecorator<ODLTable> listenerDecorator = new ListenerDecorator<ODLTable>(ODLTable.class, this.parametersDs);
			parametersPanel = pcf.createHorizontalPanel(api, parameters.findTable(listenerDecorator, TableType.PARAMETERS), parameters.findTable(parametersDs, TableType.PARAMETER_VALUES));
			if (parametersPanel != null) {
				listenerDecorator.addListener(new ODLListener() {

					@Override
					public void tableChanged(int tableId, int firstRow, int lastRow) {
						setDirty();
					}

					@Override
					public ODLListenerType getType() {
						return ODLListenerType.TABLE_CHANGED;
					}

					@Override
					public void datastoreStructureChanged() {
						setDirty();
					}
				}, new int[] { parameters.findTable(listenerDecorator, TableType.PARAMETERS).getImmutableId() });
			}
		}
		southPanel.addParametersPanel(parametersPanel);

		// it is assumed the control is no longer dirty after a call to set
		// dependencies
		isDirty = false;
		saveLastRefreshedTime();		
		updateAppearance();
	}

	private void removeListeners() {
		if (externalDs != null) {
			for (ODLListener listener : listeners) {
				this.externalDs.removeListener(listener);
			}
			listeners.clear();
		}
	}

	@Override
	public void dispose() {
		removeListeners();
		if (userPanel != null) {
			userPanel.dispose();
			userPanel = null;
		}
		gsm.unregisterListener(this);
		super.dispose();
	}

	public static interface OnRefreshReport {
		void postReportRefreshRequest(Script unfilteredScript, ReporterFrameIdentifier frameIdentifier, boolean isAutomaticRefresh, ODLDatastore<? extends ODLTable> parametersTable);
	}

	@Override
	public void selectionChanged(GlobalMapSelectedRowsManager manager) {
		if (dependencies != null && dependencies.isReadRowFlags()) {
			LOGGER.info(LoggerUtils.prefix() + " - reporter frame " + id.getCombinedId() + ", reads row flags and selection has changed. Title=" + getTitle());				
			setDirty();
		}
	}

	Script getUnfilteredScript() {
		return unfilteredScript;
	}

	public ODLDatastore<? extends ODLTable> getParametersTable() {
		return parametersDs;
	}

	// public Script getScript(){
	// return script;
	// }

	public void setTopLabel(String topLabelText){
		boolean changed=false;
		if(topLabelText==null){
			
			// remove top label if no longer needed
			if(topLabel!=null){
				remove(topLabel);
				topLabel = null;
				changed = true;
			}
		}else{
			if(topLabel==null){
				topLabel = new JLabel(topLabelText);				
				topLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
				add(topLabel, BorderLayout.NORTH);
				changed = true;
			}else{
				if(!topLabelText.equals(topLabel.getText())){
					topLabel.setText(topLabelText);
					changed = true;
				}
			}
				
		}
		
		if(changed){
			revalidate();
			repaint();			
		}
	}
}
