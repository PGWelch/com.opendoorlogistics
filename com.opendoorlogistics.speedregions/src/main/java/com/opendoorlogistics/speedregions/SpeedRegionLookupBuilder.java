/*
 * Copyright 2016 Open Door Logistics Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opendoorlogistics.speedregions;

import java.io.File;
import java.util.List;
import java.util.TreeMap;

import com.opendoorlogistics.speedregions.SpeedRegionLookup.SpeedRuleLookup;
import com.opendoorlogistics.speedregions.beans.RegionLookupBean;
import com.opendoorlogistics.speedregions.beans.SpeedRule;
import com.opendoorlogistics.speedregions.beans.SpeedRules;
import com.opendoorlogistics.speedregions.processor.QueryProcessor;
import com.opendoorlogistics.speedregions.processor.RegionProcessorUtils;
import com.opendoorlogistics.speedregions.processor.SpeedRulesFilesProcesser;
import com.vividsolutions.jts.geom.Geometry;

public class SpeedRegionLookupBuilder {
	public static final double DEFAULT_MIN_CELL_LENGTH_METRES = 10;

	public static SpeedRegionLookup buildFromSpeedRulesFiles(List<File> files ,double minDiagonalLengthMetres){
		return convertFromBean(SpeedRegionBeanBuilder.buildBeanFromSpeedRulesFiles(files,minDiagonalLengthMetres));
	}

	public static SpeedRegionLookup buildFromSpeedRulesObjs(List<SpeedRules> files ,double minDiagonalLengthMetres){
		RegionLookupBean built = SpeedRegionBeanBuilder.buildBeanFromSpeedRulesObjs(files, minDiagonalLengthMetres);
		return convertFromBean( built);
	}



	public static SpeedRegionLookup loadFromBeanFile(File built) {
		return convertFromBean(RegionProcessorUtils.fromJSON(built, RegionLookupBean.class));
	}
	
	public static SpeedRegionLookup convertFromBean(RegionLookupBean built) {
		
		SpeedRulesFilesProcesser processer = new SpeedRulesFilesProcesser();
		final TreeMap<String, TreeMap<String, SpeedRule>> rulesMap =processer.createRulesMap(built.getRules());
				
		final QueryProcessor queryProcessor = new QueryProcessor(RegionProcessorUtils.newGeomFactory(), built.getQuadtree());
		return new SpeedRegionLookup() {
			
			public String findRegionId(Geometry edge) {
				String regionId =queryProcessor.query(edge);
				return regionId;
			}
			
			public SpeedRuleLookup createLookupForEncoder(String encoder) {
				return createRulesLookupForEncoder(rulesMap, encoder);
			}
		};
	}

	private static SpeedRuleLookup createRulesLookupForEncoder(final TreeMap<String, TreeMap<String, SpeedRule>> rulesMap, String encoder) {
		TreeMap<String, SpeedRule> map = rulesMap.get(RegionProcessorUtils.stdString(encoder));
		if(map==null){
			map = new TreeMap<String, SpeedRule>();
		}
		final TreeMap<String, SpeedRule> finalMap = map;
		return new SpeedRuleLookup(){

			public SpeedRule getSpeedRule(String standardisedRegionId) {
				return finalMap.get(standardisedRegionId);
			};
		
		};
	}
	

}
