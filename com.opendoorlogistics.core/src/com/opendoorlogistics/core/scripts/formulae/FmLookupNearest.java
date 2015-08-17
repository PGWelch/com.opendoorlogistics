/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.collections.BinaryHeap;
import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.MathTransform;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionArgument;
import com.opendoorlogistics.core.geometry.GreateCircle;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.IndexedDatastores;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ProcessedLookupReferences;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ToProcessLookupReferences;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Looks up the closest object to the input geometry. For polygons the distance used is the distance to the boundary, not the polygon centre. When
 * comparing two objects which are both within the polygon, the closest distance to the centre is instead used.
 * 
 * @author Phil
 * 
 */
final public class FmLookupNearest extends FunctionImpl {
	private final MathTransform transform;
	private final String espg_srid;
	//private final Pair<Class<?>, String> cacheKey;
	private final LCType type;
	private ProcessedLookupReferences refs;

	private FmLookupNearest(LCType type, String espg_srid, MathTransform transform, Function... children) {
		super(children);
		this.type = type;
		this.espg_srid = espg_srid;
		this.transform = transform;
	//	this.cacheKey = new Pair<Class<?>, String>(FmLookupNearest.class, this.espg_srid);
	}

	// public FmLookupClosest(Function foreignKeyValue, int datastoreIndex, int otherTableId, int geometryColumn, int otherTableReturnKeyColummn,
	// String espg_srid) {
	// super(foreignKeyValue, datastoreIndex, otherTableId, otherTableReturnKeyColummn);
	// this.espg_srid = espg_srid;
	// transform = Spatial.fromWGS84(this.espg_srid);
	// if (transform == null) {
	// throw new RuntimeException("Cannot find transform for coordinate system: " + espg_srid);
	// }
	//
	// cacheKey = new Pair<Class<?>, String>(FmLookupClosest.class, this.espg_srid);
	// }

	private static class BoundingCircle {
		final Coordinate envelopeCentre;
		final double envelopeRadius;

		BoundingCircle(Geometry transformedGeom) {
			Envelope env = transformedGeom.getEnvelopeInternal();
			envelopeCentre = env.centre();
			double halfWidth = 0.5 * env.getWidth();
			double halfHeight = 0.5 * env.getHeight();
			envelopeRadius = Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight);
		}

