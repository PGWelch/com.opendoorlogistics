package com.opendoorlogistics.core.scripts.parameters.controls;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.opendoorlogistics.api.Factory;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.Parameters.ParamDefinitionField;
import com.opendoorlogistics.api.scripts.parameters.Parameters.PromptType;
import com.opendoorlogistics.api.scripts.parameters.ParametersControlFactory;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public class ControlFactory implements ParametersControlFactory {

	@Override
	public JPanel createHorizontalPanel(ODLApi api,ODLTable paramTable, ODLTableReadOnly valuesTable) {
	
		JPanel ret = new JPanel();
		ret.setLayout(new FlowLayout(FlowLayout.LEFT));

		addToPanel(api, paramTable, valuesTable, ret, new PromptType[]{PromptType.ATTACH, PromptType.ATTACH_POPUP},true);
		return ret;
	}

	private List<SmartComboBox> addToPanel(ODLApi api, ODLTable paramTable, ODLTableReadOnly valuesTable, JPanel panel, PromptType [] promptTypes, boolean isAttachedPanel) {
		Parameters parameters = api.scripts().parameters();
		StringConventions strings = api.stringConventions();

		ArrayList<SmartComboBox> list = new ArrayList<>();
		
		// get values by key
		Map<String,List<String>> valuesMap = api.stringConventions().createStandardisedMap(new Factory<List<String>>() {

			@Override
			public List<String> create() {
				return new ArrayList<>();
			}
		});
		if(valuesTable!=null){
			int n = valuesTable.getRowCount();
			for(int i =0 ; i < n ; i++){
				String key =(String) valuesTable.getValueAt(i, 0);
				String value =(String)valuesTable.getValueAt(i, 1);
				if(key!=null){
					valuesMap.get(key).add(value);
				}
			}
		}
		
		int n = paramTable.getRowCount();
		for(int i =0 ; i < n ; i++){
			String key = parameters.getByRow(paramTable, i, ParamDefinitionField.KEY);
			if(!strings.isEmptyString(key)){
				PromptType pt = parameters.getPromptType(paramTable, key);
				boolean accept=false;
				for(PromptType allowedType : promptTypes){
					if(pt==allowedType){
						accept = true;
					}
				}
				if(accept){
					List<String> possibles = valuesMap.get(key);					
					SmartComboBox box = new SmartComboBox(key, possibles,null,parameters.getColumnType(paramTable, key),isAttachedPanel);
					box.setValue(parameters.getByKey(paramTable,key, ParamDefinitionField.VALUE));
					if(!isAttachedPanel){
						panel.add(Box.createVerticalStrut(10));
					}
					panel.add(box);

					list.add(box);
					box.addValueChangedListener(new SmartComboValueChangedListener() {
						
						@Override
						public void onValueChanged(SmartComboBox scb, String newValue) {
							api.scripts().parameters().setByKey(paramTable, key, ParamDefinitionField.VALUE, newValue);
						}
					});
					
					// if (a) the current value is empty, (b) we have input values and (c) none are empty, then take the first possible value
					if(strings.isEmptyString(box.getValue()) && possibles!=null && possibles.size()>0){
						String first="";
						boolean hasEmpty=false;
						for(String possible:possibles){
							if(strings.isEmptyString(first)){
								first = possible;
							}
							
							if(strings.isEmptyString(possible)){
								hasEmpty = true;
							}
						}
						
						if(!hasEmpty){
							box.setValue(first);
						}
					}

				}
			}
		}
		
		return list;
	}


	@Override
	public JPanel createModalPanel(ODLApi api, ODLTable paramTable, ODLTableReadOnly valuesTable) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX( JPanel.LEFT_ALIGNMENT );
		panel.setMinimumSize(new Dimension(300,300));
		for(SmartComboBox scb:addToPanel(api, paramTable, valuesTable, panel,new PromptType[]{PromptType.POPUP, PromptType.ATTACH_POPUP},false)){
			scb.setAlignmentX(SmartComboBox.LEFT_ALIGNMENT);
		}
		return panel;
	}

	@Override
	public boolean hasModalParameters(ODLApi api, ODLTable paramTable, ODLTableReadOnly valuesTable) {
		Parameters parameters = api.scripts().parameters();
	
		int n = paramTable.getRowCount();
		for(int i =0 ; i < n ; i++){
			String key = parameters.getByRow(paramTable, i, ParamDefinitionField.KEY);
			if(!api.stringConventions().isEmptyString(key)){
				PromptType pt = parameters.getPromptType(paramTable, key);	
				if(pt == PromptType.ATTACH_POPUP || pt == PromptType.POPUP){
					return true;
				}
			}
		}
		return false;
	}

}
