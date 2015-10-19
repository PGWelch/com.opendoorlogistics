/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui;

import java.awt.BorderLayout;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;

import org.w3c.dom.Document;

import com.opendoorlogistics.core.scripts.io.XMLConversionHandler;
import com.opendoorlogistics.core.tables.utils.HasShortDescription;
import com.opendoorlogistics.core.utils.XMLUtils;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;

public abstract class ItemsPanel<T> extends JPanel {
	protected final List<T> items;
	protected final JScrollPane listScrollPane;
	protected final List<MyAction> actions;
	protected final String itemName;
	protected boolean printNumbered;
	protected JComponent itemsComponent;
	private XMLConversionHandler conversionHandler;
	
	protected class ItemContainer {
		final T item;
		final int indx;
		ItemContainer(T item, int indx) {
			this.item = item;
			this.indx = indx;
		}

		@Override
		public String toString() {
			return (printNumbered ? Integer.toString(indx+1) + ". " : "") + (HasShortDescription.class.isInstance(item)? ((HasShortDescription)item).getShortDescription() : item.toString());
		}
	}

	public abstract class MyAction extends SimpleAction {

		public MyAction(SimpleActionConfig config) {
			super(config);
		}

		@Override
		public void updateEnabledState() {
			setEnabled(requiresSelection == false || getSelected() != null);
		}
	}

	public void addTitleLabel(String title) {
		add(new JLabel(title),BorderLayout.NORTH);
	}
	
	protected abstract JComponent createItemsComponent();


