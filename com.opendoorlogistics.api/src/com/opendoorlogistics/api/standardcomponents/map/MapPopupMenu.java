package com.opendoorlogistics.api.standardcomponents.map;

import javax.swing.Action;
import javax.swing.JPopupMenu;

public abstract class MapPopupMenu extends JPopupMenu{

	public abstract void add(Action action, String group);

}
