package com.opendoorlogistics.studio.tables.custom;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import com.opendoorlogistics.api.app.ui.UIAction;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleActionConfig;

/**
 * Interface which supports standard edit actions on an ordered set of items
 * @author Phil
 *
 */
public interface StandardEditActionHandlers {

	enum ActionType{
		ADD,
		EDIT,
		MOVE_ITEM_UP,
		MOVE_ITEM_DOWN,
		DELETE_ITEM
	}
	
    void actionPerformed(ActionEvent e,UIAction action, ActionType type);
	
    void updateEnabledState(UIAction action,ActionType type);
    
	/**
	 * Create swing actions from the handler
	 * @param itemName
	 * @param handlers
	 * @return
	 */
	public static List<UIAction> createActions(String itemName,StandardEditActionHandlers handlers){
		ArrayList<UIAction> ret = new ArrayList<>();
		
		class MyAction extends ODLAction{
			final ActionType type;
			
			MyAction(ActionType type) {
				super();
				this.type = type;
			}


			@Override
			public void actionPerformed(ActionEvent e) {
				handlers.actionPerformed(e, this, type);
			}
			
			@Override
			public void updateEnabledState() {
				handlers.updateEnabledState(this, type);
			}

		}
		
		ret.add(SimpleActionConfig.addItem.setItemName(itemName).apply(new MyAction(ActionType.ADD)));
		ret.add(SimpleActionConfig.editItem.setItemName(itemName).apply(new MyAction(ActionType.EDIT)));
		ret.add(SimpleActionConfig.moveItemUp.setItemName(itemName).apply(new MyAction(ActionType.MOVE_ITEM_UP)));
		ret.add(SimpleActionConfig.moveItemDown.setItemName(itemName).apply(new MyAction(ActionType.MOVE_ITEM_DOWN)));
		ret.add(SimpleActionConfig.deleteItem.setItemName(itemName).apply(new MyAction(ActionType.DELETE_ITEM)));
		
		
		return ret;
	}
}
