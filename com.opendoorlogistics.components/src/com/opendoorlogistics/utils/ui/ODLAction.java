/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import com.opendoorlogistics.api.app.ui.UIAction;
import com.opendoorlogistics.codefromweb.DropDownMenuButton;

public abstract class ODLAction extends AbstractAction implements UIAction {
	private Iterable<ODLAction> childActions;

	/**
	 * This is overridden in subclasses to disable / enable the action
	 * 
	 */
	@Override
	public void updateEnabledState() {

	}

	/**
	 * Child actions are used by the static methods in ODLAction to add hierarchical levels to a popup menu or toolbar
	 * 
	 * @param name
	 * @param smallIcon
	 * @param childActions
	 */
	private ODLAction(String name, Icon smallIcon, Iterable<ODLAction> childActions) {
		this(name, smallIcon);
		this.childActions = childActions;
	}

	public ODLAction(String name, String tooltip, Icon smallIcon) {
		putValue(Action.SHORT_DESCRIPTION, tooltip);
		putValue(Action.LONG_DESCRIPTION, tooltip);

		if (smallIcon != null) {
			putValue(Action.SMALL_ICON, smallIcon);
		}

		putValue(Action.NAME, name);

	}

	public ODLAction() {

	}

	public ODLAction(String name, Icon icon) {
		super(name, icon);

	}

	public ODLAction(String name) {
		super(name);
	}

	public static void addToMenu(JMenu menu, Iterable<ODLAction> actions) {
		for (ODLAction action : actions) {
			if (action == null) {
				menu.addSeparator();
			} else if (action.childActions != null) {
				JMenu nextLevel = new JMenu(action);
				menu.add(nextLevel);
				addToMenu(nextLevel, action.childActions);
			} else {
				action.updateEnabledState();
				menu.add(action);
			}
		}
	}

	public static void addToPopupMenu(JPopupMenu popup, Iterable<ODLAction> actions) {
		for (ODLAction action : actions) {
			if (action == null) {
				popup.addSeparator();
			} else if (action.childActions != null) {
				JMenu nextLevel = new JMenu(action);
				popup.add(nextLevel);
				addToMenu(nextLevel, action.childActions);
			} else {
				action.updateEnabledState();
				popup.add(action);
			}
		}
	}

	public static void addToToolbar(JToolBar toolbar, Iterable<ODLAction> actions) {
		for (final ODLAction action : actions) {
			if (action == null) {
				toolbar.addSeparator();
			} else if (action.childActions != null) {
				
				DropDownMenuButton button = new DropDownMenuButton((Icon)action.getValue(Action.SMALL_ICON)) {

					@Override
					protected JPopupMenu getPopupMenu() {
						JPopupMenu ret = new JPopupMenu();
						addToPopupMenu(ret, action.childActions);
						return ret;
					}
		
				};
				
				if(action.getValue(Action.SHORT_DESCRIPTION)!=null){
					button.setToolTipText(action.getValue(Action.SHORT_DESCRIPTION).toString());
				};
				
				toolbar.add(button);
			} else {
				action.updateEnabledState();
				toolbar.add(action);
			}


		}
	}

	public static ODLAction createParentAction(String name, Icon smallIcon, Iterable<ODLAction> childActions){
		return new ODLAction(name,smallIcon,childActions) {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		};
	}
}
