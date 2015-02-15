/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.formulae.Functions.FmNegate;
import com.opendoorlogistics.core.formulae.StringTokeniser.StringToken;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.strings.Strings.ToString;

public final class FormulaParser {
	private UnidentifiedPolicy unidentifiedPolicy = UnidentifiedPolicy.THROW_EXCEPTION;
	
	
	public UnidentifiedPolicy getUnidentifiedPolicy() {
		return unidentifiedPolicy;
	}

	public void setUnidentifiedPolicy(UnidentifiedPolicy unidentifiedPolicy) {
		this.unidentifiedPolicy = unidentifiedPolicy;
	}

	public enum UnidentifiedPolicy{
		THROW_EXCEPTION,
		CREATE_UNIDENTIFIED_PLACEHOLDER_FUNCTION
	}
	
	private class ReadFunctionToken {
		String token;
		ArrayList<ReadFunctionToken> children = new ArrayList<>();
		FunctionFactory identified;
		TokenType tokenType;
		int precendence = -1;

		@Override
		public String toString() {
			return toString(false);
		}

		private String toString(boolean bracketsNeed) {
			StringBuilder builder = new StringBuilder();
			if (token != null) {
				builder.append(token);
			}

			boolean brackets = tokenType == TokenType.function || (children.size() > 0 && bracketsNeed == true);
			if (brackets) {
				builder.append("(");
			}

			int i = 0;
			for (ReadFunctionToken child : children) {
				if (tokenType == TokenType.function && i > 0) {
					builder.append(",");
				}
				builder.append(child.toString(tokenType != TokenType.function));
				i++;
			}

			if (brackets) {
				builder.append(")");
			}

			return builder.toString();
		}
	}

	private class SourceFormula {
		private ReadFunctionToken source;
		private Function formula;

		private SourceFormula(ReadFunctionToken source) {
			this.source = source;
		}

		@Override
		public String toString() {
			return source.tokenType.toString();
		}
	}

	private enum TokenType {
		operator, function, number, comma, nestedExpression, variable, string
	}

	private final UserVariableProvider userVariableProvider;

	private final static String anyNumberExactString = "^(" + StringTokeniser.doubleNumber + "|" + StringTokeniser.intNumber + ")$";

	private final static Pattern anyNumberExactStringMatcher = Pattern.compile(anyNumberExactString);

	final private static Pattern okVariableOrMethodName = Pattern.compile("^[a-z][\\w]*$", Pattern.CASE_INSENSITIVE);

	final private FunctionDefinitionLibrary library;

	public FormulaParser( UserVariableProvider userVariableProvider, FunctionDefinitionLibrary lib) {
		this.userVariableProvider = userVariableProvider;
		this.library =lib;
	}

//	public FormulaParser(final UserVariableProvider userVariableProvider) {
//		this.userVariableProvider = userVariableProvider;
//		this.library = new FunctionDefinitionLibrary();
//		library.build();
//	}

