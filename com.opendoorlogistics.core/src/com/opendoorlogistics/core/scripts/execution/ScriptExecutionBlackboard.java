package com.opendoorlogistics.core.scripts.execution;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.execution.adapters.DatastoreFetcher;

public interface ScriptExecutionBlackboard extends ExecutionReport, DatastoreFetcher{
	boolean isCompileOnly();
	AdapterConfig getAdapterConfig(String id);
}
