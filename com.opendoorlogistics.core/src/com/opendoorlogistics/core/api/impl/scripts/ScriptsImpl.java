package com.opendoorlogistics.core.api.impl.scripts;

import java.io.File;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStatusObservable;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.Scripts;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.execution.OptionsSubpath;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutor;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.AbstractDependencyInjector;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;

public class ScriptsImpl implements Scripts {
	private final ODLApi api;
	private volatile Parameters parameters;
	
	public ScriptsImpl(ODLApi api) {
		this.api = api;
	}
	

	@Override
	public ScriptOption loadScript(File file) {
		Script script = ScriptIO.instance().fromFile(file);
		if(script!=null){
			return new ScriptOptionImpl(api, null, script, null);
		}

		return null;
	}


	@Override
	public String findOptionIdByName(ScriptOption option, String optionName) {
		ScriptOptionImpl impl = (ScriptOptionImpl) option;
		return ScriptUtils.getOptionIdByName(impl.getOption(), optionName);
	}


	@Override
	public ExecutionReport executeScript(ScriptOption option, String optionId, ODLDatastoreAlterable<? extends ODLTableAlterable> ds) {
		ExecutionReport report = new ExecutionReportImpl();
		ScriptOptionImpl impl = (ScriptOptionImpl) option;
		if((impl.getOption() instanceof Script)==false){
			report.setFailed("Invalid input script object");
			return report;
		}
		Script script = (Script)impl.getOption();
		
		Script subsetScript = optionId != null ? OptionsSubpath.getSubpathScript(script, new String[] { optionId }, report) : script;
		if(report.isFailed()){
			return report;
		}
		
		ExecutionReport executionReport = new ExecutionReportImpl();
		
		ScriptExecutor executor = new ScriptExecutor(api, false, new AbstractDependencyInjector(api) {

			@Override
			public void logWarning(String warning) {
			//	executionReport.log(warning);
			}

			@Override
			public <T extends JPanel & ClosedStatusObservable> void showModalPanel(T panel, String title) {
				// TODO Auto-generated method stub

			}

			@Override
			public void submitControlLauncher(String instructionId, ODLComponent component, ODLDatastore<? extends ODLTable>  parametersTableCopy,String reportTopLabel,ControlLauncherCallback cb) {
				// launch ODL Studio?
			}

		});

		executionReport = executor.execute(subsetScript, ds);

				
		return executionReport;
	}


	@Override
	public Parameters parameters() {
		if(parameters==null){
			parameters = new ParametersImpl(api);
		}
		return parameters;
	}
}
