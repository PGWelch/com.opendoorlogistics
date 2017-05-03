/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances;

import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.Map;

import com.graphhopper.util.shapes.GHPoint;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.distances.DistancesConfiguration.CalculationMethod;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.api.impl.GeometryImpl;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.distances.external.FileVersionId;
import com.opendoorlogistics.core.distances.external.LoadedMatrixFile;
import com.opendoorlogistics.core.distances.external.LoadedMatrixFile.ValueType;
import com.opendoorlogistics.core.distances.external.MatrixFileReader;
import com.opendoorlogistics.core.distances.external.RoundingGrid;
import com.opendoorlogistics.core.distances.external.RoundingGrid.GridNeighboursResult;
import com.opendoorlogistics.core.distances.graphhopper.CHMatrixGenWithGeomFuncs;
import com.opendoorlogistics.core.geometry.GreateCircle;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.scripts.wizard.TagUtils;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.io.RelativeFiles;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.graphhopper.CHMatrixGeneration.CHProcessingApi;
import com.opendoorlogistics.graphhopper.CHMatrixGeneration;
import com.opendoorlogistics.graphhopper.MatrixResult;

import gnu.trove.list.array.TIntArrayList;


public final class DistancesSingleton implements Closeable{
	public static final double UNCONNECTED_TRAVEL_COST = Double.POSITIVE_INFINITY;
	
	//private final RecentlyUsedCache recentMatrixCache = new RecentlyUsedCache(128 * 1024 * 1024);
	//private final RecentlyUsedCache recentGeomCache = new RecentlyUsedCache(64 * 1024 * 1024);
	private CHMatrixGenWithGeomFuncs lastCHGraph;
	
	private DistancesSingleton() {
	}

	private static final DistancesSingleton singleton = new DistancesSingleton();

	private static class InputTableAccessor {
		private final int locCol;
		private final int latCol;
		private final int lngCol;
		private final ODLTableReadOnly table;

		InputTableAccessor(ODLTableReadOnly table) {
			locCol = findTag(PredefinedTags.LOCATION_KEY, table);
			latCol = findTag(PredefinedTags.LATITUDE, table);
			lngCol = findTag(PredefinedTags.LONGITUDE, table);
			this.table = table;
		}

		private static int findTag(String tag, ODLTableDefinition table) {
			int col = TagUtils.findTag(tag, table);
			if (col == -1) {
				throw new RuntimeException("Distances input table does not contain tag for: " + tag);
			}
			return col;
		}

		double getLatitude(int row) {
			return (Double) getValueAt(row, latCol, ODLColumnType.DOUBLE);
		}

		double getLongitude(int row) {
			return (Double) getValueAt(row, lngCol, ODLColumnType.DOUBLE);
		}

		String getLoc(int row) {
			return (String) getValueAt(row, locCol, ODLColumnType.STRING);
		}

		private Object getValueAt(int row, int col, ODLColumnType type) {
			Object ret = table.getValueAt(row, col);
			if (ret == null) {
				throw new RuntimeException("Distances input table has a null value: " + getElemDescription(row, col));
			}

			ret = ColumnValueProcessor.convertToMe(type,ret);
			if (ret == null) {
				throw new RuntimeException("Distances input table has a value which cannot be converted to correct type: " + getElemDescription(row, col) + ", type=" + Strings.convertEnumToDisplayFriendly(type.toString()));
			}

			return ret;
		}

		private String getElemDescription(int row, int col) {
			return "table=" + table.getName() + ", " + "row=" + (row + 1) + ", column=" + table.getColumnName(col);
		}

	}

