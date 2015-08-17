/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.core.formulae.Functions.FmAnd;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.scripts.elements.UserFormula;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.utils.Numbers;

public final class FunctionUtils {
	/**
	 * Extract the constant string from the formula. The method will return null
	 * if the formula does not contain a constant string.
	 * 
	 * @param formula
	 * @return
	 */
	public static String getConstantString(Function formula) {
		String ret = null;
		if (formula != null) {
			if (FmConst.class.isInstance(formula)) {
				FmConst cnst = (FmConst) formula;
				// allow a non-string object to be treated as a const string (e.g. '1')
				if (cnst.value() != null) {
					ret = cnst.value().toString();
				}
			} else if (FmLocalElement.class.isInstance(formula)) {
				// We can have a local variable with the same name as remote one... just grab the name
				ret = ((FmLocalElement) formula).getName();
			}
		}

		return ret;
	}

	public static Function [] toSingleArray(Function a, Function ...arr){
		Function [] ret = new Function[arr.length+1];
		ret[0] = a;
		for(int i =0 ; i < arr.length ; i++){
			ret[i+1] = arr[i];
		}
		return ret;
	}
	
	/**
	 * Convert the function to an equivalent array of the form a[0] && a[1] && ... && a[n].
	 * The function can then be processed by testing each element is true consecutively
	 * and returning false when the first non-true is found.
	 * @param f
	 * @return
	 */
	public static Function [] toEquivalentSplitAndArray(Function f){
		final ArrayList<Function> andArrayList = new ArrayList<Function>();
		class Helper{
			void process(Function func){
				if(func!=null){
					if(FmAnd.class.isInstance(func)){
						process(func.child(0));
						process(func.child(1));
					}else{
						andArrayList.add(func);
					}
				}
			}
		}
		
		new Helper().process(f);
		return andArrayList.toArray(new Function[andArrayList.size()]);
	}
	
	public static void main(String[] args) throws Exception {

		FunctionDefinitionLibrary lib = new FunctionDefinitionLibrary();
		lib.buildStd();
		
		FormulaParser loader = new FormulaParser(null, lib, null);
		String function = "(2 && 5 && 7 && 2) + 1";
		Function formula = loader.parse(function);
		for(Function split : toEquivalentSplitAndArray(formula)){
			System.out.println("\t" + split + " -> " + split.execute(null));
		}
//		System.out.println(formula.execute(null));
	}
	
	public interface FunctionVisitor{
		/**
		 * Visit the function and return false if no more visits should be done
		 * @param f
		 * @return
		 */
		boolean visit(Function f);
	}
	
	public static boolean visit(Function f, FunctionVisitor visitor){
		if(!visitor.visit(f)){
			return false;
		}
		int n = f.nbChildren();
		for(int i =0 ; i < n ; i++){
			if(!visit(f.child(i), visitor)){
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean containsFunctionType(Function f, Class<? extends Function> functionType){
		return visit(f, new FunctionVisitor() {
			
			@Override
			public boolean visit(Function f) {
				if(f!=null && functionType.isInstance(f)){
					// stop the search...
					return false;
				}
				return true;
			}
		})==false;
	}
	
	public static boolean isTrue(Object exec){
		if (exec != null) {
			Long val = Numbers.toLong(exec);
			if(val!=null && val.intValue()==1){
				return true;						
			}
		}
		return false;
	}
}
