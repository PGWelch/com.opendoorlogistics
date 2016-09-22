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
package com.opendoorlogistics.speedregions.beans;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.opendoorlogistics.speedregions.beans.SpeedUnit.SpeedUnitDeserialiser;

@JsonDeserialize(using = SpeedUnitDeserialiser.class)	
public enum SpeedUnit{
	MILES_PER_HOUR,
	KM_PER_HOUR;
	
	public static final SpeedUnit DEFAULT = KM_PER_HOUR;
	
	public static class SpeedUnitDeserialiser extends JsonDeserializer<SpeedUnit>{

	    @Override
	    public SpeedUnit deserialize(JsonParser parser, DeserializationContext context) throws IOException
	    {
	    	// Use default if empty...
	    	String string = parser.getText();
	        if (string==null || string.trim().length()==0) {
	            return DEFAULT;
	        }
	        
	        // do a tolerance cause insensitive check
	        string = string.trim();
	        string = string.toLowerCase();
	        for(SpeedUnit type : SpeedUnit.values()){
	        	if(type.name().trim().toLowerCase().equals(string)){
	        		return type;
	        	}
	        }
	        throw new RuntimeException("Cannot identify speed unit type: " +string);

	    }
		
	}

}