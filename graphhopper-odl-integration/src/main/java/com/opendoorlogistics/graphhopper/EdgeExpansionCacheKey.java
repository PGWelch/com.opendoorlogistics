package com.opendoorlogistics.graphhopper;

class EdgeExpansionCacheKey {
	private final int edgeId;
	private final int endNodeId;
	private final boolean reverseOrder;
	
	EdgeExpansionCacheKey(int edgeId, int endNodeId, boolean reverseOrder) {
		this.edgeId = edgeId;
		this.endNodeId = endNodeId;
		this.reverseOrder = reverseOrder;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + edgeId;
		result = prime * result + endNodeId;
		result = prime * result + (reverseOrder ? 1231 : 1237);
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
		EdgeExpansionCacheKey other = (EdgeExpansionCacheKey) obj;
		if (edgeId != other.edgeId)
			return false;
		if (endNodeId != other.endNodeId)
			return false;
		if (reverseOrder != other.reverseOrder)
			return false;
		return true;
	}


}