/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder.component;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator;
import com.opendoorlogistics.studio.components.geocoder.InteractiveGeocoderPanel;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.utils.ui.Icons;

final public class NominatimGeocoderComponent implements ODLComponent {

	@Override
	public String getId() {
		return "com.opendoorlogistics.studio.components.geocoder.nominatim";
	}

	@Override
	public String getName() {
		return "Geocode addresses using Nominatim";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		return GeocodeModel.getDsDefn();
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(final ComponentExecutionApi gui, int mode, final Object configuration, final ODLDatastore<? extends ODLTable> ioDb, final ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {
		gui.submitControlLauncher(new ControlLauncherCallback() {

			@Override
			public void launchControls(ComponentControlLauncherApi launcherApi) {
				// filter if needed
				final ODLDatastore<? extends ODLTable> selDB;
				if (((NominatimConfig) configuration).isSkipAlreadyGeocoded()) {
					ODLTableReadOnly table = ioDb.getTableAt(0);
					RowFilterDecorator<? extends ODLTable> filter = new RowFilterDecorator<ODLTable>(ioDb, table.getImmutableId());
					for (int i = 0; i < table.getRowCount(); i++) {
						if (table.getValueAt(i, GeocodeModel.LAT_COL) == null || table.getValueAt(i, GeocodeModel.LNG_COL) == null) {
							filter.addRowToFilter(table.getImmutableId(), table.getRowId(i));
						}
					}
					selDB = filter;
				} else {
					selDB = ioDb;
				}


				if (selDB.getTableAt(0).getRowCount() > 0) {

					InteractiveGeocoderPanel panel = new InteractiveGeocoderPanel((NominatimConfig) configuration, selDB, launcherApi);
					try {
						gui.showModalPanel(panel, "Nominatim geocoder");
					} catch (Throwable e) {
						throw new RuntimeException(e);
					} finally {
						panel.dispose();
					}
				} else {
					if (((NominatimConfig) configuration).isSkipAlreadyGeocoded()) {
						throw new RuntimeException("No input rows without geocodes found.");
					} else {
						throw new RuntimeException("No input rows found.");
					}
				}
			}

		});

	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return NominatimConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory, int mode, Serializable config, boolean isFixedIO) {
		return new NominatimConfigPanel((NominatimConfig) config);
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_OUTPUT_WINDOWS_ALWAYS_SYNCHRONISED;
	}

	// @Override
	// public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
	// return Arrays.asList(
	// new ODLWizardTemplateConfig("Nominatim", getName(), "Interactive geocoding of a table using an OpenStreetMap Nominatim webservice.", new
	// NominatimConfig())
	// // ,new ODLWizardTemplateConfig("TEST","TEST", "TEST", new NominatimConfig())
	//
	// );
	// }

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("nominatim-geocode.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("Nominatim", getName(), "Interactive geocoding of a table using an OpenStreetMap Nominatim webservice.", getIODsDefinition(templatesApi.getApi(), new NominatimConfig()), new NominatimConfig());
	}
}
