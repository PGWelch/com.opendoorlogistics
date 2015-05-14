package com.opendoorlogistics.components.barchart.basechart;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JLabel;

import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

public class BaseChartConfigPanel  extends VerticalLayoutPanel{
	protected final BaseConfig config;
	private final TextEntryPanel title;
	private final TextEntryPanel xLabel;
	private final TextEntryPanel yLabel;
	private final IntegerEntryPanel nbFilterGroupLevels;

	protected TextEntryPanel addPanel(String name, String initialValue){
		TextChangedListener listener = new TextChangedListener() {

			@Override
			public void textChange(String newText) {
				readFromPanel();
			}
		};

		TextEntryPanel ret= new TextEntryPanel(null, initialValue, listener);

		// set fixed label size so everything aligned
		addStandardAlignmentLine(name, ret);
		return ret;

	}
	
	protected void readFromPanel() {
		config.setTitle(title.getText());
		config.setXLabel(xLabel.getText());
		config.setYLabel(yLabel.getText());
		try {
			config.setNbFilterGroupLevels(Integer.parseInt(nbFilterGroupLevels.getText()));			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * @param name
	 * @param ret
	 */
	protected void addStandardAlignmentLine(String name, TextEntryPanel ret) {
		JLabel label= new JLabel(name);
		label.setPreferredSize( new Dimension(100, 28));
		
		addLine(label, Box.createRigidArea(new Dimension(4, 1)),ret);
	}
	
	public BaseChartConfigPanel(BaseConfig config){
		this.config = config;
		
		//int nbSeriesRows = config.getSeriesNames().size()<=1?0:config.getSeriesNames().size();
		//setLayout(new GridLayout(3+nbSeriesRows, 3, 4, 8));

		nbFilterGroupLevels = new IntegerEntryPanel(null,config.getNbFilterGroupLevels(), "This allows for hierarchical filtering of the barchart data", new IntChangedListener() {
			
			@Override
			public void intChange(int newInt) {
				//config.setNbFilterGroupLevels(newInt);
				readFromPanel();
			}
		});
		addStandardAlignmentLine("Filter group levels ", nbFilterGroupLevels);
		
		title = addPanel("Title", config.getTitle());
		xLabel = addPanel("X label", config.getXLabel());		
		yLabel = addPanel("Y label", config.getYLabel());
				
	}
}

