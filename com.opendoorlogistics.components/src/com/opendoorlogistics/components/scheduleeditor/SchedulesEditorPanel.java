/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.TileInternalFrames;
import com.opendoorlogistics.components.scheduleeditor.data.AbstractResource;
import com.opendoorlogistics.components.scheduleeditor.data.DataProvider;
import com.opendoorlogistics.components.scheduleeditor.data.EditorData;
import com.opendoorlogistics.components.scheduleeditor.data.beans.Task;
import com.opendoorlogistics.components.scheduleeditor.data.beans.TaskOrder;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.ODLAction;

public class SchedulesEditorPanel extends JPanel implements TaskMover, Disposable, DataProvider {
	private final ResourcesList vehiclesList = new ResourcesList(this);
//	private final JDesktopPane desktopPane;
	private ComponentControlLauncherApi api;
	private ODLDatastore<? extends ODLTable> ioDs;
	private EditorData data;
	private ArrayList<Task> notLoadsOrder = new ArrayList<>();
	
	public SchedulesEditorPanel(ComponentControlLauncherApi api) {
		// routes panel
		this.api = api;
	//	JPanel vehiclesPanel = new JPanel();
		setLayout(new BorderLayout());
		JLabel vehiclesLabel = new JLabel("Vehicles");
		vehiclesLabel.setHorizontalAlignment(SwingConstants.CENTER);
		add(vehiclesLabel, BorderLayout.NORTH);
		add(new JScrollPane(vehiclesList), BorderLayout.CENTER);
		vehiclesList.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));

		// do a custom renderer for the list of vehicle names
		vehiclesList.setCellRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				AbstractResource vehicle = (AbstractResource)value;
				boolean unloaded = Strings.equalsStd(vehicle.getId(), ScheduleEditorConstants.UNLOADED_VEHICLE);
				
				int count = getStopsByVehicle(vehicle.getId()).length;
				setText(vehicle.getId() + " (" + count + ")");
				
				if(unloaded && !isSelected){
					// red if we have unassigned stops, green if not
					if(count>0){
						setBackground(new Color(255, 200, 200));						
					}else{
						setBackground(new Color(200, 255, 200));
					}
				}

				return this;
			}

		});
		
		// listener for clicking on routes panel
		vehiclesList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					int index = vehiclesList.locationToIndex(evt.getPoint());
					if (index != -1) {
						launchSingleScheduleEditor(data.getResources()[index]);
					}
				}
			}
		});

		// add components to the panel
	//	desktopPane = new JDesktopPane();
	//	JPanel rightFrame = new JPanel();
		//rightFrame.setLayout(new BorderLayout());
		
		// don't use desktop scroll pane as tiling gets the incorrect size...
	//	rightFrame.add( desktopPane, BorderLayout.CENTER);
	//	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vehiclesPanel,rightFrame);
	//	setLayout(new BorderLayout());
	//	add(splitPane, BorderLayout.CENTER);
		
		// create toolbar on the right frame
		JToolBar toolBar = new JToolBar();
		toolBar.setLayout(new FlowLayout(FlowLayout.RIGHT));		
		toolBar.setFloatable(false);
		add(toolBar, BorderLayout.SOUTH);
	//	rightFrame.add(toolBar, BorderLayout.SOUTH);
//		toolBar.add(new ODLAction("Open all", "Open all routes", Icons.loadFromStandardPath("open-16x16.png")) {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				closeSingleScheduleWindows();
//
//				// open all.. doing in reverse order gets the same order as the list on-screen when tiled
//				for(int i =vehiclesList.getModel().getSize()-1; i>=0 ;i--){
//					launchSingleScheduleEditor(vehiclesList.getModel().getElementAt(i));
//				}
//				
//				tileRoutes();					
//				
//			}
//		});
		
