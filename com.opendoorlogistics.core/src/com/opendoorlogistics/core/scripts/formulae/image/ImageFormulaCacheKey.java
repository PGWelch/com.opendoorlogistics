package com.opendoorlogistics.core.scripts.formulae.image;

import com.opendoorlogistics.core.formulae.Function;

class ImageFormulaCacheKey {
	Function compiledFormula;
	Object keyval;
	double width;
	double height;
	double dotsPerCM = -1;

	public ImageFormulaCacheKey(Function compiledFormula, Object keyval, double width, double height, double dotsPerCM) {
		this.compiledFormula = compiledFormula;
		
		// ensure sensible ranges
		width = ImageFormulaUtils.validateImageDimension(width);
		height = ImageFormulaUtils.validateImageDimension(height);
		
		this.keyval = keyval;
		this.width = width;
		this.height = height;
		this.dotsPerCM = dotsPerCM;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(dotsPerCM);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(height);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((keyval == null) ? 0 : keyval.hashCode());
		temp = Double.doubleToLongBits(width);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		ImageFormulaCacheKey other = (ImageFormulaCacheKey) obj;
		if (Double.doubleToLongBits(dotsPerCM) != Double.doubleToLongBits(other.dotsPerCM))
			return false;
		if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
			return false;
		if (keyval == null) {
			if (other.keyval != null)
				return false;
		} else if (!keyval.equals(other.keyval))
			return false;
		if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
			return false;
		
		// only allow caching for the same compilation of the formula otherwise
		// we won't get changes when the user changes the settings...
		if(this.compiledFormula != other.compiledFormula){
			return false;
		}
		return true;
	}

}