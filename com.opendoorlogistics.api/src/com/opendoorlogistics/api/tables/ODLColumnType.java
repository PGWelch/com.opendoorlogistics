/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum(String.class)
public enum ODLColumnType {
	STRING, LONG, DOUBLE, COLOUR,
	IMAGE, GEOM, TIME, DATE, MAP_TILE_PROVIDER, FILE_DIRECTORY;


	private ODLColumnType() {
		this(false);
	}
	
	private ODLColumnType(boolean engineType) {
		this.engineType = engineType;
	}

	private final boolean engineType;

	public boolean isEngineType() {
		return engineType;
	}
	
	private static ODLColumnType [] STD_TYPES;
	
	static{
		ArrayList<ODLColumnType> list = new ArrayList<ODLColumnType>();
		for(ODLColumnType type : ODLColumnType.values()){
			if(!type.isEngineType()){
				list.add(type);				
			}
		}
		STD_TYPES = list.toArray(new ODLColumnType[list.size()]);
	}
	
	public static ODLColumnType [] standardTypes(){
		return STD_TYPES;
	}
	
}
