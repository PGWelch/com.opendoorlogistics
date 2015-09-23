package com.opendoorlogistics.studio.scripts.editor.adapters;

import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.opendoorlogistics.core.scripts.elements.UserFormula;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.utils.ui.ListPanel;

public class UserFormulaEditor {
	public static JPanel createUserFormulaListPanel(final List<UserFormula> formula) {
		return new ListPanel<UserFormula>(formula, "user formula") {

			@Override
			protected UserFormula createNewItem() {
				return editItem(new UserFormula("funcname() = X"));
			}

			@Override
			protected UserFormula editItem(final UserFormula item) {
				final JTextArea textArea = new JTextArea(item.getValue());
				textArea.setEditable(true);
				textArea.setLineWrap(true);
				OkCancelDialog dlg = new OkCancelDialog() {
					@Override
					protected Component createMainComponent(boolean inWindowsBuilder) {
						return new JScrollPane(textArea);
					}
				};
				dlg.setMinimumSize(new Dimension(800, 400));
				dlg.setLocationRelativeTo(this);
				dlg.setTitle("Enter formula text");
				if (dlg.showModal() == OkCancelDialog.OK_OPTION) {
					item.setValue(textArea.getText());
					return item;
				}
				return null;
			}
		};
	}
}
