/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution.dependencyinjection;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ProcessingApi;

public class ProcessingApiDecorator implements ProcessingApi {
	protected final ProcessingApi decorated;

	public ProcessingApiDecorator(ProcessingApi decorated) {
		super();
		this.decorated = decorated;
	}

	public boolean isCancelled() {
		return decorated.isCancelled();
	}

	public boolean isFinishNow() {
		return decorated.isFinishNow();
	}

	public void logWarning(String warning) {
		decorated.logWarning(warning);
	}

	public void postStatusMessage(String s) {
		decorated.postStatusMessage(s);
	}

	@Override
	public ODLApi getApi() {
		return decorated.getApi();
	}
	
}
