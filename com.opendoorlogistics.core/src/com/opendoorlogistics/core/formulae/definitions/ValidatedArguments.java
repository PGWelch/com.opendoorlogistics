/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae.definitions;

import java.util.TreeSet;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionUtils;

public final class ValidatedArguments{
	final private FunctionDefinition dfn;
	final private Function []args;
	
	public ValidatedArguments(FunctionDefinition dfn, Function[] args) {
		this.dfn = dfn;
		this.args = args;
	}
	
	public Function get(String argumentName){
		int indx = dfn.indexOf(argumentName);
		if(indx!=-1){
			return args[indx];			
		}
		return null;
	}
	
	public String getConstantString(String argumentName){
		return FunctionUtils.getConstantString(args[dfn.indexOf(argumentName)]);
	}
	
	public boolean hasArgument(String argumentName){
		return dfn.indexOf(argumentName)!=-1;
	}
	
	public static ValidatedArguments validateArguments(FunctionDefinition dfn,ExecutionReport result, Function...providedArgs){
		if(providedArgs.length!=dfn.nbArgs()){
			if(result!=null){
				result.setFailed("Incorrect number of parameters into function " + dfn.getName());
			}
			return null;
		}
		
		for(int i =0 ; i< dfn.nbArgs(); i++){
			if(providedArgs[i]==null){
				if(result!=null){
					result.setFailed("Null argument passed into formula " + dfn.getName()+"." );									
				}
				return null;				
			}
			
			if(dfn.getArg(i).isConstantString() && FunctionUtils.getConstantString(providedArgs[i])==null){
				if(result!=null){
					result.setFailed("Formula " + dfn.getName() + " expected a constant string for argument " + (i+1) + ", instead found " + providedArgs[i].toString());									
				}
				return null;
			}
		}
		return new ValidatedArguments(dfn,providedArgs);
	}
	
	public static ValidatedArguments matchAndValidate(Iterable<FunctionDefinition> dfns,ExecutionReport result, Function...providedArgs){
		// find the first with correct number of args
		TreeSet<String> names = new TreeSet<>();
		for(FunctionDefinition dfn : dfns){
			names.add(dfn.getName());
			if(dfn.nbArgs() == providedArgs.length){
				return validateArguments(dfn, result, providedArgs);
			}
		}
		
		if(result!=null){
			result.setFailed("Incorrect number of arguments into function " + names.first() + ".");
		}
		return null;
	}
}
