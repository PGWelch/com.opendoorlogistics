/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes.impl;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.components.geocode.Countries;
import com.opendoorlogistics.components.geocode.Countries.Country;
import com.opendoorlogistics.core.utils.LargeList;
import com.opendoorlogistics.core.utils.Version;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class PCGeocodeFile {
	private final DB db;
	private final String countryCode;
	private final CountryConfigs.CountryProcessor processor;
	private final Version version;

	public interface ParseLevelDetailsCB{
		void parseLevel(String country, String code, int level, long postcodeCount, String examples);
	}
	
	// private static final boolean DEBUG_PRINT_KEYS = false;

	public PCGeocodeFile(File file) {
		db = DBMaker.newFileDB(file).readOnly().closeOnJvmShutdown().transactionDisable().make();

		// load the version
		version = new Version(db.getAtomicString(PCConstants.DBNAME_VERSION).get());

		// load the country code
		countryCode = db.getAtomicString(PCConstants.DBNAME_COUNTRYCODE).get();
		processor = CountryConfigs.getProcessor(countryCode);
		if (processor == null) {
			throw new RuntimeException("Cannot find country processor for country " + countryCode);
		}

		// if(DEBUG_PRINT_KEYS){
		// Map<String,byte [] > pcMap = getPcMap();
		// for(String key : pcMap.keySet()){
		// System.out.println(key);
		// }
		// }
	}

	public void close() {
		db.close();
	}

	public String getDescription() {
		final StringBuilder builder = new StringBuilder();
		Country country = getCountry();
		if (country != null) {
			builder.append("Country: " + country.getName());
		}

		if (builder.length() > 0) {
			builder.append("  ");
		}
		builder.append("Code: " + countryCode.toUpperCase());
		builder.append(System.lineSeparator());

		parseLevels(new ParseLevelDetailsCB() {
			
			@Override
			public void parseLevel(String country, String code, int level, long postcodeCount, String examples) {
				builder.append("Postcode level " + level + " has " + NumberFormat.getIntegerInstance().format(postcodeCount) + " postcodes");
				builder.append(", examples (");
				builder.append(examples);
				builder.append(")");
				builder.append(System.lineSeparator());
				
			}
		});
		
//		for (Map.Entry<String, Object> entry : db.getAll().entrySet()) {
//
//			if (entry.getKey().startsWith(PCConstants.DBNAME_PCS)) {
//				Map<String, byte[]> map = (Map<String, byte[]>) entry.getValue();
//				Integer level = Integer.parseInt(entry.getKey().substring(PCConstants.DBNAME_PCS.length()));
//				builder.append("Postcode level " + level + " has " + NumberFormat.getIntegerInstance().format(map.size()) + " postcodes");
//				
//				int exampleCount=0;
//				builder.append(", examples (");
//				for(String s : map.keySet()){
//					if(exampleCount>0){
//						builder.append(", ");
//					}
//					builder.append(s.toUpperCase());
//					exampleCount++;
//					if(exampleCount==3){
//						break;
//					}
//				}
//				builder.append(")");
//				builder.append(System.lineSeparator());
//			}
//		}
		return builder.toString();
	}

	/**
	 * Parse the levels getting details of each one
	 * @param cb
	 */
	public void parseLevels(ParseLevelDetailsCB cb){
		Country country = getCountry();

		for (Map.Entry<String, Object> entry : db.getAll().entrySet()) {

			if (entry.getKey().startsWith(PCConstants.DBNAME_PCS)) {
				Map<String, byte[]> map = (Map<String, byte[]>) entry.getValue();
				Integer level = Integer.parseInt(entry.getKey().substring(PCConstants.DBNAME_PCS.length()));
				int size = map.size();
			//	builder.append("Postcode level " + level + " has " + NumberFormat.getIntegerInstance().format(map.size()) + " postcodes");
				
				int exampleCount=0;
				StringBuilder builder = new StringBuilder();
				for(String s : map.keySet()){
					if(exampleCount>0){
						builder.append(", ");
					}
					builder.append(s.toUpperCase());
					exampleCount++;
					if(exampleCount==3){
						break;
					}
				}

				cb.parseLevel(country.getName(), country.getTwoDigitCode().toUpperCase(), level, size, builder.toString());
			}
		}
	}
	
	public Country getCountry() {
		Country country = Countries.findBy2DigitCode(countryCode);
		return country;
	}
	
	public static class PCFindResult{
		private final List<PCRecord> list;
		private final PCFindResultType type;
		private final int level;
		
		public PCFindResult(List<PCRecord> list, PCFindResultType type, int level) {
			super();
			this.list = list;
			this.type = type;
			this.level = level;
		}

		public List<PCRecord> getList() {
			return list;
		}

		public PCFindResultType getType() {
			return type;
		}
		
		public int getLevel(){
			return level;
		}
	}

	public static enum PCFindResultType{
		FOUND,
		INVALID_FORMAT,
		NO_MATCHES
	}
	
	/**
	 * Find the postcode record
	 * 
	 * @param isoCountryCode
	 * @param postcode
	 * @return
	 */
	public PCFindResult find(String postcode) {
		// try splitting input
		postcode = processor.standardisePostcode(postcode);
		List<String> split = processor.splitByLevels(postcode, true);
		assert split == null || split.size() == processor.nbLevels();

		if(split == null){
			return new PCFindResult(new ArrayList<PCRecord>() , PCFindResultType.INVALID_FORMAT,-1);
		}
		
		// try highest (most accurate) resolution first
		for (int level = processor.nbLevels() - 1; level >= 0; level--) {

			Map<String, byte[]> pcMap = getPCMap(level);
			Map<Integer, String> intToStr = db.get(PCConstants.DBNAME_INT2ST);

			// get the binary record. try full postcode first (in case user has entered a partial postcode e.g. GL51)
			String cleanedPC = Strings.std(postcode);
			byte[] bytes = pcMap.get(cleanedPC);

			// then try the country-specific split (in case user has entered a corrupt or unknown end of postcode, e.g. GL51 XXX)
			if (bytes == null && split != null && level < processor.nbLevels() - 1 && split.get(level) != null) {
				cleanedPC = Strings.std(split.get(level));
				bytes = pcMap.get(cleanedPC);
			}

			if (bytes == null) {
				continue;
			}

			// deserialise
			List<PCRecord> ret = PCSerialiser.multiDeserialise(bytes, intToStr);
			return new PCFindResult(ret, PCFindResultType.FOUND, level);
		}

		return new PCFindResult(new ArrayList<PCRecord>() , PCFindResultType.NO_MATCHES,-1);
	}

	private Map<String, byte[]> getPCMap(int level) {
		Map<String, byte[]> pcMap = db.get(PCConstants.DBNAME_PCS + Integer.toString(level));
		return pcMap;
	}

	public List<PCRecord> getPostcodes(int level, ComponentExecutionApi reporter) {
		Map<String, byte[]> pcMap = getPCMap(level);
		LargeList<PCRecord> ret = new LargeList<>(pcMap.size());
		int nbParsed = 0;
		if (pcMap != null) {
			Map<Integer, String> intToStr = db.get(PCConstants.DBNAME_INT2ST);
			for (byte[] bytes : pcMap.values()) {
				if (reporter.isCancelled() || reporter.isFinishNow()) {
					return ret;
				}

				ret.addAll(PCSerialiser.multiDeserialise(bytes, intToStr));
				if (nbParsed % 10000 == 0) {
					reporter.postStatusMessage("Loaded " + nbParsed + " postcodes.");
				}
				nbParsed++;
			}
		}
		return ret;
	}

//	public PCRecord findFirst(String postcode) {
//		List<PCRecord> list = find(postcode);
//		if (list.size() > 0) {
//			return list.get(0);
//		}
//		return null;
//	}
//
//	public String getFindFirstSummary(String postcode) {
//		PCRecord rec = findFirst(postcode);
//		if (rec == null) {
//			return "Postcode \"" + postcode + "\" did not match to anything.";
//		} else {
//			return "Postcode \"" + postcode + "\" matched: " + rec.toString();
//		}
//	}

	public static void main(String[] args) {
		//File file = new File("C:\\Users\\Phil\\Dropbox\\Personal\\Business\\Data\\output\\pt.gdf");
		for(File file : new File("C:\\Users\\Phil\\Dropbox\\Personal\\Business\\Data\\output\\").listFiles()){
			if(file.getAbsolutePath().toLowerCase().endsWith(".gdf")){
				PCGeocodeFile pcf = new PCGeocodeFile(file);
				System.out.println(pcf.getDescription());		
				System.out.println();
			}
		}
	}
}
