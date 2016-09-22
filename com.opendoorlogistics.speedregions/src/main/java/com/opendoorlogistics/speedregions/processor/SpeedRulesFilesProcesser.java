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
package com.opendoorlogistics.speedregions.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.MultiPolygon;
import org.geojson.Polygon;

import com.opendoorlogistics.speedregions.beans.QuadtreeNode;
import com.opendoorlogistics.speedregions.beans.SpeedRule;
import com.opendoorlogistics.speedregions.beans.SpeedRules;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class SpeedRulesFilesProcesser {
	private static class TempPolygonRecord implements Comparable<TempPolygonRecord>{
		int fileIndex;
		int positionInFile;
		Polygon polygon;
		String stdRegionId;
		long polyNumber;
		
		TempPolygonRecord(int fileIndex, int positionInFile, Polygon polygon, String stdRegionId, long polyNumber) {
			this.fileIndex = fileIndex;
			this.positionInFile = positionInFile;
			this.polygon = polygon;
			this.stdRegionId = stdRegionId;
			this.polyNumber = polyNumber;
		}

		public int compareTo(TempPolygonRecord o) {
			// first positions in files first
			int diff = Integer.compare(positionInFile, o.positionInFile);
			
			// then files
			if(diff==0){
				diff = Integer.compare(fileIndex, o.fileIndex);
			}
			
			// then global polygon just to ensure we always add to the treeset
			if(diff==0){
				diff = Long.compare(polyNumber, this.polyNumber);
			}
			
			return diff;
		}
		
		
	}

	/**
	 * Build the quadtree from the speed rules files.
	 * All regionId strings in the quadtree are standardised.
	 * @param files
	 * @param geomFactory
	 * @param minSideLengthMetres
	 * @return
	 */
	public QuadtreeNode buildQuadtree(List<SpeedRules> files,GeometryFactory geomFactory,double minSideLengthMetres){

		List<SpeedRule> globalRules = new ArrayList<SpeedRule>();
		
		TreeSet<TempPolygonRecord> prioritised=prioritisePolygons(files, globalRules);
		
		// Now build the quadtree using the global rule id as the 'id' for each polygon.
		// We never compare ids when building the quadtree, only null versus non-nullness, so this is OK
		RegionQuadtreeBuilder builder = new RegionQuadtreeBuilder(geomFactory, minSideLengthMetres);
		for(TempPolygonRecord poly:prioritised){
			// need to turn a geojson polygon into a JTS polygon
			int nLists = poly.polygon.getCoordinates().size();
			if(nLists==0){
				throw new RuntimeException("Invalid polygon");
			}
			int nHoles = Math.max(nLists-1,0);
			LinearRing exterior=null;
			LinearRing [] holes=  new LinearRing[nHoles];
			for(int i =0 ; i<nLists ; i++){
				List<LngLatAlt> list = poly.polygon.getCoordinates().get(i);
				int nc = list.size();
				Coordinate [] coords = new Coordinate[nc];
				for(int j=0; j<nc;j++){
					LngLatAlt ll = list.get(j);
					coords[j] = new Coordinate(ll.getLongitude(), ll.getLatitude());
				}
				LinearRing ring = geomFactory.createLinearRing(coords);
				if(i==0){
					exterior = ring;
				}else{
					holes[i-1] = ring;
				}
			}
			com.vividsolutions.jts.geom.Polygon jtsPolygon=geomFactory.createPolygon(exterior, holes);
			builder.add(jtsPolygon, poly.stdRegionId);
		}
		
		return builder.build();
	}

	/**
	 * Validate the speed rules in all the files and return as a list
	 * @param files
	 * @return
	 */
	public List<SpeedRule> validateRules(List<SpeedRules> files){
		
		// add rules to one big map
		ArrayList<SpeedRule> rules = new  ArrayList<SpeedRule>();
		for(SpeedRules file : files){
			if(file.getRules()!=null){
				rules.addAll(file.getRules());				
			}
		}

		// create rules map will thrown an exception if the rules are invalid
		createRulesMap(rules);

		return rules;
	}
	
	/**
	 * Create a map which lets you lookup the speed rule by encoder first and then by regionid.
	 * All string keys are standardised
	 * @param files
	 * @return
	 */
	public TreeMap<String, TreeMap<String, SpeedRule>> createRulesMap(List<SpeedRule> rules){
		TreeMap<String, TreeMap<String, SpeedRule>>  ret = new TreeMap<String, TreeMap<String,SpeedRule>>();
		for(SpeedRule rule : rules){
			for(String encoder : rule.getFlagEncoders()){
				encoder= ProcessorUtils.stdString(encoder);
				for(String regionid : rule.getRegionIds()){
					regionid = ProcessorUtils.stdString(regionid);
					
					TreeMap<String, SpeedRule> map4Encoder = ret.get(encoder);
					if(map4Encoder==null){
						map4Encoder = new TreeMap<String, SpeedRule>();
						ret.put(encoder, map4Encoder);
					}
					
					SpeedRule prexisting = map4Encoder.get(regionid);
					if(prexisting!=null){
						throw new RuntimeException("More than one speed rule exists for encoder type " + encoder + " and regionId " + regionid + ".");
					}
					map4Encoder.put(regionid, rule);
				}
			}
		}
		
		return ret;
	}
	
	private TreeSet<TempPolygonRecord> prioritisePolygons(List<SpeedRules> files, List<SpeedRule> globalRules) {
		// prioritise all polygons and link them up to the local rules in their files
		int nfiles = files.size();
		TreeSet<TempPolygonRecord> prioritisedPolygons = new TreeSet<SpeedRulesFilesProcesser.TempPolygonRecord>();
		TreeSet<String> stdRegionIds = new TreeSet<String>();
		for(int ifile = 0 ; ifile < nfiles ; ifile++){
			SpeedRules file = files.get(ifile);
			
			FeatureCollection fc = file.getFeatures();
			if(fc!=null){
				int nfeatures = fc.getFeatures().size();
				for(int iFeat=0 ;iFeat <nfeatures ; iFeat++ ){
					Feature feature = fc.getFeatures().get(iFeat);
					String regionId=null;
					if(feature.getProperties()!=null){
						for(Map.Entry<String,Object> entry : feature.getProperties().entrySet()){
							if(ProcessorUtils.stdString("regionid").equals(ProcessorUtils.stdString(entry.getKey())) && entry.getValue()!=null){
								regionId = entry.getValue().toString();
							}
						}
					}
	
					if(regionId==null){
						throw new RuntimeException("Found feature without a regionid property");
					}
					
					if(regionId.trim().length()==0){
						throw new RuntimeException("Found feature with an empty regionid property");						
					}
			
					String stdRegionId = ProcessorUtils.stdString(regionId);
					if(stdRegionIds.contains(stdRegionId)){
						throw new RuntimeException("RegionId " + regionId + " was found in more than one feature. RegionIds should be unique.");			
					}
					stdRegionIds.add(stdRegionId);
					
					if(feature.getGeometry()==null){
						throw new RuntimeException("Found feature without geometry");								
					}
				
					if(feature.getGeometry() instanceof Polygon){
						prioritisedPolygons.add(new TempPolygonRecord(ifile, iFeat, (Polygon)feature.getGeometry(), stdRegionId, prioritisedPolygons.size()));
					}
					else if(feature.getGeometry() instanceof MultiPolygon){
						
						// split multipolyon into single polygons (potentially with holes)
						MultiPolygon mp = (MultiPolygon)feature.getGeometry();
						for(List<List<LngLatAlt>> coordLists: mp.getCoordinates()){
							Polygon polygon = new Polygon();
							int nLists = coordLists.size();
							for(int i =0 ; i<nLists ; i++){
								if(i==0){
									polygon.setExteriorRing(coordLists.get(0));
								}else{
									polygon.addInteriorRing(coordLists.get(i));
								}
							}
							
							prioritisedPolygons.add(new TempPolygonRecord(ifile, iFeat, polygon, stdRegionId, prioritisedPolygons.size()));	
						}
					}else{
						throw new RuntimeException("Found feature with non-polygon geometry type");							
					}
				}
			}
		}
		
		return prioritisedPolygons;
	}
	

	
}
