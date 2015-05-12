package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;

/**
 * A function which is dependent on the row in which it is executed in.
 * Any function dependent on the row should extend this function, as optimisation
 * take place assuming this.
 * @author Phil
 *
 */
public abstract class FmRowDependent extends FunctionImpl {

	public FmRowDependent(Function... children) {
		super(children);
	}

}
