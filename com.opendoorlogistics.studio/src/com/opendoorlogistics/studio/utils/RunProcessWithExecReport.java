/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.utils;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;

public class RunProcessWithExecReport {
	public static interface RunMe<T>{
		T runMe( ExecutionReport report);
	}
	
	public static class RunResult<T>{
		public final ExecutionReport report;
		public final T result;
		public RunResult(ExecutionReport report, T result) {
			this.report = report;
			this.result = result;
		}
		public ExecutionReport getReport() {
			return report;
		}
		public T getResult() {
			return result;
		}
		
		
	}
	
	public static <T> RunResult<T> runProcess(JFrame parent,RunMe<T> runMe){
		if(!SwingUtilities.isEventDispatchThread()){
			throw new RuntimeException();
		}
		ExecutionReportImpl report = new ExecutionReportImpl();
		T result=null;
		try {
			result = runMe.runMe( report);
		} catch (Exception e) {
			report.setFailed(e);
		}
		
		if(report.isFailed() || report.size()>0){
			ExecutionReportDialog dlg = new ExecutionReportDialog(parent, "A problem occurred", report, false);
			dlg.setVisible(true);;
		}
		
		RunResult<T> ret = new RunResult<T>(report,result);
		return ret;
	}
}
