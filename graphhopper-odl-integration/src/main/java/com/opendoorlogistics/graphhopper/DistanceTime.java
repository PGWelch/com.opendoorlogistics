package com.opendoorlogistics.graphhopper;

class DistanceTime {
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