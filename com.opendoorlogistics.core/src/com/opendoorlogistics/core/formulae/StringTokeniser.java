/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opendoorlogistics.core.utils.strings.Strings;

public final class StringTokeniser {
	final public static String [] minuses = new String[]{"-", "\u2013", "\u2014"};
	final public static String doubleNumber = "(\\d+\\.\\d*)";
	final public static String intNumber = "\\d+";
	final public static String[] tokensInLine = new String[] {
		"(\"[ \\t#-~!]*\")",
		"('[ \\t#-~!]*')",
		"(<=)", "&&","&", "\\|\\|","\\|" ,"==", ">=", "!=", "<>", "=", "<", ">", "/", "\\++", "\\(", "\\)", 
		"\\–", "\\-", "%", "\\*", ",", "\\}", "\\{", ":" ,"#",
		"\\[", "\\]"
		}; 
	protected final static  Pattern lineTokeniserPattern;
	protected final static  Pattern intNumberCheck;

	final public static String VARIABLE = "([a-z][abcdefghijklmnopqrstuvwxyz0123456789._]*)";
//	final private static Pattern okVariableOrMethodName = Pattern.compile("^[a-z][\\w]*$", Pattern.CASE_INSENSITIVE);

	static{ 
		
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < tokensInLine.length; i++) {
			if (i > 0)
				builder.append("|");
			builder.append(tokensInLine[i]);
		}

//		lineTokeniserPattern = Pattern.compile("^[\\s]*(\\w+"  + "|" + doubleNumber + "|" + intNumber + "|" + builder.toString() + ")[\\s]*?", Pattern.CASE_INSENSITIVE);
		lineTokeniserPattern = Pattern.compile("^[\\s]*("+ VARIABLE + "|" + doubleNumber + "|" + intNumber + "|" + builder.toString() + ")[\\s]*?", Pattern.CASE_INSENSITIVE);
		intNumberCheck = Pattern.compile("^\\d+$");
	}

//	public static ArrayList<ArrayList<String>> groupTokensByCommaSeparators(List<String> tokens)  {
//		ArrayList<ArrayList<String>> grouped = new ArrayList<>();
//
//		int n = tokens.size();
//		ArrayList<String> current = new ArrayList<>();
//		for (int i = 0; i < n; i++) {
//			if (tokens.get(i).equals(",")) {
//				grouped.add(current);
//				current = new ArrayList<>();
//			} else {
//				current.add(tokens.get(i));
//			}
//		}
//
//		if (current.size() > 0) {
//			grouped.add(current);
//		}
//		return grouped;
//	}

	public static class StringToken{
		private final String original;
		private final String lowerCase;
		private final int position;
		
		public StringToken(String original, int position) {
			this.original = original;
			this.lowerCase = original.toLowerCase();
			this.position = position;
		}

		public String getOriginal() {
			return original;
		}

		public String getLowerCase() {
			return lowerCase;
		}

		public int getPosition() {
			return position;
		}
		
		@Override
		public String toString(){
			return lowerCase;
		}
	}
	
	public static void main(String []args){
		class Print{
			void print(String s){
				List<StringToken> list = tokenise(s);
				System.out.println(s + " -> " + list);
			}
		}
		
//		char l = '\u201C';
//		char r = '\u201D';
//		
//		System.out.println(l);
//		System.out.println(r);
		Print print = new Print();
		print.print("1+7+polygon.name*2");
		print.print("5+7.433*4");
	}
	
	public static List<StringToken> tokenise(String s) {

		// replace odd quotes with normal quotes.		
		s = Strings.standardiseSpeechMarks(s);
		
		ArrayList<StringToken> list = new ArrayList<>();
		int currentIndx = 0;
		Matcher matcher = null;
		while (true) {
			CharSequence sub = s.subSequence(currentIndx, s.length());
			matcher = lineTokeniserPattern.matcher(sub);
			if (matcher.find()) {
				String m = matcher.group(1);
				list.add(new StringToken(m ,list.size()));
				currentIndx += matcher.end();
				
			} else {
				String noSpaces =sub.toString().trim();
				if(noSpaces.length()>0){
					throw new RuntimeException("Could not identify part of formula: " + noSpaces);
				}
				break;
			}
		}
		
//		// hack - join doubles
//		ArrayList<StringToken> ret = new ArrayList<>();
//		while(list.size()>0){
//			int nbToRemove=1;
//			if(list.size()>=3){
//				if(intNumberCheck.matcher(list.get(0).getOriginal()).matches()
//						&& list.get(1).getOriginal().equals(".") 
//						&& intNumberCheck.matcher(list.get(2).getOriginal()).matches()){
//					ret.add( new StringToken(list.get(0).getOriginal() + "." + list.get(2).getOriginal(), list.get(0).getPosition()));
//					nbToRemove = 3;
//				}
//				else{
//					ret.add(list.get(0));					
//				}
//			}else{
//				ret.add(list.get(0));
//			}
//			
//			for(int i =0 ; i < nbToRemove ; i++){
//				list.remove(0);
//			}
//		}
		
		return list;
	}
	
//	static Integer safeParseInt(String s){
//		if(intNumberCheck.matcher(s).find()){
//			return Integer.parseInt(s);
//		}
//		return null;
//	}
	
	public static String[] minuses(){
		return minuses;
	}
	
	public static boolean isMinus(String token){
		if(token==null){
			return false;
		}
		for(String minus : minuses){
			if(token.equals(minus)){
				return true;
			}
		}	
		return false;
	}

//	public static double [] readLineAsDoubles(String line){
//		List<String> tokens = tokenise(line, false);
//		int i =0;
//		while(i < tokens.size()-1){
//			if(isMinus(tokens.get(i))){
//				tokens.set(i+1, "-" + tokens.get(i+1));
//				tokens.remove(i);
//			}else{
//				i++;
//			}
//		}
//	
//		int n = tokens.size();
//		double [] ret = new double[n];
//		for(i = 0 ;  i < n ; i++){
//			ret[i] = Double.parseDouble(tokens.get(i));
//		}
//		return ret;
//	}
	
	public static boolean isIntegerNumber(String s){
		return intNumberCheck.matcher(s).matches();
	}
}