	private synchronized ODLCostMatrix calculateGraphhopper(DistancesConfiguration request, StandardisedStringTreeMap<LatLong> points, final ProcessingApi processingApi) {
		CHMatrixGeneration graph=initGraphhopperGraph(request, processingApi);
		
		final StringBuilder statusMessage = new StringBuilder();
		statusMessage.append ("Loaded the graph " + new File(request.getGraphhopperConfig().getGraphDirectory()).getAbsolutePath());				
		statusMessage.append(System.lineSeparator() + "Calculating " + points.size() + "x" + points.size() + " matrix using Graphhopper road network distances.");
		if(processingApi!=null){
			processingApi.postStatusMessage(statusMessage.toString());			
		}

		// check for user cancellation
		if(processingApi!=null && processingApi.isCancelled()){
			return null;
		}

		// convert input to an array of graphhopper points
		int n = points.size();
		int i =0;
		List<Map.Entry<String, LatLong>> list = IteratorUtils.toList(points.entrySet());
		GHPoint []ghPoints = new GHPoint[n];
		for(Map.Entry<String, LatLong> entry:list){
			ghPoints[i++] = new GHPoint(entry.getValue().getLatitude(), entry.getValue().getLongitude());
		}
		
		// calculate the matrix 
		CHProcessingApi chprocApi=new CHProcessingApi() {
			
			@Override
			public void postStatusMessage(String s) {
				if(processingApi!=null){
					processingApi.postStatusMessage(s);	
				}
			}
			
			@Override
			public boolean isCancelled() {
				return processingApi!=null?processingApi.isCancelled():false;
			}
		};
		
		MatrixResult result = graph.calculateMatrix(ghPoints,chprocApi);
		if(processingApi!=null && processingApi.isCancelled()){
			return null;
		}

		// convert result to the output data structure
		ODLCostMatrixImpl output = ODLCostMatrixImpl.createEmptyMatrix(list);
		for (int ifrom = 0; ifrom < n; ifrom++) {
			for (int ito = 0; ito < n; ito++) {
				double timeSeconds = result.getTimeMilliseconds(ifrom, ito) * 0.001;
				timeSeconds *= request.getGraphhopperConfig().getTimeMultiplier();
				if(!result.isInfinite(ifrom, ito)){
					setOutputValues(ifrom, ito, result.getDistanceMetres(ifrom, ito), timeSeconds, request.getOutputConfig(), output);					
				}else{
					for(int k=0; k<3 ;k++){
						output.set(UNCONNECTED_TRAVEL_COST, ifrom, ito, k);						
					}
				}
			}
		}
		
		return output;
	}

	/**
	 * @param request
	 * @param processingApi
	 */
	private synchronized CHMatrixGeneration initGraphhopperGraph(DistancesConfiguration request, final ProcessingApi processingApi) {
		String dir = request.getGraphhopperConfig().getGraphDirectory();
		
		File current =RelativeFiles.validateRelativeFiles(dir, AppConstants.GRAPHHOPPER_DIRECTORY);
		if(current==null){
			throw new RuntimeException("Cannot identify Graphhopper directory: " + dir);			
		}
		current = current.getAbsoluteFile();
		
		if(processingApi!=null){
			processingApi.postStatusMessage("Loading the road network graph: " + current.getAbsolutePath());			
		}
		
		// check current file is valid
		if(!current.exists() || !current.isDirectory() || current.listFiles().length==0){
			throw new RuntimeException("Invalid or empty Graphhopper directory: " + dir);
		}
		
		// check if last loaded file is now an invalid file (i.e. no longer exists)
		if(lastCHGraph!=null){
			File file = new File(lastCHGraph.getGraphhopper().getGraphHopperLocation());
			if(!file.exists() || !file.isDirectory()){
				lastCHGraph.dispose();
				lastCHGraph = null;
			}
		}
		
		// check if using different file
		if(lastCHGraph!=null){
			File lastFile = new File(lastCHGraph.getGraphhopper().getGraphHopperLocation());
			if(!lastFile.equals(current)){
				lastCHGraph.dispose();
				lastCHGraph = null;				
			}		
		}
		
		// check if different file times
		if(lastCHGraph!=null && lastCHGraph.getNodesLastModifiedTime()!=CHMatrixGenWithGeomFuncs.getNodesFileLastModified(current.getAbsolutePath())){
			if(processingApi!=null){
				processingApi.postStatusMessage("Reloading the road network graph as file times have changed: " + current.getAbsolutePath());			
			}			
			lastCHGraph.dispose();
			lastCHGraph = null;	
		}
		
		// load the graph if needed
		if(lastCHGraph==null){
			lastCHGraph = new CHMatrixGenWithGeomFuncs(current.getAbsolutePath());
		}
		
		// adapt to the correct vehicle type
		String vehicleType = request.getGraphhopperConfig().getVehicleType();
		vehicleType = Strings.std(vehicleType);
		if(vehicleType.length()>0){
			return new CHMatrixGeneration(lastCHGraph.getGraphhopper(), vehicleType);
		}else{
			return lastCHGraph;
		}
	}

