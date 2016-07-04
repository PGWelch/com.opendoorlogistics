package com.opendoorlogistics.components.matrixexporter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.xml.bind.annotation.XmlRootElement;

import org.hsqldb.lib.HashSet;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputDistanceUnit;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputTimeUnit;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.MatrixExporter;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.utils.Exceptions;
import com.opendoorlogistics.core.utils.Serialization;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;
import com.opendoorlogistics.utils.ui.Icons;

public class MatrixExporterComponent implements ODLComponent, MatrixExporter {

	@Override
	public String getId() {
		return "com.opendoorlogistics.components.matrixexporter";
	}

	@Override
	public String getName() {
		return "Travel matrix text file exporter";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds = api.tables().createDefinitionDs();
		ODLTableDefinitionAlterable table = ds.createTable("Locations", -1);
		if (configuration != null && (((TravelMatrixExporterConfig) configuration)).isIncludeId()) {
			table.addColumn(-1, PredefinedTags.ID, ODLColumnType.STRING, 0);
		}
		table.addColumn(-1, PredefinedTags.LATITUDE, ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, PredefinedTags.LONGITUDE, ODLColumnType.DOUBLE, 0);
		return ds;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		return null;
	}

	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs,
			ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {

		TravelMatrixExporterConfig c = (TravelMatrixExporterConfig) configuration;
		if(Strings.isEmptyWhenStandardised(c.getExportFilename())){
			throw new RuntimeException("The output filename for the matrix is empty. You need to set the output filename in the matrix exporter settings.");
		}
		
		c = (TravelMatrixExporterConfig)Serialization.deepCopy(c);
		c.getDistancesConfiguration().getOutputConfig().setOutputDistanceUnit(OutputDistanceUnit.KILOMETRES);
		c.getDistancesConfiguration().getOutputConfig().setOutputTimeUnit(OutputTimeUnit.MILLISECONDS);
		
		ODLTableReadOnly table = ioDs.getTableAt(0);
		int nrows = table.getRowCount();

		// create table in the format expected by distances
		ODLApi odlApi = api.getApi();
		ODLTableAlterable copyTable = odlApi.tables().createAlterableTable("Locations");
		copyTable.addColumn(-1, PredefinedTags.LOCATION_KEY, ODLColumnType.STRING, 0);
		copyTable.addColumn(-1, PredefinedTags.LATITUDE, ODLColumnType.DOUBLE, 0);
		copyTable.addColumn(-1, PredefinedTags.LONGITUDE, ODLColumnType.DOUBLE, 0);
		
		Map<String,LatLong> idToLL = api.getApi().stringConventions().createStandardisedMap();
		Map<String,String> standardToOriginalId = api.getApi().stringConventions().createStandardisedMap();
		for(int oldRow =0 ; oldRow<nrows ; oldRow++){
			int newRow=copyTable.createEmptyRow(-1);
			
			if(!c.isIncludeId()){
				// create dummy id
				copyTable.setValueAt(Integer.toString(oldRow), newRow, 0);
				
				// copy lat and long
				copyTable.setValueAt(table.getValueAt(oldRow, 0), newRow, 1);	
				copyTable.setValueAt(table.getValueAt(oldRow, 1), newRow, 2);	
			}else{
				if(Strings.isEmptyWhenStandardised(table.getValueAt(oldRow, 0))){
					throw new RuntimeException("Found input location with empty id on row " + (oldRow+1));
				}
				
				// copy all columns
				for(int j=0;j<3;j++){
					copyTable.setValueAt(table.getValueAt(oldRow, j), newRow, j);									
				}
			}
			
			String id =(String)copyTable.getValueAt(newRow, 0); 
			idToLL.put(id, new LatLongImpl((Double)copyTable.getValueAt(newRow, 1), (Double)copyTable.getValueAt(newRow, 2)));
			standardToOriginalId.put(id, id);
		}

		ODLCostMatrix matrix = api.calculateDistances(c.getDistancesConfiguration(), copyTable);

		// now write to file
		UpdateTimer timer = new UpdateTimer(250);
		long nbLines=0;
		File file = new File(c.getExportFilename());
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))){
			// header...
			if(c.isIncludeId()){
				writer.write("FromID\t");
			}
			writer.write("FromLatitude\t");
			writer.write("FromLongitude\t");
			if(c.isIncludeId()){
				writer.write("ToID\t");
			}
			writer.write("ToLatitude\t");
			writer.write("ToLongitude\t");
			writer.write(Strings.convertEnumToDisplayFriendly(c.getDistancesConfiguration().getOutputConfig().getOutputDistanceUnit()) + "\t");
			writer.write("Time");
			writer.write(System.lineSeparator());
			
			int nf= matrix.getNbFroms();
			int nt = matrix.getNbTos();
			for(int f = 0 ; f<nf ; f++){
				for(int t = 0 ; t<nt; t++){
					// From
					if(c.isIncludeId()){
						writer.write(standardToOriginalId.get(matrix.getFromId(f))+"\t");
					}
					LatLong from = idToLL.get(matrix.getFromId(f));
					writer.write(""+ from.getLatitude() + "\t");
					writer.write("" + from.getLongitude() +"\t");
					
					// To
					if(c.isIncludeId()){
						writer.write(standardToOriginalId.get(matrix.getToId(t)) +"\t");
					}
					LatLong to = idToLL.get(matrix.getToId(t));
					writer.write("" + to.getLatitude()+ "\t");
					writer.write("" + to.getLongitude() + "\t");
					
					// Distance
					writer.write("" + matrix.get(f, t, ODLCostMatrix.COST_MATRIX_INDEX_DISTANCE));
					writer.write("\t");
					
					// Time
					double millis =  matrix.get(f, t, ODLCostMatrix.COST_MATRIX_INDEX_TIME);
					ODLTime time  = new ODLTime(Math.round(millis));
					writer.write(time.toString());
					
					// New line
					writer.write(System.lineSeparator());
					
					nbLines++;
					if(timer.isUpdate()){
						StringBuilder builder = new StringBuilder();
						builder.append("Written " + nbLines + " out of "+ (nf*nt) + " lines to file.");
					}
				}
				
				if(api.isCancelled()){
					throw new RuntimeException("User cancelled");
				}
			}
		}
		catch (Exception e) {
			throw Exceptions.asUnchecked(e);
		}
		
		api.submitControlLauncher(new ControlLauncherCallback() {
			
			@Override
			public void launchControls(ComponentControlLauncherApi launcherApi) {
				// build up text
				StringBuilder builder = new StringBuilder();
				builder.append("Finished exporting " + matrix.getNbFroms() + "x" + matrix.getNbTos() + " matrix to file " + file.getAbsolutePath());
				
				// create panel to report it
				JTextPane textPane = new JTextPane();
				textPane.setText(builder.toString());
				JScrollPane scroller = new JScrollPane(textPane);
				class MyPanel extends JPanel implements Disposable{

					@Override
					public void dispose() {
						// TODO Auto-generated method stub
						
					}
					
				}
				MyPanel panel = new MyPanel();
				panel.setLayout(new BorderLayout());
				panel.add(scroller,BorderLayout.CENTER);
				panel.setPreferredSize(new Dimension(400, 200));
				launcherApi.registerPanel("Export finished",null, panel, false);
			}
		});
				
	//	api.
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return TravelMatrixExporterConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	//	JLabel label = new JLabel("Travel matrix exporter component. Select the output filename when you run the component.");

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

	//	panel.add(label);
		
		TravelMatrixExporterConfig c = (TravelMatrixExporterConfig) config;

		FileBrowserPanel fileBrowserPanel = new FileBrowserPanel("Output file", c.getExportFilename(), new FilenameChangeListener(){

			@Override
			public void filenameChanged(String newFilename) {
				c.setExportFilename(newFilename);
			}
			
		}, false, "OK" );
		panel.add(Box.createRigidArea(new Dimension(1, 10)));
		panel.add(fileBrowserPanel);
		
		if (api != null) {
			// add(Box.createRigidArea(new Dimension(0,6)));

			// With flags=0 we (deliberately) loose the ability to modify cost type or output travel units (which are ignored)
			long flags=0;
			JPanel distances = api.getApi().uiFactory().createDistancesEditor(c.getDistancesConfiguration(), flags);
			panel.add(Box.createRigidArea(new Dimension(1, 10)));
			
			JPanel leftAlignHack = new JPanel(new BorderLayout());
			leftAlignHack.add(distances, BorderLayout.WEST);
			panel.add(leftAlignHack);
			// distances.setBorder(BorderFactory.createTitledBorder("Distances"));
			// leftAlignHack.add(distances, BorderLayout.WEST);
			// add(leftAlignHack);
		}
		

		return panel;
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("export-matrix.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {

		for (int includeId = 0; includeId <= 1; includeId++) {
			String name = "Export travel matrix,";
			if (includeId == 0) {
				name += " without id field";
			}else{
				name += " including id field";
			}
			
			Serializable config = createConfig(includeId==1);
			templatesApi.registerTemplate(name, name, name, getIODsDefinition(templatesApi.getApi(), config), config);
		}
	}


	@Override
	public Serializable createConfig(boolean exportIds) {
		TravelMatrixExporterConfig config = new TravelMatrixExporterConfig();
		config.setIncludeId(exportIds);
		return config;
	}
	
	@XmlRootElement
	public static class TravelMatrixExporterConfig implements Serializable {
		private boolean includeId;
		private String exportFilename = "";
		private DistancesConfiguration distancesConfiguration = new DistancesConfiguration();

		// private String exportFilename;
		public boolean isIncludeId() {
			return includeId;
		}

		public void setIncludeId(boolean includeId) {
			this.includeId = includeId;
		}

		public DistancesConfiguration getDistancesConfiguration() {
			return distancesConfiguration;
		}

		public void setDistancesConfiguration(DistancesConfiguration distancesConfiguration) {
			this.distancesConfiguration = distancesConfiguration;
		}

		public String getExportFilename() {
			return exportFilename;
		}

		public void setExportFilename(String exportFilename) {
			this.exportFilename = exportFilename;
		}

		
	}


}
