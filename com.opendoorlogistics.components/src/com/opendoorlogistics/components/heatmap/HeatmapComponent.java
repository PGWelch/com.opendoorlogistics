package com.opendoorlogistics.components.heatmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;
import com.opendoorlogistics.components.heatmap.HeatmapGenerator.HeatMapResult;
import com.opendoorlogistics.components.heatmap.HeatmapGenerator.InputPoint;
import com.opendoorlogistics.components.heatmap.HeatmapGenerator.SingleContourGroup;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.operations.GridTransforms;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.LargeList;
import com.opendoorlogistics.utils.ui.Icons;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class HeatmapComponent implements ODLComponent {
	private static final Icon ICON = Icons.loadFromStandardPath("heatmap.png");
	
	@Override
	public String getId() {
		return "com.opendoorlogistics.components.heatmap";
	}

	@Override
	public String getName() {
		return "Heatmap from points";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ODLDatastoreAlterable<?extends ODLTableDefinitionAlterable> ds = api.tables().createDefinitionDs();
		ODLTableDefinitionAlterable table = ds.createTable("Points", -1);
		table.addColumn(-1, "Latitude", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "Longitude", ODLColumnType.DOUBLE, 0);
		table.setColumnDefaultValue(table.addColumn(-1, "Weight", ODLColumnType.DOUBLE, TableFlags.FLAG_IS_OPTIONAL), 1.0);
		return ds;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		ODLDatastoreAlterable<?extends ODLTableDefinitionAlterable> ds = api.tables().createDefinitionDs();
		ODLTableDefinitionAlterable table = ds.createTable("ContourPolygons", -1);
		table.addColumn(-1, "Level", ODLColumnType.LONG, 0);
		table.addColumn(-1, "Min", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "Max", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "Colour", ODLColumnType.COLOUR, 0);
		table.addColumn(-1, "Geometry", ODLColumnType.GEOM, 0);
		return ds;
	}

	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {
		HeatMapConfig c = (HeatMapConfig)configuration;
		GridTransforms transform = null;
		StringConventions sc = api.getApi().stringConventions();
		if(!sc.isEmptyString(sc.standardise(c.getEPSG()))){
			transform = GridTransforms.getAndCache(sc.standardise(c.getEPSG()));
		}
		
		GeometryFactory factory = new GeometryFactory();
		
		// get input points array
		ODLTableReadOnly table = ioDs.getTableAt(0);
		List<InputPoint> points = new ArrayList<InputPoint>();
		Envelope envelope = new Envelope();
		int n = table.getRowCount();
		for(int i =0 ; i < n ; i++){
			Double lat = (Double)table.getValueAt(i, 0);
			Double lng = (Double)table.getValueAt(i, 1);
			Double weight = (Double)table.getValueAt(i, 2);
			if(weight==null){
				weight= 1.0;
			}
			if(lat!=null && lng!=null){
				Coordinate coordinate = new Coordinate(lng, lat);
				Point point = factory.createPoint(coordinate);
				if(transform!=null){
					point = (Point)transform.wgs84ToGrid(point);
				}
				envelope.expandToInclude(point.getCoordinate());
				points.add(new InputPoint(point, weight));
				
			}
		}

		// check for empty
		if(envelope.isNull() || points.size()==0){
			return;
		}
		
		// choose the area based on points having no effect outside it
		envelope.expandBy(c.getPointRadius() * 2 * HeatmapGenerator.GAUSSIAN_CUTOFF);
		
		// get cell length based on resolution
		double maxDim = Math.max(envelope.getHeight(), envelope.getWidth());
		double cellLength = maxDim / c.getResolution();
		

		// create result
		HeatMapResult result = HeatmapGenerator.build(points, c.getPointRadius(), envelope, cellLength, c.getNbContourLevels(),api);
		if(api.isCancelled()){
			return;
		}
		
		// write out
		ODLTable outTable = outputDs.getTableAt(0);
		for(SingleContourGroup r : result.groups){
			if(r.geometry!=null){
				int row = outTable.createEmptyRow(-1);
				outTable.setValueAt(r.level+1, row, 0);
				outTable.setValueAt(result.levelLowerLimits[r.level], row, 1);
				outTable.setValueAt(result.levelUpperLimits[r.level], row, 2);
				outTable.setValueAt(Colours.temperature((double)r.level / (c.getNbContourLevels()+1)), row, 3);
				Geometry p = r.geometry;
				if(transform!=null){
					p = transform.gridToWGS84(p);
				}
				outTable.setValueAt(new ODLLoadedGeometry(p) ,row, 4);	
			}			
		}
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return HeatMapConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		HeatMapConfig c = (HeatMapConfig)config;
		
		UIFactory uiFactory = api.getApi().uiFactory();
		class MyPanel extends JPanel implements Disposable{

			@Override
			public void dispose() {
				// TODO Auto-generated method stub
				
			}
			
		}
		MyPanel ret = new MyPanel();
		ret.setLayout(new BoxLayout(ret, BoxLayout.Y_AXIS));
		
		ret.add(uiFactory.createTextEntryPane("EPSG projection ", c.getEPSG(), "EPSG projection code or empty if using latitude and longitude", new TextChangedListener() {
			
			@Override
			public void textChange(String newText) {
				c.setEPSG(newText);
			}
		}));

		ret.add(uiFactory.createDoubleEntryPane("Radius of point influence ", c.getPointRadius(), "Radius of point influence (in projected units or degrees if unprojected), used as standard deviation in Gaussian equation", new DoubleChangedListener() {
			
			@Override
			public void doubleChange(double newValue) {
				c.setPointRadius(newValue);
			}
		}));
		
		ret.add(uiFactory.createIntegerEntryPane("Number of contour levels " , c.getNbContourLevels(), "Number of contour levels", new IntChangedListener() {
			
			@Override
			public void intChange(int newInt) {
				c.setNbContourLevels(newInt);
			}
		}));
		
		ret.add(uiFactory.createIntegerEntryPane("Calculation resolution " , c.getResolution(), "Calculation resolution - higher resolutions take longer to calculate", new IntChangedListener() {
			
			@Override
			public void intChange(int newInt) {
				c.setResolution(newInt);
			}
		}));
		
//		JCheckBox checkBox = new JCheckBox("Smooth polygon edges", c.isSimplify());
//		checkBox.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				c.setSimplify(checkBox.isSelected());
//			}
//		});
//		ret.add(checkBox);
		
		return ret;
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return ICON;
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("Heatmap", getName(),
				"Generate heatmap contour polygons from input points with weights.",getIODsDefinition(templatesApi.getApi(), new HeatMapConfig()), 
			new ScriptTemplatesBuilder.BuildScriptCallback() {
				
				@Override
				public void buildScript(ScriptOption builder) {
					ODLApi api = templatesApi.getApi();
					
					// Add instruction to generate the heatmap
					HeatMapConfig config  = new HeatMapConfig();
					ScriptAdapter input = builder.addDataAdapterLinkedToInputTables("heatmapinput", getIODsDefinition(api, config));
					ScriptInstruction instruction = builder.addInstruction(input.getAdapterId(), getId(), ODLComponent.MODE_DEFAULT, config);
					instruction.setOutputDatastoreId(builder.createUniqueDatastoreId("CountourPolygons"));
					
					// add showmap option
					ScriptOption showMapOption = builder.addOption("Show heatmap", "Show heatmap"); 
					showMapOption.setSynced(false);
					ODLTableDefinition heatmapOutTable= getOutputDsDefinition(api, ODLComponent.MODE_DEFAULT, config).getTableAt(0);
					ScriptAdapter mapInput = showMapOption.addDataAdapter("Heatmap map");
					Maps maps = api.standardComponents().map();
					ScriptAdapterTable drawableTable = mapInput.addSourcelessTable(maps.getDrawableTableDefinition());
					drawableTable.setSourceTable(instruction.getOutputDatastoreId(), heatmapOutTable.getName());
					drawableTable.setSourceColumn("geometry", "Geometry");
					drawableTable.setSourceColumn("colour", "Colour");
					//drawableTable.setSourceColumn("legendKey", "Level");
					String legendFormula ="c(\"Level \") & Level & \" (\" & stringformat(\"%.3f\", min) & \"-\" & stringformat(\"%.3f\", max) & \")\"";// "level & " = "min & \" <= density < \" & max";
					drawableTable.setFormula("legendKey",legendFormula);
					drawableTable.setFormula("tooltip", legendFormula);
					drawableTable.setFormula("nonOverlappingPolygonLayerGroupKey", "\"yes\"");
					drawableTable.setFormula("opaque", "0.35");
					Serializable mapConfig;
					try {
						mapConfig = maps.getConfigClass().newInstance();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					maps.setCustomTooltips(true, mapConfig);
					showMapOption.addInstruction(mapInput.getAdapterId(), maps.getId(), ODLComponent.MODE_DEFAULT, mapConfig);
						
					// add export option
					ScriptOption exportOutput = builder.addOption("Export contour polygons", "Export contour polygons");
					String outTableName =  heatmapOutTable.getName();
					exportOutput.addCopyTable(instruction.getOutputDatastoreId(), outTableName, ScriptOption.OutputType.REPLACE_CONTENTS_OF_EXISTING_TABLE, outTableName);
					exportOutput.setSynced(false);
				}
			});		
	}

}