//		toolBar.add(new ODLAction("Tile", "Tile all open route windows",Icons.loadFromStandardPath("application-tile-horizontal-16x16.png")) {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				tileRoutes();
//			}
//		});
		toolBar.add(new ODLAction("Close windows", "Close all route windows", Icons.loadFromStandardPath("close-all-windows.png")) {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				closeSingleScheduleWindows();
			}
		});
		
		// allowing unloading by dropping onto the desktop pane
		DropTarget dropTarget = new DropTarget();
		dropTarget.setActive(true);
		//desktopPane.setDropTarget(dropTarget);
		try {
			dropTarget.addDropTargetListener(new DropTargetListener() {
				
				@Override
				public void dropActionChanged(DropTargetDragEvent dtde) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void drop(DropTargetDropEvent dtde) {
					Transferable transferable = dtde.getTransferable();
					if(transferable!=null){
						String []stopIds = TaskTransferHandler.getStopIds(transferable);
						if(stopIds!=null){
							moveStop(stopIds, ScheduleEditorConstants.UNLOADED_VEHICLE, 0);
						}
					}
				}
				
				@Override
				public void dragOver(DropTargetDragEvent dtde) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void dragExit(DropTargetEvent dte) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void dragEnter(DropTargetDragEvent dtde) {
					// TODO Auto-generated method stub
					
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void setData(ComponentControlLauncherApi api,ODLDatastore<? extends ODLTable> ioDs) {
		this.api = api;
		this.ioDs = ioDs;
		this.data = EditorData.read(api.getApi(),ioDs);
		
		// set vehicles
		this.vehiclesList.setListData(data.getResources());
		
		// Update not-loads 1 - remove any not loads no longer unloaded...		
		Task [] newNotLoads = data.getTasksByResource(ScheduleEditorConstants.UNLOADED_VEHICLE);
		StandardisedStringSet newNotLoadsIds = Task.toTaskIds(Arrays.asList(newNotLoads));
		Iterator<Task> it = notLoadsOrder.iterator();
		while(it.hasNext()){
			Task next = it.next();
			if(newNotLoadsIds.contains(next.getId())==false){
				it.remove();
			}
		}
		
		// Update not-loads 2 - add any new not-loads at the front of the queue. 
		// Preserve order by doing this in reverse order.
		StandardisedStringSet currentNotLoadsIds = Task.toTaskIds(notLoadsOrder);
		for(int i = newNotLoads.length-1; i>=0;i--){
			Task notLoad=newNotLoads[i];
			if(currentNotLoadsIds.contains(notLoad.getId())==false){
				notLoadsOrder.add(0, notLoad);
			}			
		}

		// Update open windows; if vehicle no longer exists they will close themselves
		for(JPanel frame : api.getRegisteredPanels()){
			if (SingleScheduleFrame.class.isInstance(frame)) {
				SingleScheduleFrame sre = (SingleScheduleFrame)frame;
				AbstractResource vehicle = data.getResource(sre.getVehicleId());
				if(vehicle==null){
					api.disposeRegisteredPanel(sre);
				}else{
					sre.setData(data);
					api.setTitle(frame, sre.getTitle());
				}
				
			}
		}
	}

	
	private Task [] getStopsByVehicle(String vehicleId){
		if(Strings.equalsStd(ScheduleEditorConstants.UNLOADED_VEHICLE, vehicleId)){
			return notLoadsOrder.toArray(new Task[notLoadsOrder.size()]);				
		}else{			
			return data.getTasksByResource(vehicleId);
		}
		
	}
	
	private SingleScheduleFrame launchSingleScheduleEditor(AbstractResource vehicle) {
		// close if already exists
		for(JPanel frame : api.getRegisteredPanels()){
			if (SingleScheduleFrame.class.isInstance(frame)) {
				SingleScheduleFrame sre = (SingleScheduleFrame)frame;
				if (Strings.equalsStd(sre.getVehicleId(), vehicle.getId())) {
					api.disposeRegisteredPanel(frame);
				}
			}
		}


		SingleScheduleFrame sre = new SingleScheduleFrame(vehicle.getId(), this,api.getApi());
		sre.setData(data);
		
		// register panel setting it *not* refreshable as the main panel controls the refresh
		api.registerPanel("SSF" + vehicle.getId(), sre.getTitle(), sre, false);
	
//		try {
//			sre.setMaximum(true);
//		} catch (PropertyVetoException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return sre;
	}

//	public static void main(String[] args) {
//		try {
//			// set look and feel correctly
//			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//				if ("Nimbus".equals(info.getName())) {
//					UIManager.setLookAndFeel(info.getClassName());
//				}
//			}
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
//
//		// create random data
//		Random random = new Random();
//		final VehicleType[] vehicleTypes = new VehicleType[2];
//		int nbPerType=2;
//		for (int i = 0; i < vehicleTypes.length; i++) {
//			VehicleType vehicle = new VehicleType();
//			String id = i==0? "Lorry" : "Van";
//			vehicle.setId(id);
//			vehicle.setName(id);
//			vehicle.setNumber(2);
//			vehicleTypes[i] = vehicle;
//		}
//
//		final EditorStop[] stops = new EditorStop[25];
//		for (int i = 0; i < stops.length; i++) {
//			stops[i] = new EditorStop();
//			stops[i].setId("Stop" + (i + 1));
//			stops[i].setName(ExampleData.getRandomBusinessName(random));
//		}
//
//		final EditorStopOrder[] stopOrder = new EditorStopOrder[stops.length / 2];
//		for (int i = 0; i < stopOrder.length; i++) {
//			stopOrder[i] = new EditorStopOrder();
//			stopOrder[i].setVehicleId(vehicleTypes[random.nextInt(vehicleTypes.length)].getId() + (1+random.nextInt(nbPerType)));
//			stopOrder[i].setStopId(stops[2 * i].getId());
//		}
//		Arrays.sort(stopOrder, new Comparator<EditorStopOrder>() {
//
//			@Override
//			public int compare(EditorStopOrder o1, EditorStopOrder o2) {
//				return o1.getVehicleId().compareTo(o2.getVehicleId());
//			}
//		});
//		
//		// map to datastore
//		class MapToDs{
//			ODLDatastore<? extends ODLTable>  doMapping(){
//				// copy data into arrays
//				BeanMappedRow[][] rows = new BeanMappedRow[3][];
//				rows[RouteEditorConstants.VEHICLES_TABLE_INDEX] = new BeanMappedRow[vehicleTypes.length];
//				System.arraycopy(vehicleTypes, 0, rows[RouteEditorConstants.VEHICLES_TABLE_INDEX], 0, vehicleTypes.length);
//
//				rows[RouteEditorConstants.STOPS_TABLE_INDEX] = new BeanMappedRow[stops.length];
//				System.arraycopy(stops, 0, rows[RouteEditorConstants.STOPS_TABLE_INDEX], 0, stops.length);
//
//				rows[RouteEditorConstants.STOP_ORDER_TABLE_INDEX] = new BeanMappedRow[stopOrder.length];
//				System.arraycopy(stopOrder, 0, rows[RouteEditorConstants.STOP_ORDER_TABLE_INDEX], 0, stopOrder.length);
//
//				RouteEditorComponent component = new RouteEditorComponent();
//				BeanDatastoreMapping beanMapping = component.getBeanMapping();
//				ODLDatastore<? extends ODLTable> iods = beanMapping.writeObjectsToDatastore(rows);
//				return iods;
//			}
//		}
//		ODLDatastore<? extends ODLTable> iods = new MapToDs().doMapping();
//		
//		UndoRedoDecorator<ODLTable> undoRedo = new UndoRedoDecorator<>(ODLTable.class, iods);
//		System.out.println(iods);
//
//		ODLApiImpl api = new ODLApiImpl();
//		RouteEditorPanel panel = new RouteEditorPanel(api);
//		panel.setData(undoRedo);
//		ShowPanel.showPanel(panel);
//	}

	@Override
	public boolean moveStop(String[] stopIds, String vehicleId, int position) {
		// check the stops are known...
		if(stopIds.length==0){
			return false;
		}
		for(String id:stopIds){
			if(data.getTask(id)==null){
				return false;
			}
		}
		
		ODLTable stopsOrder = ioDs.getTableAt(ScheduleEditorConstants.TASK_ORDER_TABLE_INDEX);
		boolean rollbackSupported = ioDs.isRollbackSupported();
		long tableFlags = stopsOrder.getFlags();
		boolean flagsOk = ((tableFlags & TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS) == TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS);
		if (!rollbackSupported|| !flagsOk) {
			showMessage("Cannot move stops. Route editing is unsupported on tables which have are filtered, sorted etc...");
			return false;
		}

		// get the vehicle record
		AbstractResource vehicle = data.getResource(vehicleId);
		if (vehicle == null) {
			return false;
		}

		// get the set of stop ids to be inserted
		StandardisedStringSet movedSet = new StandardisedStringSet(false);
		for (String stopId : stopIds) {
			movedSet.add(stopId);
		}
		
		// get the current stops order on the vehicle
		Task[] currentRoute =getStopsByVehicle(vehicleId);
	
		// find the stops before and after the insertion position which aren't being moved themselves
		Task stopBeforeNewStops = null;
		for (int i = position - 1; i >= 0; i--) {
			if (movedSet.contains(currentRoute[i].getId()) == false) {
				stopBeforeNewStops = currentRoute[i];
				break;
			}
		}
		Task stopAfterNewStops = null;
		for (int i = position; i < currentRoute.length; i++) {
			if (movedSet.contains(currentRoute[i].getId()) == false) {
				stopAfterNewStops = currentRoute[i];
				break;
			}
		}

		// now parse and update the raw stop-order table
		if (ioDs.isInTransaction()) {
			showMessage("Cannot move stops as datastore has an open transaction.");
			return false;
		}

		ioDs.startTransaction();
		BeanTableMappingImpl beanMapping = new ScheduleEditorComponent().getBeanMapping().getTableMapping(ScheduleEditorConstants.TASK_ORDER_TABLE_INDEX);
		try {
			// remove all stops being moved from the stop-order table
			int row = 0;
			while (row < stopsOrder.getRowCount()) {
				// should this stop be removed?
				TaskOrder record = (TaskOrder) beanMapping.readObjectFromTableByRow(stopsOrder, row);
				if (movedSet.contains(record.getTaskId())) {
					stopsOrder.deleteRow(row);
				} else {
					row++;
				}
			}
			
			// also remove from our internal record of not loads order
			Iterator<Task> it = notLoadsOrder.iterator();
			while(it.hasNext()){
				if(movedSet.contains(it.next().getId())){
					it.remove();
				}
			}

			// add new ones to the stops order table if we're not 'adding' to the unassigned vehicle
			if (Strings.equalsStd(vehicleId, ScheduleEditorConstants.UNLOADED_VEHICLE) == false) {
				boolean addedNewStops = false;
				int n = stopsOrder.getRowCount();
				for (row = 0; row < n; row++) {

					// is this a stop we should add the inserted stops before?
					TaskOrder record = (TaskOrder) beanMapping.readObjectFromTableByRow(stopsOrder, row);
					if (stopAfterNewStops != null && Strings.equalsStd(record.getTaskId(), stopAfterNewStops.getId())) {
						addTasks(vehicleId, stopIds, beanMapping, stopsOrder, row);
						addedNewStops = true;
						break;
					}

					// is this a stop we should add the inserted stops after?
					if (stopBeforeNewStops != null && Strings.equalsStd(record.getTaskId(), stopBeforeNewStops.getId())) {
						addTasks(vehicleId, stopIds, beanMapping, stopsOrder, row + 1);
						addedNewStops = true;
						break;
					}
				}

				// add at the end if all else fails
				if (!addedNewStops) {
					addTasks(vehicleId, stopIds, beanMapping, stopsOrder, stopsOrder.getRowCount());
				}
			}
			else{
				// update the internal not-loads array
				for(int i = stopIds.length-1;i>=0;i--){
					Task stop = data.getTask(stopIds[i]);
					if(stop==null){
						throw new RuntimeException("Unknown stop-id " + stopIds[i]);
					}
					notLoadsOrder.add(Math.min(position,notLoadsOrder.size()), stop);
				}
			}
			
			
			// save the changes
			ioDs.endTransaction();

			// Update all controls. This call is probably not needed when running this class
			// from a component as the framework will call an update anyway...
			setData(api,ioDs);
		} catch (Exception e) {
			ioDs.rollbackTransaction();
			e.printStackTrace();
			showMessage("An error occurred when moving stops.");
			return false;
		}

		// set the new data
		return true;
	}

	private void showMessage(String message) {
		JOptionPane.showMessageDialog(this, message);
	}

	private void addTasks(String vehicleId, String[] stopIdsToAdd, BeanTableMappingImpl beanMapping, ODLTable table, int row) {
		for (String id : stopIdsToAdd) {
			if (data.getTask(id) == null) {
				throw new RuntimeException("Unknown stop-id " + id);
			}

			TaskOrder so = new TaskOrder();
			so.setResourceId(vehicleId);
			so.setTaskId(id);

			table.insertEmptyRow(row, -1);
			long rowId = table.getRowId(row);
			row++;
			beanMapping.updateTableRow(so, table, rowId);
		}
	}

	@Override
	public void dispose() {
		closeSingleScheduleWindows();
	}

//	/**
//	 * 
//	 */
//	private void tileRoutes() {
//		for(JInternalFrame frame:desktopPane.getAllFrames()){
//			try {
//				frame.setMaximum(false);
//			} catch (PropertyVetoException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		TileInternalFrames.tile(desktopPane);
//	}

	/**
	 * 
	 */
	private void closeSingleScheduleWindows() {
		for(JPanel frame : api.getRegisteredPanels()){
			if(SingleScheduleFrame.class.isInstance(frame)){
				api.disposeRegisteredPanel(frame);
			}
		}
	}
	
	@Override
	public EditorData getData(){
		return data;
	}
}
