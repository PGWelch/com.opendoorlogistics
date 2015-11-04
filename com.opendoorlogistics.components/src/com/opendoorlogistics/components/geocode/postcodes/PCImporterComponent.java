/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes;

import java.io.File;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCConstants;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCGeocodeFile;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord.StrField;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.utils.ui.Icons;

final public class PCImporterComponent implements ODLComponent {

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return PCImporterConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(final ComponentConfigurationEditorAPI factory,int mode,Serializable config, boolean isFixedIO) {
		return PCImporterConfig.createConfigEditorPanel(factory,(PCImporterConfig) config, "Import");
	}

	@Override
	public String getId() {
		return getClass().getName();
	}

	@Override
	public String getName() {
		return "Import postcodes";
	}

	@Override
	public ODLDatastore<ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		return null;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		ODLDatastoreAlterable<ODLTableDefinitionAlterable> ret = ODLFactory.createDefinition();
		ret.createTable("Postcodes", -1);
		ODLTableDefinitionAlterable dfn = ret.getTableAt(0);

		for (StrField fld : StrField.values()) {
			TableUtils.addColumn(dfn, fld.getDisplayText(), ODLColumnType.STRING, 0	,null, fld.getTags());
		}

		TableUtils.addColumn(dfn,"Latitude", ODLColumnType.DOUBLE, 0, "Latitude of the postcode", PredefinedTags.LATITUDE);
		TableUtils.addColumn(dfn,"Longitude", ODLColumnType.DOUBLE, 0, "Longitude of the postcode", PredefinedTags.LONGITUDE);
		return ret;
	}

	@Override
	public void execute(ComponentExecutionApi reporter,int mode,Object configuration, ODLDatastore<? extends ODLTable> input, ODLDatastoreAlterable<? extends ODLTableAlterable> output) {
		PCImporterConfig pic = (PCImporterConfig) configuration;
		if(reporter.getApi().stringConventions().isEmptyString(pic.getGeocoderDbFilename())){
			throw new RuntimeException("Postcode database file does not exist");			
		}
		
		File file = PCConstants.resolvePostcodeFile(reporter.getApi(), new File(pic.getGeocoderDbFilename()));
		if (file.exists() == false) {
			throw new RuntimeException("Postcode database file does not exist");
		}

		
		reporter.postStatusMessage("Started postcode import.");

		PCGeocodeFile pc = new PCGeocodeFile(file);
		ODLTable table = output.getTableAt(0);
		long nbImported = 0;
		for (PCRecord record : pc.getPostcodes(pic.getLevel(), reporter)) {
			if (reporter.isFinishNow() || reporter.isCancelled()) {
				break;
			}
			int col = 0;
			int row = table.createEmptyRow(-1);
			for (StrField fld : StrField.values()) {
				table.setValueAt(record.getField(fld), row, col++);
			}
			table.setValueAt(record.getLatitude(), row, col++);
			table.setValueAt(record.getLongitude(), row, col++);

			nbImported++;
			if (nbImported % 10000 == 0) {
				reporter.postStatusMessage("Imported " + nbImported + " postcodes.");
			}
		}
		pc.close();

		reporter.postStatusMessage("Finished postcode import.");
	}

	@Override
	public long getFlags(ODLApi api,int mode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return Icons.loadFromStandardPath("postcode-importer.png");
	}
	
//	@Override
//	public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
//		return Arrays.asList(new ODLWizardTemplateConfig(getName(), getName(), "Import postcodes for a country, using data derived from www.geonames.org", new PCDatabaseSelectionConfig()));
//	}


	// @Override
	// public ComponentType getComponentType() {
	// // TODO Auto-generated method stub
	// return null;
	// }

	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate(getName(), getName(), "Import postcodes for a country, using data derived from www.geonames.org",
				getIODsDefinition(templatesApi.getApi(), new PCImporterConfig()),				
				new PCImporterConfig());
	}
}
