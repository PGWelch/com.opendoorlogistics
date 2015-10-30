/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;


public interface ExecutionReport {

	void setFailed();

	void setFailed(String reason);
	
	void setFailed(Throwable reason);
	
	boolean isFailed();

	void add(ExecutionReport report);

	void add(ExecutionReport report, boolean copyFailedStatus);

	String getReportString(boolean includeExceptionTraces,boolean showSuccessFailureMessage);
	
	ExecutionReport deepCopy();
	
	void log(String s);
	
	int size();
}
