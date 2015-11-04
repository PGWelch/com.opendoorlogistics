/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.Serializable;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
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
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.components.geocode.Countries.Country;
import com.opendoorlogistics.components.geocode.postcodes.builder.GDFFileBuilder;
import com.opendoorlogistics.components.geocode.postcodes.builder.CodePointOpen2GeonamesFormat;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCConstants;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCGeocodeFile;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCGeocodeFile.PCFindResult;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord.StrField;
import com.opendoorlogistics.core.CommandLineInterface;
import com.opendoorlogistics.core.CommandLineInterface.Command;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.Icons;

public class PCGeocoderComponent implements ODLComponent {
	static{
		// register the builder commands lines
		CommandLineInterface.registerCommand(new Command() {
			
			@Override
			public String[] getKeywords() {
				return new String[]{"cp2Geonames"};
			}
			
			@Override
			public String getDescription() {
				return "Convert a directory of CodePoint postcode files to a single Geonames format file. Usage -cp2Geonames inputDirectory outputFileName";
			}
			
			@Override
			public boolean execute(String[] args) {
				if(args.length!=2){
					System.out.println("Expected two arguments");
					return false;
				}
				
				CodePointOpen2GeonamesFormat.process(args[0], args[1]);
				return true;
			}
		});
		
		CommandLineInterface.registerCommand(new Command() {
			
			@Override
			public String[] getKeywords() {
				return new String[]{"buildgdf"};
			}
			
			@Override
			public String getDescription() {
				return "Build one or more postcode geocoding data files from the input Geonames format file. Usage -buildgdf inputfile outputdirectory";
			}
			
			@Override
			public boolean execute(String[] args) {
				if(args.length!=2){
					System.out.println("Expected two arguments");
					return false;
				}				
				try {
					new GDFFileBuilder().buildFromGeonamesFile(args[0], new String[]{}, true, args[1]);
					return true;
				} catch (Exception e) {
					System.out.println(Strings.getExceptionMessagesAsSingleStr(e));
					return false;
				}
			}
		});
	}
	
	@Override
	public String getId() {
		return "com.opendoorlogistics.components.geocode.postcodegeocoder";
	}
	
	@Override
	public String getName() {
		return "Geocode postcodes";
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return PCGeocoderConfig.class;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		ODLDatastoreAlterable< ? extends ODLTableDefinitionAlterable>  ret =api.tables().createDefinitionDs();
		ret.createTable("Postcodes",-1);
		ODLTableDefinitionAlterable dfn = ret.getTableAt(0);
		TableUtils.addColumn(dfn,"Postcode", ODLColumnType.STRING, 0,null,PredefinedTags.POSTCODE);		
		TableUtils.addColumn(dfn,PredefinedTags.LATITUDE, ODLColumnType.DOUBLE, 0,null, PredefinedTags.LATITUDE);
		TableUtils.addColumn(dfn,PredefinedTags.LONGITUDE, ODLColumnType.DOUBLE, 0,null, PredefinedTags.LONGITUDE);
		TableUtils.addColumn(dfn, "MatchInformation", ODLColumnType.STRING, TableFlags.FLAG_IS_OPTIONAL, "Information on the match or reason for failure");
		//TableUtils.addColumn(dfn, name, type, flags, description, tags)
		dfn.addColumn(-1,"IsMatched", ODLColumnType.STRING, TableFlags.FLAG_IS_OPTIONAL);	
		
		for(StrField fld : StrField.values()){
			if(TableUtils.addColumn(dfn, "Matched"+fld.getDisplayText(), ODLColumnType.STRING, TableFlags.FLAG_IS_OPTIONAL	,null, fld.getTags())==-1){
				throw new RuntimeException();
			}
			
		}
		return ret;
	}


