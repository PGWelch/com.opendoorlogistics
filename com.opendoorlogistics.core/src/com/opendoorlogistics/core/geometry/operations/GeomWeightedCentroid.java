/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GeomWeightedCentroid {
	private static class CacheKey {
		final HashSet<Pair<ODLGeom, Double>> items;
		final String epsg;

		public CacheKey(Collection<Pair<ODLGeom, Double>> items, String espg) {
			super();
			this.items = new HashSet<>(items);
			this.epsg = espg;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((epsg == null) ? 0 : epsg.hashCode());
			result = prime * result + ((items == null) ? 0 : items.hashCode());
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

			if (epsg == null) {
				if (other.epsg != null)
					return false;
			} else if (!epsg.equals(other.epsg))
				return false;
			if (items == null) {
				if (other.items != null)
					return false;
			} else if (!items.equals(other.items))
				return false;
			return true;
		}

	}

	/**
	 * Calculate weighted centre using the input table and row ids.
	 * @param table
	 * @param rowIds
	 * @param geomColIndx
	 * @param weightColIndx Can be -1 if not using a column (each weight is 1)
	 * @param ESPGCode
	 * @return
	 */
	public ODLGeom calculate(ODLTableReadOnly table, long[] rowIds, int geomColIndx, int weightColIndx, String ESPGCode) {
		if (rowIds == null || rowIds.length == 0) {
			return null;
		}

		// put input values into a collection
		ArrayList<Pair<ODLGeom, Double>> geoms = new ArrayList<>(rowIds.length);
		for (long id : rowIds) {
			// get geometry
			Object geomVal = table.getValueById(id, geomColIndx);
			if (geomVal == null) {
				return null;
			}
			ODLGeom geom = (ODLGeom) ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, geomVal);
			if (geom == null) {
				return null;
			}

			// get weight
			Double d = 1.0;
			if(weightColIndx>=0){
				Object weightVal = table.getValueById(id, weightColIndx);
				if (weightVal != null) {
					d = (Double) ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, weightVal);
					if (d == null) {
						return null;
					}
				}				
			}

			geoms.add(new Pair<ODLGeom, Double>(geom, d));
		}

		return calculate(geoms, ESPGCode);
	}

	public ODLGeom calculate(Collection<Pair<ODLGeom, Double>> geoms, String ESPGCode) {
		// check cache
		CacheKey record = new CacheKey(geoms, ESPGCode);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.GEOM_CENTROID_CACHE);
		ODLGeom ret = (ODLGeom) cache.get(record);
		if (ret == null) {

			GridTransforms transforms =Strings.isEmpty(ESPGCode)?null: new GridTransforms(ESPGCode);

			// transform all geoms and get their individual centroids
			double weightSum = 0;
			double xSum = 0;
			double ySum = 0;
			for (Pair<ODLGeom, Double> pair : geoms) {
				Geometry g = ((ODLGeomImpl) pair.getFirst()).getJTSGeometry();
				if (g == null) {
					return null;
				}

				g = transforms!=null? transforms.wgs84ToGrid(g):g;
				Point pnt = g.getCentroid();
				double w = pair.getSecond();
				weightSum += w;
				xSum += pnt.getX() * w;
				ySum += pnt.getY() * w;
			}

			if (weightSum > 0) {
				Point pGrid = new GeometryFactory().createPoint(new Coordinate(xSum / weightSum, ySum / weightSum));
				Point pTrans = (Point)( transforms!=null?transforms.gridToWGS84(pGrid): pGrid);
				ret = new ODLLoadedGeometry(pTrans);

				// estimate size as size of keys as these will be the most expensive..
				long nbBytes = 0;
				for (Pair<ODLGeom, Double> pair : geoms) {
					nbBytes += ((ODLGeomImpl) pair.getFirst()).getEstimatedSizeInBytes();
				}
				nbBytes += 100; // add some extra for hashset, cache record etc
				cache.put(record, ret, nbBytes);
			}
		}

//		if(ret!=null){
//			System.out.println(ret);
//		}
		return ret;
	}
}
