/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.graphhopper;

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
import com.graphhopper.routing.ch.PreparationWeighting;
import com.graphhopper.routing.util.BikeFlagEncoder;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.routing.util.LevelEdgeFilter;
import com.graphhopper.routing.util.MotorcycleFlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.RAMDirectory;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.PMap;
import com.graphhopper.util.shapes.GHPoint;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;

public class CHMatrixGeneration {
	/**
	 * The location of these strings in graphhopper changes from 0.5 to latest code
	 * so we keep a common reference to them here for the rest of the code to use
	 */
	public static final String VEHICLE_TYPE_CAR = EncodingManager.CAR;
	public static final String VEHICLE_TYPE_BIKE = EncodingManager.BIKE;
	public static final String VEHICLE_TYPE_FOOT = EncodingManager.FOOT;
	public static final String VEHICLE_TYPE_BIKE2 = EncodingManager.BIKE2;
	public static final String VEHICLE_TYPE_MOTORCYCLE = EncodingManager.MOTORCYCLE;
	public static final String VEHICLE_TYPE_RACINGBIKE = EncodingManager.RACINGBIKE;
	public static final String VEHICLE_TYPE_MOUNTAINBIKE = EncodingManager.MOUNTAINBIKE;


	
	protected final GraphHopper hopper;
	private final EncodingManager encodingManager;
	protected final CHGraph chGraph;
	private final LevelEdgeFilter levelEdgeFilter;
	private final boolean useExpansionCache = true;
	private final boolean outputText = false;
	private final boolean ownsHopper;
	private final FlagEncoder flagEncoder;
	private final EdgeFilter edgeFilter;
	private final Weighting prepareWeighting;

	public static interface CHProcessingApi {
		boolean isCancelled();

		void postStatusMessage(String s);
	}

	public static GraphHopper createHopper( String graphFolder) {
		return createHopper(false, graphFolder);
	}
	
	public static GraphHopper createHopper(boolean memoryMapped, String graphFolder) {
		GraphHopper ret = null;

		ret = new GraphHopper().forDesktop();

		// initialise the encoders ourselves as we can use multiple
		// encoders for same vehicle type corresponding to different
		// times of day (i.e. rush hours)
		ret.setEncodingManager(createEncodingManager(graphFolder));
		
		// don't need to write so disable the lock file (allows us to run out of program files)
		ret.setAllowWrites(false);

		if (memoryMapped) {
			ret.setMemoryMapped();
		}

		ret.setGraphHopperLocation(graphFolder);
		ret.importOrLoad();

		return ret;
	}

