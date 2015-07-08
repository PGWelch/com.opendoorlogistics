/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Lesser Public License v2.1
 *  which accompanies this distribution, and is available at
 *  http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes.builder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.opendoorlogistics.components.geocode.postcodes.impl.CountryConfigs;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCConstants;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord.StrField;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCSerialiser;
import com.opendoorlogistics.core.utils.KeyboardInput;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class GDFFileBuilder {
	private boolean log=true;
	
	private static class CountryRec{
		private CountryRec(String countryCode) {
			this.countryProcessor = CountryConfigs.getProcessor(countryCode);
			this.countryCode = countryCode;
		}
		final CountryConfigs.CountryProcessor countryProcessor;
		final String countryCode;
		
		DB db = null;
		String indexFilename;
		String tmpIndexFilename;
		HashMap<String, Integer> strToInt=  new HashMap<>();
		Map<Integer, String> intToStr;
		List<Map<String, byte[]>> pcMaps = new ArrayList<>();
		
		void addStrings(PCRecord rec){
			for (PCRecord.StrField fld : PCRecord.StrField.values()) {
				String s = rec.getField(fld);
				if(s!=null){
					addString(s);					
				}
			}			
		}

		private void addString(String s) {
			Integer id = strToInt.get(s);
			if (id == null) {
				id = strToInt.size();
				strToInt.put(s, id);
				intToStr.put(id, s);
			}
		}
	}

	private void finishProcessing(CountryRec rec, boolean compact) {
		
		// create final file
		CountryRec finalRec = new CountryRec(rec.countryCode);
		createEmptyDb(new File(rec.indexFilename),finalRec);

		// copy strings into new file
		for(Map.Entry<Integer, String> entry : rec.intToStr.entrySet()){
			finalRec.intToStr.put(entry.getKey(), entry.getValue());
		}
		
		// copy and merge into new file
		assert rec.pcMaps.size() == finalRec.pcMaps.size();
		assert rec.pcMaps.size() == finalRec.countryProcessor.nbLevels();
		for(int i =0 ; i < rec.pcMaps.size() ;i++){
			for(Map.Entry<String,byte[]> entry : rec.pcMaps.get(i).entrySet()){
				
				// merge all entries
				List<PCRecord> list = PCSerialiser.multiDeserialise(entry.getValue(),rec.intToStr);
				PCRecord merged = PCRecord.merge(list);
				merged.setField(StrField.POSTAL_CODE, entry.getKey().toUpperCase());
				
				// convert merged to bytes
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(bytes);
				PCSerialiser.serialize(merged, rec.strToInt, dos);
				byte[] byteArray = bytes.toByteArray();
				
				// save bytes
				finalRec.pcMaps.get(i).put(entry.getKey(), byteArray);
			}
		}
		
		// close and delete temporary
		if(rec.db!=null){
			rec.db.close();			
		}
		new File(rec.tmpIndexFilename).delete();
		new File(rec.tmpIndexFilename + ".p").delete();
		
		
		finalRec.db.commit();
		finalRec.db.close();
		
		// compact and close the final file
		if(compact){
			// try sleeping for a bit as we're getting file lock issues
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			log("Compacting " + rec.indexFilename);

			finalRec.db = DBMaker.newFileDB(new File(rec.indexFilename)).make();
			finalRec.db.compact();
			finalRec.db.commit();
			finalRec.db.close();
		}
	}
	

	/**
	 * Build the binary lookup file(s) from the geonames postcode files
	 * 
	 * @param geonamesFile
	 * @param outputdirectory
	 */
	public void buildFromGeonamesFile(String geonamesFile,String[] ignoreCountries,boolean doCompact, String outputdirectory) {
		// get ignore set
		TreeSet<String> ignoreSet = new TreeSet<>();
		if(ignoreCountries!=null){
			for(String country : ignoreCountries){
				country = Strings.std(country);
				ignoreSet.add(country);
			}
		}
		
		// assume file sorted by country
		HashSet<String> processedCountries = new HashSet<>();
		String currentCountry = null;

		Scanner scanner = null;
		CountryRec countryRec = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		try {
			int lineNb = 0;
			int skipped = 0;
			scanner = new Scanner(new File(geonamesFile), "UTF-8");

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				// split, including trailing and empty tabs....
				String[] split = line.split("\t", -1);
				if (split.length < 11) {
					throw new RuntimeException("Invalid geonames record on line " + lineNb + " : " + line);
				}

				// get the country and check for new country
				String cleanedCountry = Strings.std(split[0]);
				if (cleanedCountry.equals(currentCountry) == false) {
					
					// close old file
					if (countryRec != null) {
						finishProcessing(countryRec,doCompact);
					}
					
					// check if we should skip this country, if not then init the record
					countryRec=null;					
					if(ignoreSet.contains(cleanedCountry)==false){
						countryRec = initCountry(cleanedCountry, processedCountries, outputdirectory);						
					}
					
					currentCountry = cleanedCountry;
				}

				// skip current country if flagged
				if(countryRec==null){
					lineNb++;
					continue;
				}
				
				// create the pc object 
				PCRecord pcRecord = createPCRecObject(split, lineNb);
				if(pcRecord!=null){
					pcRecord = countryRec.countryProcessor.processRecord(pcRecord);	
					if(pcRecord==null){
						log("Country specific processing rejected line " + lineNb + " : " + Strings.toCommas(split));						
					}
				}
				
				if(pcRecord==null){
					skipped++;
				}
				
				// save the strings
				if (pcRecord != null) {
					countryRec.addStrings(pcRecord);	
				}

				// save to the map file for the different levels
				if (pcRecord != null) {
					List<String> levels = countryRec.countryProcessor.splitByLevels(pcRecord.getField(StrField.POSTAL_CODE),false); 
					if(levels==null){
						log("Found problem row at line " + lineNb + " : " + Strings.toCommas(split));	
						skipped++;
					}else{
						for(int i =0 ; i < levels.size() ; i++){
							// save to the map and also save the string as an int
							saveToPCMap(levels.get(i), pcRecord, countryRec.strToInt,baos, countryRec.pcMaps.get(i));
							countryRec.addString(levels.get(i));
						}
					}
				}

				// go to next line
				lineNb++;
				
				if(lineNb%25000==0){
					log("Processed " + lineNb + " lines in file " + geonamesFile);
				}

			}
			log(lineNb, skipped);

		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			
			if (countryRec!=null && countryRec.db != null) {
				finishProcessing(countryRec,doCompact);
			}

			if (scanner != null) {
				scanner.close();
			}
		}

		log("Finished building GDF files");
	}

	private void saveToPCMap(String pc, PCRecord pcRecord, Map<String, Integer> strToInt, ByteArrayOutputStream bytes ,Map<String, byte[]>outMap) throws IOException {
		// always standardise the key
		pc =  Strings.std(pc);
		
		// get any pre-existing record and save it to the output stream
		bytes.reset();
		byte[] current = outMap.get(pc);
		if (current != null) {
			bytes.write(current);
		}

		// serialise the current postcode and add to the bytes stream
		DataOutputStream dos = new DataOutputStream(bytes);
		PCSerialiser.serialize(pcRecord, strToInt, dos);

		// save the final bytes stream
		byte[] byteArray = bytes.toByteArray();
		outMap.put(pc, byteArray);
	}

	private CountryRec initCountry(String newCountry, HashSet<String> processedCountries, String outputdirectory) {

		// update the current name and that we've started processing  this country
		if (processedCountries.contains(newCountry)) {
			throw new RuntimeException("Input file is not ordered by country");
		}
		processedCountries.add(newCountry);

		// create new db and other objects
		CountryRec ret = new CountryRec(newCountry);
		ret.indexFilename = outputdirectory + File.separator + newCountry + "." + PCConstants.DBFILE_EXTENSION;
		ret.tmpIndexFilename = ret.indexFilename + PCConstants.TEMP_FILE_EXTENSION;
		createEmptyDb(new File(ret.tmpIndexFilename), ret);
		log("Started processing " + newCountry + ", country has " + ret.pcMaps.size() + " postcode levels defined.");						
		
		return ret;
	}

	private PCRecord createPCRecObject(String[] split, int lineNb) {
		PCRecord pc = new PCRecord();
		for (PCRecord.StrField fld : PCRecord.StrField.values()) {
			pc.setField(fld, split[fld.ordinal()]);
		}
		int indx = PCRecord.StrField.values().length;
		if (split[indx].length() > 0 && split[indx + 1].length() > 0) {
			pc.setLatitude(new BigDecimal(split[indx++]));
			pc.setLongitude(new BigDecimal(split[indx++]));
		} else {
			// skip this record as has no geocode
			log("Found row with no geocodes, line " + lineNb + " : " + Strings.toCommas(split));
			return null;
		}

		pc.setAccuracy((short) -1);
		if (indx < split.length) {
			String accuracy = split[indx];
			if (accuracy.length() > 0) {
				pc.setAccuracy(Short.parseShort(accuracy));
			}
		}

		return pc;
	}

	private void createEmptyDb(File outFile,CountryRec rec) {
		rec.db= DBMaker.newFileDB(outFile).closeOnJvmShutdown().transactionDisable().cacheSize(1000000).make();
		
		// save the version
		rec.db.createAtomicString(PCConstants.DBNAME_VERSION, PCConstants.pcgeocode_file_version.toString());
		
		// save the country code
		rec.db.createAtomicString(PCConstants.DBNAME_COUNTRYCODE, rec.countryCode);
		
		// create the string map, ensuring we always have empty string as this is used when merging
		rec.intToStr = rec.db.getHashMap(PCConstants.DBNAME_INT2ST);
		rec.addString("");
		
		// create a pc map for each level
		for(int i = 0 ; i < rec.countryProcessor.nbLevels() ; i++){
			Map<String,byte[]> map = rec.db.getHashMap(PCConstants.DBNAME_PCS+ Integer.toString(i));
			rec.pcMaps.add(map);
		}
	}

	private void log(int lineNb, int skipped) {
		log("Processed " + lineNb + " rows" + ", skipped " + skipped + " rows");
	}

	private void log(String s) {
		if(log){
			System.out.println(s);			
		}
	}

//	private void wipeDirectory(String directory) {
//		if (KeyboardInput.yesNoPrompt("Delete all files from directory \"" + directory + "\" (recommended)?", true)) {
//			File file = new File(directory);
//			if (!file.isDirectory()) {
//				throw new RuntimeException("Not a valid directory:" + directory);
//			}
//			for (File child : file.listFiles()) {
//				if (child.isDirectory() == false) {
//					child.delete();
//				}
//			}
//		}
//	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}


}
