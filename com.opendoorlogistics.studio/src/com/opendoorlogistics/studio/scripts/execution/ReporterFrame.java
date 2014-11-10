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
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager.GlobalSelectionChangedCB;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;
import com.opendoorlogistics.utils.ui.Icons;

final class ReporterFrame<T extends JPanel & Disposable> extends ODLInternalFrame implements  GlobalSelectionChangedCB{
	private final GlobalMapSelectedRowsManager gsm;
	private final ReporterFrameIdentifier id;
	private final Border defaultBorder;
	private final Border outOfDateBorder = BorderFactory.createLineBorder(Color.RED, 2);
	private final RefreshMode refreshMode;
	//private JCheckBox autorefreshBox;
	private JButton manualRefreshButton;
	private JLabel refreshLabel;
	private T userPanel;
	private volatile boolean isDirty = false;
	private OnRefreshReport refreshCB;
	private ODLDatastore<? extends ODLTableReadOnly> ds;
	private DataDependencies dependencies;
	private Script unfilteredScript;
	private HashSet<ODLListener> listeners = new HashSet<>();
	private String title;

	public enum RefreshMode{
		AUTOMATIC,
		MANUAL,
		NEVER
	}
	
	public ReporterFrame(T userPanel, ReporterFrameIdentifier id, String title, RefreshMode refreshMode, GlobalMapSelectedRowsManager gmsrm) {
		super(id.getCombinedId());
		this.id = id;
		this.userPanel = userPanel;
		this.title = title;
		this.defaultBorder = getBorder();
		this.refreshMode = refreshMode;
		this.gsm = gmsrm;
		
		gsm.registerListener(this);
		
		setLayout(new BorderLayout());
		add(userPanel, BorderLayout.CENTER);

		if (refreshMode == RefreshMode.MANUAL) {
			createRefreshToolbar();
			//autorefreshBox.setSelected(refreshMode == PanelRefreshMode.AUTOMATIC);
		}
		updateAppearance();
	}

	public void setRefresherCB(OnRefreshReport cb) {
		this.refreshCB = cb;
		updateAppearance();
	}

	private void createRefreshToolbar() {
		// instantiate and configure the toolbar obejct
		JToolBar refreshBar = new JToolBar();
		refreshBar.setFloatable(false);
		add(refreshBar, BorderLayout.SOUTH);

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
					refreshCB.postReportRefreshRequest(unfilteredScript,id, false);
				}
			}
		});
		refreshBar.add(manualRefreshButton);

		refreshBar.addSeparator();

//		// add the autorefresh checkbox
//		autorefreshBox = new JCheckBox("Automatically refresh control when data changes (can be slow)");
//		autorefreshBox
//				.setToolTipText("<html>Automatically refresh the report when data changes.<br>This can cause the UI to be slow or temporarily freeze for reports that take longer to generate.</html>");
//		autorefreshBox.setHorizontalAlignment(SwingConstants.LEFT);
//		autorefreshBox.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if (autorefreshBox.isSelected() && isDirty) {
//					runAutorefresh();
//				}
//			}
//		});
//		refreshBar.add(autorefreshBox);

	}

	private void updateAppearance() {
		if (refreshMode == RefreshMode.MANUAL) {
			boolean manualEnabled = isDirty && refreshCB != null;
			manualRefreshButton.setEnabled(manualEnabled);
			refreshLabel.setEnabled(manualEnabled);

			//boolean autoenabled = refreshCB != null && (dependencies == null || dependencies.isWritten() == false);
			//autorefreshBox.setEnabled(autoenabled);

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
		}else{
			setTitle(title);
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
		if(this.userPanel != panel){
			
			// remove old panel
			if(this.userPanel!=null){
				remove(this.userPanel);
				this.userPanel.dispose();		
			}
			
			// add new panel
			this.userPanel = panel;
			add(userPanel, BorderLayout.CENTER);				
		}
		
		isDirty = false;
		updateAppearance();

		// need revalidate here or it doesn't always repaint (repaint doesn't work)
		revalidate();
	}

	/**
	 * Set the reporter frame to be dirty. This can be called from other threads.
	 */
	public synchronized void setDirty() {
		if (isDirty == false) {
			isDirty = true;

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

	public RefreshMode getRefreshMode(){
		return refreshMode;
	}
	
	private void runAutorefresh() {
		if (refreshCB != null && unfilteredScript != null && isDirty) {
			refreshCB.postReportRefreshRequest(unfilteredScript,id, true);
		}
	
	}

	public void setDependencies(ODLDatastore<? extends ODLTableReadOnly> ds, Script unfilteredScript, DataDependencies dependencies) {
		this.unfilteredScript = unfilteredScript;

		// remove any listeners from the saved datastore
		removeListeners();

		if (ds != null && dependencies != null) {
			this.ds = ds;
			this.dependencies = dependencies;

			if (this.dependencies.isReadTableSet()) {
				ODLListener listener = new ODLListener() {

					@Override
					public void datastoreStructureChanged() {
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

			int[] tableids = this.dependencies.getReadTableIds();
			if (tableids.length > 0) {
				ODLListener listener = new ODLListener() {

					@Override
					public void datastoreStructureChanged() {
					}

					@Override
					public void tableChanged(int tableId, int firstRow, int lastRow) {
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
		}else{
			this.ds=null;
			this.dependencies = null;
		}

		// it is assumed the control is no longer dirty after a call to set dependencies
		isDirty = false;
		updateAppearance();
	}

	private void removeListeners() {
		if (ds != null) {
			for (ODLListener listener : listeners) {
				this.ds.removeListener(listener);
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
		void postReportRefreshRequest(Script unfilteredScript,ReporterFrameIdentifier frameIdentifier, boolean isAutomaticRefresh);
	}

	@Override
	public void selectionChanged(GlobalMapSelectedRowsManager manager) {
		if(dependencies!=null && dependencies.isReadRowFlags()){
			setDirty();
		}
	}
	
	Script getUnfilteredScript(){
		return unfilteredScript;
	}
	
//	public Script getScript(){
//		return script;
//	}
}