	/**
	 * Encoding manager which understands the convention that car_day,
	 * car_night and car should all use the car encoder, but returning
	 * the relevant id in toString().
	 * Method is public as its called from unit tests
	 * @param directory
	 * @return
	 */
	public static EncodingManager createEncodingManager(String directory) {
		Directory dir = new RAMDirectory(directory, true);
		StorableProperties properties = new StorableProperties(dir);
		if (!properties.loadExisting()) {
			properties.close();
			throw new RuntimeException("Cannot find properties file");
		}

		// check encoding for compatiblity
		properties.checkVersions(false);
		
		// get all the flag encoders, with correct ids in their toString method
		String allEncoderString= properties.get("graph.flagEncoders");
		List<FlagEncoder> encoders = new ArrayList<>();
        for(String encoderString: allEncoderString.split(",")){
        	encoderString = encoderString.trim().toLowerCase();
        	String typeWithVariant = encoderString.split("\\|")[0];
        	String [] splitTypeVariant = typeWithVariant.split("_");
        	String type = splitTypeVariant[0].trim();
        	String variant = splitTypeVariant.length>1? splitTypeVariant[1].trim():"";
        	PMap propertiesMap = new PMap(encoderString);

        	class ToStringHelper{
        		String id(String baseId){
        			return variant.length()>0?baseId+"_"+variant:baseId;
        		}
        	}
        	ToStringHelper h= new ToStringHelper();
        	
        	FlagEncoder encoder = null;
            if (type.equals(EncodingManager.CAR)){
                encoder= new CarFlagEncoder(propertiesMap){
                	@Override
                	public String toString(){
                		return h.id(super.toString());
                	}
                };
            }

            else if (type.equals(EncodingManager.BIKE)){
            	encoder= new BikeFlagEncoder(propertiesMap){
                	@Override
                	public String toString(){
                		return h.id(super.toString());
                	}            		
            	};
            }
            else if (type.equals(EncodingManager.FOOT)){
            	encoder= new FootFlagEncoder(propertiesMap){
                	@Override
                	public String toString(){
                		return h.id(super.toString());
                	}
            	};
            }
            else if (type.equals(EncodingManager.MOTORCYCLE)){
            	encoder= new MotorcycleFlagEncoder(propertiesMap){
                	@Override
                	public String toString(){
                		return h.id(super.toString());
                	}
            	};
            }
            else{
            	properties.close();
            	throw new RuntimeException("Unsupported vehicle type");
            }
            
            if (propertiesMap.has("version") && encoder.getVersion()!=propertiesMap.getInt("version", -1)){
            	properties.close();
            	throw new RuntimeException("Graph was built with wrong encoder version - probably built using an older version of the graphhopper library?");
            }

            encoders.add(encoder);
        }
        
		int bytesForFlags = 4;
		if ("8".equals(properties.get("graph.bytesForFlags"))) {
			bytesForFlags = 8;
		}
		
		// closing properties is probably unneccessary	
		properties.close();
		
		return new EncodingManager(encoders, bytesForFlags);
	}
	/**
	 * Load the graph and use it in this class
	 * @param graphFolder
	 */
	public CHMatrixGeneration(String graphFolder) {
		this(graphFolder, false);
	}

	/**
	 * Load the graph and use it in this class
	 * @param graphFolder
	 * @param memoryMapped
	 */
	public CHMatrixGeneration(String graphFolder, boolean memoryMapped) {
		this( createHopper(memoryMapped, graphFolder), true, null);
	}

	public CHMatrixGeneration(String graphFolder, boolean memoryMapped, String vehicleType) {
		this( createHopper(memoryMapped, graphFolder), true, vehicleType);
	}

	
	/**
	 * Wrap the instance of hopper but don't own it (i.e. don't dispose of it later)
	 * @param hopper
	 * @param namedFlagEncoder
	 */
	public CHMatrixGeneration(GraphHopper hopper, String namedFlagEncoder) {
		this(hopper,false,namedFlagEncoder);
	}

	/**
	 * Get the possible vehicle types. Just because a vehicle type is possible, it doesn't mean its supported
	 * in the input graph
	 * @return
	 */
	public static String [] getPossibleVehicleTypes(){
		return new String[] {	VEHICLE_TYPE_CAR, VEHICLE_TYPE_BIKE, VEHICLE_TYPE_FOOT, VEHICLE_TYPE_BIKE2,
				VEHICLE_TYPE_MOTORCYCLE, VEHICLE_TYPE_RACINGBIKE, VEHICLE_TYPE_MOUNTAINBIKE 
				 };
	}
	
	public static FlagEncoder identifyFlagEncoder(GraphHopper graphHopper, String namedFlagEncoder){
		EncodingManager encodingManager = graphHopper.getEncodingManager();
		FlagEncoder flagEncoder=null;
		if (namedFlagEncoder == null) {
			// Pick the first supported encoder from a standard list, ordered by most commonly used first.
			// This allows the user to build the graph for the speed profile they want and it just works...
			FlagEncoder foundFlagEncoder = null;
			for (String vehicleType : getPossibleVehicleTypes()) {
				if (encodingManager.supports(vehicleType)) {
					foundFlagEncoder = encodingManager.getEncoder(vehicleType);
					break;
				}
			}
			if (foundFlagEncoder == null) {
				throw new RuntimeException("The road network graph does not support any of the standard vehicle types");
			}
			flagEncoder = foundFlagEncoder;
		} else {
			namedFlagEncoder = namedFlagEncoder.toLowerCase().trim();
			flagEncoder = encodingManager.getEncoder(namedFlagEncoder);
			if (flagEncoder == null) {
				throw new RuntimeException("Vehicle type is unsuported in road network graph: " + namedFlagEncoder);
			}
		}
		return flagEncoder;
	}
	