	private ODLCostMatrix calculateGreatCircle(DistancesConfiguration request, StandardisedStringTreeMap<LatLong> points, ProcessingApi processingApi) {
		if(processingApi!=null){
			processingApi.postStatusMessage("Calculating " + points.size() + "x" + (points.size() + " matrix using great circle distance (i.e. straight line)"));			
		}
		
		List<Map.Entry<String, LatLong>> list = IteratorUtils.toList(points.entrySet());
		ODLCostMatrixImpl output = ODLCostMatrixImpl.createEmptyMatrix(list);

		int n = list.size();
		for (int ifrom = 0; ifrom < n; ifrom++) {
			Map.Entry<String, LatLong> from = list.get(ifrom);
			for (int ito = 0; ito < n; ito++) {
				Map.Entry<String, LatLong> to = list.get(ito);

				double distanceMetres = GreateCircle.greatCircleApprox(from.getValue(), to.getValue());

				distanceMetres *= request.getGreatCircleConfig().getDistanceMultiplier();

				double timeSecs = distanceMetres / request.getGreatCircleConfig().getSpeedMetresPerSec();

				// output cost and time
				setOutputValues(ifrom, ito, distanceMetres, timeSecs, request.getOutputConfig(), output);

				// check for user cancellation
				if (processingApi!=null && processingApi.isCancelled()) {
					break;
				}

			}
		}

		return output;
	}
	
	private ODLCostMatrix calculateFromFile(DistancesConfiguration request, StandardisedStringTreeMap<LatLong> points, ProcessingApi processingApi) {
		File file = MatrixFileReader.resolveExternalMatrixFileOrThrowException(request.getExternalConfig(), processingApi.getApi().io().getLoadedExcelFile());
		
		// load the file
		LoadedMatrixFile loadedMatrixFile = MatrixFileReader.loadFile(file, processingApi);		

		// match to locations
		List<Map.Entry<String, LatLong>> list = IteratorUtils.toList(points.entrySet());
		RoundingGrid grid = MatrixFileReader.ROUNDING_GRID;		
		TIntArrayList indices = new TIntArrayList(list.size());
		for(Map.Entry<String, LatLong> entry:list){
			
			// check for a known entry in the rounding grid, checking within a couple of metres
			Integer foundIndx=null;
			List<GridNeighboursResult> ngbs =grid.calculateNeighbouringGridCells(entry.getValue(), 3); 
			for(GridNeighboursResult ngb:ngbs){
				foundIndx = loadedMatrixFile.getLocationsToIndices().get(ngb.getLatLong());
				if(foundIndx!=-1){
					break;
				}
			}
			
			if(foundIndx==-1){
				throw new RuntimeException("Latitude-longitude " + entry.getValue().toString() + " does not exist in the matrix loaded from file " + file.getName() + ".");
			}
			
			indices.add(foundIndx);
		}

		// create cost matrix, including the logic to check if the file has changed
		@SuppressWarnings("serial")
		ODLCostMatrixImpl output = new ODLCostMatrixImpl(points.keySet(), ODLCostMatrixImpl.STANDARD_COST_FIELDNAMES){
			@Override
			public boolean isStillValid() {
				return !FileVersionId.isFileModified(loadedMatrixFile.getFileVersionId());
			}	
		};

		// copy values across, calculating cost from distance and time
		int n = list.size();
		for (int ifrom = 0; ifrom < n; ifrom++) {
			for (int ito = 0; ito < n; ito++) {
				
				double distanceMetres =loadedMatrixFile.get(indices.get(ifrom), indices.get(ito), ValueType.KM)* 1000;
				double timeSecs =loadedMatrixFile.get(indices.get(ifrom), indices.get(ito), ValueType.SECONDS);

				// output cost and time
				setOutputValues(ifrom, ito, distanceMetres, timeSecs, request.getOutputConfig(), output);

				// check for user cancellation
				if (processingApi!=null && processingApi.isCancelled()) {
					break;
				}

			}
		}

		return output;
	}


