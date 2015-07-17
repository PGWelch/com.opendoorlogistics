/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.controls;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ComboBoxEditor;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.opendoorlogistics.codefromweb.BottomLineBorder;
import com.opendoorlogistics.codefromweb.BoundsPopupMenuListener;
import com.opendoorlogistics.core.utils.strings.Strings;

public abstract class DynamicComboBox<T> extends EditableComboBox<T> {
	private static Font firstLineFont = new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC,12);
	
	public DynamicComboBox(T initialValue, boolean addPopupListener, boolean editable) {
		setEditable(editable);

		// fill in a non-empty initial value if needed
		if (Strings.isEmpty(initialValue)) {
			List<T> available = getAvailableItems();
			if (available != null && available.size() > 0 && available.get(0)!=null && Strings.isEmpty(available.get(0).toString()) == false) {
				initialValue = available.get(0);
			}
		}

		if(isEditable()){
			getEditor().setItem(initialValue);			
		}

		// Initialise listeners ensuring called in correct order, so bounds popup only
		// called after updating the items...
		BoundsPopupMenuListener bpml = new BoundsPopupMenuListener(true,false);
		if (addPopupListener) {
			addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					updateMenu();
					bpml.popupMenuWillBecomeVisible(e);
				//	ComboBoxWithWiderPopup.adjustPopupWidth(DynamicComboBox.this, null);
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					bpml.popupMenuWillBecomeInvisible(e);
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					bpml.popupMenuCanceled(e);
				}
			});
		}else{			
			addPopupMenuListener(bpml);
		}



		final JLabel firstLine = new JLabel();
		firstLine.setBorder(new BottomLineBorder());
		firstLine.setFont(firstLineFont);
		//firstLine.setForeground(new Color(0, 0, 100));
	//	Font.SANS_SERIF
	//	Font f;
	//	firstLine.getFont().
		final JLabel laterLines = new JLabel();
		setRenderer(new ListCellRenderer<T>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = index == 0 ? firstLine : laterLines;
				setLabelText(value, label);
				label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
				label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
				label.setOpaque(true);
				return label;
			}

		});
	}

	
	protected abstract List<T> getAvailableItems();

	public void updateMenu() {
		ComboBoxEditor editor = getEditor();
		Object value =editor.getItem();
		removeAllItems();
		
		if(isEditable()){
			if (value != null) {
				addItem((T)value);
				setSelectedIndex(0);
			}		
		}

		List<T> items = getAvailableItems();
		if (items != null) {
			for (T field : items) {
//				if (value == null || value.toString().equals(field) == false) {
					addItem(field);
	//			}
			}
		}
	}

	/**
	 * Null-pointer safe version of asList
	 * @param vals
	 * @return
	 */
	protected List<T> asList(T [] vals){
		if(vals==null){
			return new ArrayList<>();
		}
		return Arrays.asList(vals);
	}
	
	public T getValue(){
		return (T)getEditor().getItem();		
	}


	/**
	 * Override this to control the text shown for a value
	 * @param value
	 * @param label
	 */
	protected void setLabelText (T value, JLabel label) {
		label.setText(value!=null? value.toString():"");
	}
}