	/**
	 * 
	 * @param graphFolder
	 * @param memoryMapped
	 * @param hopper
	 * @param ownsHopper
	 *            Whether this class owns the graphhopper graph (and wrapper object) and should dispose of it later.
	 * @param namedFlagEncoder
	 */
	private CHMatrixGeneration( GraphHopper hopper, boolean ownsHopper, String namedFlagEncoder) {
		this.hopper = hopper;
		this.ownsHopper = ownsHopper;
		encodingManager = hopper.getEncodingManager();
		flagEncoder = identifyFlagEncoder(hopper, namedFlagEncoder);
		edgeFilter = new DefaultEdgeFilter(flagEncoder);

		WeightingMap weightingMap = new WeightingMap("fastest");
	//	Weighting weighting = hopper.createWeighting(weightingMap, flagEncoder);
	//	prepareWeighting = new PreparationWeighting(weighting);

		// get correct weighting for flag encoder
		Weighting weighting = hopper.getWeightingForCH(weightingMap, flagEncoder);
		prepareWeighting = new PreparationWeighting(weighting);
		
		// save reference to the correct CH graph
		chGraph = hopper.getGraphHopperStorage().getGraph(CHGraph.class,weighting);

		// and create a level edge filter to ensure we (a) accept virtual (snap-to) edges and (b) don't descend into the
		// base graph
		levelEdgeFilter = new LevelEdgeFilter(chGraph);
	}

	public void dispose() {
		if (ownsHopper) {
			hopper.close();
		}
	}

	public GHResponse getResponse(GHPoint from, GHPoint to) {
		// The flag encoder's toString method returns the vehicle type
		GHRequest req = new GHRequest(from, to).setVehicle(flagEncoder.toString());
		GHResponse rsp = hopper.route(req);
		if (rsp.hasErrors()) {
			return null;
		}

		return rsp;
	}

	public MatrixResult calculateMatrixOneByOne(GHPoint[] points) {
		int n = points.length;
		MatrixResult ret = new MatrixResult(n);
		for (int fromIndex = 0; fromIndex < n; fromIndex++) {

			// Loop over TO in reverse order so the first A-B we process doesn't have the same
			// location for FROM and TO - this makes it quicker to debug as the first call is no longer a 'dummy' one.
			for (int toIndex = n - 1; toIndex >= 0; toIndex--) {
				GHPoint from = points[fromIndex];
				GHPoint to = points[toIndex];
				GHResponse rsp = getResponse(from, to);
				if (rsp == null) {
					continue;
				}
				ret.setDistanceMetres(fromIndex, toIndex, rsp.getDistance());
				ret.setTimeMilliseconds(fromIndex, toIndex, rsp.getTime());
			}
		}

		return ret;
	}

	public CHGraph getCHGraph() {
		return chGraph;
	}

	public EdgeExplorer createBackwardsEdgeExplorer(Graph graph) {
		return graph.createEdgeExplorer(new DefaultEdgeFilter(flagEncoder, true, false));
	}

	private QueryResult[] queryPositions(GHPoint[] points, List<QueryResult> validResults) {
		QueryResult[] queryResults = new QueryResult[points.length];
		for (int i = 0; i < points.length; i++) {
			queryResults[i] = createSnapToResult(points[i].getLat(), points[i].getLon());
			if (queryResults[i].isValid()) {
				validResults.add(queryResults[i]);
			}
		}
		return queryResults;
	}