	private void setOutputValues(int ifrom, int ito, double distanceMetres, double timeSecs, DistancesOutputConfiguration outputConfig, ODLCostMatrixImpl output) {
		double value = processOutput(distanceMetres, timeSecs, outputConfig);
		output.set(value, ifrom, ito, ODLCostMatrix.COST_MATRIX_INDEX_COST);
		output.set(processedDistance(distanceMetres, outputConfig), ifrom, ito, ODLCostMatrix.COST_MATRIX_INDEX_DISTANCE);
		output.set(processedTime(timeSecs, outputConfig), ifrom, ito, ODLCostMatrix.COST_MATRIX_INDEX_TIME);
	}



	public static DistancesSingleton singleton() {
		return singleton;
	}

	private static class AToBCacheKey{
		final private DistancesConfiguration request;
		final private LatLong from;
		final private LatLong to;
		final static int ESTIMATED_SIZE_BYTES=200;
		
		AToBCacheKey(DistancesConfiguration request, LatLong from, LatLong to) {
			this.request = request.deepCopy();
			this.from = new LatLongImpl(from);
			this.to = new LatLongImpl(to);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((request == null) ? 0 : request.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
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
			AToBCacheKey other = (AToBCacheKey) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (request == null) {
				if (other.request != null)
					return false;
			} else if (!request.equals(other.request))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}
		
		
	}
	
	private static class MatrixCacheKey {
		final private DistancesConfiguration distanceConfig;
		final private StandardisedStringTreeMap<LatLong> points;
		final private int hashcode;

		private MatrixCacheKey(DistancesConfiguration request, StandardisedStringTreeMap<LatLong> points) {
			this.distanceConfig = request.deepCopy();
			this.points = points;

			final int prime = 31;
			int result = 1;
			result = prime * result + ((points == null) ? 0 : points.hashCode());
			result = prime * result + ((request == null) ? 0 : request.hashCode());
			hashcode = result;
		}

		@Override
		public int hashCode() {
			return hashcode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MatrixCacheKey other = (MatrixCacheKey) obj;
			if (points == null) {
				if (other.points != null)
					return false;
			} else if (!points.equals(other.points))
				return false;
			if (distanceConfig == null) {
				if (other.distanceConfig != null)
					return false;
			} else if (!distanceConfig.equals(other.distanceConfig))
				return false;
			return true;
		}

	}

	/**
	 * This is called from the driving distance function
	 * @param request
	 * @param from
	 * @param to
	 * @param processingApi
	 * @return
	 */
	public synchronized double calculateDistanceMetres(DistancesConfiguration request, LatLong from, LatLong to, ProcessingApi processingApi){
		if(request.getMethod() == CalculationMethod.EXTERNAL_MATRIX){
			throw new RuntimeException("Unsupported mode: " + CalculationMethod.EXTERNAL_MATRIX.toString());
		}
		
		if(request.getMethod() == CalculationMethod.GREAT_CIRCLE){
			return GreateCircle.greatCircleApprox(from, to);
		}
		
		AToBCacheKey key = new AToBCacheKey(request, from, to);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.A_TO_B_DISTANCE_METRES_CACHE);
		Double ret = (Double) cache.get(key);
		if (ret != null) {
			return ret;
		}

