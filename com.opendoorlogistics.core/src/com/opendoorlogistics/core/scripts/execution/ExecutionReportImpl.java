/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution;

import java.util.ArrayList;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ExecutionReportImpl implements ExecutionReport{
	private boolean failed = false;
	private ArrayList<LogEntry> logs = new ArrayList<>();
	
	private class LogEntry{
		final Throwable throwable;
		final String s;
		
		LogEntry(Throwable throwable, String s) {
			this.throwable = throwable;
			this.s = s;
		}
		
	}
	
	
	@Override
	public void log(String s) {
		logs.add(new LogEntry(null, s));
	}

	@Override
	public void setFailed(String reason) {
		failed = true;
		if (reason != null) {
			log(reason);
		}
	}

	@Override
	public boolean isFailed() {
		return failed;
	}

	@Override
	public String toString() {
		return getReportString(true,true);
	}

	@Override
	public void setFailed(Throwable reason) {
		logs.add(new LogEntry(reason, null));
		setFailed((String)null);
	}

	@Override
	public void add(ExecutionReport report) {
		if(ExecutionReportImpl.class.isInstance(report)==false){
			throw new RuntimeException();
		}
		
		if(report.isFailed()){
			failed = true;
		}
		
		for(LogEntry s : ((ExecutionReportImpl)report).logs){
			logs.add(s);
		}
	}

	@Override
	public String getReportString(boolean includeExceptionTraces,boolean showSuccessFailureMessage) {
		
		// build list of lines first
		ArrayList<String> lines = new ArrayList<>();
		for (LogEntry line : logs) {
			if(line.s!=null){
				lines.add(line.s);		
			}
			
			if(line.throwable!=null){
				if(includeExceptionTraces){
					lines.add(Strings.getStackTrace(line.throwable));
				}else{
					for(String s:Strings.getExceptionMessages(line.throwable)){
						lines.add(s);
					}
				}
			}
		}
		
		// put into single string
		StringBuilder builder = new StringBuilder();
		String lastLine = null;
		for(String line:lines){
			// don't repeat lines
			if(!Strings.equalsStd(lastLine, line)){
				builder.append(line);				
				builder.append(System.lineSeparator());
			}
			lastLine = line;
		}
		
		if(showSuccessFailureMessage){
			if (failed) {
				builder.append("Execution of the operation failed." + System.lineSeparator());
			} else {
				builder.append("Execution of the operation succeeded." + System.lineSeparator());
			}			
		}

		return builder.toString();
	}

	@Override
	public void setFailed() {
		failed = true;
	}

	@Override
	public ExecutionReport deepCopy() {
		ExecutionReportImpl ret = new ExecutionReportImpl();
		ret.failed = failed;
		
		// logs are immutable
		ret.logs.addAll(logs);
		return ret;
	}

	@Override
	public int size() {
		return logs.size();
	}

}
