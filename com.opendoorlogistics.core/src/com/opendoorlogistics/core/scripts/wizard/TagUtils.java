/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.wizard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class TagUtils {
	private TagUtils(){}

	public static <T extends ODLTableDefinition> T findTableWithTag(String tag, ODLDatastore<T> ds ){
		for(T table : TableUtils.getTables(ds)){
			if(hasTag(tag, table)){
				return table;
			}
		}
		return null;
	}
	
	public static boolean hasTag(String tag, ODLTableDefinition table){
		if(Strings.equalsStd(tag, table.getName())){
			return true;
		}
		return table.getTags()!=null && Strings.containsStandardised(tag, table.getTags());
	}
	
	public static int findTag(String tag, ODLTableDefinition table, boolean throwExceptionIfNotFound){
		int nc= table.getColumnCount();
		for(int i =0 ; i<nc;i++){
			if(hasTag(tag, table, i)){
				return i;
			}
		}
		
		if(throwExceptionIfNotFound){
			throw new RuntimeException("Cannot identify tag or field \"" + tag + "\" in table \"" + table.getName() + "\".");
		}
		
		return -1;
	}
	
	/**
	 * Find the first column with the tag
	 * @param tag
	 * @param table
	 * @return
	 */
	public static int findTag(String tag, ODLTableDefinition table){
		return findTag(tag, table, false);
	}
	
	public static boolean hasTag(String tag, ODLTableDefinition table, int column){
		if(Strings.equalsStd(tag, table.getColumnName(column))){
			return true;
		}
		return table.getColumnTags(column)!=null && Strings.containsStandardised(tag, table.getColumnTags(column));
	}

//	/**
//	 * Find the components and their configurations with generate travel costs
//	 * @return
//	 */
//	public static List<Pair<ODLComponent, List<ODLWizardTemplateConfig>>> findTravelCostGenerators(){
//		return findComponentsWhereOutputTableHasTag(PredefinedTags.TRAVEL_COSTS);
//	}
//
//	private static List<Pair<ODLComponent, List<ODLWizardTemplateConfig>>> findComponentsWhereOutputTableHasTag(final ODLApi api,final String tag){
//		ArrayList<Pair<ODLComponent, List<ODLWizardTemplateConfig>>> ret = new ArrayList<>();
//		for(final ODLComponent component : ODLGlobalComponents.getProvider()){
//
//			class Helper{
//				boolean accept(Serializable config){
//					for(ODLTableDefinition table : TableUtils.getTables(component.getOutputDsDefinition(api,ODLComponent.MODE_DEFAULT,config))){
//						if(hasTag(tag, table)){
//							return true;
//						}
//					}
//					return false;
//				}
//			}
//			
//			Helper helper = new Helper();
//			ArrayList<ODLWizardTemplateConfig> okConfigs = new ArrayList<>();
//			
//			if(IteratorUtils.size(ScriptTemplatesImpl.getTemplates(api,component))>0){
//				// Test each pre-defined config
//				for(ODLWizardTemplateConfig config : ScriptTemplatesImpl.getTemplates(api,component)){
//					if(helper.accept(config.getConfig())){
//						okConfigs.add(config);
//					}
//				}
//			}else{
//				// if we have no pre-defined configs also test if a new instances of the config class is a distance generator
//				Serializable config = null;
//				if(component.getConfigClass()!=null){
//					try {
//						config = component.getConfigClass().newInstance();
//					} catch (Throwable e) {
//						throw new RuntimeException(e);
//					};
//				}
//				
//				if(helper.accept(config)){
//					okConfigs.add(new ODLWizardTemplateConfig(component.getName(), component.getName(), component.getName(), config));
//				}
//			}
//			
//			// save to the return list if we have one or more OK configs
//			if(okConfigs.size()>0){
//				ret.add(new Pair<ODLComponent, List<ODLWizardTemplateConfig>>(component, okConfigs));
//			}
//		}
//		
//		return ret;
//	}
	
	public static int countCommonColumnTags(ODLTableDefinition tableA, int columnA, ODLTableDefinition tableB, int columnB){
		StandardisedStringSet setA = getStdColumnTags(tableA, columnA);	
		StandardisedStringSet setB = getStdColumnTags(tableB, columnB);
		
		int ret=0;
		for(String s:setA){
			if(setB.contains(s)){
				ret++;
			}
		}
		return ret;
	}

	/**
	 * Get the standardised column tags for the input column. 
	 * The column name is also included and if the column is geometry,
	 * the predefined geometry tag is also included.
	 * @param table
	 * @param column
	 * @return
	 */
	private static StandardisedStringSet getStdColumnTags(ODLTableDefinition table, int column) {
		StandardisedStringSet ret = new StandardisedStringSet(false,table.getColumnTags(column));
		ret.add(table.getColumnName(column));
		if(table.getColumnType(column)==ODLColumnType.GEOM){
			ret.add(PredefinedTags.GEOMETRY);
		}
		return ret;
	}
	
	/**
	 * Places the input tags into an unmodifiable set
	 * @param tags
	 * @return
	 */
	public static Set<String> createTagSet(String ...tags){
		return Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(tags)));
	}
}
