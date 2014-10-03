/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae;

import java.util.Arrays;


public abstract class FunctionImpl implements Function {
	protected final Function []children;

	
	protected FunctionImpl(){
		children = null;

	}
	
	public FunctionImpl(Function...children){
		this.children = children;
	}

	/**
	 * Used for deep copying only...
	 * @param deepCopyThis
	 * @ 
	 */
	protected FunctionImpl(FunctionImpl deepCopyThis) {
		if(deepCopyThis.children!=null){
			this.children = new Function[deepCopyThis.children.length];
			for(int i = 0 ; i < children.length ;i++){
				this.children[i] = deepCopyThis.children[i].deepCopy();
			}			
		}else{
			children = null;
		}
	}
	
	@Override
	public int nbChildren(){
		return children!=null ? children.length : 0;
	}
	
	public Function child(int i){
		return children[i];
	}
	
	/**
	 * Execute all child formula and return null if there's an error
	 * @param parameters
	 * @param cannnotBeNull
	 * @return
	 */
	protected Object [] executeChildFormulae(FunctionParameters parameters, boolean cannnotBeNull){
		Object [] ret = new Object[children.length];
		for(int i =0 ;i<children.length ; i++){
			ret[i] = children[i].execute(parameters);
			if(ret[i]==null && cannnotBeNull ){
				return null;
			}
			if(ret[i]== Functions.EXECUTION_ERROR){
				return null;
			}
		}
		return ret;
	}
	
//	public boolean retrievesExternalData(){
//		if(children!=null){
//			for(Formula child: children){
//				if(child.retrievesExternalData()){
//					return true;
//				}
//			}		
//		}
//
//		return false;
//	}

	
	public void replaceChild(int i, Function newChild){
		children[i] = newChild; 
	}
	
	protected Function [] deepCopy(Function [] array) {
		Function []ret = new Function[array.length];
		for(int i =0;  i< array.length;i++){
			if(array[i]!=null){
				ret[i]= array[i].deepCopy(); 				
			}
		}
		return ret;
	}
	
//	protected FormulaWithSource [] deepCopyToWithSource(Formula [] array) {
//		FormulaWithSource []ret = new FormulaWithSource[array.length];
//		for(int i =0;  i< array.length;i++){
//			if(array[i]!=null){
//				ret[i]= (FormulaWithSource)array[i].deepCopy(); 				
//			}
//		}
//		return ret;
//	}

//	@Override
//	public MapToGraphType mapToGraphType(){
//		return MapToGraphType.NoConvert;
//	}

	protected String toString(String functionName){
		StringBuilder builder = new StringBuilder();
		builder.append(functionName +"(");
		
		for(int i =0 ; i < children.length ; i++){
			builder.append((i>0?",":""));
			builder.append(children[i].toString());
		}
		builder.append(")");
		return builder.toString();
	}

	protected String toStringWithChildOp(String childOperator){
		StringBuilder builder = new StringBuilder();
		for(int i =0 ; i< children.length ; i++){
			if(i>0){
				builder.append(childOperator);
			}
			builder.append(childToString(i));
		}
		return builder.toString();	
	}
	
	@Override
	public String toString() {
		// Default implementation for toString should just print the first child.
		if(nbChildren()>0){
			return child(0).toString();
		}
		return "";
	}
	
	@Override
	public boolean hasBrackets(){
		return false;
	}


	protected String childToString(int childNb){
		Function child = child(childNb);
		if(child.nbChildren()>0){
			if(child.hasBrackets()==false){
				return "(" + child.toString()+ ")";		
			}
		}
		return child.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		Class<?> cls = getClass();
		int result = cls.hashCode();
		result = prime * result + Arrays.hashCode(children);
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
		FunctionImpl other = (FunctionImpl) obj;
		if (!Arrays.equals(children, other.children))
			return false;
		return true;
	}
	
	protected static Function[] combineChildren(FunctionImpl func, Function f) {
		int nChildren = func.nbChildren();
		Function []combined = new Function[nChildren+ 1];
		for(int i =0 ; i < nChildren ; i++){
			combined[i] = func.child(i).deepCopy();
		}
		combined[func.children.length] = f.deepCopy();
		return combined;
	}

}
