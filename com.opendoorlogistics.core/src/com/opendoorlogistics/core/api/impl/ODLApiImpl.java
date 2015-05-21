/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import java.io.File;

import com.opendoorlogistics.api.Functions;
import com.opendoorlogistics.api.IO;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StandardComponents;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.Values;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.api.geometry.Geometry;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.tables.utils.TableUtils;

public class ODLApiImpl implements ODLApi{
	private StringConventions conventions;
	private StandardComponents standardComponents;
	private Values conversionApi;
	private Tables tables;
	private Geometry geometry;
	private UIFactory uiFactory;
	private Functions functions;
	private IO io;

	@Override
	public Values values() {
		if(conversionApi==null){
			conversionApi = new ValuesImpl();
		}
		return conversionApi;
	}

	@Override
	public Tables tables() {
		if(tables==null){
			tables = new TablesImpl();			
		}
		return tables;
	}

	@Override
	public StringConventions stringConventions() {
		if(conventions==null){
			conventions = new StringConventionsImpl();
		}
		return conventions;
	}

	@Override
	public Geometry geometry() {
		if(geometry==null){
			geometry = new GeometryImpl();
		}
		return geometry;
	}

	@Override
	public UIFactory uiFactory() {
		if(uiFactory==null){
			uiFactory = new UIFactoryImpl();
		}
		return uiFactory;
	}

	@Override
	public StandardComponents standardComponents() {
		if(standardComponents==null){
			standardComponents = new StandardComponentsImpl();
		}
		return standardComponents;
	}

	@Override
	public ODLComponentProvider registeredComponents() {
		return ODLGlobalComponents.getProvider();
	}

	@Override
	public Functions functions() {
		if(functions==null){
			functions = new Functions() {
				
				@Override
				public boolean isFunctionError(Object functionReturnValue) {
					return functionReturnValue == com.opendoorlogistics.core.formulae.Functions.EXECUTION_ERROR;
				}
			};
		}
		
		return functions;
	}

	@Override
	public IO io() {
		if(io == null){
			io = new IO() {
				
				@Override
				public File getStandardDataDirectory() {
					return getAbsFile(AppConstants.DATA_DIRECTORY);
				}

				private File getAbsFile(String s) {
					File ret = new File(s);
					ret = ret.getAbsoluteFile();
					return ret;
				}

				@Override
				public File getStandardConfigDirectory() {
					return getAbsFile(AppConstants.ODL_CONFIG_DIR);
				}
			};
		}
		
		return io;
	}


	
}
