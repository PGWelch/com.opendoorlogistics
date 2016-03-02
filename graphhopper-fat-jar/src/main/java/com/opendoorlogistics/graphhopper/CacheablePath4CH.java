package com.opendoorlogistics.graphhopper;

import java.util.HashMap;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.PathBidirRef;
import com.graphhopper.routing.ch.Path4CH;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;

/**
 * CH path extractor with caching
 * 
 * @author Phil
 *
 */
public class CacheablePath4CH extends PathBidirRef {
	private final Graph routingGraph;
	private final HashMap<EdgeExpansionCacheKey, DistanceTime> expansionCache;
//	private final EdgeExpansionCacheKey cacheKey = new EdgeExpansionCacheKey(-1, -1);
	private final FlagEncoder encoder;
	public CacheablePath4CH(Graph chGraph, FlagEncoder encoder, HashMap<EdgeExpansionCacheKey, DistanceTime> expansionCache) {
		super(chGraph.getBaseGraph(), encoder);
		this.routingGraph = chGraph;
		this.expansionCache = expansionCache;
		this.encoder = encoder;
	}

	@Override
	protected final void processEdge(int tmpEdge, int endNode) {
		DistanceTime dt = null;
		if (expansionCache != null) {
			// try getting from cache
			EdgeExpansionCacheKey key = new EdgeExpansionCacheKey(tmpEdge, endNode, this.reverseOrder);
			dt = expansionCache.get(key);

			// calculate using a new instance without the cache if needed
			if (dt == null) {
				dt = unpackSingleEdge2DistTime(tmpEdge, endNode, this.reverseOrder);
				expansionCache.put(key, dt);
			}

		} else {
			DistanceTime dtTmp = unpackSingleEdge2DistTime(tmpEdge, endNode, this.reverseOrder);
			dt = dtTmp;

		}

		distance += dt.getDistance();
		time += dt.getMillis();

	}

	private DistanceTime unpackSingleEdge2DistTime(int tmpEdge, int endNode, boolean isReverseOrder) {
		Path tmpPath = unpackSingleEdge(routingGraph, routingGraph.getBaseGraph(), encoder, tmpEdge, endNode,isReverseOrder);
		DistanceTime dtTmp = new DistanceTime(tmpPath.getDistance(), tmpPath.getTime());
		return dtTmp;
	}
	
	/**
	 * Unpack a single edge of graph (either CH edge or base edge)
	 * 
	 * @param routingGraph
	 * @param baseGraph
	 * @param encoder
	 * @param edge
	 * @param endNode
	 * @return
	 */
	public static Path unpackSingleEdge(Graph routingGraph, Graph baseGraph, FlagEncoder encoder, int edge, int endNode, boolean isReverseOrder) {

		class SingleEdgeUnpacker extends Path4CH {

			public SingleEdgeUnpacker(Graph routingGraph, Graph baseGraph, FlagEncoder encoder) {
				super(routingGraph, baseGraph, encoder);
				this.reverseOrder = isReverseOrder;
			}

			/**
			 * Parameters are passed into the method explicity because if we just use the outer method
			 * parameters directly we accidentally get the endNode variable from the Path base class instead...
			 * @param tmpEdge
			 * @param endNode
			 */
			public void processSingleEdge(int tmpEdge, int endNode) {
				// Access the protected method in the base class
				super.processEdge(tmpEdge, endNode);
			}
		}

		SingleEdgeUnpacker unpacker = new SingleEdgeUnpacker(routingGraph, baseGraph, encoder);
		unpacker.processSingleEdge(edge, endNode);
		return unpacker;
	}
}