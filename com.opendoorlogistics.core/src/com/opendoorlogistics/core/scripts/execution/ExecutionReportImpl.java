/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
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
	public void add(ExecutionReport report, boolean copyFailedStatus) {
		if(ExecutionReportImpl.class.isInstance(report)==false){
			throw new RuntimeException();
		}
		
		if(copyFailedStatus && report.isFailed()){
			failed = true;
		}
		
		for(LogEntry s : ((ExecutionReportImpl)report).logs){
			logs.add(s);
		}
	}

	@Override
	public void add(ExecutionReport report) {
		add(report,true);
	}

	@Override
	public String getReportString(boolean includeExceptionTraces,boolean showSuccessFailureMessage) {
		
		List<String> lines = getLines(includeExceptionTraces);
		
		StandardisedStringSet printedLines = new StandardisedStringSet(false);
		
		// put into single string
		StringBuilder builder = new StringBuilder();
		for(String line:lines){
			// don't repeat lines
			if(!printedLines.contains(line)){
				builder.append(line);				
				builder.append(System.lineSeparator());
				printedLines.add(line);
			}
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

	public List<String> getLines(boolean includeExceptionTraces) {
		// filter list of logs
		ArrayList<LogEntry> filtered = new ArrayList<>(logs);
		HashSet<Throwable> throwables = new HashSet<>();
		Iterator<LogEntry> it = filtered.iterator();
		while(it.hasNext()){
			LogEntry entry = it.next();
			
			// add the throwable to the set
			if(entry.throwable!=null){
				throwables.add(entry.throwable);
			}
			
			// filter runtimeexcepptions whose cause has already been logged; these are likely just a rethrow...
			if(entry.throwable!=null && Strings.isEmpty(entry.s)){
				Throwable throwable = entry.throwable;
				
				if(!RuntimeException.class.isInstance(throwable)){
					continue;
				}
				
				if(!Strings.isEmpty(throwable.getMessage())){
					continue;
				}
				
				if(throwable.getCause()==null){
					continue;
				}
				
				if(!throwables.contains(throwable.getCause())){
					continue;		
				}
				
				it.remove();
				
			}
		}
		
		// build list of lines first
		ArrayList<String> lines = new ArrayList<>();
		for (LogEntry line : filtered) {
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
		return lines;
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
