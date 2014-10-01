/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

import com.opendoorlogistics.studio.components.geocoder.model.GeocodeAverageType;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModelListener;
import com.opendoorlogistics.utils.ui.SimpleAction;

abstract class GeocodeToolbar extends JToolBar implements GeocodeModelListener{
	static final int BUTTON_HEIGHT = 44;
	static final int BUTTON_WIDTH = 118;
	
	private final Action gotoNextAction;
	private final Action gotoPreviousAction;
	private final ArrayList<JButton> optionButtons  = new ArrayList<>();
	private final GeocodeModel model;

	
	GeocodeToolbar(final GeocodeModel model){
		setFloatable(false);
		this.model = model;

		// create previous action
		gotoPreviousAction =new SimpleAction("Previous record", null, "arrow-left-3-16x16.png", null) {

			@Override
			public void actionPerformed(ActionEvent e) {
				model.gotoPreviousRecord();
			}
		}; 
		JButton previousButton=add(gotoPreviousAction);
		previousButton.setHideActionText(false);
		setButtonBorder(previousButton);
		setButtonSize(previousButton);
	//	previousButton.setHorizontalTextPosition(SwingConstants.LEFT);
		
		// create buttons for each search result option
		for(final GeocodeAverageType option : GeocodeAverageType.values()){
			JButton button = new JButton(option.getText(null,true));
			button.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					model.setGeocode(option.getAverage(model));
					if(model.hasNextRecord()){
						model.gotoNextRecord();
					}
				}
			});
			setButtonBorder(button);
			optionButtons.add(button);
			setButtonSize(button);			
			add(button);
		}
		
		// create next action
		gotoNextAction = new SimpleAction("Next record", null, "arrow-right-3-16x16.png", null) {

			@Override
			public void actionPerformed(ActionEvent e) {
				model.gotoNextRecord();
			}
		};
		JButton nextButton =add(gotoNextAction);
		nextButton.setHideActionText(false);
		setButtonBorder(nextButton);
		setButtonSize(nextButton);			
	
		JButton exitButton = add(new AbstractAction("Exit") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				onExit();
			}
		});
		setButtonBorder(exitButton);
		setButtonSize(exitButton);
		
		model.addListener(this);
		modelChanged(true, true);
	}

	private void setButtonBorder(JButton button) {
		button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	}
	
	private void setButtonSize(JButton button){
		Dimension size=new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
		button.setSize(size);
		button.setMaximumSize(size);
		button.setMinimumSize(size);
		button.setPreferredSize(size);
	}
	


	@Override
	public void modelChanged(boolean recordChanged, boolean searchResultsChanged) {
		gotoPreviousAction.setEnabled(model.hasPreviousRecord());
		gotoNextAction.setEnabled(model.hasNextRecord());
		
		if(optionButtons.size() != GeocodeAverageType.values().length){
			throw new RuntimeException();
		}
		
		for(int i =0 ;i<optionButtons.size(); i++){
			GeocodeAverageType type = GeocodeAverageType.values()[i];
			optionButtons.get(i).setText(type.getText(model,true));
			optionButtons.get(i).setEnabled(type.isAvailable(model));
		}
	}
	
	protected abstract void onExit();
}
