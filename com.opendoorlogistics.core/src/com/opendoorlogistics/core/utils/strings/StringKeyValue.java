/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.strings;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class representing a simple key-value string pair/
 * @author Phil
 *
 */
final public class StringKeyValue extends AbstractMap.SimpleEntry<String, String> implements Comparable<StringKeyValue>{

	public StringKeyValue(String key , String value) {
		super(key, value);
	}

	public static List<StringKeyValue> parseCommaSeparated(String s, boolean standardise){
		String [] split = s.split(",");
		ArrayList<StringKeyValue> ret = new ArrayList<>();
		for(String pair : split){
			String []kv = pair.split("=");
			if(kv.length!=2){
				throw new RuntimeException("Found unparsable key value pair in string: " + s);
			}

			StringKeyValue kvo=new StringKeyValue(kv[0], kv[1]);
			if(standardise){
				kvo = kvo.standardise();
			}
			ret.add(kvo);
		}
		return ret;
	}
	
	/**
	 * Turn the parameter map into flags. It is assumed the strings in the parameter map
	 * are standardised
	 * @param standardisedFlagMap
	 * @param s
	 * @return
	 */
	public static long getFlags(Map<StringKeyValue, Long> standardisedFlagMap, String s){
		long ret=0;
		for(StringKeyValue kv :parseCommaSeparated(s, true) ){
			Long value = standardisedFlagMap.get(kv);
			if(value==null){
				throw new RuntimeException("Unknown key-value option:" + kv);
			}
			ret |= value;
		}
		return ret;
	}
	
	public StringKeyValue standardise(){
		return new StringKeyValue(Strings.std(getKey()), Strings.std(getValue()));
	}
	
	public static void main(String[] args){
		for(StringKeyValue kv :parseCommaSeparated("  legend  =  top  , fade=true", true) ){
			System.out.println(kv);
		}
	}

	@Override
	public String toString(){
		return getKey() + "=" + getValue();
	}

	@Override
	public int compareTo(StringKeyValue o) {
		int diff = getKey().compareTo(o.getKey());
		if(diff==0){
			 diff = getValue().compareTo(o.getValue());
		}
		return diff;
	}
}
