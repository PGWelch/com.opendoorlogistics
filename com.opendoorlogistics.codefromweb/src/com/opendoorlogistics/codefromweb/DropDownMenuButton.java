package com.opendoorlogistics.codefromweb;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Based on the example at
 * http://www.jroller.com/santhosh/entry/dropdownbutton_for_swing by santhosh
 * kumar - santhosh@in.fiorano.com
 */
public abstract class DropDownMenuButton extends JButton {

	public DropDownMenuButton(Icon icon) {
		this(icon, true);
	}
	
	public DropDownMenuButton(Icon icon, boolean addListener) {
		super(icon);
		setMargin(new Insets(3, 0, 3, 0));
		
		if(addListener){
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					processPopup();
				}
			});			
		}
	}

	protected abstract JPopupMenu getPopupMenu();

	public void processPopup() {
		JPopupMenu popup = getPopupMenu();
		if (popup != null) {

			popup.addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					setSelected(true);
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					setSelected(false);

					// don't leave the listener hanging around...
					((JPopupMenu) e.getSource()).removePopupMenuListener(this);
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

			});
			popup.show(DropDownMenuButton.this, 0, getHeight());
		}
	}

}