	@Override
	public ODLDatastore<ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		return null;
	}

	private boolean isStrictRejection(PCFindResult result, PCGeocoderConfig pgc, String [] reason){
		if(pgc.isStrictMatch()){
			if(result.getList().size()>1){
				reason[0] = "Rejected as strict matching is on and matched to more than one postcode record.";
				return true;
			}
			
			if(result.getLevel() < pgc.getMinimumLevel()){
				reason[0] = "Rejected as matched to a postcode less than the minimum level.";
				return true;	
			}
		}
		return false;
	}
	
	@Override
	public void execute(ComponentExecutionApi api,int mode,Object configuration, ODLDatastore<? extends ODLTable> input, ODLDatastoreAlterable<? extends  ODLTableAlterable> output) {
		final PCGeocoderConfig pgc = (PCGeocoderConfig)configuration;
		
		// get the file, if its not absolute then assume its in the installation directory
		File file = PCConstants.resolvePostcodeFile(api.getApi(), new File(pgc.geocoderDbFilename));

		PCGeocodeFile pc = new PCGeocodeFile(file);
		
		class Counter{
			int nbMatched=0;
			int nbUnmatched=0;
			int nbSkipped=0;
			int nr;
			int nbRejectedStrictMatch=0;
			int nbIncorrectFormat=0;
			int nbNoMatchesFound=0;
		}
		final Counter counter = new Counter();
		final Country country = pc.getCountry();
		
		try {
			ODLTable table = input.getTableAt(0);
			counter.nr = table.getRowCount();

			for(int row =0 ; row < counter.nr ; row++){
				
				// check whether to skip
				if(pgc.isSkipAlreadyGeocodedRecords() && table.getValueAt(row, 1)!=null && table.getValueAt(row, 2)!=null){
					counter.nbSkipped++;
					continue;
				}
				
				String pcValue = (String)table.getValueAt(row, 0);
				
				PCRecord matched=null;
				String []reason=new String[1];
				if(pcValue!=null){
					PCFindResult result = pc.find(pcValue);
					List<PCRecord> list = result.getList();
					if(list!=null && list.size()>0){
						
						if(isStrictRejection(result, pgc, reason)){
							matched = null;
							counter.nbRejectedStrictMatch++;
						}else{
							// merge matched results
							matched = PCRecord.merge(list);
							counter.nbMatched++;
							int col=1;
							table.setValueAt(matched.getLatitude(), row, col++);
							table.setValueAt(matched.getLongitude(), row, col++);
							
							if(list.size()>1){
								table.setValueAt("Matched to " + list.size() + " postcodes in level " + result.getLevel(), row, col++);							
							}
							else{
								table.setValueAt("Matched to postcode " +  matched.getField(StrField.POSTAL_CODE) + " in level " + result.getLevel(), row, col++);
							}
							
							table.setValueAt("1", row, col++);
							for(StrField fld  :StrField.values()){
								table.setValueAt(matched.getField(fld), row, col++);			
							}	
						}


					}else{
						switch (result.getType()) {
						case INVALID_FORMAT:
							reason[0] = "Could not identify input postcode format";
							counter.nbIncorrectFormat++;
							break;

						
						case NO_MATCHES:
							reason[0] = "Input postcode format was correct but no matches found";
							counter.nbNoMatchesFound++;
							break;
							
						default:
							// this should never be called...
							throw new RuntimeException();
						}
					}
				}else{
					reason[0] = "No input postcode provided";
				}
				
				// blank if not matched and record reason
				if(matched==null){
					counter.nbUnmatched++;
					int col=1;
					
					// set lat and long values to null
					table.setValueAt(null, row, col++);
					table.setValueAt(null, row, col++);
					
					table.setValueAt(reason[0], row, col++);
					
					// set isMatched to 0
					table.setValueAt("0", row, col++);
					
					// blank the string fields
					for(int i =0 ; i < StrField.values().length ; i++){
						table.setValueAt(null, row, col++);			
					}				
				}
			}
	
		} 
		finally{
			pc.close();			
		}
	
		// show results summary after script has run
		if(pgc.isShowSummary()){
			api.submitControlLauncher(new ControlLauncherCallback() {
				
				@Override
				public void launchControls(ComponentControlLauncherApi launcherApi) {
					// build up text
					StringBuilder builder = new StringBuilder();
					builder.append("PC Geocoder using file: " + pgc.getGeocoderDbFilename() + System.lineSeparator());
					if(country!=null){
						builder.append("Country: " + country.getName());
					}
					builder.append(System.lineSeparator());
					builder.append("Number of records skipped: " + counter.nbSkipped + System.lineSeparator());
					builder.append("Number of records processed: " + (counter.nr-counter.nbSkipped) + System.lineSeparator());
					builder.append("Number of records matched: " + counter.nbMatched + System.lineSeparator());
					builder.append("Number of records unmatched: " + counter.nbUnmatched + System.lineSeparator());
					if(pgc.isStrictMatch()){
						builder.append("Number of records unmatched due to strict matching: " + counter.nbRejectedStrictMatch + System.lineSeparator());						
					}
					builder.append("Number of records unmatched due to incorrect postcode format: " + counter.nbIncorrectFormat + System.lineSeparator());						
					builder.append("Number of records unmatched due to no matches found: " + counter.nbNoMatchesFound + System.lineSeparator());						

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
					launcherApi.registerPanel("PCSummary",null, panel, false);
				}
			});
			

		}
	}
	
	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory,int mode,Serializable config, boolean isFixedIO) {
		return new PCGeocoderConfigPanel(factory.getApi(),(PCGeocoderConfig)config);
	}

	@Override
	public long getFlags(ODLApi api,int mode) {
		return 0;
	}

//	@Override
//	public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
//		return Arrays.asList(
//				new ODLWizardTemplateConfig(getName(), getName(), "Batch geocoding of a table using postcode/zipcode available from www.geonames.org.", new PCDatabaseSelectionConfig())
//
//			//	new ODLWizardTemplateConfig("GB" + getName(), "GB" + getName(), "Batch geocoding of a table using postcode/zipcode available from www.geonames.org.", new PCDatabaseSelectionConfig("gb.gdf"))
//
//				);
//	}
	

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate(getName(), getName(), "Batch geocoding of a table using postcode/zipcode available from www.geonames.org.",
				getIODsDefinition(templatesApi.getApi(), new PCGeocoderConfig()),	
				new PCGeocoderConfig());
	}


	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return Icons.loadFromStandardPath("postcode-geocode.png");
	}
	
	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}
}
