/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.components.shapefileexporter;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.rog.builder.ROGBuilder;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;
import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel.CheckChangedListener;
import com.opendoorlogistics.utils.ui.Icons;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShapefileExporterComponent implements ODLComponent {

	@Override
	public String getId() {
		return "com.opendoorlogistics.components.shapefileexporter";
	}

	@Override
	public String getName() {
		return "Shapefile exporter";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ExportConfig c = (ExportConfig)configuration;
		if(c.isConvertShpToROGMode()){
			return null;
		}
		
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds = api.tables().createAlterableDs();
		ODLTableDefinitionAlterable table = ds.createTable("Shapefile", -1);
		table.setFlags(table.getFlags() | TableFlags.FLAG_COLUMN_WILDCARD);
		table.addColumn(-1, "the_geom", ODLColumnType.GEOM, 0);
		return ds;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	private int geomColumn(ODLTableDefinition tableDefinition) {
		int ret = TableUtils.findColumnIndx(tableDefinition, "the_geom");
		if (ret == -1) {
			throw new RuntimeException("Cannot find the_geom field.");
		} else if (tableDefinition.getColumnType(ret) != ODLColumnType.GEOM) {
			throw new RuntimeException("the_geom field is not of geometry type.");
		}
		return ret;
	}

	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {
		ExportConfig c = (ExportConfig) configuration;
		File file = new File(c.getFilename());

		if (c.isConvertShpToROGMode()) {
			buildODLGRFile(api, c, file);
			return;
		}

		api.postStatusMessage("Exporting shapefile " + c.getFilename());

		// get the geometry type
		ODLTable table = ioDs.getTableAt(0);
		Geometries geomtype = identifyType(table);
		if (geomtype == null) {
			throw new RuntimeException("Cannot identify a valid geometry type for the shapefile export. All geometries must the same type and one or more must be present.");
		}

		// built the feature type
		SimpleFeatureType type = createFeatureType(table, geomtype);

		try {

			int geomCol = geomColumn(table);

			// build list of features
			String[] fieldnames = getNonGeomFieldNames(table);
			List<SimpleFeature> features = new ArrayList<SimpleFeature>();
			for (int i = 0; i < table.getRowCount(); i++) {

				// check geometry is not null as this is not generally supported in shapefiles
				ODLGeom geom = (ODLGeom) table.getValueAt(i, geomCol);
				if (geom == null) {
					continue;
				}
				Geometry g = ((ODLGeomImpl) geom).getJTSGeometry();
				if (g == null) {
					continue;
				}

				// write geom first
				SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
				featureBuilder.add(assignToType(g, geomtype));

				for (int j = 0; j < table.getColumnCount(); j++) {
					if (j != geomCol && fieldnames[j] != null) {
						Object value = table.getValueAt(i, j);
						switch (table.getColumnType(j)) {
						case LONG:
							if (value != null) {
								featureBuilder.add(((Long) value).intValue());
							} else {
								featureBuilder.add(new Integer(0));
							}
							break;

						case DOUBLE:
							if (value != null) {
								featureBuilder.add(((Double) value).intValue());
							} else {
								featureBuilder.add(new Double(0));
							}
							break;

						default:
							String s = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, value);
							if (s == null) {
								s = "";
							}

							// max length in shapefile is 255
							if (s.length() > 255) {
								s = s.substring(0, 255);
							}

							featureBuilder.add(s);
							break;
						}
					}
				}

				SimpleFeature feature = featureBuilder.buildFeature(null);
				features.add(feature);
			}

			// create shapefile
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", file.toURI().toURL());
			params.put("create spatial index", Boolean.FALSE);
			ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

			// create type in shapefile
			newDataStore.createSchema(type);

			Transaction transaction = new DefaultTransaction("create");
			// String typeName = newDataStore.getTypeNames()[0];
			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

				SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
				featureStore.setTransaction(transaction);
				try {
					featureStore.addFeatures(collection);
					transaction.commit();
				} catch (Exception problem) {
					problem.printStackTrace();
					transaction.rollback();
				} finally {
					transaction.close();
				}

				if (c.isBuildROGFile()) {
					buildODLGRFile(api, c, file);
				}
			} else {
				throw new RuntimeException(type.getTypeName() + " does not support read/write access");

			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param api
	 * @param c
	 * @param file
	 */
	private void buildODLGRFile(ComponentExecutionApi api, ExportConfig c, File file) {
		api.postStatusMessage("Starting build of .odlrg file...");
		int nbThreads = c.getNbROGBuilderThreads();
		if (nbThreads < 1 || nbThreads > 10) {
			throw new RuntimeException("Number of .odlrg builder threads must be between 1 and 10");
		}

		new ROGBuilder(file, false, 1, nbThreads, api).build();
	}

	@XmlRootElement
	private static class ExportConfig implements Serializable {
		private String filename = "";
		private boolean buildROGFile;
		private boolean convertShpToROGMode;

		private int nbROGBuilderThreads = 1;

		public String getFilename() {
			return filename;
		}

		@XmlAttribute
		public void setFilename(String filename) {
			this.filename = filename;
		}

		public boolean isBuildROGFile() {
			return buildROGFile;
		}

		public void setBuildROGFile(boolean buildROGFile) {
			this.buildROGFile = buildROGFile;
		}

		public int getNbROGBuilderThreads() {
			return nbROGBuilderThreads;
		}

		public void setNbROGBuilderThreads(int nbROGBuilderThreads) {
			this.nbROGBuilderThreads = nbROGBuilderThreads;
		}

		public boolean isConvertShpToROGMode() {
			return convertShpToROGMode;
		}

		public void setConvertShpToROGMode(boolean convertShpToROGMode) {
			this.convertShpToROGMode = convertShpToROGMode;
		}

	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return ExportConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		final ExportConfig c = (ExportConfig) config;
		final JTextField field = new JTextField();
		field.setText(c.getFilename());
		field.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				save();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				save();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				save();
			}

			private void save() {
				c.setFilename(field.getText());
			}
		});

		final VerticalLayoutPanel ret = new VerticalLayoutPanel();

		FileBrowserPanel filePanel = new FileBrowserPanel(c.getFilename(), new FilenameChangeListener() {

			@Override
			public void filenameChanged(String newFilename) {
				c.setFilename(newFilename);
			}
		}, false, "OK", new FileNameExtensionFilter("Shapefile (.shp)", "shp"));

		ret.addLine(new JLabel(c.isConvertShpToROGMode() ? "Input shapefile:" : "Output shapefile:"));
		ret.addLine(filePanel);
		ret.addWhitespace();

		final IntegerEntryPanel nbRogThreads = new IntegerEntryPanel("Number .odlrg builder threads", c.getNbROGBuilderThreads(), null, new IntChangedListener() {

			@Override
			public void intChange(int newInt) {
				c.setNbROGBuilderThreads(newInt);
			}
		});
		nbRogThreads.setEnabled(c.isBuildROGFile() || c.isConvertShpToROGMode());

		if (!c.isConvertShpToROGMode()) {
			ret.addCheckBox("Build .odlrg file (needs at least 6 GB memory - use java Xmx flag)", c.isBuildROGFile(), new CheckChangedListener() {

				@Override
				public void checkChanged(boolean isChecked) {
					c.setBuildROGFile(isChecked);
					nbRogThreads.setEnabled(isChecked);
				}
			});
		}
		else{
			ret.addLine(new JLabel("Warning - building an .odlrg file requires lots of memory. Run ODL Studio with minimum 6 GB using java Xmx flag."));
		}
		ret.addHalfWhitespace();
		ret.addLine(nbRogThreads);

		return ret;
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("shapefile-exporter.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		ExportConfig c = new ExportConfig();
		templatesApi.registerTemplate(getName(), getName(), getName(), getIODsDefinition(templatesApi.getApi(), c), c);

		c = new ExportConfig();
		c.setConvertShpToROGMode(true);
		String s = "Create .odlrg file from pre-existing .shp";
		templatesApi.registerTemplate(s, s,s, getIODsDefinition(templatesApi.getApi(), c), c);		
	}

	private void getAtomicGeometries(Geometry g, List<Geometry> outList) {
		if (GeometryCollection.class.isInstance(g)) {
			for (int i = 0; i < g.getNumGeometries(); i++) {
				getAtomicGeometries(g.getGeometryN(i), outList);
			}
		} else {
			outList.add(g);
		}
	}

	/**
	 * Assign the geometry to the input type assuming the geometry has already passed the isAssignableToType test.
	 * 
	 * @param g
	 * @param targetType
	 * @return
	 */
	private Geometry assignToType(Geometry g, Geometries targetType) {
		Geometries currentType = Geometries.get(g);
		if (currentType == targetType) {
			return g;
		}

		ArrayList<Geometry> list = new ArrayList<>();
		getAtomicGeometries(g, list);

		GeometryFactory factory = new GeometryFactory();
		switch (targetType) {
		case MULTIPOINT:
			return factory.createMultiPoint(list.toArray(new Point[list.size()]));
		case MULTILINESTRING:
			return factory.createMultiLineString(list.toArray(new LineString[list.size()]));
		case MULTIPOLYGON:
			return factory.createMultiPolygon(list.toArray(new Polygon[list.size()]));

		default:
			return g;
		}

	}

	/**
	 * Check if the input geometry can be assigned to the type
	 * 
	 * @param g
	 * @param type
	 * @return
	 */
	private boolean isAssignableToType(Geometry g, Geometries type) {
		Geometries thisType = Geometries.get(g);

		// check for exactly the same type
		if (thisType == type) {
			return true;
		}

		// check for having simple type and complex type being acceptable
		if (type == Geometries.MULTIPOLYGON && thisType == Geometries.POLYGON) {
			return true;
		}

		if (type == Geometries.MULTILINESTRING && thisType == Geometries.LINESTRING) {
			return true;
		}

		if (type == Geometries.MULTIPOINT && thisType == Geometries.POINT) {
			return true;
		}

		if (thisType == Geometries.GEOMETRYCOLLECTION) {
			for (int i = 0; i < g.getNumGeometries(); i++) {
				if (!isAssignableToType(g.getGeometryN(i), type)) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Identify a geometry type for the table
	 * 
	 * @param table
	 * @return
	 */
	private Geometries identifyType(ODLTableReadOnly table) {
		int n = table.getRowCount();
		int geomCol = geomColumn(table);
		for (Geometries type : Geometries.values()) {

			int count = 0;
			for (int i = 0; i < n; i++) {
				ODLGeom geom = (ODLGeom) table.getValueAt(i, geomCol);
				if (geom != null) {
					Geometry g = ((ODLGeomImpl) geom).getJTSGeometry();
					if (!isAssignableToType(g, type)) {
						count = -1;
						break;
					}
					count++;
				}
			}

			if (count > 0) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Get the non-geom field names or null if field not allowed to be exported (repeat field name when truncated)
	 * 
	 * @param table
	 * @return
	 */
	private String[] getNonGeomFieldNames(ODLTableDefinition table) {
		int n = table.getColumnCount();
		String[] ret = new String[n];
		int gc = geomColumn(table);
		for (int i = 0; i < n; i++) {
			if (i != gc) {
				String s = table.getColumnName(i);
				if (s.length() >= 10) {
					s = s.substring(0, 10);
				}

				// check unique...
				boolean unique = true;
				for (int j = 0; j < i; j++) {
					if (Strings.equals(ret[j], s)) {
						unique = false;
					}
				}

				if (unique) {
					ret[i] = s;
				}
			}
		}
		return ret;
	}

	private SimpleFeatureType createFeatureType(ODLTableReadOnly table, Geometries geomtype) {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(table.getName());
		builder.setCRS(DefaultGeographicCRS.WGS84);

		// add geom first
		builder.add("the_geom", geomtype.getBinding());

		// add other types
		int geomCol = geomColumn(table);
		int nc = table.getColumnCount();
		String[] fieldnames = getNonGeomFieldNames(table);
		for (int i = 0; i < nc; i++) {
			if (i != geomCol && fieldnames[i] != null) {

				Class<?> cls;
				switch (table.getColumnType(i)) {
				case LONG:
					cls = Integer.class;
					break;

				case DOUBLE:
					cls = Double.class;
					break;

				default:
					cls = String.class;
					break;
				}

				builder.add(fieldnames[i], cls);
			}
		}

		SimpleFeatureType type = builder.buildFeatureType();
		return type;

	}

}
