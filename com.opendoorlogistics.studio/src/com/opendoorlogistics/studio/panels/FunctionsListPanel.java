/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.opendoorlogistics.codefromweb.PackTableColumn;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionArgument;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;

final public class FunctionsListPanel extends JPanel {
	private final List<FunctionDefinition> allFunctions;
	private final List<FunctionDefinition> topLevel ;
	private final JTable table;
	private List<FunctionDefinition> current ;
	private final JButton upButton;
	
	private class DescriptionPane extends JEditorPane{
		
		DescriptionPane(){
			super("text/html","");
		}

		
		void update(){
			StringBuilder builder = new StringBuilder();
			builder.append("<html><head></head><body>");
			FunctionDefinition dfn = table.getSelectedRow()!=-1 ? current.get(table.getSelectedRow()):null;
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

	private List<FunctionDefinition> filterForGroup( String group){
		ArrayList<FunctionDefinition> ret = new ArrayList<FunctionDefinition>();
		for(FunctionDefinition dfn : allFunctions){
			if(Strings.equalsStd(group, dfn.getGroup())){
				ret.add(dfn);
			}
		}
		return ret;
	}
	
	public FunctionsListPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		FunctionDefinitionLibrary library =FunctionsBuilder.getAllDefinitions();
		allFunctions = library.toList();

		// Get the top level
		topLevel = new ArrayList<FunctionDefinition>();
		StandardisedStringSet groups = new StandardisedStringSet(true);
		for(FunctionDefinition dfn : allFunctions){
			if(dfn.getGroup()!=null){
				// first time?
				String groupname = dfn.getGroup();
				if(!groups.contains(groupname)){
					groups.add(groupname);
					FunctionDefinition groupDfn = new FunctionDefinition(groupname);
					groupDfn.setDescription("Function group");
					groupDfn.setGroup(groupname);
					topLevel.add(groupDfn);
				}
			}else{
				// top level
				topLevel.add(dfn);
			}
		}

		current = topLevel;

		table = new JTable(createTableModel(current));
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			@Override
		    public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
		    	Component ret = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				FunctionDefinition dfn = row < current.size() ? current.get(row):null;    	
				if(dfn == null || dfn.getGroup()==null || current!=topLevel){
			    	ret.setForeground(Color.BLACK );					
				}else{
			    	ret.setForeground(new Color(0, 0, 100));										
				}
				
				if(dfn!=null && dfn.getType() == FunctionType.CONSTANT){
					FmConst fmconst =(FmConst) dfn.getFactory().createFunction();
					if(fmconst.value()!=null && fmconst.value() instanceof Color){
						ret.setBackground((Color)fmconst.value());
					}
				}else{
					ret.setBackground(row%2==0 ? Color.white : new Color(240,240,230));
				}
		    	return ret;
		    }
		});
		
		DescriptionPane descriptionPane = new DescriptionPane();
		descriptionPane.update();
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// should we go down?
				FunctionDefinition dfn = table.getSelectedRow()!=-1 ? current.get(table.getSelectedRow()):null;
				if(dfn!=null && current == topLevel && dfn.getGroup()!=null){
					current = filterForGroup(dfn.getGroup());
					table.setModel(createTableModel(current));
					packTable(table);
					upButton.setEnabled(true);
				}

				descriptionPane.update();
			}
		});
		
		// create up button
		upButton = new JButton("Browse to top level");
		upButton.setEnabled(false);
		upButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(current!=topLevel){
					current = topLevel;
					table.setModel(createTableModel(current));
					packTable(table);
					descriptionPane.update();
					upButton.setEnabled(false);
				}
			}
		});
		
		packTable(table);
		
		// have panel just for the description  so we can add the button to the south of it
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(descriptionPane, BorderLayout.CENTER);
		southPanel.add(upButton, BorderLayout.SOUTH);

		add(new JLabel("The following functions are available in the calculated fields of a table adapter:"), BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(southPanel, BorderLayout.SOUTH);
	}

	private void packTable(JTable table) {
		PackTableColumn.packAll(table, 4);
		table.getColumnModel().getColumn(0).setMinWidth(60);
	}



	@SuppressWarnings("serial")
	private AbstractTableModel createTableModel(final List<FunctionDefinition> fnclist) {
		return new AbstractTableModel() {

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				FunctionDefinition dfn = fnclist.get(rowIndex);
				switch (columnIndex) {
				case 0:
					return Strings.convertEnumToDisplayFriendly(dfn.getType().name());

				case 1:
					if(fnclist !=topLevel || dfn.getGroup()==null){
						return dfn.getSignature(dfn.getType()!=FunctionType.OPERATOR);						
					}
					return dfn.getGroup();

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

		};
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
