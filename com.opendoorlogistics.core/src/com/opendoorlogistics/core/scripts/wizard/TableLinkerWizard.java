/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.codefromweb.LevenshteinDistance;
import com.opendoorlogistics.codefromweb.LongestCommonSubstring;
import com.opendoorlogistics.core.geometry.functions.FmLatitude;
import com.opendoorlogistics.core.geometry.functions.FmLongitude;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.scripts.formulae.FmRowId;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class TableLinkerWizard {
	public static final long FLAG_USE_ROWID_FOR_LOCATION_KEY = 1 << 0;

	public static AdaptedTableConfig createBestGuess(ODLTableDefinition source, ODLTableDefinition target) {
		return createBestGuessWithScores(source, target, 0).getFirst();
	}

	public static AdaptedTableConfig createBestGuess(ODLTableDefinition source, ODLTableDefinition target, long flags) {
		return createBestGuessWithScores(source, target, flags).getFirst();
	}

	private static Pair<AdaptedTableConfig, List<List<MatchScore>>> createBestGuessWithScores(ODLTableDefinition source, ODLTableDefinition target,
			long flags) {
		// get scores
		List<List<MatchScore>> allScores = scoreMatches(source, target);

		// create unassigned adapter for the table
		AdaptedTableConfig tc = new AdaptedTableConfig();
		tc.setFromTable(source != null ? source.getName() : "");
		tc.setName(target.getName());
		tc.setFlags(target.getFlags());
		DatastoreCopier.copyTableDefinition(target, tc);

		// use scores first
		boolean matchedGeom=false;
		for (int col = 0; col < tc.getColumnCount(); col++) {
			
			// get top scoring match
			List<MatchScore> scores = allScores.get(col);
			MatchScore ms = null;
			if (scores.size() > 0) {
				ms = scores.get(0);
			}

			AdapterColumnConfig config = tc.getColumn(col);

			// Priority 1 - use the top match if we get a field name (turn off tags as creating problems)
			if (ms != null && ms.isFieldNameMatch() ) {
				config.setFrom(source.getColumnName(ms.getSourceColumnIndex()));
				continue;				
			} 
			
			// Priority 2 - match colours, images, geometries to first found as they are unusual fields
			for(ODLColumnType type : new ODLColumnType[]{ODLColumnType.GEOM,ODLColumnType.IMAGE,ODLColumnType.COLOUR}){
				if(tc.getColumnType(col) ==type){
					for(int srcCol=0; source!=null && srcCol < source.getColumnCount(); srcCol++){
						if(source.getColumnType(srcCol) == type){
							config.setFrom(source.getColumnName(srcCol));
							
							if(type ==ODLColumnType.GEOM){
								matchedGeom = true;
							}
							break;
						}
					}
				}				
			}
		}

		
		for (int col = 0; col < tc.getColumnCount(); col++) {

			// Skip config if already filled
			AdapterColumnConfig config = tc.getColumn(col);
			if(!Strings.isEmpty(config.getFrom()) || !Strings.isEmpty(config.getFormula())){
				continue;
			}
			
			// Priority 3 - check for case where we have a geometry and we want a lat or long, unless we've already matched a geom to another field
			if(!matchedGeom){
			boolean targetIsLat = TagUtils.hasTag(PredefinedTags.LATITUDE, target, col);
			boolean targetIsLng = TagUtils.hasTag(PredefinedTags.LONGITUDE, target, col);
			if((targetIsLat || targetIsLng) && source!=null){
				boolean isSet=false;
				for(int srcCol=0;srcCol < source.getColumnCount(); srcCol++){
					if(source.getColumnType(srcCol)==ODLColumnType.GEOM){
						FmLocalElement local = new FmLocalElement(-1, source.getColumnName(srcCol));
						String formula;
						if(targetIsLat){
							formula = new FmLatitude(local).toString();
						}else{
							formula = new FmLongitude(local).toString();							
						}
						config.setUseFormula(true);
						config.setFormula(formula);
						isSet = true;
						break;
					}
				}
				if(isSet){
					continue;
				}
			}
			}
			
			// Priority 4 - if flagged use default formula for location key
			if ((flags & FLAG_USE_ROWID_FOR_LOCATION_KEY) == FLAG_USE_ROWID_FOR_LOCATION_KEY
					&& TagUtils.hasTag(PredefinedTags.LOCATION_KEY, target, col)) {				
				config.setUseFormula(true);
				config.setFormula(new FmRowId().toString());
				continue;								
			}
			
			// Priority 5 - set up a formula using the default value... use the canonical string representation
			if(target.getColumnDefaultValue(col)!=null && target.getColumnType(col)!=ODLColumnType.IMAGE){
				Object val = ColumnValueProcessor.convertToMe(ODLColumnType.STRING,target.getColumnDefaultValue(col), target.getColumnType(col)); 
				if(val!=null){
					String sval = val.toString();
					if(!ColumnValueProcessor.isNumeric(target.getColumnType(col))){
						sval = "\"" + sval + "\"";
					}
					config.setUseFormula(true);
					config.setFormula(sval);
				}
				continue;
			}
		
			// Turn off fuzzy matching; it often creates more problems than it solves...
//			// use fuzzy matching if column is required and nothing else worked 
//			if (ms != null && (isRequired && ms.getFieldNameLongestCommonSubstring() >= 3)) {
//				config.setFrom(source.getColumnName(ms.getSourceColumnIndex()));
//				continue;
//			}


		}

		// set the datastore to external by default
		tc.setFromDatastore(ScriptConstants.EXTERNAL_DS_NAME);
		
//		// Do special processing for the drawables table...
//		if ( target != null && TagUtils.hasTag(PredefinedTags.DRAWABLES, target)) {
//			// blank lat long if have geometry
//			boolean hasGeom = false;
//			for (AdapterColumnConfig column : tc.getColumns()) {
//				if (column.getType() == ODLColumnType.GEOM) {
//					if (Strings.isEmpty(column.getFrom()) == false) {
//						hasGeom = true;
//					}
//				}
//			}
//
//			// if we have geometry then wipe the lat / long fields as geom take priority
//			if (hasGeom) {
//				int nc = tc.getColumnCount();
//				for (int col = 0; col < nc; col++) {
//					for (String tag : new String[] { PredefinedTags.LATITUDE, PredefinedTags.LONGITUDE }) {
//						if (TagUtils.hasTag(tag, tc, col)) {
//							AdapterColumnConfig colObj = tc.getColumn(col);
//							colObj.setFormula(null);
//							colObj.setFrom(null);
//							colObj.setUseFormula(false);
//
//						}
//					}
//				}
//			}
//		}

		return new Pair<AdaptedTableConfig, List<List<MatchScore>>>(tc, allScores);
	}

	private static List<List<MatchScore>> scoreMatches(ODLTableDefinition source, ODLTableDefinition target) {
		List<List<MatchScore>> allScores = new ArrayList<>();
		for (int j = 0; j < target.getColumnCount(); j++) {
			if (source != null) {
				ArrayList<MatchScore> scores = new ArrayList<>();
				for (int k = 0; k < source.getColumnCount(); k++) {

					// check for exact fieldname match
					String targetName = target.getColumnName(j);
					String sourceName = source.getColumnName(k);
					MatchScore ms = new MatchScore(sourceName,targetName,k);
					if (Strings.equalsStd(targetName, sourceName)) {
						ms.fieldNameMatch = true;
					}

					// check for matched tags
					ms.nbMatchedTags = TagUtils.countCommonColumnTags(source, k, target, j);

					// get longest common substring
					ms.fieldNameLongestCommonSubstring = LongestCommonSubstring.longestSubstr(sourceName, targetName);

					// edit distance
					ms.editDistance = LevenshteinDistance.getLevenshteinDistance(sourceName, targetName);
					
					scores.add(ms);
				}

				Collections.sort(scores);
				allScores.add(scores);

			} else {
				allScores.add(new ArrayList<MatchScore>());
			}
		}
		return allScores;
	}

	public static class MatchScore implements Comparable<MatchScore> {
		final String sourceFieldName;
		final String destinationFieldName;
		final int sourceColumnIndex;

		public MatchScore(String sourceFieldName, String destinationFieldName, int sourceColumnIndex) {
			super();
			this.sourceFieldName = sourceFieldName;
			this.destinationFieldName = destinationFieldName;
			this.sourceColumnIndex = sourceColumnIndex;
		}

		private boolean fieldNameMatch;
		private int nbMatchedTags;
		private int fieldNameLongestCommonSubstring;
		private int editDistance;
		
		public boolean isFieldNameMatch() {
			return fieldNameMatch;
		}

		public int getNbMatchedTags() {
			return nbMatchedTags;
		}

		public int getFieldNameLongestCommonSubstring() {
			return fieldNameLongestCommonSubstring;
		}

		public int getSourceColumnIndex() {
			return sourceColumnIndex;
		}

		@Override
		public String toString() {
			return sourceFieldName + "->" +  destinationFieldName + ", srcCol="
					+ sourceColumnIndex + ", fieldNameMatch?=" + fieldNameMatch + ", nbMatchedTags=" + nbMatchedTags
					+ ", longestSubstring=" + fieldNameLongestCommonSubstring + ", editDistance=" + editDistance ;
		}

		@Override
		public int compareTo(MatchScore o) {
			// name match first
			if (fieldNameMatch != o.fieldNameMatch) {
				return fieldNameMatch ? -1 : +1;
			}

			// then tags
			if (nbMatchedTags != o.nbMatchedTags) {
				return nbMatchedTags > o.nbMatchedTags ? -1 : +1;
			}

			// then common substring
			if (fieldNameLongestCommonSubstring != o.fieldNameLongestCommonSubstring) {
				return fieldNameLongestCommonSubstring > o.fieldNameLongestCommonSubstring ? -1 : +1;
			}
			
			// then edit distance
			if(editDistance != o.editDistance){
				return editDistance < o.editDistance ? -1 : +1;
			}

			return 0;
		}

	}

	// public static class ScoredTableLink{
	// private final ODLTableDefinition source;
	// private final ODLTableDefinition target;
	// private final ArrayList<ArrayList<Score>> scores = new ArrayList<>();
	//
	// private static class Score{
	// int sourceIndex;
	// double score;
	//
	// Score(int sourceIndex, double score) {
	// this.sourceIndex = sourceIndex;
	// this.score = score;
	// }
	// }
	//
	// public ScoredTableLink(ODLTableDefinition source, ODLTableDefinition target) {
	// this.source = source;
	// this.target = target;
	// for(int i =0 ; i< target.getColumnCount() ; i++){
	// scores.add(new ArrayList<Score>());
	// }
	// }
	//
	// public void add(int targetIndex, int sourceIndex, double score){
	// scores.a
	// }
	// }
}
