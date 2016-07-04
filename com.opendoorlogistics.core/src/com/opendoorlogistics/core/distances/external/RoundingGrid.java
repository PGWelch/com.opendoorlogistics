package com.opendoorlogistics.core.distances.external;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.ObjIntConsumer;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.geometry.GreateCircle;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.utils.Numbers;

public class RoundingGrid {
	public static class GridNeighboursResult implements Comparable<GridNeighboursResult> {

		private final LatLong latLong;
		private final double cartesianMetresFromRawLatLong;
		private final int offsetI;
		private final int offsetJ;
		public GridNeighboursResult(LatLong latLong,int offsetI, int offsetJ, double diff) {
			this.latLong = latLong;
			this.offsetI = offsetI;
			this.offsetJ = offsetJ;
			this.cartesianMetresFromRawLatLong = diff;
		}

		@Override
		public int compareTo(GridNeighboursResult o) {
			return Double.compare(cartesianMetresFromRawLatLong, o.cartesianMetresFromRawLatLong);
		}

		@Override
		public String toString() {
			return "[latLong=" + latLong + ", metresDiff=" + cartesianMetresFromRawLatLong + ", offset=" + offsetI + "," + offsetJ + "]";
		}

		public LatLong getLatLong() {
			return latLong;
		}

		
	}

	private static final double EARTH_RADIUS_METRES = 6371000;

	private static final double EARTH_CIRCUMFERENCE_METRES = EARTH_RADIUS_METRES * 2 * Math.PI;

	/**
	 * See https://en.wikipedia.org/wiki/Spherical_coordinate_system#Cartesian_coordinates. The axis alignment (i.e. is +z North?) doesn't matter as long as
	 * this transformation is used consistently.
	 * 
	 * @param ll
	 * @return
	 */
	private static ThreeDPoint calculateXYZ(LatLong ll) {
		ThreeDPoint ret = new ThreeDPoint();
		double lat = Math.toRadians(ll.getLongitude());
		double lng = Math.toRadians(ll.getLatitude());

		ret.x = EARTH_RADIUS_METRES * Math.sin(lat) * Math.cos(lng);
		ret.y = EARTH_RADIUS_METRES * Math.sin(lat) * Math.sin(lng);
		ret.z = EARTH_RADIUS_METRES * Math.cos(lat);
		return ret;
	}
	//

	// public static LatLong roundToMetreGrid( LatLong ll){
	// return roundToCustomGrid(ll, 1, 10);
	// }

	public static void main(String[] args) {

		RoundingGrid grid = new RoundingGrid();
		Random random = new Random(123);
		for (int i = 0; i < 1000; i++) {
			LatLongImpl ll = LatLongImpl.random(random);
			LatLong rounded = grid.snapToGrid(ll);
			double distance = GreateCircle.greatCircle(ll, rounded, true);
			System.out.println("" + i + " - " + ll.toString() + " rounds to " + rounded.toString() + " with distance " + distance);
			// if(Math.abs(ll.getLatitude() - rounded.getLatitude())>0.001){
			// throw new RuntimeException();
			// }
			// if(Math.abs(ll.getLongitude() - rounded.getLongitude())>0.001){
			// throw new RuntimeException();
			// }

			if (distance > 10) {
				throw new RuntimeException();
			}
		}
		
		for (int i = 0; i < 50; i++) {
			LatLongImpl ll = LatLongImpl.random(random);
			System.out.println(ll);
			for(GridNeighboursResult ngb : grid.calculateNeighbouringGridCells(ll)){
				System.out.println("\t" + ngb);
			}
//			System.out.println(grid.calculateNeighbouringGridCells(ll));
			System.out.println(System.lineSeparator() + System.lineSeparator());
		}
		
	}

	/**
	 * Round the lat-long to a standard grid of input resolution
	 * 
	 * @param ll
	 * @param gridResolutionMetres
	 * @param latitudeSafetyFactor
	 * @return
	 */
	// public static LatLong roundToCustomGrid(LatLong ll, double gridResolutionMetres, double latitudeSafetyFactor){
	// // Use a latitude limit where the a horizontal (constant latitude) section through the Earth is only X metres
	// double minEarthSectionRadius = gridResolutionMetres * latitudeSafetyFactor;
	// double latLimit = Math.acos(minEarthSectionRadius/EARTH_RADIUS_METRES);
	// latLimit = 0.5* Math.PI - latLimit;
	// latLimit = Math.toDegrees(latLimit);
	// double minLat = -90 + latLimit;
	// double maxLat = +90 - latLimit;
	// double latRange = maxLat - minLat;
	//
	// double halfEarthCircumference = 0.5 * EARTH_CIRCUMFERENCE_METRES;
	// double nbLatitudeCells = halfEarthCircumference/ gridResolutionMetres;
	// double latitudeCellHeight = latRange / nbLatitudeCells;
	//
	// // get snapped-to-grid latitude first
	// double latitude = Numbers.clamp(ll.getLatitude(), minLat,maxLat);
	// long latitudeIndex = Math.round((latitude - minLat)/latitudeCellHeight);
	// double snapped2GridLatitude = latitudeCellHeight * latitudeIndex+ minLat;
	//
	// // get circumference of a constant latitude circle at this latitude
	// double circleRadius = EARTH_RADIUS_METRES * Math.cos(Math.toRadians(snapped2GridLatitude));
	// double circleCircum = 2 * Math.PI * circleRadius;
	//
	// // get nb of longitude cells at this latitude
	// double nbLongitudeCells = circleCircum / gridResolutionMetres;
	// double longitudeCellWidth = 360 / nbLongitudeCells;
	//
	// // then get snapped-to-grid longitude
	// double minLong = -180;
	// double longitude = Numbers.clamp(ll.getLongitude(), minLong, 180);
	// long longitudeIndx = Math.round((longitude - minLong)/longitudeCellWidth);
	// double snapped2GridLongitude = longitudeCellWidth *longitudeIndx + minLong;
	//
	// return new LatLongImpl(snapped2GridLatitude, snapped2GridLongitude);
	//
	// }

