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
 * Based on the example at http://www.jroller.com/santhosh/entry/dropdownbutton_for_swing by santhosh kumar - santhosh@in.fiorano.com
 */
public abstract class DropDownMenuButton extends JButton {

	public DropDownMenuButton(Icon icon) {
		super(icon);
		setMargin(new Insets(3, 0, 3, 0));
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				JPopupMenu popup = getPopupMenu();
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
		});
	}

	protected abstract JPopupMenu getPopupMenu();

}