		CHMatrixGeneration graph=initGraphhopperGraph(request, processingApi);
		
		ret = CHMatrixGenWithGeomFuncs.calculateDistanceMetres(graph,from, to);
		if(ret!=null){
			cacheAToBDouble(key, ret, cache);
		}
		
		return ret;
	}


	public enum CacheOption{
		USE_CACHING,
		NO_CACHING
	}
	
	/**
	 * This is called from the driving time function
	 * @param request
	 * @param from
	 * @param to
	 * @param processingApi
	 * @return
	 */
	public synchronized ODLTime calculateDrivingTime(DistancesConfiguration request, LatLong from, LatLong to, CacheOption cacheOption, ProcessingApi processingApi){
		if(request.getMethod() != CalculationMethod.ROAD_NETWORK){
			throw new IllegalArgumentException();
		}
		
		AToBCacheKey key=null;
		ODLTime ret=null;
		RecentlyUsedCache cache =null;
		if(cacheOption != CacheOption.NO_CACHING){
			key = new AToBCacheKey(request, from, to);
			cache = ApplicationCache.singleton().get(ApplicationCache.A_TO_B_TIME_SECONDS_CACHE);
			ret = (ODLTime) cache.get(key);
			if (ret != null) {
				return ret;
			}			
		}


		CHMatrixGeneration graph=initGraphhopperGraph(request, processingApi);
		
		ret = CHMatrixGenWithGeomFuncs.calculateTime(graph,from, to);
		
		if(cacheOption!=CacheOption.NO_CACHING){
			if(ret!=null){
				int estimatedSize = 16 + AToBCacheKey.ESTIMATED_SIZE_BYTES;
				cache.put(key, ret,estimatedSize);
			}
					
		}

		return ret;
	}
	
	
	/**
	 * @param key
	 * @param ret
	 * @param cache
	 */
	protected void cacheAToBDouble(AToBCacheKey key, Double ret, RecentlyUsedCache cache) {
		int estimatedSize = 8 + AToBCacheKey.ESTIMATED_SIZE_BYTES;
		cache.put(key, ret,estimatedSize);
	}

	
	public synchronized ODLGeom calculateRouteGeom(DistancesConfiguration request, LatLong from, LatLong to,CacheOption cacheOption, ProcessingApi processingApi){
		// If we're using great circle or external matrix just return a straight line.
		// When using an external matrix the route geometry will be undefined / unavailable so a straight line is fine.
		if(request.getMethod() == CalculationMethod.GREAT_CIRCLE || request.getMethod() == CalculationMethod.EXTERNAL_MATRIX){
			ODLGeom geom = processingApi.getApi().geometry().createLineGeometry(from, to);
			return geom;
		}

		
		AToBCacheKey key = new AToBCacheKey(request, from, to);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.ROUTE_GEOMETRY_CACHE);
		ODLGeom ret=null;
		if(cacheOption!=CacheOption.NO_CACHING){
			ret = (ODLGeom) cache.get(key);
			if (ret != null) {
				return ret;
			}			
		}


		CHMatrixGeneration graph=initGraphhopperGraph(request, processingApi);
		
		ret = CHMatrixGenWithGeomFuncs.calculateRouteGeom(graph,from, to);
		if(ret!=null && cacheOption!=CacheOption.NO_CACHING){
			int estimatedSize = 40 * ret.getPointsCount() + AToBCacheKey.ESTIMATED_SIZE_BYTES;
			cache.put(key, ret,estimatedSize);
		}
		
