/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.panels;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.opendoorlogistics.codefromweb.PackTableColumn;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionArgument;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;

final public class FunctionsListPanel extends JPanel {
	public FunctionsListPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		FunctionDefinitionLibrary library =FunctionsBuilder.getAllDefinitions();

		final List<FunctionDefinition> fnclist = library.toList();

		final JTable table = new JTable(new AbstractTableModel() {

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				FunctionDefinition dfn = fnclist.get(rowIndex);
				switch (columnIndex) {
				case 0:
					return Strings.convertEnumToDisplayFriendly(dfn.getType().name());

				case 1:
					return dfn.getSignature(dfn.getType()!=FunctionType.OPERATOR);

				case 2:
					return dfn.getDescription();
				}
				return null;
			}

			@Override
			public int getRowCount() {
				return fnclist.size();
			}

			@Override
			public int getColumnCount() {
				return 3;
			}

			@Override
			public String getColumnName(int column) {
				switch (column) {
				case 0:
					return "Type";

				case 1:
					return "Function";

				case 2:
					return "Description";
				}
				return null;
			}

		});

		class MyEditorPane extends JEditorPane{
			MyEditorPane(){
				super("text/html","");
			}
			
			void update(){
				StringBuilder builder = new StringBuilder();
				builder.append("<html><head></head><body>");
				FunctionDefinition dfn = table.getSelectedRow()!=-1 ? fnclist.get(table.getSelectedRow()):null;
				if(dfn!=null){
					builder.append("<p>"+Strings.convertEnumToDisplayFriendly(dfn.getType().name())+ " <b>" + dfn.getName() + "</b>")	;	
					if(Strings.isEmpty(dfn.getDescription())==false){
						builder.append("<br>" + dfn.getDescription())	;						
					}
					builder.append("</p>");
					
					if(dfn.nbArgs()>0){
						builder.append("<p>");
						for(int i = 0 ; i< dfn.nbArgs() ; i++){
							if(i>0){
								builder.append("<br>");
							}
							FunctionArgument arg = dfn.getArg(i);
							builder.append("Argument " + Integer.toString(i+1) + " - <b>" + arg.getName() +"</b>");
							if(Strings.isEmpty(arg.getDescription())==false){
								builder.append(" - " + arg.getDescription());
							}
						}						
						builder.append("</p>");
					}
				}
				//    html+="<body bgcolor='#777779'><hr/><font size=50>This is Html content</font><hr/>";
				builder.append("</body></html>");
				setText(builder.toString());
			}
		}
		final MyEditorPane editorPane = new MyEditorPane();
		editorPane.update();
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				editorPane.update();
			}
		});
		
		
		PackTableColumn.packAll(table, 4);
		table.getColumnModel().getColumn(0).setMinWidth(60);
		add(new JLabel("The following functions are available in the calculated fields of a table adapter:"), BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(editorPane, BorderLayout.SOUTH);
	}

	public static void main(String[] args) {
		InitialiseStudio.initialise(false);
		ShowPanel.showPanel(new FunctionsListPanel());
	}
	
	public static ODLInternalFrame createFrame(){
		ODLInternalFrame frame = new ODLInternalFrame("Functions list");
		frame.setTitle("Available table adapter functions");
		frame.add(new FunctionsListPanel());
		return frame;
	}
}