	// public static class CustomRoundingGrid{
	private final double minLong = -180;
	private final double gridResolutionMetres;
	private final double minLat;
	private final double maxLat;

	private final double latitudeCellHeight;

	public RoundingGrid() {
		this(1, 10);
	}

	public RoundingGrid(double gridResolutionMetres, double latitudeSafetyFactor) {
		this.gridResolutionMetres = gridResolutionMetres;

		// Use a latitude limit where the a horizontal (constant latitude) section through the Earth is only X metres
		double minEarthSectionRadius = gridResolutionMetres * latitudeSafetyFactor;
		double latLimit = Math.acos(minEarthSectionRadius / EARTH_RADIUS_METRES);
		latLimit = 0.5 * Math.PI - latLimit;
		latLimit = Math.toDegrees(latLimit);
		minLat = -90 + latLimit;
		maxLat = +90 - latLimit;
		double latRange = maxLat - minLat;

		double halfEarthCircumference = 0.5 * EARTH_CIRCUMFERENCE_METRES;
		double nbLatitudeCells = halfEarthCircumference / gridResolutionMetres;
		latitudeCellHeight = latRange / nbLatitudeCells;
	}

	private double calcLongitudeCellWidth(double latitudeSnapped2Grid) {
		// get circumference of a constant latitude circle at this latitude
		double circleRadius = EARTH_RADIUS_METRES * Math.cos(Math.toRadians(latitudeSnapped2Grid));
		double circleCircum = 2 * Math.PI * circleRadius;

		// get nb of longitude cells at this latitude
		double nbLongitudeCells = circleCircum / gridResolutionMetres;
		double longitudeCellWidth = 360 / nbLongitudeCells;
		return longitudeCellWidth;
	}

	private double calcSnappedLatitude(long latitudeIndx) {
		return latitudeCellHeight * latitudeIndx + minLat;
	}

	private long calcSnappedLatitudeIndex(double latitude) {
		latitude = Numbers.clamp(latitude, minLat, maxLat);
		long latitudeIndex = Math.round((latitude - minLat) / latitudeCellHeight);
		return latitudeIndex;
	}

	private double calcSnappedLongitude(double latitudeSnapped2Grid, long longitudeIndx) {
		return calcLongitudeCellWidth(latitudeSnapped2Grid) * longitudeIndx + minLong;
	}

	private long calcSnappedLongitudeIndex(double latitudeSnapped2Grid, double longitude) {

		double longitudeCellWidth = calcLongitudeCellWidth(latitudeSnapped2Grid);

		// then get snapped-to-grid longitude
		longitude = Numbers.clamp(longitude, minLong, 180);
		long longitudeIndx = Math.round((longitude - minLong) / longitudeCellWidth);
		return longitudeIndx;
	}

	public List<GridNeighboursResult> calculateNeighbouringGridCells(LatLong ll) {
		return calculateNeighbouringGridCells(ll, 1);
	}

	/**
	 * Return neighbours (include the closest snap-to), sorted by closest first
	 * @param ll
	 * @param borderWidth
	 * @return
	 */
	public List<GridNeighboursResult> calculateNeighbouringGridCells(LatLong ll, int borderWidth) {
		if(borderWidth<0){
			throw new IllegalArgumentException();
		}
		
		ThreeDPoint rawCentre = calculateXYZ(ll);

		long latIndx = calcSnappedLatitudeIndex(ll.getLatitude());

		int length = 1 + 2 * borderWidth;
		ArrayList<GridNeighboursResult> toSort = new ArrayList<>(length * length);
		for (int i = -borderWidth; i <= borderWidth; i++) {
			long latIndx2 = latIndx + i;
			double snappedLat2 = calcSnappedLatitude(latIndx2);
			if (snappedLat2 >= minLat && snappedLat2 <= maxLat) {
				long longIndx = calcSnappedLongitudeIndex(snappedLat2, ll.getLongitude());
				for (int j = -borderWidth; j <= borderWidth; j++) {
					long longIndx2 = longIndx + j;
					double snappedLng2 = calcSnappedLongitude(snappedLat2, longIndx2);
					LatLong snappedLatLng = new LatLongImpl(snappedLat2, snappedLng2);
					if (snappedLatLng.isValid()) {
						ThreeDPoint pnt = calculateXYZ(snappedLatLng);
						// use xyz Cartesian diff as Great Circle calculation is an approximation with inaccuracies at low separation
						double diff = Math.sqrt(ThreeDPoint.absSqd(ThreeDPoint.subtract(rawCentre, pnt)));
						toSort.add(new GridNeighboursResult(snappedLatLng,i,j, diff));
					}
				}
			}

		}

		Collections.sort(toSort);
		return toSort;
	}
	// }

	private double snapLatitudeToGrid(double latitude) {
		return calcSnappedLatitude(calcSnappedLatitudeIndex(latitude));
	}

	private double snapLongitudeToGrid(double latitudeSnapped2Grid, double longitude) {
		return calcSnappedLongitude(latitudeSnapped2Grid, calcSnappedLongitudeIndex(latitudeSnapped2Grid, longitude));
	}

	public LatLong snapToGrid(LatLong ll) {
		double snappedLat = snapLatitudeToGrid(ll.getLatitude());
		double snappedLong = snapLongitudeToGrid(snappedLat, ll.getLongitude());
		return new LatLongImpl(snappedLat, snappedLong);
	}
}