		// give a straight line if all else fails
		if(ret==null){
			ret = new GeometryImpl().createLineGeometry(from, to);
		//	ret = processingApi.getApi().geometry().createLineGeometry(from, to);			
		}
		return ret;
	}

	public synchronized ODLCostMatrix calculate(DistancesConfiguration request, ProcessingApi processingApi, ODLTableReadOnly... tables) {
		
		// get all locations
		StandardisedStringTreeMap<LatLong> points = getPoints(tables);

		MatrixCacheKey key = new MatrixCacheKey(request, points);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.DISTANCE_MATRIX_CACHE);
		ODLCostMatrix ret = (ODLCostMatrix) cache.get(key);
		if (ret != null && ret.isStillValid()) {
			return ret;
		}

		switch (request.getMethod()) {
		case GREAT_CIRCLE:
			ret = calculateGreatCircle(request, points, processingApi);
			break;

		case ROAD_NETWORK:
			ret = calculateGraphhopper(request, points, processingApi);
			break;
			
		case EXTERNAL_MATRIX:
			ret = calculateFromFile(request, points, processingApi);
			break;
			
		default:
			throw new UnsupportedOperationException(request.getMethod().toString() + " is unsupported.");
		}

		if(ret.getSizeInBytes() < Integer.MAX_VALUE){
			cache.put(key, ret, (int)ret.getSizeInBytes());			
		}

		return ret;
	}

	private StandardisedStringTreeMap<LatLong> getPoints(ODLTableReadOnly... tables) {
		StandardisedStringTreeMap<LatLong> points = new StandardisedStringTreeMap<>(false);
		for (ODLTableReadOnly table : tables) {
			InputTableAccessor accessor = new InputTableAccessor(table);
			int nr = table.getRowCount();
			for (int row = 0; row < nr; row++) {
				LatLongImpl ll = new LatLongImpl(accessor.getLatitude(row), accessor.getLongitude(row));
				String id = accessor.getLoc(row);
				if (points.get(id) != null && points.get(id).equals(ll) == false) {
					throw new RuntimeException("Location id defined twice with different latitude/longitude pairs: " + id);
				}
				points.put(id, ll);
			}
		}
		return points;
	}

	private double processOutput(double distanceMetres, double timeSeconds, DistancesOutputConfiguration config) {
		// get distance in correct units
		double distance = processedDistance(distanceMetres, config);

		// get time in correct units
		double time = processedTime(timeSeconds, config);

		switch (config.getOutputType()) {

		case DISTANCE:
			return distance;

		case TIME:
			return time;

		case SUMMED:
			return config.getTimeWeighting() * time + config.getDistanceWeighting() * distance;

		default:
			throw new UnsupportedOperationException();
		}
	}

	private double processedTime(double timeSeconds, DistancesOutputConfiguration config) {
		double time = 0;
		switch (config.getOutputTimeUnit()) {
		case MILLISECONDS:
			time = timeSeconds * 1000;
			break;
			
		case SECONDS:
			time = timeSeconds;
			break;

		case MINUTES:
			time = timeSeconds * (1.0 / 60.0);
			break;

		case HOURS:
			time = timeSeconds * (1.0 / (60.0 * 60.0));
			break;

		default:
			throw new UnsupportedOperationException();
		}
		return time;
	}

	private double processedDistance(double distanceMetres, DistancesOutputConfiguration config) {
		double distance = 0;
		switch (config.getOutputDistanceUnit()) {
		case METRES:
			distance = distanceMetres;
			break;

		case KILOMETRES:
			distance = distanceMetres / 1000;
			break;

		case MILES:
			distance = (distanceMetres / 1000) * 0.621371;
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return distance;
	}

	@Override
	public synchronized void close() {
		closeCHGraph();
	}
	
	public synchronized void closeCHGraph(){
		if(lastCHGraph!=null){
			lastCHGraph.dispose();
			lastCHGraph = null;
		}
	}
}