	private Function compileNestedExpression( ReadFunctionToken token) {
		ArrayList<SourceFormula> tmp = new ArrayList<>();
		for (ReadFunctionToken child : token.children) {
			tmp.add(new SourceFormula(child));
		}

		// Convert and remove + and - which are indicating sign as needed
		int removePlusIndx = 0;
		int compileMinusIndx = 0;
		while ((removePlusIndx != -1 || compileMinusIndx != -1) && tmp.size() > 0) {

			removePlusIndx = -1;
			compileMinusIndx = -1;

			// If we have an operator at the beginning or two in a row
			// these could be a + or minus indicating sign instead...
			ReadFunctionToken rft = tmp.get(0).source;
			if (StringTokeniser.isMinus(rft.token)) {
				compileMinusIndx = 0;
			} else if (isPlus(rft.token)) {
				removePlusIndx = 0;
			}

			// Check for any operator twice in a row
			int n = tmp.size();
			for (int i = 1; i < n && removePlusIndx == -1 && compileMinusIndx == -1; i++) {
				SourceFormula last = tmp.get(i - 1);
				SourceFormula current = tmp.get(i);
				if (last.source.tokenType == TokenType.operator && current.source.tokenType == TokenType.operator) {
					if (i + 1 >= tmp.size()) {
						throwException(token.token);
					}

					// cannot have 3 operators in a row
					SourceFormula next = tmp.get(i + 1);
					if (next.source.tokenType == TokenType.operator) {
						throwException(token.token);
					}

					// can only have two in a row if second is + or -
					if (isPlus(current.source.token)) {
						removePlusIndx = i;
						break;
					} else if (StringTokeniser.isMinus(current.source.token)) {
						compileMinusIndx = i;
						break;
					} else {
						throwException(token.token);
					}
				}
			}

			if (compileMinusIndx != -1) {
				if (compileMinusIndx + 1 >= tmp.size()) {
					throwException(token.token);
				}
				SourceFormula next = tmp.get(compileMinusIndx + 1);
				Function formula = generateFormula( next.source);
				next.formula = new FmNegate(formula);
				tmp.remove(compileMinusIndx);
				continue;
			}

			if (removePlusIndx != -1) {
				tmp.remove(removePlusIndx);
				continue;
			}
		}

		// parse in order of precedence
		int chosenIndx;
		int precendence;
		do {
			chosenIndx = -1;
			precendence = Integer.MAX_VALUE;

			int n = tmp.size();
			for (int i = 0; i < n; i++) {
				SourceFormula sf = tmp.get(i);
				if (sf.formula == null) {
					if (sf.source.precendence < precendence) {
						chosenIndx = i;
						precendence = sf.source.precendence;
					}
				}
			}

			if (chosenIndx != -1) {
				// compile this
				SourceFormula current = tmp.get(chosenIndx);

				if (current.source.tokenType == TokenType.operator) {
					// compile operator which combines preceding and next tokens
					if (chosenIndx < 1 || chosenIndx > tmp.size() - 1) {
						throwException(current.source.token);
					}

					// get previous and next
					SourceFormula previous = tmp.get(chosenIndx - 1);
					SourceFormula next = tmp.get(chosenIndx + 1);

					// compile previous and next if needed
					// Also check for unconverted operators (would indicate corrupt syntax with 2 operators in a row)
					if (previous.formula == null) {
						if (previous.source.tokenType == TokenType.operator) {
							throwException(current.source.token);
						}
						previous.formula = generateFormula( previous.source);
					}
					if (next.formula == null) {
						if (next.source.tokenType == TokenType.operator) {
							throwException(current.source.token);
						}
						next.formula = generateFormula( next.source);
					}

					// compile current
					current.formula = current.source.identified.createFunction( previous.formula, next.formula);

					// remove previous and next
					tmp.remove(chosenIndx + 1);
					tmp.remove(chosenIndx - 1);
				} else {
					current.formula = generateFormula( current.source);
					if(current.formula==null){
						throw new RuntimeException("Failed to compile formula " + current.source);
					}
				}
			}

		} while (chosenIndx != -1);

		// We should now only have one formula
		if (tmp.size() != 1) {
			throw new RuntimeException();
		}
		return tmp.get(0).formula;
	}

	private Function generateFormula( ReadFunctionToken token) {
		Function ret = null;

		switch (token.tokenType) {
		case function:
			// Can do one-by-one conversion of parameters
			int i = 0;
			Function[] children = new Function[token.children.size()];
			for (ReadFunctionToken child : token.children) {
				children[i++] = generateFormula( child);
			}
			ret = token.identified.createFunction( children);
			break;

		case number:
			if (token.children.size() > 0) {
				throwException(token.token);
			}
			
			if(StringTokeniser.isIntegerNumber(token.token)){
				ret = new FmConst(Long.parseLong(token.token));
			}
			else{
				ret = new FmConst(Double.parseDouble(token.token));				
			}
			break;

		case string:
			// get substring...
			String str = token.token.substring(1, token.token.length()-1);
			
			// check if its really a variable with speech marks around it...
			if(userVariableProvider!=null){
				ret =userVariableProvider.getVariable(str);				
			}
			if(ret==null){
				ret = new FmConst(str);				
			}
			
			break;
			
		case variable:
			FunctionFactory constFactory = library.identify(token.token, FunctionType.CONSTANT);
			if(constFactory!=null){
				ret = constFactory.createFunction();
			}
			
			// variables can mean different things in different contexts...
			// Usually referring to a field in the current row...
			// But lookup(datastore , tablename, foreign key name, primary key name, fetch field name)
			if(ret==null && userVariableProvider!=null){
				ret =userVariableProvider.getVariable(token.token);				
			}
			
			if(ret==null){
				if(unidentifiedPolicy == UnidentifiedPolicy.THROW_EXCEPTION){
					throw new RuntimeException("Unidentified variable: " + token.token);					
				}else{
					ret = FmUnidentified.FACTORY.createFunction();
				}
			}
			break;

		case nestedExpression:
			ret = compileNestedExpression( token);
			break;

		default:
			throwException(token.token);
			break;
		}

		return ret;
	}

