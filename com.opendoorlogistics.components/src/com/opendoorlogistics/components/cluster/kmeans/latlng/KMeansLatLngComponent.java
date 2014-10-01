/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.kmeans.latlng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.components.cluster.BasicCluster;
import com.opendoorlogistics.components.cluster.kmeans.KMeansAlgorithm;
import com.opendoorlogistics.components.cluster.kmeans.KMeansAlgorithm.CreateMean;
import com.opendoorlogistics.components.cluster.kmeans.KMeansConfig;
import com.opendoorlogistics.components.cluster.kmeans.Mean;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.utils.ui.Icons;

final public class KMeansLatLngComponent implements ODLComponent {

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return KMeansConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory,int mode,Serializable config, boolean isFixedIO) {
		return KMeansConfig.createConfigEditorPanel((KMeansConfig) config);
	}

	@Override
	public String getId() {
		return "com.opendoorlogistics.components.cluster.kmeans.latlng.KMeansLatLngComponent";
	}

	@Override
	public String getName() {
		return "Cluster using K-means";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = api.tables().createDefinitionDs();
		ret.createTable("Points", -1);
		ODLTableDefinitionAlterable dfn = ret.getTableAt(0);
		dfn.addColumn(-1, "Latitude", ODLColumnType.DOUBLE, 0);
		dfn.addColumn(-1, "Longitude", ODLColumnType.DOUBLE, 0);
		dfn.addColumn(-1, "ClusterNumber", ODLColumnType.LONG, 0);
		return ret;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable>ret= BeanMapping.buildDatastore(BasicCluster.class).getDefinition();
		ret.setTableName(ret.getTableAt(0).getImmutableId(), "K-means cluster result");
		return ret;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void execute(ComponentExecutionApi reporter,int mode,Object configuration, ODLDatastore<? extends ODLTable> input, ODLDatastoreAlterable<? extends ODLTableAlterable> output) {
		ODLTable tbl = input.getTableAt(0);
		int nr = tbl.getRowCount();

		// convert to our internal points class
		ArrayList<KMeanPointLngLat> lngLats = new ArrayList<>();
		for (int row = 0; row < nr; row++) {
			Double lat = (Double) tbl.getValueAt(row, 0);
			Double lng = (Double) tbl.getValueAt(row, 1);
			if (lat != null && lng != null) {
				KMeanPointLngLat lnglat = new KMeanPointLngLat();
				lnglat.id = row;
				lnglat.latitude = lat;
				lnglat.longitude = lng;
				lngLats.add(lnglat);
			}
		}

		reporter.postStatusMessage("Starting k-means on " + lngLats.size() + " points");

		// run algorithm
		KMeansConfig kc = (KMeansConfig) configuration;
		KMeansAlgorithm algorithm = new KMeansAlgorithm();
		List<Mean<KMeanPointLngLat>> means = algorithm.execute(kc.getK(), kc.getRandomSeed(), new CreateMean<KMeanPointLngLat>() {

			@Override
			public Mean<KMeanPointLngLat> createMean(KMeanPointLngLat copyThis) {
				MeanLngLat mean = new MeanLngLat();
				mean.getMean().latitude = copyThis.latitude;
				mean.getMean().longitude = copyThis.longitude;
				return mean;
			}
		}, lngLats, reporter);

		if (!reporter.isCancelled()) {
			reporter.postStatusMessage("Writing results to table");

			// write results back to input table
			for (KMeanPointLngLat latLng : lngLats) {
				tbl.setValueAt(latLng.clusterNumber + 1, latLng.id, 2);
			}

			// also output cluster table
			BasicCluster[] clusters = new BasicCluster[means.size()];
			for (int i = 0; i < clusters.length; i++) {
				clusters[i] = new BasicCluster();
				BasicCluster c = clusters[i];
				c.setClusterId(Integer.toString(i + 1));
				c.setLatitude(means.get(i).getMean().latitude);
				c.setLongitude(means.get(i).getMean().longitude);
				c.setAssignedLocationsCount(0);
			}
			for (KMeanPointLngLat latLng : lngLats) {
				int cIndex = latLng.clusterNumber;
				BasicCluster c = clusters[cIndex];
				c.setAssignedLocationsCount(c.getAssignedLocationsCount() + 1);
			}
			BeanMapping.buildDatastore(BasicCluster.class).getTableMapping(0).writeObjectsToTable(clusters, output.getTableAt(0));

			reporter.postStatusMessage("Finished k-means");
		}
	}

	@Override
	public long getFlags(ODLApi api,int mode) {
		return 0;
	}

//	@Override
//	public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
//		return Arrays.asList(new ODLWizardTemplateConfig("K-means", getName(), "K-means clustering using latitude & longitude", null));
//	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("K-means", getName(), "K-means clustering using latitude & longitude",getIODsDefinition(templatesApi.getApi(), new KMeansConfig()), new KMeansConfig());
		
	}
	
	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return Icons.loadFromStandardPath("kmeans.png");
	}

	// @Override
	// public ComponentType getComponentType() {
	// // TODO Auto-generated method stub
	// return null;
	// }

	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}


}