	public ItemsPanel(List<T> items, String itemName) {
		this.items = items;
		this.itemName = itemName;
		this.itemsComponent = createItemsComponent();
		setLayout(new BorderLayout());

		listScrollPane = new JScrollPane();
		listScrollPane.setViewportView(itemsComponent);
		setLayout(new BorderLayout());
		add(listScrollPane, BorderLayout.CENTER);

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		add(toolBar, BorderLayout.SOUTH);

		// create right-click popup menu on the list
		final JPopupMenu popup = new JPopupMenu();
		itemsComponent.addMouseListener(new PopupMenuMouseAdapter() {
			
			@Override
			protected void launchMenu(MouseEvent me) {
				popup.show(me.getComponent(), me.getX(), me.getY());				
			}
		});

//		// create double click event on list
//		itemsComponent.addMouseListener(new MouseAdapter() {
//			public void mouseClicked(MouseEvent evt) {
//				if (evt.getClickCount() >= 2) {
//					// launchSelectedTable();
//				}
//			}
//		});

		// create all actions and add as buttons and menu items
		actions = createActions();
		for (Action action : actions) {
			toolBar.add(action);
			popup.add(action);
		}


		// create double click event on the list
		itemsComponent.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() >= 2) {
					editSelected();
				}
			}
		});

		updateList();

		updateAppearance();
	}
	
	private void onAction(String actionStr){
		// See http://stackoverflow.com/questions/17589304/how-to-invoke-jtable-action-from-outside-jbutton
		Action action = itemsComponent.getActionMap().get(actionStr);
		if(action!=null){
			ActionEvent newAE = new ActionEvent(itemsComponent, ActionEvent.ACTION_PERFORMED, actionStr);
		    action.actionPerformed(newAE);
		}
	}

	protected List<MyAction> createActions() {
		ArrayList<MyAction> ret = new ArrayList<>();

		ret.add(new MyAction(SimpleActionConfig.addItem.setItemName(itemName)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				T item = createNewItem();
				if (item != null) {
					addNewItem(item);
				}
			}
		});

		ret.add(new MyAction(SimpleActionConfig.editItem.setItemName(itemName)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				editSelected();
			}
		});

		ret.add(new MyAction(SimpleActionConfig.copyItem.setItemName(itemName)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				onAction("copy");
			}
			
			@Override
			public void updateEnabledState() {
				setEnabled(conversionHandler!=null && getSelected()!=null);
			}
		});

		ret.add(new MyAction(SimpleActionConfig.pasteItem.setItemName(itemName)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				onAction("paste");
			}
			
			@Override
			public void updateEnabledState() {
				setEnabled(conversionHandler!=null );
			}
		});

		ret.add(new MyAction(SimpleActionConfig.moveItemUp.setItemName(itemName)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				int indx = getSelectedIndx();
				if (indx > 0) {
					T item = items.remove(indx);
					indx--;
					items.add(indx, item);
					updateList();
					setSelectedIndex(indx);
				}

			}
		});

		ret.add(new MyAction(SimpleActionConfig.moveItemDown.setItemName(itemName)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				int indx = getSelectedIndx();
				if (indx < items.size() - 1) {
					T item = items.remove(indx);
					indx++;
					items.add(indx, item);
					updateList();
					setSelectedIndex(indx);
				}
			}
		});

		ret.add(new MyAction(SimpleActionConfig.deleteItem.setItemName(itemName)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				items.remove(getSelectedIndx());
				updateList();
			}
		});

		return ret;
	}

	protected abstract void updateList();

	protected void updateAppearance() {
		for (MyAction action : actions) {
			action.updateEnabledState();
		}
	}
	
	protected abstract int getSelectedIndx();
	
	protected abstract void setSelectedIndex(int index);
	
	protected abstract T createNewItem();

	protected abstract T editItem(T item);

	protected T getSelected(){
		int indx = getSelectedIndx();
		if(indx!=-1){
			return items.get(indx);
		}
		return null;
	}

	public void editSelected() {
		int indx = getSelectedIndx();
		T selected = getSelected();
		if(selected==null){
			return;
		}
		T edited = editItem(selected);
		if (edited != null) {
			items.set(indx, edited);
			updateList();
			setSelectedIndex(indx);
		}
	}

	public void update(){
		updateList();
		updateAppearance();
	}
	
	public void select(T object){
		for(int i =0 ; i<items.size() ; i++){
			if(items.get(i)==object){
				setSelectedIndex(i);
				break;
			}
		}
	}

	public boolean isPrintNumbered() {
		return printNumbered;
	}

	public void setPrintNumbered(boolean printNumbered) {
		this.printNumbered = printNumbered;
	}

	public XMLConversionHandler getConversionHandler() {
		return conversionHandler;
	}

	public void setConversionHandler(XMLConversionHandler conversionHandler) {
		this.conversionHandler = conversionHandler;
		if(conversionHandler!=null){			
			itemsComponent.setTransferHandler(new XMLTransferHandler());
		}else{
			itemsComponent.setTransferHandler(null);
		}
		updateAppearance();
	}

	private void addNewItem(T item) {
		items.add(item);
		updateList();
		setSelectedIndex(items.size() - 1);
	}
	
	
	private class XMLTransferHandler extends TransferHandler{

		@Override
		public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
			Transferable c = createTransferable(comp);
			clip.setContents(c, null);
			exportDone(comp, c, action);
		}

		@Override
		public int getSourceActions(JComponent c) {
			return COPY_OR_MOVE;
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			return conversionHandler!=null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean importData(JComponent c, Transferable t) {
			if(conversionHandler!=null){
				if (canImport(new TransferHandler.TransferSupport(c, t))) {

					DataFlavor flavor = DataFlavor.stringFlavor;
					if (t.isDataFlavorSupported(flavor)) {
						try {
							Object data = t.getTransferData(flavor);
							if (data instanceof String) {
								Document doc = XMLUtils.parse((String)data);
								if(doc!=null){
									List<Object> list = conversionHandler.fromXML(doc);
									if(list!=null){
										for(Object obj:list){
											addNewItem((T)obj);
										}
									}
								}
							}
						} catch (Throwable e) {
						}
					}

				}	
			}

			return false;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			if(conversionHandler!=null && getSelected()!=null){
				Document doc = conversionHandler.toXML(getSelected());
				if(doc!=null){
					String s= XMLUtils.toString(doc, XMLUtils.getPrettyPrintFormat());
					return new StringSelection(s);					
				}
			}
			return null;
		}
	}
}
