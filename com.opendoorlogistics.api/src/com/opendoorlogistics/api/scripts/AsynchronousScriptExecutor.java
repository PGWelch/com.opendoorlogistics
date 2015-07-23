package com.opendoorlogistics.api.scripts;

import java.io.File;

public interface AsynchronousScriptExecutor {
	/**
	 * Post a script execution
	 * @param file
	 * @param optionIds
	 * @return
	 */
	PendingScriptExecution postScriptExecution(File file , String [] optionIds);
	
	
	public interface PendingScriptExecution{
		boolean isDone();
	}
}
