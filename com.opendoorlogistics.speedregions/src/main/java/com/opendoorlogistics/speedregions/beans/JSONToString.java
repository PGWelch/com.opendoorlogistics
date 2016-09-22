package com.opendoorlogistics.speedregions.beans;

import com.opendoorlogistics.speedregions.processor.RegionProcessorUtils;

public abstract class JSONToString {

	@Override
	public String toString(){
		return RegionProcessorUtils.toJSON(this);
	}
}
