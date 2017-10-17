package com.opendoorlogistics.core.formulae;

import java.math.BigDecimal;

import com.opendoorlogistics.core.utils.Numbers;

public class FmLegendRangeText extends FunctionImpl{
	public FmLegendRangeText (Function minValue, Function maxValue, Function nbBins, Function thisValue) {
		super(minValue,maxValue,nbBins,thisValue);
	}


	@Override
	public Function deepCopy() {
		return new FmLegendRangeText(child(0).deepCopy(), child(1).deepCopy(),child(2).deepCopy(),child(3).deepCopy());
	}

	public static void main(String [] args){
		double val = 0;
		while(val < 1){
			val += 0.025;
			System.out.println(val + " .... " + rangeText(0, 1, 10, val));
		}
		
		System.out.println("");
		
		val = 0;
		while(val < 120){
			val += 2.5;
			System.out.println(val + " .... " + rangeText(0, 100, 10, val));
		}

	}
	
	private static String toSignificantFiguresString(double d, int significantFigures){
		BigDecimal bd = new BigDecimal(d);
	    String s= String.format("%."+significantFigures+"G", bd);
	    while(s.length() > 0 && s.contains(".") && s.endsWith("0")){
	    	s = s.substring(0, s.length()-1);
	    }
	    if(s.length()>0 && s.endsWith(".")){
	    	s = s.substring(0, s.length()-1);
	    }
	    return s;
	}
	
	private static String rangeText(double min, double max, int nbBins, double value){
		// swap if wrong way round
		if(min > max){
			double tmp = min;
			min = max;
			max = tmp;
		}
		
		if(min == max){
			return toSignificantFiguresString(min, nbBins);
		}
		
		// round min and max to 1 dp
		max = Math.ceil(max * 10)/10;
		min = Math.floor(min * 10)/10;
		double binWidth= (max - min )/ nbBins;
		int bin = (int)Math.floor( (value-min) / binWidth);
		if(bin <=0){
			return "< " + toSignificantFiguresString(min+binWidth, 3);
		}
		else if(bin>=(nbBins-1)){
			return "> " + toSignificantFiguresString( min + binWidth*(nbBins-1),3);
		}
		return toSignificantFiguresString(min + bin * binWidth, 3) + "-" + toSignificantFiguresString(min + (bin+1)*binWidth, 3);
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		Object [] children=executeChildFormulae(parameters, true);
		if(children==null){
			return Functions.EXECUTION_ERROR;
		}
		Double min = Numbers.toDouble(children[0]);
		Double max = Numbers.toDouble(children[1]);
		Long bins = Numbers.toLong(children[2], false);
		Double value = Numbers.toDouble(children[3]);
		if (min== null || max== null || bins==null || value == null || bins<1) {
			return Functions.EXECUTION_ERROR;
		}

		String text = rangeText(min, max, bins.intValue(), value);
		return text;
	}
	

}