	private boolean isPlus(String token) {
		return token != null && token.equals("+");
	}

	public Function parse(String formula){
		List<StringToken> tokens = StringTokeniser.tokenise(formula);
		return parseTokens(tokens);
	}
	
	private Function parseTokens(List<StringToken> tokens) {

		ReadFunctionToken[] tmp = new ReadFunctionToken[1];
		int readTokens = readFunctionTokenTree(tokens, 0, tmp, null);
		assert readTokens == tokens.size();

		// TODO... if we have userformulae we replace them in the tree now.
		
		Function formula = generateFormula( tmp[0]);
		return formula;
	}


	private int readFunctionTokenTree(List<StringToken> tokens, int level, ReadFunctionToken[] out, String[] lastTokenRead) {
		ReadFunctionToken ret = new ReadFunctionToken();
		ret.tokenType = TokenType.nestedExpression;
		out[0] = ret;

		int n = tokens.size();
		int i = 0;
		while (i < n) {
			String token = tokens.get(i).getLowerCase();
			if (lastTokenRead != null) {
				lastTokenRead[0] = token;
			}
			if (token.equals(")")) {
				// stop reading function
				i++;
				break;
			} else if (token.equals("(")) {
				// check if we're inside a function
				ReadFunctionToken function = null;
				if (ret.children.size() > 0) {
					ReadFunctionToken last = ret.children.get(ret.children.size() - 1);
					if (last.tokenType == TokenType.function) {
						function = last;
					}
				}

				// recurse
				ReadFunctionToken[] tmp = new ReadFunctionToken[1];
				i++;
				i += readFunctionTokenTree(tokens.subList(i, tokens.size()), level + 1, tmp, null);
				if (function != null) {
					List<List<ReadFunctionToken>> parameters = split(tmp[0].children, ",");

					for (List<ReadFunctionToken> parameter : parameters) {
						if (parameter.size() == 1) {
							function.children.add(parameter.get(0));
						} else {
							ReadFunctionToken node = new ReadFunctionToken();
							node.tokenType = TokenType.nestedExpression;
							node.children.addAll(parameter);
							function.children.add(node);
						}
					}

				} else {
					ret.children.add(tmp[0]);
				}
			} else if (token.equals(".")) {
				// Must be an attribute
				if (ret.children.size() == 0 || i >= n - 1) {
					throwException(tokens);
				}

				// Get attribute name
				i++;
				token = tokens.get(i).getOriginal();

				// Check valid name
				if (okVariableOrMethodName.matcher(token).find() == false) {
					throwException(tokens);
				}

				// Check we have a previous token we can add an attribute nameto
				ReadFunctionToken last = ret.children.get(ret.children.size() - 1);
				if (last.token == null) {
					throwException(tokens);
				}

				// Previous token must be a variable or function
				if (last.tokenType != TokenType.function && last.tokenType != TokenType.variable
						&& last.tokenType != TokenType.nestedExpression) {
					throwException(tokens);
				}

				// Go to one after attribute name and check not brackets
				i++;
				if (i < n && tokens.get(i).getLowerCase().equals("(")) {
					throwException(tokens);
				}
			} else {

				// create token and identify type
				final ReadFunctionToken rft = new ReadFunctionToken();
				rft.token = token;
				boolean okVariableName = okVariableOrMethodName.matcher(token).find();

				FunctionDefinition opDfn = library.identifyOperator(token);
				if(opDfn!=null){
					rft.identified = opDfn.getFactory();
					rft.tokenType = TokenType.operator;
					rft.precendence = opDfn.getOperatorPrecendence();
				}
				
//				else{
//					// try identifying an operator; operators are ordered by precedence
//					for (int precedence = 0; precedence < operators.size(); precedence++) {
//						MathsOperator op = operators.get(precedence);
//						if (op.matchesName(token)) {
//							rft.identified = op;
//							rft.tokenType = TokenType.operator;
//							rft.precendence = precedence;
//							break;
//						}
//					}			
//				}


				// try identifying number
				if (rft.tokenType == null && anyNumberExactStringMatcher.matcher(token).find()) {
					rft.tokenType = TokenType.number;
				}

				// try identifying comma
				if (rft.tokenType == null && token.equals(",")) {
					rft.tokenType = TokenType.comma;
				}

				// check if its a function
				if (rft.tokenType == null && i < n - 1 && tokens.get(i + 1).getLowerCase().equals("(")) {
					if (!okVariableName) {
						throwException(token);
					}
					
					rft.identified = library.identify(rft.token, FunctionType.FUNCTION);

					if (rft.identified == null) {
						if(unidentifiedPolicy == UnidentifiedPolicy.THROW_EXCEPTION){
							throw new RuntimeException("Unknown function \"" + rft.token + "\"");							
						}else{
							rft.identified = FmUnidentified.FACTORY;
						}
					}
					rft.tokenType = TokenType.function;
				}

				// check for string (including empty strings). save original case
				if(rft.tokenType==null && token.length()>=2 && token.charAt(0)=='"' && token.charAt(token.length()-1)=='"'){
					rft.tokenType = TokenType.string;
					rft.token = tokens.get(i).getOriginal();
				}
				
				// must be a variable...
				if (rft.tokenType == null) {
					if (!okVariableName) {
						throwException(token);
					}
					rft.tokenType = TokenType.variable;
				}
				i++;

				// break if we're reading a comma in the top-level
				if (rft.tokenType == TokenType.comma && level == 0) {
					break;
				} else {
					// otherwise save the token
					ret.children.add(rft);
				}
			}
		}
		return i;
	}


