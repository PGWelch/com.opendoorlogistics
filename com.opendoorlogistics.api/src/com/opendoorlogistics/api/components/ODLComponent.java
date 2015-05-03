/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.components;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;


public interface ODLComponent extends  net.xeoh.plugins.base.Plugin{
	public static final int MODE_DEFAULT=-1;
	public static final int MODE_DATA_UPDATER = 1;

	public static final int MODE_FIRST_USER_MODE = 1000;
	
	/**
	 * Get a unique ID for the component. This should be unique worldwide;
	 * it is therefore recommended to follow the Java package name convention
	 * using a company's website domain name.
	 * @return
	 */
	String getId();
	
	/**
	 * Get the display name for the component; does not have to be unique.
	 * @return
	 */
	String getName();

	/**
	 * Get the expected datastore the component will operate on. If this is null
	 * then the components works directly on the datastore provided to it without
	 * adapting it to this definition.
	 * @param configuration
	 * @return
	 */
	
	/**
	 * Get the datastore structure the component operates on. 
	 * The following convention is used:
	 * <ul><li> If this method returns null, no input datastore is needed.</li> 
	 * <li> If this method returns a non-null datastore structure but containing no tables,
	 * all tables in the component's input datastore (defined by the script configuration)
	 * are passed to it.</li> 
	 * <li> If a table is defined but has no columns, all the original columns from the input
	 * table are passed into the component.</li> 
	 * <li> If a table is defined and it has columns, the script executor ensures these columns
	 * (with their correct types) are passed to the component or reports a failure if
	 * this cannot be done.</li> 
	 * </ul>
	 * @param configuration Current configuration object for the component.
	 * @return
	 */
	ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration);
	
	/**
	 * If the component provides an outputDb definition then the output database
	 * is automatically created using this table structure. If the component
	 * doesn't provide this, it can still create the tables manually in the output db.
	 * @param configuration
	 * @return
	 */
	ODLDatastore<? extends ODLTableDefinition>  getOutputDsDefinition(ODLApi api,int mode, Serializable configuration);

	/**
	 * Execute the component using its input configuration object. 
	 * The component reads and writes to the ioDb and can optionally
	 * output to the outputDb. Progress is reported back through the 
	 * progress reporter object; the component code should also check this
	 * periodically to see if it should stop processing. 
	 * The progress reporter object is only used when the component is called
	 * from a GUI; outside of a GUI a dummy implementation is provided so
	 * the component code doesn't have to check for the report being null.
	 * @param mode Execution mode of the component. Components can support several execution modes using the same input data. 
	 * @param configuration
	 * @param ioDb
	 * @param outputDb
	 * @param reporter
	 */
	void execute(ComponentExecutionApi api,int mode, Object configuration,ODLDatastore<? extends ODLTable> ioDs, ODLDatastoreAlterable<? extends  ODLTableAlterable> outputDs);
	
	/**
	 * Get the component's configuration class which corresponds to the configuration
	 * object passed into the other methods. Class must be serializable with a no-arguments
	 * constructor.
	 * @return
	 */
	Class<? extends Serializable> getConfigClass();
	
	/**
	 * Create a JPanel with controls to edit the configuration object.
	 * Available ioDS is currently always null but it it planned
	 * to make this available in the future.
	 * @param config
	 * @param factory TODO
	 * @param isFixedIO If false, do not allow the user to edit settings
	 * which would change the input or output definitions (used in the wizard UI).
	 * @return
	 */
	JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api,int mode,Serializable config, boolean isFixedIO);
	
	public static final long FLAG_OUTPUT_WINDOWS_ALWAYS_SYNCHRONISED = 1<<0;

	public static final long FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED = 1<<1;

	/**
	 * If a component writes to the spreadsheet and the user modifies the table it writes
	 * to when the component is running, the system can consider this an error and report it.
	 * To stop this, you can stop the user being able to interact with the datastore when the
	 * component is running by NOT returning this flag in the getFlags method.
	 * Component which don't write to the datastore should always return this flag.
	 */
	public static final long FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING = 1<<2;
	
	
	/**
	 * The script execution framework does a single read of all field values in all rows of the input data tables,
	 * to ensure data dependencies are correctly recorded. A component can turn this off if needed,
	 * but should ensure that its execution is deterministic with regards to the data it reads - i.e.
	 * it should never decide to read a smaller or larger range of the data (which could result in 
	 * uncertain dependencies).
	 */
	public static final long FLAG_DISABLE_FRAMEWORK_DATA_READ_FOR_DEPENDENCIES= 1<<3;
	
//	/**
//	 * Get any default fixed IO structure configurations that the component defines
//	 * @return
//	 */
//	Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api);
	
	long getFlags(ODLApi api,int mode);
	
	/**
	 * Return a 16x16 icon used in menus etc. This can be null (but the component
	 * may not appear).
	 * @return
	 */
	Icon getIcon(ODLApi api,int mode);
	
	/**
	 * Is the execution mode supported? 
	 * Default mode must always be supported.
	 * @param mode
	 * @return
	 */
	boolean isModeSupported(ODLApi api,int mode);
	
	void registerScriptTemplates(ScriptTemplatesBuilder templatesApi);
}
