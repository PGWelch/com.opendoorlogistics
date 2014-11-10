/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.dialogs;

import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Callable;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

import com.opendoorlogistics.studio.panels.ProgressPanel;
import com.opendoorlogistics.studio.panels.ProgressPanel.ProgressReporter;

final public class ProgressDialog<TResult> extends JDialog implements PropertyChangeListener, ProgressReporter {
	public static final String USER_MESSAGE_PROP = "UserMessage";
	private final ProgressPanel panel;
	private boolean isDisposed = false;

	// private JProgressBar progressBar;
	// private JTextArea taskOutput;
	// private JButton finishNowButton;
	// private JButton cancelButton;
	// private volatile boolean cancelled;
	// private volatile boolean finishNow;

	public interface OnFinishedSwingThreadCB<TResult> {
		void onFinished(TResult result, boolean userCancelled, boolean userFinishedNow);
	}

	private class MyTask extends SwingWorker<TResult, Void> {
		private Callable<TResult> runnable;
		private OnFinishedSwingThreadCB<TResult> cb;

		private TResult result;

		public MyTask(Callable<TResult> runnable, OnFinishedSwingThreadCB<TResult> cb) {
			super();
			this.runnable = runnable;
			this.cb = cb;
		}

		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public TResult doInBackground() {
			try {
				result = runnable.call();
				return result;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

		/*
		 * Executed in event dispatch thread
		 */

		@Override
		public void done() {
			runnable = null;
			dispose();
			cb.onFinished(result, panel.isCancelled(), panel.isFinishedNow());
		}
	}

	public void setText(String text) {
		panel.setText(text);
	}

	public ProgressDialog(JFrame frame, String title, boolean showFinishNow, boolean showCancel) {
		super(frame, true);

		setTitle(title);

		// JPanel mainPanel = new JPanel();
		// mainPanel.setLayout(new BorderLayout());
		panel = new ProgressPanel(showFinishNow,showCancel);
		setContentPane(panel);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosing(WindowEvent e) {
				panel.cancel();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				// set cancel button disabled
			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}
		});

		setMinimumSize(new Dimension(ProgressPanel.STANDARD_DIALOG_WIDTH, ProgressPanel.STANDARD_DIALOG_HEIGHT));
		pack();

	}

	public void start(Callable<TResult> runnable, OnFinishedSwingThreadCB<TResult> onFinished) {
		MyTask task = new MyTask(runnable, onFinished);
		start(task);
	}

	public void start(SwingWorker<?, ?> task) {
		task.addPropertyChangeListener(this);

		// the task itself will shutdown the progress bar afterwards...
		task.execute();

		start();
	}

	public void start() {
		panel.start();
		setVisible(true);
	}

	// public DependencyInjector getGuiFascade(){
	// return guiFascade;
	// }
	//
	/**
	 * Invoked when task's progress property changes.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(USER_MESSAGE_PROP)) {
			panel.setText((String) evt.getNewValue());
			// taskOutput.setText((String)evt.getNewValue());
		}
	}

	public boolean isFinishedNow() {
		return panel.isFinishedNow();
	}

	public boolean isCancelled() {
		return panel.isCancelled();
	}

	@Override
	public ProgressPanel getProgressPanel() {
		return panel;
	}

	@Override
	public void dispose() {
		if (!isDisposed) {
			isDisposed = true;
			super.dispose();
		}
	}

	@Override
	public boolean isDisposed() {
		return isDisposed;
	}

}