		/**
		 * Calculate the minimum separation using the bounding circle
		 * 
		 * @param cpg
		 * @return
		 */
		double minimumSeparation(BoundingCircle cpg) {
			double ret = envelopeCentre.distance(cpg.envelopeCentre);
			ret -= envelopeRadius;
			ret -= cpg.envelopeRadius;
			return ret;
		}
	}

	/**
	 * Cached geometry. This is the geometry converted to the coord system together with a bounding circle.
	 * 
	 * @author Phil
	 * 
	 */
	private static class CachedProcessedGeom {
		public CachedProcessedGeom(LatLong ll, MathTransform transform) {
			Geometry geom = new GeometryFactory().createPoint(new Coordinate(ll.getLongitude(), ll.getLatitude()));
			if (transform != null) {
				try {
					geom = JTS.transform(geom, transform);
				} catch (Throwable e) {
					geom = null;
				}

				if (geom != null) {
					boundingCircle = new BoundingCircle(geom);
				} else {
					boundingCircle = null;
				}
			} else {
				boundingCircle = null;
			}
			geometry = geom;
		}

		CachedProcessedGeom(Geometry transformedGeom) {
			this.geometry = transformedGeom;
			boundingCircle = new BoundingCircle(transformedGeom);
		}

		final Geometry geometry;
		final BoundingCircle boundingCircle;

		public long getSizeInBytes(){
			long bytes =8;
			if(geometry!=null){
				bytes += Spatial.getEstimatedSizeInBytes(geometry);
			}
			
			// bounding circle
			bytes += 40;
			
			return bytes;
		}
	}

	/**
	 * Get in the coord system, caching when possible
	 * 
	 * @param geom
	 * @return
	 */
	private CachedProcessedGeom toCoordSystem(ODLGeomImpl geom) {
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.LOOKUP_NEAREST_TRANSFORMED_GEOMS);
		
		class CacheKey{
			final String espg;
			final ODLGeom geom;
			private CacheKey(String espg, ODLGeom geom) {
				super();
				this.espg = espg;
				this.geom = geom;
			}
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((espg == null) ? 0 : espg.hashCode());
				result = prime * result + ((geom == null) ? 0 : geom.hashCode());
				return result;
			}
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				CacheKey other = (CacheKey) obj;
				if (espg == null) {
					if (other.espg != null)
						return false;
				} else if (!espg.equals(other.espg))
					return false;
				if (geom == null) {
					if (other.geom != null)
						return false;
				} else if (!geom.equals(other.geom))
					return false;
				return true;
			}
			
		}
		
		CacheKey key = new CacheKey(espg_srid, geom);
		
		CachedProcessedGeom cached = (CachedProcessedGeom)cache.get(key);
		if (cached != null) {
			return cached;
		}

		try {
			Geometry wgs84 = geom.getJTSGeometry();
			if (wgs84 == null) {
				return null;
			}

			cached = new CachedProcessedGeom(JTS.transform(wgs84, transform));
			cache.put(key, cached, cached.getSizeInBytes());
			
			return cached;
		} catch (Throwable e) {
			// return value will be null, so error reported later
		}

		return null;
	}

	private enum LCType {
		LL, LG, GG, GL,
	}

	private CachedProcessedGeom getSearchGeom(FunctionParameters parameters) {
		switch (type) {
		case LL:
			// untransformed
			return getLatLongFromExecution(parameters, false);

		case LG:
			// transformed to wgs84
			return getLatLongFromExecution(parameters, true);

		case GG:
		case GL: {
			// read geometry object
			Object keyVal = child(0).execute(parameters);
			if (keyVal == null || keyVal == Functions.EXECUTION_ERROR) {
				return null;
			}

			// get transformed geometry
			ODLGeomImpl odlGeom = (ODLGeomImpl) ColumnValueProcessor.convertToMe(ODLColumnType.GEOM,keyVal);
			return toCoordSystem(odlGeom);
		}

		}

		return null;
	}

	// protected Object getTransformedLatLongGeometrySearchObject(FunctionParameters parameters){
	// LatLong ll = getLatLongFromExecution(parameters);
	// if(ll==null){
	// return Functions.EXECUTION_ERROR;
	// }
	// Geometry ret = toCoordSystem(new ODLGeom( new GeometryFactory().createPoint(new Coordinate(ll.getLongitude(), ll.getLatitude()))));
	// if(ret==null){
	// return Functions.EXECUTION_ERROR;
	// }
	// return ret;
	// }

	private CachedProcessedGeom getLatLongFromExecution(FunctionParameters parameters, boolean transformed) {
		LatLong ll = getLatLongFromExecution(parameters);
		return new CachedProcessedGeom(ll, transformed ? transform : null);
	}

	private LatLong getLatLongFromExecution(FunctionParameters parameters) {
		Object[] longlat = executeChildFormulae(parameters, true);
		if (longlat == null) {
			return null;
		}

		LatLong ll = getLatLongFromObjects(longlat[0], longlat[1]);
		if (ll == null) {
			return null;
		}
		return ll;
	}

	private LatLong getLatLongFromObjects(Object oLng, Object oLat) {
		Double lng = Numbers.toDouble(oLng);
		Double lat = Numbers.toDouble(oLat);
		if (lng == null || lat == null) {
			return null;
		}

		LatLong ll = new LatLongImpl(lat, lng);
		return ll;
	}

	// protected Object getGeometrySearchObject(FunctionParameters parameters){
	// Object keyVal = child(0).execute(parameters);
	// if (keyVal == null || keyVal == Functions.EXECUTION_ERROR) {
	// return Functions.EXECUTION_ERROR;
	// }
	//
	// // get transformed geometry
	// Geometry geom=toCoordSystem(keyVal);
	// if(geom==null){
	// return Functions.EXECUTION_ERROR;
	// }
	//
	// return geom;
	// }

	private Object executeLL(FunctionParameters parameters, ODLTableReadOnly table) {
		LatLong ll = getLatLongFromExecution(parameters);
		if (ll == null) {
			return Functions.EXECUTION_ERROR;
		}

		int nr = table.getRowCount();
		int closestRow = -1;
		double closest = Double.MAX_VALUE;
		for (int row = 0; row < nr; row++) {
			Pair<LatLong, Boolean> other = getLatLongFromRow(table, row);
			if (other.getSecond() == false) {
				// critical error
				return Functions.EXECUTION_ERROR;
			} else if (other.getFirst() != null) {
				double dist = GreateCircle.greatCircleApprox(ll, other.getFirst());
				if (dist < closest) {
					closest = dist;
					closestRow = row;
				}
			}
		}

		if (closestRow != -1) {
			return getReturnObject(table, closestRow);
		}

		return null;
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		TableParameters tp = (TableParameters) parameters;
		ODLTableReadOnly table = (ODLTableReadOnly) tp.getTableById(refs.datastoreIndx, refs.tableId);

		if (type == LCType.LL) {
			return executeLL(parameters, table);
		}

		CachedProcessedGeom searchObject = getSearchGeom(parameters);
		if (searchObject == null || searchObject.geometry == null) {
			return Functions.EXECUTION_ERROR;
		}

		class RowElement implements Comparable<RowElement>{
			int row;
			CachedProcessedGeom geom;
			double minDistance;
			
			@Override
			public int compareTo(RowElement o) {
				int ret= Double.compare(minDistance, o.minDistance);
				if(ret==0){
					ret= Integer.compare(row, o.row);
				}
				return ret;
			}
		}
		
		// Get all geometries, transformed into the coord system and with bounding circles.
		// Place them in a binary heap, sorted by their minimum possible distance according to bounding circle
		int nr = table.getRowCount();
		BinaryHeap sortedHeap = new BinaryHeap();
		for (int row = 0; row < nr; row++) {
			CachedProcessedGeom otherGeom = null;
			switch (type) {
			case GL:
				Pair<LatLong, Boolean> result = getLatLongFromRow(table, row);
				if (result.getSecond() == false) {
					// critical error
					return Functions.EXECUTION_ERROR;
				} else if (result.getFirst() != null) {

					LatLong ll = result.getFirst();

					// put into our comparison object and convert
					otherGeom = new CachedProcessedGeom(ll, transform);
				}
				break;

			case GG:
			case LG: {
				Object val = table.getValueAt(row, refs.columnIndices[0]);
				if (val != null) {

					ODLGeomImpl odlGeom = (ODLGeomImpl) ColumnValueProcessor.convertToMe(ODLColumnType.GEOM,val);
					if (odlGeom == null) {
						// critical error
						return Functions.EXECUTION_ERROR;
					}

					otherGeom = toCoordSystem(odlGeom);
					if (otherGeom == null || otherGeom.geometry == null) {
						// critical error
						return Functions.EXECUTION_ERROR;
					}
				}
			}
			default:
				break;
			}

			if (otherGeom != null) {
				RowElement rowElement = new RowElement();
				rowElement.row = row;
				rowElement.minDistance =  searchObject.boundingCircle.minimumSeparation(otherGeom.boundingCircle);
				rowElement.geom = otherGeom;
				sortedHeap.add(rowElement);
			}
		}


		// loop over the table
		RowElement closest=null;
		double closestDistance = Double.MAX_VALUE;
		while(sortedHeap.size()>0){
			RowElement row = (RowElement)sortedHeap.pop();
			if(row.minDistance > closestDistance){
				// We can stop searching now as the minimum possible distance is greater than our closest
				break;
			}
			
			// Explicitly get the distance
			double distance = searchObject.geometry.distance(row.geom.geometry);
			if(distance < closestDistance){
				closestDistance = distance;
				closest = row;
			}
		}


		if(closest!=null){
			return getReturnObject(table, closest.row);	
		}

		return null;
	}

	private Object getReturnObject(ODLTableReadOnly table, int closestRow) {
		return table.getValueAt(closestRow, refs.columnIndices[refs.columnIndices.length - 1]);
	}


	/**
	 * 
	 * @param searchGeom
	 * @param table
	 * @param row
	 * @return Return false for critical error.
	 */
	private Pair<LatLong, Boolean> getLatLongFromRow(ODLTableReadOnly table, int row) {
		Object lng = table.getValueAt(row, refs.columnIndices[0]);
		Object lat = table.getValueAt(row, refs.columnIndices[1]);
		boolean result = true;
		LatLong ll = null;
		if (lng == null && lat == null) {
			// ok, both null
			result = true;
		} else {
			// try getting latlong object
			ll = getLatLongFromObjects(lng, lat);
			if (ll == null) {
				if (lng != null && lat != null && Strings.isEmpty(lng.toString()) && Strings.isEmpty(lat.toString())) {
					// both non-null but empty; this is OK
					result = true;
				} else {
					// corrupt data; critical error
					result = false;
				}
			}
		}

		return new Pair<LatLong, Boolean>(ll, result);
	}



	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}

	public static Iterable<FunctionDefinition> createDefinitions(final IndexedDatastores<? extends ODLTableReadOnly> datastores,
			final int defaultDatastoreIndex, final ExecutionReport result) {
		ArrayList<FunctionDefinition> dfns = new ArrayList<>();
		for (final LCType type : LCType.values()) {

			final FunctionDefinition dfn = new FunctionDefinition("lookupnearest" + type.name().toLowerCase());
			dfn.setGroup("lookupNearest");
			dfn.setDescription("Find the nearest object to the input value in the other table.");
			if (type != LCType.LL) {
				dfn.addArg("ESPG_SRID", ArgumentType.STRING_CONSTANT, 
						"Spatial Reference System Identifier (SRID) from the ESPG SRID database "
						+ "to use for performing the distance calculations. The reference system "
						+ "must be a grid-based one allowing Pythagoras distance calculations (i.e. "
						+ "a spherical longitude-latitude coord system will not work).");
			}
		
			// add FROM arguments
			switch (type) {
			case LG:
			case LL:
				dfn.addArg("longitude", "Longitude to compare to geographic objects in the other table.");
				dfn.addArg("latitude", "Latitude to compare to geographic objects in the other table.");
				break;

			case GG:
			case GL:
				dfn.addArg("geometry", "Geometry to compare to geographic objects in the other table.");
				break;
			}

			dfn.addArg("table_reference", ArgumentType.TABLE_REFERENCE_CONSTANT, "Reference to the table to search in.");

			// add OTHER arguments
			switch (type) {
			case GL:
			case LL:
				dfn.addArg(new FunctionArgument("longitude_field_name", ArgumentType.STRING_CONSTANT,
						"Field name of the longitude field in the other table.", false));
				dfn.addArg(new FunctionArgument("latitude_field_name", ArgumentType.STRING_CONSTANT,
						"Field name of the latitude field in the other table.", false));
				break;

			case LG:
			case GG:
				dfn.addArg(new FunctionArgument("geometry_field_name", ArgumentType.STRING_CONSTANT,
						"Field name of the geometry field in the other table.", false));
				break;

			}

			dfn.addArg("return_field_name", "Name of the field from the other table to return the value of.");

			// only build the factory if we have actual datastore to build against
			if (datastores != null) {
				dfn.setFactory(new FunctionFactory() {

					@Override
					public Function createFunction(Function... children) {

						int i = 0;

						// get math transform to grid unless working completely in longitude-latitude
						String srid = null;
						MathTransform transform = null;

						if (type != LCType.LL) {
							srid = FunctionUtils.getConstantString(get(children, i++));
							transform = Spatial.fromWGS84(srid);
						}

						// get function(s) to get the geometry
						FmLookupNearest ret = null;
						switch (type) {
						case LL:
						case LG:
							ret = new FmLookupNearest(type, srid, transform, get(children, i++), get(children, i++));
							break;

						case GL:
						case GG:
							ret = new FmLookupNearest(type, srid, transform, get(children, i++));
							break;
						}

						// get the table reference - to do process this
						ToProcessLookupReferences toProcess = new ToProcessLookupReferences();
						toProcess.tableReferenceFunction = children[i++];
						int nbFieldnames = (type == LCType.LL || type == LCType.GL) ? 3 : 2;
						toProcess.fieldnameFunctions = new Function[nbFieldnames];
						for (int j = 0; j < nbFieldnames; j++) {
							toProcess.fieldnameFunctions[j] = get(children, i++);
						}

						// check no more fields left
						if (i != children.length) {
							throwWrongNbArgs();
						}

						// process table references
						ret.refs = FunctionsBuilder.processLookupReferenceNames(dfn.getName(), datastores, defaultDatastoreIndex, toProcess, result);
						return ret;
					}

					private Function get(Function[] children, int i) {
						if (i >= children.length) {
							throwWrongNbArgs();
						}
						return children[i];
					}

					private void throwWrongNbArgs() {
						throw new RuntimeException("Wrong number of arguments given for function: " + dfn.getName());
					}
				});
			}

			dfns.add(dfn);
			// }
		}

		// sort by number of arguments
		Collections.sort(dfns, new Comparator<FunctionDefinition>() {

			@Override
			public int compare(FunctionDefinition o1, FunctionDefinition o2) {
				return Integer.compare(o1.nbArgs(), o2.nbArgs());
			}
		});
		return dfns;
	}

	public static void main(String[] args) {
		for (FunctionDefinition dfn : createDefinitions(null, -1, null)) {
			System.out.println(dfn + " - " + dfn.nbArgs() + " arguments");

		}
	}
}
