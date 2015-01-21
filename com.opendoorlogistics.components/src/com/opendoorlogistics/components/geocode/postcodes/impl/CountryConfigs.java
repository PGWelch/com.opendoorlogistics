/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord.StrField;
import com.opendoorlogistics.core.gis.postcodes.UKPostcodes;
import com.opendoorlogistics.core.gis.postcodes.UKPostcodes.UKPostcodeLevel;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class CountryConfigs {
	private enum HierarchyType{
		NONE(1),
		TWO_LEVEL_HYPHEN(2),
		TWO_LEVEL_SPACE(2),
		GB(4);
		
		private HierarchyType(int nbLevels){
			this.nbLevels= nbLevels;
		}
		
		private final int nbLevels;
		
		int nbLevels(){
			return nbLevels;
		}
	}
	
	public static interface CountryProcessor{
		PCRecord processRecord(PCRecord record);
		
		/**
		 * Split the postcode supplied by geonames into different
		 * hierarchical levels. 
		 * @param pc
		 * @param returnPartial
		 * @return Null if entry not understood what-so-ever OR a list of size nbLevels()
		 * where some entries might be null (e.g. the most detailed geographic level)
		 * if only higher levels are recognised and returnPartial is true
		 */
		List<String> splitByLevels(String pc, boolean returnPartial);
		
		int nbLevels();
		
		String standardisePostcode(String pc);
	}
	
	private static class DefaultProcessor implements CountryProcessor{
		private final HierarchyType hierarchyType;
		
		public DefaultProcessor( HierarchyType hierarchyType) {
			super();
			this.hierarchyType = hierarchyType;
		}


		@Override
		public PCRecord processRecord(PCRecord record) {
			return record;
		}

		@Override
		public List<String> splitByLevels(String pc, boolean returnPartial) {
			switch (hierarchyType) {
			
			case TWO_LEVEL_HYPHEN:
				return twoLevelSplit("-",pc, returnPartial);
				
			case TWO_LEVEL_SPACE:
				return twoLevelSplit(" ",pc, returnPartial);
				
			default:
				ArrayList<String> ret = new ArrayList<>();
				ret.add(pc);				
				return ret;
			}
		}

		private List<String> twoLevelSplit(String separator, String pc, boolean returnPartial) {
			ArrayList<String> ret = new ArrayList<>();			
			pc = Strings.std(pc);

			String [] split = pc.split(separator);
			if(split.length==2){
				ret.add(pc);
				ret.add(0,split[0]);
				return ret;
			}
			
			// There is no point returning a partial as we have no way of knowing
			// which part of the postcode hierarchy the input belongs to
			return null;
		}
		
		@Override
		public int nbLevels(){
			return hierarchyType.nbLevels();
		}


		@Override
		public String standardisePostcode(String pc) {
			return Strings.std(pc).toUpperCase();
		}
	}
	
	private static final CountryProcessor defaultProcessor = new DefaultProcessor(HierarchyType.NONE);

	private static final CountryProcessor twoLevelHyphen = new DefaultProcessor(HierarchyType.TWO_LEVEL_HYPHEN);

	private static final CountryProcessor twoLevelSpace = new DefaultProcessor(HierarchyType.TWO_LEVEL_SPACE);
	

	private static final CountryProcessor france = new DefaultProcessor(HierarchyType.NONE) {
		private Pattern getNumber = Pattern.compile("\\D*(\\d+).*");
		
		@Override
		public PCRecord processRecord(PCRecord record) {
			String pc = record.getField(StrField.POSTAL_CODE);
			pc = Strings.std(pc);
			Matcher matcher = getNumber.matcher(pc);
			if(matcher.matches()==false){
				return null;	
			}
			pc = matcher.group(1);
			record.setField(StrField.POSTAL_CODE, pc);
			return record;
		}

	};
	
	public static class GBProcessor extends DefaultProcessor{
		private final boolean removeExtraDistrictLetter;
		
		public GBProcessor(boolean removeExtraDistrictLetter ) {
			super(HierarchyType.GB);
			this.removeExtraDistrictLetter = removeExtraDistrictLetter;
		}
	
		@Override
		public List<String> splitByLevels(String pc, boolean returnPartial) {
			pc = standardisePostcode(pc);
			UKPostcodeLevel level = identifyLevel(pc);
			if(level==null || (level.compareTo(UKPostcodeLevel.Unit)<0 && returnPartial==false)){
				return null;
			}
			
			ArrayList<String> ret = new ArrayList<>(UKPostcodeLevel.values().length);
			
			for(UKPostcodeLevel fetchLevel : UKPostcodeLevel.values()){
				String fetched=null;
				if(fetchLevel.compareTo(level)<0){
					Pattern pattern=null;
					switch(fetchLevel){
					case Area:
						pattern = UKPostcodes.areaFromAnyLevelPC;
						break;
						
					case District:
						pattern = UKPostcodes.districtFromAnyLevelPC;
						break;
						
					case Sector:
						pattern = UKPostcodes.sectorFromUnit;
						break;
						
					default:
						throw new RuntimeException();
					}
					
					Matcher matcher = pattern.matcher(pc);
					if(!matcher.matches()){
						throw new RuntimeException();
					}
					fetched = matcher.group(1);
				}
				else if(fetchLevel==level){
					fetched = pc;
				}
				
				ret.add(fetched);
			}
			return ret;
		}
		
		/**
		 * Identify the level for a postcode that's already UK-standardised.
		 * @param standardisedPc
		 * @return
		 */
		public UKPostcodeLevel identifyLevel(String standardisedPc){
			standardisedPc = standardisePostcode(standardisedPc);
			
			if(UKPostcodes.isUnit.matcher(standardisedPc).find()){
				return UKPostcodeLevel.Unit;
			}
			
			if(UKPostcodes.isSector.matcher(standardisedPc).find()){
				return UKPostcodeLevel.Sector;
			}
			
			if(UKPostcodes.isDistrict.matcher(standardisedPc).find()){
				return UKPostcodeLevel.District;
			}
			
			if(UKPostcodes.isArea.matcher(standardisedPc).find()){
				return UKPostcodeLevel.Area;
			}
				
			return null;
		}
		
		/**
		 * Standardise a full or partial UK postcode
		 * @param s
		 */
		@Override
		public String standardisePostcode(String s){
			return UKPostcodes.standardisePostcode(s, removeExtraDistrictLetter);
		}
		
	}


	public static void main(String[] args) {
	//	Pattern getNumber = Pattern.compile("^([a-z][a-z]?).*");
		GBProcessor gb = new GBProcessor(true);
		System.out.println(gb.splitByLevels("E1W 1AA", false));
//		Pattern pattern = gb.areaFromAnyLevelStdPC;
//		String s = "ab10 1aa";
//		int hash = s.hashCode();
//		System.out.println(hash);
//		Matcher matcher = pattern.matcher(s);
//		if(matcher.matches()){
//			System.out.println(matcher.group(1));
//		}else{
//			System.out.println("no match");	
//		}
	}
	
	
	private static final GBProcessor gb = new GBProcessor(true);
	
	public static CountryProcessor getProcessor(String countryCode){
		countryCode = Strings.std(countryCode);
		switch(countryCode){
		case "br":
			return twoLevelHyphen;
			
		case "cz":
			return twoLevelSpace;
			
		case "fr":
			return france;
			
		case "jp":
			return twoLevelHyphen;	
		
		case "pl":
			return twoLevelHyphen;	
	
		case "pt":
			return twoLevelHyphen;	
			
		case "sk":
			return twoLevelSpace;
		
		case "gb":
			return gb;
			
		default:
			return defaultProcessor;
			

		}
	}
}
