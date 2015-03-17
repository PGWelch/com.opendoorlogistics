package com.opendoorlogistics.api;

public interface Func {
	/**
	 * Execute the function
	 * @param row Row number of -1 if the function is not operating on rows of a table.
	 * @return
	 */
	Object execute(int row);
}