	private List<List<ReadFunctionToken>> split(List<ReadFunctionToken> tokens, String deliminator) {
		List<List<ReadFunctionToken>> ret = new ArrayList<>();
		ArrayList<ReadFunctionToken> current = new ArrayList<>();
		for (ReadFunctionToken token : tokens) {
			if (token.token != null && token.token.equals(deliminator)) {
				ret.add(current);
				current = new ArrayList<>();
			} else {
				current.add(token);
			}
		}

		if (current.size() > 0) {
			ret.add(current);
		}
		return ret;
	}

	private static void throwException(List<StringToken> tokens)  {
		throw new RuntimeException(Strings.toString(new ToString<StringToken>() {

			@Override
			public String toString(StringToken o) {
				return o.getOriginal();
			}
		}, " ", tokens));
	}
	
//	private static void throwException(List<String> tokens)  {
//		throw new RuntimeException("Error parsing tokens \"" + tokens + "\"");
//	}

	private static void throwException(String token)  {
		throw new RuntimeException("Error parsing token \"" + token + "\"");
	}

	public static void main(String[] args) throws Exception {
		FunctionDefinitionLibrary lib = new FunctionDefinitionLibrary();
		lib.build();
		FormulaParser loader = new FormulaParser(null, lib);
		Function formula = loader.parse("postcodeukdistrict(\"gl51 8bg\")");
		System.out.println(formula.execute(null));
	}
	
	@Override
	public String toString(){
		return library.toString();
	}
	
	/**
	 * Placeholder formula to mark anything that couldn't be identified
	 * @author Phil
	 *
	 */
	public static class FmUnidentified extends FunctionImpl{
		public FmUnidentified(Function ... children) {
			super(children);
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			return Functions.EXECUTION_ERROR;
		}

		@Override
		public Function deepCopy() {
			// TODO Auto-generated method stub
			return null;
		}
		
		public static final FunctionFactory FACTORY = new FunctionFactory() {
			
			@Override
			public Function createFunction(Function... children) {
				return new FmUnidentified(children);
			}
		};
		
		public static boolean containsUnidentified(Function f){
			if(f!=null ){
				if(FmUnidentified.class.isInstance(f)){
					return true;
				}
				int n = f.nbChildren();
				for(int i =0 ; i < n ; i++){
					if(containsUnidentified(f.child(i))){
						return true;
					}
				}
			}
			return false;
		}
	}
	
}
