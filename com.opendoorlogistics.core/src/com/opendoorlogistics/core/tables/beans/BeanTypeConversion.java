/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.beans;

import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.vividsolutions.jts.geom.Geometry;

final public class BeanTypeConversion {
	private interface ODLTypeToExternalJavaType {
		Object convertOdlTypeToExternalJavaType(Object odlType);
	}

	private static class SupportedType {
		private final Class<?> externalType;
		private final ODLColumnType odlType;
		private final ODLTypeToExternalJavaType odlTypeToExternalJavaType;

		public SupportedType(Class<?> externalType, ODLColumnType odlType, ODLTypeToExternalJavaType odlTypeToExternalJavaType) {
			super();
			this.externalType = externalType;
			this.odlType = odlType;
			this.odlTypeToExternalJavaType = odlTypeToExternalJavaType;
		}
	}

	private static final HashMap<Class<?>, SupportedType> supportedTypes = new HashMap<>();

	static {
		ArrayList<SupportedType> types = new ArrayList<>();
		
		// include the 'no conversion' supported types (i.e. converting Long to Long etc...)
		for(ODLColumnType col : ODLColumnType.values()){
			types.add(new SupportedType(ColumnValueProcessor.getJavaClass(col), col, new ODLTypeToExternalJavaType() {

				@Override
				public Object convertOdlTypeToExternalJavaType(Object odlType) {
					return odlType;
				}
			}));		
		}
		
//		types.add(new SupportedType(Long.class, ODLColumnType.LONG, new ODLTypeToJavaType() {
//
//			@Override
//			public Object toJavaType(Object odlType) {
//				return odlType;
//			}
//		}));

		types.add(new SupportedType(Long.TYPE, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return odlType;
			}
		}));

		types.add(new SupportedType(Integer.class, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Long) odlType).intValue();
			}
		}));

		types.add(new SupportedType(Integer.TYPE, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Long) odlType).intValue();
			}
		}));

		types.add(new SupportedType(Short.class, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Long) odlType).shortValue();
			}
		}));

		types.add(new SupportedType(Short.TYPE, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Long) odlType).shortValue();
			}
		}));

		types.add(new SupportedType(Byte.class, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Long) odlType).byteValue();
			}
		}));

		types.add(new SupportedType(Byte.TYPE, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Long) odlType).byteValue();
			}
		}));

		types.add(new SupportedType(Boolean.class, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				if (odlType == null) {
					return null;
				}
				return ((Long) odlType) == 1;
			}
		}));

		types.add(new SupportedType(Boolean.TYPE, ODLColumnType.LONG, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				if (odlType == null) {
					return false;
				}
				return ((Long) odlType) == 1;
			}
		}));


//		types.add(new SupportedType(Double.class, ODLColumnType.DOUBLE, new ODLTypeToJavaType() {
//
//			@Override
//			public Object toJavaType(Object odlType) {
//				return odlType;
//			}
//		}));

		types.add(new SupportedType(Double.TYPE, ODLColumnType.DOUBLE, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return odlType;
			}
		}));

		types.add(new SupportedType(Float.class, ODLColumnType.DOUBLE, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Double) odlType).floatValue();
			}
		}));

		types.add(new SupportedType(Float.TYPE, ODLColumnType.DOUBLE, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((Double) odlType).floatValue();
			}
		}));

//		types.add(new SupportedType(String.class, ODLColumnType.STRING, new ODLTypeToJavaType() {
//
//			@Override
//			public Object toJavaType(Object odlType) {
//				return odlType;
//			}
//		}));

		types.add(new SupportedType(Color.class, ODLColumnType.COLOUR, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return odlType;
			}
		}));

		types.add(new SupportedType(Geometry.class, ODLColumnType.GEOM, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return ((ODLGeomImpl)odlType).getJTSGeometry();
			}
		}));

		types.add(new SupportedType(ODLGeom.class, ODLColumnType.GEOM, new ODLTypeToExternalJavaType() {

			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				return odlType;
			}
		}));
		
		types.add(new SupportedType(ODLTime.class, ODLColumnType.TIME, new ODLTypeToExternalJavaType() {
			
			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				// no actual conversion
				return odlType;
			}
		}));

		
		types.add(new SupportedType(LocalDate.class, ODLColumnType.DATE, new ODLTypeToExternalJavaType() {
			
			@Override
			public Object convertOdlTypeToExternalJavaType(Object odlType) {
				// no actual conversion
				return odlType;
			}
		}));
		
		for (SupportedType type : types) {
			supportedTypes.put(type.externalType, type);
		}
	}

	/**
	 * Get the java type ODL uses internally for the external java type
	 * @param externalType
	 * @return
	 */
	public static ODLColumnType getInternalType(Class<?> externalType){
		SupportedType type = supportedTypes.get(externalType);
		if(type!=null){
			return type.odlType;
		}
		
		// for jts Geometry types we have an inheritance hierarchy and
		// hence we also allow subtypes of Geometry
		if(Geometry.class.isAssignableFrom(externalType)){
			return ODLColumnType.GEOM;
		}
		return null;
	}
	
	/**
	 * Get the external value from an internal one. 
	 * The internal one is the type used by ODL's column values.
	 * @param externalType
	 * @param internalValue
	 * @return
	 */
	public static Object getExternalValue(Class<?> externalType, Object internalValue){
		if(internalValue==null){
			return null;
		}
		SupportedType supportedType = supportedTypes.get(externalType);
		if(supportedType==null){
			throw new RuntimeException("Unsupported java type");
		}
		return supportedType.odlTypeToExternalJavaType.convertOdlTypeToExternalJavaType(internalValue);
	}
}
