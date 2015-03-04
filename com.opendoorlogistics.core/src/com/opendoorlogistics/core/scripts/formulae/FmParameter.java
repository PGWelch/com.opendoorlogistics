package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.core.formulae.Function;

public class FmParameter extends FmLookup{

	public FmParameter(Function foreignKeyValue, int externalDsIndex, int parametersTableId, int parametersKeyColumn, int otherTableReturnKeyColummn) {
		super(new Function[]{foreignKeyValue}, externalDsIndex, parametersTableId, new int[]{parametersKeyColumn}, otherTableReturnKeyColummn, LookupType.RETURN_FIRST_MATCH);
	}

}