	public QueryResult createSnapToResult(double latitude, double longitude) {
		return hopper.getLocationIndex().findClosest(latitude, longitude, edgeFilter);
	}

	public EdgeExplorer createForwardsEdgeExplorer(Graph graph) {
		return graph.createEdgeExplorer(new DefaultEdgeFilter(flagEncoder, false, true));
	}

	/**
	 * A map of node id to EdgeEntries generated for a single shortest path query
	 * 
	 * @author Phil
	 *
	 */
	public static class ShortestPathTree extends TIntObjectHashMap<EdgeEntry> {
		public final int startNodeId;
		public final boolean reverseQuery;

		public ShortestPathTree(int nodeId, boolean reverseQuery) {
			this.startNodeId = nodeId;
			this.reverseQuery = reverseQuery;
		}

	}

	public ShortestPathTree search(int startNode, EdgeExplorer edgeExplorer, boolean isBackwards) {
		return search(startNode, edgeExplorer, levelEdgeFilter, isBackwards);
	}

	public ShortestPathTree search(int startNode, EdgeExplorer edgeExplorer, EdgeFilter edgeFilter, boolean isBackwards) {

		PriorityQueue<EdgeEntry> openSet = new PriorityQueue<>();
		ShortestPathTree shortestWeightMap = new ShortestPathTree(startNode, isBackwards);

		EdgeEntry firstEdge = new EdgeEntry(EdgeIterator.NO_EDGE, startNode, 0);
		shortestWeightMap.put(startNode, firstEdge);
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
				if (!edgeFilter.accept(iter)) {
					continue;
				}

				// As turn restrictions aren't enabled at the moment we should be safe putting
				// a non-existent edge for the previous or next edge, though we should fix this in the future...
				int previousOrNextEdge = -1;
				double tmpWeight = prepareWeighting.calcWeight(iter, isBackwards, previousOrNextEdge) + currEdge.weight;

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

		return shortestWeightMap;
	}

	public GraphHopper getGraphhopper() {
		return hopper;
	}

	public FlagEncoder getFlagEncoder() {
		return flagEncoder;
	}

	public MatrixResult calculateMatrix(GHPoint[] points, CHProcessingApi processingApi) {
		if (outputText) {
			System.out.println("Starting calculate matrix");
		}

		// query positions
		List<QueryResult> validResults = new ArrayList<QueryResult>(points.length);
		QueryResult[] snapToResults = queryPositions(points, validResults);
		if (processingApi != null && processingApi.isCancelled()) {
			return null;
		}

		// Create a query graph from the snap-to results, this is a graph including virtual
		// edges based on the snapped-to locations. The closest node in the QueryResults will
		// be changed to a virtual node in the QueryGraph.
		if (outputText) {
			System.out.println("Creating query graph");
		}
		if (processingApi != null) {
			processingApi.postStatusMessage("Querying positions against graph");
		}
		final QueryGraph queryGraph = new QueryGraph(chGraph);
		queryGraph.lookup(validResults);
		if (processingApi != null && processingApi.isCancelled()) {
			return null;
		}

		// run the search forward individually from each point
		final ShortestPathTree[] forwardTrees = new ShortestPathTree[points.length];
		final TIntObjectHashMap<List<FromIndexEdge>> visitedByNodeId = new TIntObjectHashMap<>();
		if (processingApi != null) {
			processingApi.postStatusMessage("Performing forward search");
		}
		searchAllForward(snapToResults, queryGraph, forwardTrees, visitedByNodeId, processingApi);
		if (processingApi != null && processingApi.isCancelled()) {
			return null;
		}

		// run the search backward for all
		MatrixResult ret = searchAllBackward(snapToResults, queryGraph, forwardTrees, visitedByNodeId, processingApi);
		if (processingApi != null && processingApi.isCancelled()) {
			return null;
		}

		if (outputText) {
			System.out.println("Finished calculate matrix");
		}

		return ret;
	}

