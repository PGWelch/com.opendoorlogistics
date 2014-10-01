/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

import com.opendoorlogistics.api.ui.Disposable;

public class ProgressPanel extends JPanel  {
	private JProgressBar progressBar;
	private JTextArea taskOutput;
	private JButton finishNowButton;
	private JButton cancelButton;
	private volatile boolean cancelled;
	private volatile boolean finishNow;
	
	public ProgressPanel(boolean showButtons){
		setLayout(new BorderLayout());	
		

		finishNowButton = new JButton("Stop early");
		finishNowButton.setToolTipText("For an optimisation process, stop as soon as a solution is available, regardless of solution quality.");
		finishNowButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				finishNow();
			}
		});

		cancelButton = new JButton("Cancel");
		cancelButton.setToolTipText("Cancel the operation. No solution will be returned.");
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});

		progressBar = new JProgressBar();
		progressBar.setValue(0);
	//	JPanel progressPanel = new JPanel();
	//	progressPanel.add(progressBar);

		taskOutput = new JTextArea(3, 20);
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);
		taskOutput.setFocusable(false);
		//taskOutput.setMinimumSize(new Dimension(500,200));
		taskOutput.setOpaque(true);
		
		add(progressBar, BorderLayout.NORTH);
		add(taskOutput, BorderLayout.CENTER);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		if(showButtons){
			JPanel buttonsPanel = new JPanel();
			buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			buttonsPanel.add(finishNowButton);
			buttonsPanel.add(cancelButton);
			add(buttonsPanel, BorderLayout.SOUTH);			
		}

	}
	
	public void finishNow() {
		finishNowButton.setEnabled(false);
		finishNow = true;
	}

	public void cancel() {
		finishNowButton.setEnabled(false);
		cancelButton.setEnabled(false);
		cancelled = true;
		
	}
	
	public boolean isFinishedNow(){
		return finishNow;
	}
	
	public boolean isCancelled(){
		return cancelled;
	}
	
	public void setText(String text){
		this.taskOutput.setText(text);
		taskOutput.setLineWrap(true);
	}
	
	public void start(){
		progressBar.setIndeterminate(true);
	//	setVisible(true);		
	}

	/**
	 * Interface use to access progress dialogs and progress internal frames
	 * @author Phil
	 *
	 */
	public interface ProgressReporter extends Disposable{
		ProgressPanel getProgressPanel();
		boolean isDisposed();
		void setVisible(boolean visible);
	}
	
	public static final int STANDARD_DIALOG_WIDTH = 500;
	public static final int STANDARD_DIALOG_HEIGHT = 200;
}
