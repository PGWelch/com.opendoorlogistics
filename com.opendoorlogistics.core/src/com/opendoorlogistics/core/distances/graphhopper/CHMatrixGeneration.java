/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances.graphhopper;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.PathBidirRef;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.ch.Path4CH;
import com.graphhopper.routing.ch.PreparationWeighting;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.LevelEdgeFilter;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import com.opendoorlogistics.api.components.ContinueProcessingCB;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.AppProperties;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CHMatrixGeneration implements Disposable {
	private final String graphFolder;
	private final GraphHopper hopper;
	private final EncodingManager encodingManager;
	private final EdgeFilter edgeFilter;
	private final PreparationWeighting prepareWeighting;
	private final boolean useExpansionCache = true;
	private final boolean outputText = false;
	private final FlagEncoder flagEncoder;
	private final Weighting weighting;
	private final CHGraph chGraph;
	private final LevelEdgeFilter levelEdgeFilter;

	private static class FromIndexEdge {
		private final int fromIndex;
		private final EdgeEntry edge;

		FromIndexEdge(int fromIndex, EdgeEntry edge) {
			super();
			this.fromIndex = fromIndex;
			this.edge = edge;
		}

	}

	public ODLGeom calculateRouteGeom(LatLong from, LatLong to){
		GHRequest req = new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude()).setVehicle("car");
		GHResponse rsp = hopper.route(req);

		if(rsp.hasErrors()) {
		   return null;
		}


		PointList pointList = rsp.getPoints();	
		int n = pointList.size();
		if(n<2){
			return null;
		}
		
		Spatial.initSpatial();
		Coordinate[] coords = new Coordinate[n];
		for(int i =0 ; i<n;i++){
			coords[i] = new Coordinate(pointList.getLongitude(i), pointList.getLatitude(i));
		}
		
		GeometryFactory factory = new GeometryFactory();
		Geometry geometry = factory.createLineString(coords);
		ODLGeomImpl ret = new ODLLoadedGeometry(geometry);
		return ret;
	}

	private static GraphHopper createHopper(){
		String config = AppProperties.getValue("graphhopper.config");
		GraphHopper ret = null;
		if(Strings.equalsStd(config, "mobile")){
			ret = new GraphHopper().forMobile();
		}
		else if(Strings.equalsStd(config, "server")){
			ret= new GraphHopper().forServer();
		}
		else if(Strings.equalsStd(config, "desktop")){
			ret= new GraphHopper().forDesktop();
		}	
		
		System.err.println("Unidentified grapphopper config, defaulting to desktop.");
		ret= new GraphHopper().forDesktop();
		
		// don't need to write so disable the lock file (allows us to run out of program files)
		ret.setAllowWrites(false);
		return ret;
	}
	
	public CHMatrixGeneration(String graphFolder) {
		this.graphFolder = graphFolder;
		this.hopper = createHopper();
		hopper.setGraphHopperLocation(this.graphFolder);
		hopper.setEncodingManager(new EncodingManager(EncodingManager.CAR));
		flagEncoder = hopper.getEncodingManager().getEncoder(EncodingManager.CAR);
		hopper.importOrLoad();
		encodingManager = hopper.getEncodingManager();

		String vehicle = flagEncoder.toString();
		if (!hopper.getEncodingManager().supports(vehicle)) {
			throw new RuntimeException(new IllegalArgumentException("Vehicle " + vehicle + " unsupported. " + "Supported are: " + hopper.getEncodingManager()));
		}

		edgeFilter = new DefaultEdgeFilter(encodingManager.getEncoder(vehicle));

//		if (hopper.getPreparation() == null) {
//			throw new RuntimeException("Preparation object is null. CH-preparation wasn't done or did you forgot to call disableCHShortcuts()?");
//		}

		WeightingMap weightingMap = new WeightingMap("fastest");
		weighting = hopper.createWeighting(weightingMap, flagEncoder);
		prepareWeighting = new PreparationWeighting(weighting);

		// save reference to the CH graph
		chGraph = hopper.getGraphHopperStorage().getGraph(CHGraph.class);
		
		// and create a level edge filter to ensure we (a) accept virtual (snap-to) edges and (b) don't descend into the base graph
		levelEdgeFilter = new LevelEdgeFilter(chGraph);
	}

	@Override
	public void dispose() {
		hopper.close();
	}


	public GHResponse getResponse(GHPoint from , GHPoint to){
		GHRequest req = new GHRequest(from, to).setVehicle("car");
		GHResponse rsp = hopper.route(req);
		if (rsp.hasErrors()) {
			return null;
		}

		return rsp;
	}
	
	/**
	 * Return the distance in metres or positive infinity if route not found found
	 * @param from
	 * @param to
	 * @return
	 */
	public double calculateDistanceMetres(LatLong from ,LatLong to){
		GHResponse resp = getResponse(from, to);
		if(resp!=null){
			return resp.getDistance();
		}
		return Double.POSITIVE_INFINITY;
	}

	
	/**
	 * Return the time or null if route not found
	 * @param from
	 * @param to
	 * @return
	 */	
	public ODLTime calculateTime(LatLong from ,LatLong to){
		GHResponse resp = getResponse(from, to);
		if(resp!=null){
			return new ODLTime(resp.getMillis());
		}
		return null;
	}
	
	/**
	 * @param from
	 * @param to
	 * @return
	 */
	protected GHResponse getResponse(LatLong from, LatLong to) {
		GHResponse resp = getResponse(new GHPoint(from.getLatitude(), from.getLongitude()), new GHPoint(to.getLatitude(), to.getLongitude()));
		return resp;
	}
	
	public MatrixResult calculateMatrixOneByOne(GHPoint[] points) {
		int n = points.length;
		MatrixResult ret = new MatrixResult(n);
		for (int fromIndex = 0; fromIndex < n; fromIndex++) {

			// Loop over TO in reverse order so the first A-B we process doesn't have the same
			// location for FROM and TO - this makes it quicker to debug as the first call is no longer a 'dummy' one.
			for (int toIndex = n-1; toIndex >=0; toIndex--) {
				GHPoint from = points[fromIndex];
				GHPoint to = points[toIndex];
				GHResponse rsp = getResponse(from, to);
				if(rsp==null){
					continue;
				}
				ret.setDistanceMetres(fromIndex, toIndex, rsp.getDistance());
				ret.setTimeMilliseconds(fromIndex, toIndex, rsp.getTime());
			}
		}
		return ret;
	}

	public MatrixResult calculateMatrix(GHPoint[] points, ProcessingApi processingApi) {
		if (outputText) {
			System.out.println("Starting calculate matrix");
		}

		// query positions
		List<QueryResult> validResults = new ArrayList<QueryResult>(points.length);
		QueryResult[] queryResults = queryPositions(points, validResults);
		if(processingApi!=null && processingApi.isCancelled()){
			return null;
		}
		
		// create the query graph
		if (outputText) {
			System.out.println("Creating query graph");
		}
		if(processingApi!=null){
			processingApi.postStatusMessage("Querying positions against graph");			
		}
		
		//final QueryGraph queryGraph = new QueryGraph(hopper.getGraphHopperStorage());
		final QueryGraph queryGraph = new QueryGraph(chGraph);
		queryGraph.lookup(validResults);
		if(processingApi!=null && processingApi.isCancelled()){
			return null;
		}
		
		// run the search forward individually from each point
		final SearchResult[] forwardTrees = new SearchResult[points.length];
		final TIntObjectHashMap<List<FromIndexEdge>> visitedByNodeId = new TIntObjectHashMap<>();
		if(processingApi!=null){
			processingApi.postStatusMessage("Performing forward search");					
		}
		searchAllForward(queryResults, queryGraph, forwardTrees, visitedByNodeId, processingApi);
		if(processingApi!=null && processingApi.isCancelled()){
			return null;
		}
		
		// run the search backward for all
		MatrixResult ret = searchAllBackward(queryResults, queryGraph, forwardTrees, visitedByNodeId, processingApi);
		if(processingApi!=null && processingApi.isCancelled()){
			return null;
		}
		
		if (outputText) {
			System.out.println("Finished calculate matrix");
		}

		return ret;
	}

	private MatrixResult searchAllBackward(QueryResult[] queryResults, final QueryGraph queryGraph, final SearchResult[] forwardTrees,
			final TIntObjectHashMap<List<FromIndexEdge>> visitedByNodeId, ProcessingApi processingApi) {

		if (outputText) {
			System.out.println("Running backward searches and extracting matrix results");
		}

		// instantiate return object
		final int n = queryResults.length;
		MatrixResult ret = new MatrixResult(n);

		// create a cache of expanded edge results
		final HashMap<EdgeNodeIdHashKey, DistanceTime> expansionCache;
		if (useExpansionCache) {
			expansionCache = new HashMap<>();
		} else {
			expansionCache = null;
		}

		// now query all in a reverse direction, building up the final matrix
		// for each one
		EdgeExplorer inEdgeExplorer = queryGraph.createEdgeExplorer(new DefaultEdgeFilter(flagEncoder, true, false));
		UpdateTimer timer = new UpdateTimer(100);
		for (int toIndex = 0; toIndex < n; toIndex++) {
	
			// check for user quitting
			if(processingApi!=null && processingApi.isCancelled()){
				return null;
			}
			
			if (queryResults[toIndex].isValid()) {
				// run query
				SearchResult reverseTree = search(prepareWeighting, queryResults[toIndex].getClosestNode(), inEdgeExplorer, true);

				// This reverse tree is used to find all results going TO the current point.

				// Parse all nodes of the reverse tree finding the minimum cost meeting node for each from
				final double[] minCost = new double[n];
				Arrays.fill(minCost, Double.POSITIVE_INFINITY);
				final int[] minCostNode = new int[n];
				Arrays.fill(minCostNode, -1);
				reverseTree.forEachEntry(new TIntObjectProcedure<EdgeEntry>() {

					@Override
					public boolean execute(int meetingPointNode, EdgeEntry reverseEdge) {
						// Use list of all FROM trees which encountered this node
						List<FromIndexEdge> list = visitedByNodeId.get(meetingPointNode);
						if (list == null) {
							return true;
						}
						int size = list.size();
						for (int i = 0; i < size; i++) {
							FromIndexEdge fie = list.get(i);
							int fromIndex = fie.fromIndex;
							EdgeEntry forwardEdge = fie.edge;
							// see if this meeting point has a lower cost  than the other
							double cost = forwardEdge.weight + reverseEdge.weight;
							if (cost < minCost[fromIndex]) {
								minCost[fromIndex] = cost;
								minCostNode[fromIndex] = meetingPointNode;
							}
						}
						return true;
					}
				});

				// extract the path for each one so we can get the distance and time
				for (int fromIndex = 0; fromIndex < n; fromIndex++) {
					int meetingPointNode = minCostNode[fromIndex];
					if (meetingPointNode != -1) {
						
						// use a cache of expanded CH edges for performance reasons
						CacheablePath4CH pathCh = new CacheablePath4CH(queryGraph, flagEncoder, expansionCache);
						pathCh.setSwitchToFrom(false);
						pathCh.setEdgeEntry(forwardTrees[fromIndex].get(meetingPointNode));
						pathCh.setEdgeEntryTo(reverseTree.get(meetingPointNode));
						Path path = pathCh.extract();
						ret.setTimeMilliseconds(fromIndex, toIndex, path.getTime());
						ret.setDistanceMetres(fromIndex, toIndex, path.getDistance());
					}
				}
			}
			
			if(timer.isUpdate() && processingApi!=null){
				processingApi.postStatusMessage("Performed backwards search for " + (toIndex+1) + "/" + n + " points");
			}
		}
		return ret;
	}

	private QueryResult[] queryPositions(GHPoint[] points, List<QueryResult> validResults) {
		if (outputText) {
			System.out.println("Querying positions against graph");
		}
		QueryResult[] queryResults = new QueryResult[points.length];
		for (int i = 0; i < points.length; i++) {
			queryResults[i] = hopper.getLocationIndex().findClosest(points[i].getLat(), points[i].getLon(), edgeFilter);
			if (queryResults[i].isValid()) {
				validResults.add(queryResults[i]);
			}
		}
//		for(int i =0 ; i<queryResults.length;i++){
//			if(queryResults[i]!=null){
//				System.out.println("" + i + " : " + queryResults[i].getQueryDistance() + " " + queryResults[i].getClosestNode());
//			}
//		}
		return queryResults;
	}

	private void searchAllForward(QueryResult[] queryResults, final QueryGraph queryGraph, final SearchResult[] forwardTrees,
			final TIntObjectHashMap<List<FromIndexEdge>> visitedByNodeId, ContinueProcessingCB continueCB) {

		if (outputText) {
			System.out.println("Running forward searches");
		}

		EdgeExplorer outEdgeExplorer = queryGraph.createEdgeExplorer(new DefaultEdgeFilter(flagEncoder, false, true));
		for (int fromIndex = 0; fromIndex < queryResults.length; fromIndex++) {
			
			// check for user quitting
			if(continueCB!=null && continueCB.isCancelled()){
				return;
			}
			
			final int finalFromIndx = fromIndex;
			if (queryResults[fromIndex].isValid()) {
				forwardTrees[fromIndex] = search(prepareWeighting, queryResults[fromIndex].getClosestNode(), outEdgeExplorer, false);
				forwardTrees[fromIndex].forEachEntry(new TIntObjectProcedure<EdgeEntry>() {

					@Override
					public boolean execute(int nodeId, EdgeEntry edge) {
						List<FromIndexEdge> visited = visitedByNodeId.get(nodeId);
						if (visited == null) {
							visited = new ArrayList<>(1);
							visitedByNodeId.put(nodeId, visited);
						}
						visited.add(new FromIndexEdge(finalFromIndx, edge));
						return true;
					}
				});
			}
		}
	}


	private static class SearchResult extends TIntObjectHashMap<EdgeEntry> {

	}

	private static class DistanceTime {
		private final double distance;
		private final long time;

		DistanceTime(double distance, long time) {
			this.distance = distance;
			this.time = time;
		}

		public double getDistance() {
			return distance;
		}

		public long getMillis() {
			return time;
		}

	}

	private static class EdgeNodeIdHashKey {
		private int edgeId;
		private int endNodeId;

		public EdgeNodeIdHashKey(int edgeId, int endNodeId) {
			this.edgeId = edgeId;
			this.endNodeId = endNodeId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + edgeId;
			result = prime * result + endNodeId;
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
			EdgeNodeIdHashKey other = (EdgeNodeIdHashKey) obj;
			if (edgeId != other.edgeId)
				return false;
			if (endNodeId != other.endNodeId)
				return false;
			return true;
		}

		public void setEdgeId(int edgeId) {
			this.edgeId = edgeId;
		}


		public void setEndNodeId(int endNodeId) {
			this.endNodeId = endNodeId;
		}

		
	}

	private SearchResult search(PreparationWeighting weighting, int node, EdgeExplorer edgeExplorer, boolean reverse) {

		PriorityQueue<EdgeEntry> openSet = new PriorityQueue<>();
		SearchResult shortestWeightMap = new SearchResult();

		EdgeEntry firstEdge = new EdgeEntry(EdgeIterator.NO_EDGE, node, 0);
		shortestWeightMap.put(node, firstEdge);
		openSet.add(firstEdge);

		// int nodeCount = 0;

		while (openSet.size() > 0) {

			// The node at the adjacent edge is now settled.
			EdgeEntry currEdge = openSet.poll();
			int currNode = currEdge.adjNode;
			EdgeIterator iter = edgeExplorer.setBaseNode(currNode);

			// nodeCount++;
			// System.out.println("" + nodeCount + ": " + currEdge.edge +
			// " weight=" + currEdge.weight + " opencount=" + openSet.size());

			while (iter.next()) {
				int adjNode = iter.getAdjNode();

				// Filter out the base (no CH) graph
				if(!levelEdgeFilter.accept(iter)){
					continue;
				}
				
				// As turn restrictions aren't enabled at the moment we should be safe putting
				// a non-existent edge for the previous or next edge, though we should fix this in the future...
				int previousOrNextEdge=-1;
				double tmpWeight = weighting.calcWeight(iter, reverse,previousOrNextEdge) + currEdge.weight;

				EdgeEntry de = shortestWeightMap.get(adjNode);
				if (de == null) {
					de = new EdgeEntry(iter.getEdge(), adjNode, tmpWeight);
					de.parent = currEdge;
					shortestWeightMap.put(adjNode, de);
					openSet.add(de);
				} else if (de.weight > tmpWeight) {
					// Update the weight (i.e. travel cost) on the node.
					// This should never be called for a settled node as the
					// existing weight will be lower than the tmpWeight
					openSet.remove(de);
					de.edge = iter.getEdge();
					de.weight = tmpWeight;
					de.parent = currEdge;
					openSet.add(de);
				}
			}

		}
		
//		System.out.println("From " + node + " " + shortestWeightMap.size() + " nodes found in search");
//		if(shortestWeightMap.size()==1){
//			shortestWeightMap.forEachEntry(new TIntObjectProcedure<EdgeEntry>() {
//
//				@Override
//				public boolean execute(int a, EdgeEntry b) {
//					System.out.println("\t" + b.weight);
//					return true;
//				}
//			});
//		}
		return shortestWeightMap;
	}