	private MatrixResult searchAllBackward(QueryResult[] snapToResults, final QueryGraph snapToGraph, final ShortestPathTree[] forwardTrees,
			final TIntObjectHashMap<List<FromIndexEdge>> visitedByNodeId, CHProcessingApi processingApi) {

		if (outputText) {
			System.out.println("Running backward searches and extracting matrix results");
		}

		// instantiate return object
		final int n = snapToResults.length;
		MatrixResult ret = new MatrixResult(n);

		// create a cache of expanded edge results
		final HashMap<EdgeExpansionCacheKey, DistanceTime> expansionCache;
		if (useExpansionCache) {
			expansionCache = new HashMap<>();
		} else {
			expansionCache = null;
		}

		// now query all in a reverse direction, building up the final matrix
		// for each one
		EdgeExplorer inEdgeExplorer = createBackwardsEdgeExplorer(snapToGraph);
		// UpdateTimer timer = new UpdateTimer(100);
		long lastUpdateTime = System.currentTimeMillis();
		for (int toIndex = 0; toIndex < n; toIndex++) {

			// check for user quitting
			if (processingApi != null && processingApi.isCancelled()) {
				return null;
			}

			if (snapToResults[toIndex].isValid()) {
				// run query
				ShortestPathTree reverseTree = search(snapToResults[toIndex].getClosestNode(), inEdgeExplorer, true);

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
							// see if this meeting point has a lower cost than the other
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
						PathBidirRef pathCh = new CacheablePath4CH(snapToGraph, getFlagEncoder(), expansionCache);
						// PathBidirRef pathCh = new Path4CH(snapToGraph, snapToGraph.getBaseGraph(),getFlagEncoder());
						pathCh.setSwitchToFrom(false);
						EdgeEntry edgeEntry = forwardTrees[fromIndex].get(meetingPointNode);
						pathCh.setEdgeEntry(edgeEntry);

						EdgeEntry edgeEntryTo = reverseTree.get(meetingPointNode);
						pathCh.setEdgeEntryTo(edgeEntryTo);

						Path path = pathCh.extract();
						ret.setTimeMilliseconds(fromIndex, toIndex, path.getTime());
						ret.setDistanceMetres(fromIndex, toIndex, path.getDistance());
					}
				}
			}

			if (System.currentTimeMillis() - lastUpdateTime > 100 && processingApi != null) {
				lastUpdateTime = System.currentTimeMillis();
				processingApi.postStatusMessage("Performed backwards search for " + (toIndex + 1) + "/" + n + " points");
			}
		}
		return ret;
	}

	private void searchAllForward(QueryResult[] snapToResults, final QueryGraph queryGraph, final ShortestPathTree[] forwardTrees,
			final TIntObjectHashMap<List<FromIndexEdge>> visitedByNodeId, CHProcessingApi continueCB) {

		if (outputText) {
			System.out.println("Running forward searches");
		}

		EdgeExplorer outEdgeExplorer = createForwardsEdgeExplorer(queryGraph);
		for (int fromIndex = 0; fromIndex < snapToResults.length; fromIndex++) {

			// check for user quitting
			if (continueCB != null && continueCB.isCancelled()) {
				return;
			}

			final int finalFromIndx = fromIndex;
			if (snapToResults[fromIndex].isValid()) {
				forwardTrees[fromIndex] = search(snapToResults[fromIndex].getClosestNode(), outEdgeExplorer, false);
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

	private static class FromIndexEdge {
		private final int fromIndex;
		private final EdgeEntry edge;

		FromIndexEdge(int fromIndex, EdgeEntry edge) {
			super();
			this.fromIndex = fromIndex;
			this.edge = edge;
		}

	}

	public Weighting getWeighting(){
		return prepareWeighting;
	}
	
	public LevelEdgeFilter getLevelEdgeFilter(){
		return levelEdgeFilter;
	}
}
