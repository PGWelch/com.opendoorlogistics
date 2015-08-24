package com.opendoorlogistics.core.utils.stats;


public class Statistics {

	public static double ROUND_ERROR = 0.00000001;
	private double count;
	private double mean;
	private double populationStdDev;
	private double sampleStdDev;	
	private double skew;

	public Statistics() {
	}

	public Statistics(double count, double sum, double sum2) {
		this(count, sum, sum2, false);
	}

	public Statistics(double count, double sum, double sum2, boolean forceZeroStdDev) {
		this.count = count;
		this.mean = sum / count;
		if (!forceZeroStdDev) {
			double meanOfSqd = sum2 / count;
			double diff = meanOfSqd - mean * mean;
			assert diff > -ROUND_ERROR;
			if (diff >= 0) {
				populationStdDev = Math.sqrt(diff);
			}
			
			if (count>1){
				if (diff >= 0) {
					sampleStdDev = Math.sqrt(diff*count/(count-1));					
				}
			}else{
				populationStdDev = Double.POSITIVE_INFINITY;
			}
		}
	}

	public Statistics(double count, double sum, double sum2, double sum3) {
		this(count, sum, sum2, sum3, false);
	}

	public Statistics(double count, double sum, double sum2, double sum3, boolean forceZeroStdDev) {
		this(count, sum, sum2, forceZeroStdDev);
		if (count > 0 && populationStdDev > ROUND_ERROR) {
			// From http://en.wikipedia.org/wiki/Skewness#Definition
			skew = sum3 / count - 3 * mean * populationStdDev * populationStdDev - mean * mean * mean;
			skew /= populationStdDev * populationStdDev * populationStdDev;
		}
	}

	public static Statistics Create(Iterable<Double> doubleList) {
		double count = 0;
		double sum = 0;
		double sum2 = 0;
		for (Double d : doubleList) {
			count++;
			sum += d;
			sum2 += d * d;
		}
		return new Statistics(count, sum, sum2);
	}

	/**
	 * Return the mean value for the input array
	 * 
	 * @param arr
	 * @return
	 */
	public static double Mean(double[] arr) {
		double sum = Sum(arr);
		if (arr.length > 0) {
			sum /= arr.length;
		}
		return sum;
	}
	
	/**
	 * Return the sum of the input array
	 * 
	 * @param arr
	 * @return
	 */
	public static double Sum(double[] arr) {
		double sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i];
		}
		return sum;
	}




	
	/**
	 * Calculate the sample standard deviation (sample std. dev divides by n-1.
	 * population std. dev divides by n)
	 * 
	 * @param arr
	 * @return
	 */
	public static double SampleStandardDev(double[] arr) {
		double mean = Mean(arr);
		double sum = 0;
		for (int i = 0; i < arr.length; i++) {
			double temp = arr[i] - mean;
			temp *= temp;
			sum += temp;
		}
		if (arr.length > 1) {
			double variance = sum / (arr.length - 1);
			return Math.sqrt(variance);
		}
		else {
			return 0;
		}
	}

	@Override
	public String toString() {
		return "_count=" + count + ",_mean=" + mean + ", _sampleStdDev=" + sampleStdDev + ", _skew=" + skew;
	}

	public double getCount() {
		return count;
	}

	public double getMean() {
		return mean;
	}

	public double getPopulationStdDev() {
		return populationStdDev;
	}

	public double getSampleStdDev() {
		return sampleStdDev;
	}

	public double getSkew() {
		return skew;
	}
	
//	public String meanStdToString(int nbDp){
//		return Strings.ToString(_mean, nbDp) + "+-" + Strings.ToString(_sampleStdDev, nbDp);
//	}
//	
	
	
}