//	/**
//	 * Get the result of expanding a single CH edge
//	 * 
//	 * @param graph
//	 * @param tmpEdge
//	 * @param endNode
//	 * @return
//	 */
//	private DistanceTime getExpandedCHEdge(Graph graph, final int tmpEdge, final int endNode) {
//		class SingleEdgeExpander extends Path4CH {
//			public SingleEdgeExpander(Graph g, FlagEncoder encoder) {
//				super(g, encoder);
//			}
//
//			/**
//			 * Override to make method available to code below...
//			 */
//			@Override
//			protected void processEdge(int tmpEdge, int endNode) {
//				super.processEdge(tmpEdge, endNode);
//			}
//
//		}
//		SingleEdgeExpander see = new SingleEdgeExpander(graph, flagEncoder);
//		see.processEdge(tmpEdge, endNode);
//		return new DistanceTime(see.getDistance(), see.getTime());
//	}
	
	public String getGraphFolder(){
		return graphFolder;
	}
	
	public GraphHopper getGraphhopper(){
		return hopper;
	}
	
	public static void main(String[]args){
		CHMatrixGeneration gen = new CHMatrixGeneration("C:\\Demo\\Graphhopper");
		int n = 1000;
		GHPoint a = new GHPoint(52.407995203838	,-1.50572174886011);
		GHPoint b = new GHPoint(52.	,0.3);
		GHPoint [] array = new GHPoint[]{a,b};
		gen.calculateMatrixOneByOne(array);			
		long startMillis = System.currentTimeMillis();
		for(int i=0 ; i < n ; i++){
			gen.calculateMatrixOneByOne(array);			
		}
		long endMillis = System.currentTimeMillis();
		System.out.println("Time for " + n + " calculations:" + (endMillis - startMillis) + " milliseconds");
	//	System.out.println(result);
		
		//noRouteExample();
	}
	
	public static void noRouteExample(){
		// problem position
		GHPoint a = new GHPoint(52.407995203838	,-1.50572174886011);
		
		// second position can be anywhere (except at the first position)
		GHPoint b = new GHPoint(52.	,-1.3);		
		
		GraphHopper hopper = new GraphHopper().forDesktop();
		hopper.setInMemory();
		
		
		hopper.setGraphHopperLocation("C:\\data\\graphhopper\\graphhopper\\europe_great-britain-gh");
//		hopper.setGraphHopperLocation("C:\\Demo\\Graphhopper");
		hopper.importOrLoad();
		
		GHRequest req = new GHRequest(a,b);
		GHResponse rsp = hopper.route(req);
		for(Throwable thr : rsp.getErrors()){
			System.out.println(thr);
		}
		//System.out.println("Found = " + rsp.isFound());
		System.out.println("Distance = " + rsp.getDistance());

	}
	
	/**
	 * Modified implementation of Graphhopper's original Path4CH class which allows 
	 * for the caching the expanding of shortcut edges.
	 * @author Phil based on Peter Karich's implementation 
	 *
	 */
	private class CacheablePath4CH extends PathBidirRef
	{
	    private final Graph routingGraph;
	    private final HashMap<EdgeNodeIdHashKey, DistanceTime> expansionCache;
	    private final EdgeNodeIdHashKey cacheKey = new EdgeNodeIdHashKey(-1, -1);
	    private final FlagEncoder encoder;
	    public CacheablePath4CH( Graph chGraph,FlagEncoder encoder,HashMap<EdgeNodeIdHashKey, DistanceTime> expansionCache )
	    {
	        super(chGraph.getBaseGraph(), encoder);
	        this.routingGraph = chGraph;
	        this.expansionCache = expansionCache;
	        this.encoder = encoder;
	    }

	    @Override
	    protected final void processEdge( int tmpEdge, int endNode )
	    {
			if (expansionCache!=null) {
				// try getting from cache
				EdgeNodeIdHashKey edgnid = new EdgeNodeIdHashKey(tmpEdge, endNode);
				cacheKey.setEdgeId(tmpEdge);
				cacheKey.setEndNodeId(endNode);
				DistanceTime dt = expansionCache.get(edgnid);
				
				// calculate using a new instance without the cache if needed
				if (dt == null) {
					CacheablePath4CH noCache = new CacheablePath4CH(routingGraph, encoder, null);
					noCache.processEdge(tmpEdge, endNode);
					dt = new DistanceTime(noCache.getDistance(), noCache.getTime());
					expansionCache.put(edgnid, dt);
				}
				
				distance += dt.getDistance();
				time += dt.getMillis();
			} else {
		        expandEdge((CHEdgeIteratorState) routingGraph.getEdgeIteratorState(tmpEdge, endNode), false);
			}	    	
	    }


	    /**
	     * Peter Karich's implementation from Path4CH
	     * @param mainEdgeState
	     * @param reverse
	     */
	    private void expandEdge( CHEdgeIteratorState mainEdgeState, boolean reverse )
	    {
	        if (!mainEdgeState.isShortcut())
	        {
	            double dist = mainEdgeState.getDistance();
	            distance += dist;
	            long flags = mainEdgeState.getFlags();
	            time += calcMillis(dist, flags, reverse);
	            addEdge(mainEdgeState.getEdge());
	            return;
	        }

	        int skippedEdge1 = mainEdgeState.getSkippedEdge1();
	        int skippedEdge2 = mainEdgeState.getSkippedEdge2();
	        int from = mainEdgeState.getBaseNode(), to = mainEdgeState.getAdjNode();

	        // get properties like speed of the edge in the correct direction
	        if (reverse)
	        {
	            int tmp = from;
	            from = to;
	            to = tmp;
	        }

	        // getEdgeProps could possibly return an empty edge if the shortcut is available for both directions
	        if (reverseOrder)
	        {
	            CHEdgeIteratorState edgeState = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge1, to);
	            boolean empty = edgeState == null;
	            if (empty)
	                edgeState = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge2, to);

	            expandEdge(edgeState, false);

	            if (empty)
	                edgeState = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge1, from);
	            else
	                edgeState = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge2, from);

	            expandEdge(edgeState, true);
	        } else
	        {
	            CHEdgeIteratorState iter = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge1, from);
	            boolean empty = iter == null;
	            if (empty)
	                iter = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge2, from);

	            expandEdge(iter, true);

	            if (empty)
	                iter = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge1, to);
	            else
	                iter = (CHEdgeIteratorState) routingGraph.getEdgeIteratorState(skippedEdge2, to);

	            expandEdge(iter, false);
	        }
	    }
	}

}
