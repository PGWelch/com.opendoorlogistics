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

import org.geotools.geometry.jts.JTS;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.Spatial;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

/**
 * Perform union on multiple geomtries, caching the result. Union is performed in the input grid system.
 * 
 * @author Phil
 *
 */
public class GeomUnion {

	private Geometry combineIntoOneGeometry(Collection<com.vividsolutions.jts.geom.Geometry> geometryCollection) {
		if (geometryCollection.size() == 1) {
			return geometryCollection.iterator().next();
		}

		GeometryFactory factory = new GeometryFactory();

		// note the following geometry collection may be invalid (say with overlapping polygons)
		GeometryCollection gc = (GeometryCollection) factory.buildGeometry(geometryCollection);

		return gc.union();
	}

	private Object createCacheKey(Iterable<ODLGeom> inputGeoms, String ESPGCode) {
		class CacheKey {
			HashSet<ODLGeom> set = new HashSet<>();
			String espg;

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((espg == null) ? 0 : espg.hashCode());
				result = prime * result + ((set == null) ? 0 : set.hashCode());
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
				if (set == null) {
					if (other.set != null)
						return false;
				} else if (!set.equals(other.set))
					return false;
				return true;
			}

		}
		CacheKey key = new CacheKey();
		key.espg = ESPGCode;

		// put all geoms in a hashset
		for (ODLGeom geom : inputGeoms) {
			key.set.add(geom);
		}

		return key;
	}

	private static final String INVALID_UNION = "invalid-union";

	public ODLGeom union(Iterable<ODLGeom> inputGeoms, String ESPGCode) {
		Object key = createCacheKey(inputGeoms, ESPGCode);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.GEOMETRY_MERGER_CACHE);
		Object cached = cache.get(key);
		if (cached == INVALID_UNION) {
			return null;
		}
		ODLGeom ret = (ODLGeom) cached;

		if (ret == null) {
			// estimate the key size, assuming the input geometries are stored elsewhere
			int estimatedKeySize = 8 + com.opendoorlogistics.core.utils.iterators.IteratorUtils.size(inputGeoms) * 8 + 20;

			try {
				ret = calculateUnion(inputGeoms, ESPGCode);
			} catch (Exception e) {
				// record the union as invalid in-case we do it again
				cache.put(key, INVALID_UNION, estimatedKeySize);
				throw new RuntimeException(e);
			}

			if (ret != null) {
				// estimate size of geometry in bytes
				long size = ((ODLGeomImpl) ret).getEstimatedSizeInBytes();
				size += estimatedKeySize;
				cache.put(key, ret, size);
			}

		}
		return ret;
	}

	private ODLGeom calculateUnion(Iterable<ODLGeom> inputGeoms, String ESPGCode) {
		try {
			Spatial.initSpatial();
			GridTransforms transforms = new GridTransforms(ESPGCode);

			PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING_SINGLE);
			GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(pm);

			// process shapes into grid with reduced precision
			ArrayList<Geometry> gridGeoms = new ArrayList<>();
			for (ODLGeom geom : inputGeoms) {
				if (geom != null) {
					ODLGeomImpl gimpl = (ODLGeomImpl) geom;
					if (gimpl.getJTSGeometry() != null) {
						com.vividsolutions.jts.geom.Geometry g = gimpl.getJTSGeometry();

						// convert to grid
						g = JTS.transform(g, transforms.getWGS84ToGrid().getMathTransform());

						// reduce precision as it stops holes appearing with our UK postcode data
						g = reducer.reduce(g);

						gridGeoms.add(g);

					}

				}
			}

			if (gridGeoms.size() == 0) {
				return null;
			}

			// combine
			Geometry combinedGrid = combineIntoOneGeometry(gridGeoms);

			// transform back
			Geometry combinedWGS84 = JTS.transform(combinedGrid, transforms.getGridToWGS84().getMathTransform());

			return new ODLLoadedGeometry(combinedWGS84);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
