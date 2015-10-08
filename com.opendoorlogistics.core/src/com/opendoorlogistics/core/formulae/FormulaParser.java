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
import com.opendoorlogistics.core.scripts.elements.UserFormula;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
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

	/**
	 * Dummy placeholder used when processing user formulae
	 * @author Phil
	 *
	 */
	private class UserFormulaPlaceholder implements FunctionFactory{
		private final List<UserFormulaInternal> ufs;

		UserFormulaPlaceholder(List<UserFormulaInternal> ufs) {
			this.ufs = ufs;
		}

		@Override
		public Function createFunction(Function... children) {
			throw new RuntimeException();
		}
		
	}
	
	private class UserFormulaInternal{
		private final String name;
		private final List<String> parameters = new ArrayList<String>();
		private final List<StringToken> definition;
		private StandardisedStringTreeMap<Integer> parameterNumbers = new StandardisedStringTreeMap<Integer>(false);
		
		UserFormulaInternal(String formula){
			List<StringToken> tokens = StringTokeniser.tokenise(formula);
			
//			if (okVariableOrMethodName.matcher(token).find() == false) {
//				throwException(tokens);
//			}
			
			int iToken = 0;
			int nt = tokens.size();
			boolean ok = true;
			
			String errorMessage=null;
			if(iToken< nt){
				name = tokens.get(iToken++).getLowerCase();
				if (!isOkVariableOrMethodName(name)) {
					ok = false;
					errorMessage = "Invalid function name: " + name;
				}		
			}else{
				ok = false;
				errorMessage = "Missing function name";
				name = null;
			}
			
			if(iToken>=nt || !tokens.get(iToken++).getLowerCase().equals("(")){
				ok = false;
				errorMessage = "Missing left bracket ( at start of user formula";
			}
			
			// get parameters
			boolean nextIsParam=true;
			while(ok){
				
				// check we still have a token
				if(iToken>=nt){
					ok = false;
					errorMessage = "Incorrect function format.";
					break;
				}
				
				StringToken token = tokens.get(iToken++);
				
				// check for end of parameters
				String sToken = token.getLowerCase();
				if(sToken.equals(")")){
					break;
				}
				
				if(nextIsParam){
					if(!isOkVariableOrMethodName(sToken)){
						ok = false;
						errorMessage = "Invalid parameter name: " + sToken;
						break;
					}
					
					// check parameter name is unique
					if(parameterNumbers.get(sToken)!=null){
						ok = false;
						errorMessage = "Parameter name used twice or more times: " + sToken;
						break;	
					}
					
					parameterNumbers.put(sToken, parameters.size());
					parameters.add(sToken);
					nextIsParam = false;
					
				}else{
					if(!sToken.equals(",")){
						ok = false;
						errorMessage = "Comma is missing from list of parameters.";
					}
					nextIsParam = true;
				}
			}
			
			// check for equals
			if(ok){
				if(iToken >=nt || !tokens.get(iToken++).getLowerCase().equals("=")){
					ok = false;
					errorMessage = "Equals sign = missing after parameter names.";										
				}
			}
			
			// check for non-empty
			if(ok && iToken>=nt){
				ok = false;
				errorMessage = "Empty user function definition.";														
			}
			
			if(ok){
				definition = tokens.subList(iToken, tokens.size());
			}
			else{
				definition =null;
				throw new RuntimeException("Error parsing user formula: " + formula + (errorMessage!=null ? System.lineSeparator() + errorMessage : ""));
			}
		}
	}
	
	private enum TokenType {
		operator, function, number, comma, nestedExpression, variable, string
	}

	private final UserVariableProvider userVariableProvider;

	private final static String anyNumberExactString = "^(" + StringTokeniser.doubleNumber + "|" + StringTokeniser.intNumber + ")$";

	private final static Pattern anyNumberExactStringMatcher = Pattern.compile(anyNumberExactString);

	final private static Pattern okVariableOrMethodName = Pattern.compile("^" + StringTokeniser.VARIABLE +"$", Pattern.CASE_INSENSITIVE);

	final private FunctionDefinitionLibrary library;
	
	final private StandardisedStringTreeMap<ArrayList<UserFormulaInternal>> userFormulae = new StandardisedStringTreeMap<ArrayList<UserFormulaInternal>>(false);

//	public FormulaParser( UserVariableProvider userVariableProvider, FunctionDefinitionLibrary lib) {
//		this(userVariableProvider, lib, new ArrayList<String>());
//	}
	
	public FormulaParser( UserVariableProvider userVariableProvider, FunctionDefinitionLibrary lib,List<UserFormula> userformulaeDfns  ) {
		this.userVariableProvider = userVariableProvider;
		this.library =lib;
		
		if(userformulaeDfns!=null){
			for(UserFormula s : userformulaeDfns){
				UserFormulaInternal uf = new UserFormulaInternal(s.getValue());
				ArrayList<UserFormulaInternal> list = userFormulae.get(uf.name);
				if(list==null){
					list = new ArrayList<UserFormulaInternal>();
					
					userFormulae.put(uf.name, list);
				}else{
					// check don't have 2 with same number of parameters
					for(UserFormulaInternal other:list){
						if(other.parameters.size()==uf.parameters.size()){
							throw new RuntimeException("Found two or more user formulae with the same name and number of parameters: " + uf.name);
						}
					}					
				}
				list.add(uf);
				
			}
		}
	}

	
	private static boolean isOkVariableOrMethodName(String name){
		return okVariableOrMethodName.matcher(name).find() ;
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

		// Do first parse to build the top-level tree
		ReadFunctionToken[] tmp = new ReadFunctionToken[1];
		int readTokens = readFunctionTokenTree(tokens, 0,null, tmp);
		assert readTokens == tokens.size();

		// Then parse for user formula and replace them in the tree
		tmp[0] = recurseProcessUserFormulae(tmp[0]);
		
		Function formula = generateFormula( tmp[0]);
		return formula;
	}

	private ReadFunctionToken recurseProcessUserFormulae(ReadFunctionToken rft){
		
		if(rft.identified!=null && UserFormulaPlaceholder.class.isInstance(rft.identified)){
			// Use the number of children to identify the correct overload
			int nc = rft.children!=null ? rft.children.size():0;
			List<UserFormulaInternal> ufs = ((UserFormulaPlaceholder)rft.identified).ufs;
			UserFormulaInternal found = null;
			for(UserFormulaInternal uf:ufs){
				if(uf.parameters.size() == nc){
					found = uf;
				}
			}
			
			if(found==null){
				throw new RuntimeException("Cannot find a definition of user formula " + ufs.get(0).name + " taking " + nc + " parameters.");
			}
			
			// Create a string map of parameter name to ReadFunctionToken tree
			StandardisedStringTreeMap<ReadFunctionToken> parameters = new StandardisedStringTreeMap<FormulaParser.ReadFunctionToken>(false);
			for(int i =0 ; i<nc ; i++){
				parameters.put(found.parameters.get(i), rft.children.get(i));
			}
			
			// We now need to create the ReadFunctionToken tree for the user formula, copying the parameters into it
			ReadFunctionToken[] tmp = new ReadFunctionToken[1];			
			readFunctionTokenTree(found.definition, 0, parameters, tmp);
			
			// And replace this node in the tree with the user formula
			rft = tmp[0];
		}
		
		// parse children
		int nc = rft.children!=null ? rft.children.size():0;		
		for(int i =0 ; i < nc ; i++){
			ReadFunctionToken child = rft.children.get(i);
			child = recurseProcessUserFormulae(child);
			rft.children.set(i, child);
		}
		return rft;
	}
	
	private int readFunctionTokenTree(List<StringToken> tokens, int level,StandardisedStringTreeMap<ReadFunctionToken> userFormulaeParameters, ReadFunctionToken[] out) {
		ReadFunctionToken ret = new ReadFunctionToken();
		ret.tokenType = TokenType.nestedExpression;
		out[0] = ret;

		int n = tokens.size();
		int i = 0;
		while (i < n) {
			String token = tokens.get(i).getLowerCase();
			
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
				i += readFunctionTokenTree(tokens.subList(i, tokens.size()), level + 1,userFormulaeParameters, tmp);
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
				ReadFunctionToken rft = new ReadFunctionToken();
				rft.token = token;
				boolean okVariableName = okVariableOrMethodName.matcher(token).find();

				FunctionDefinition opDfn = library.identifyOperator(token);
				if(opDfn!=null){
					rft.identified = opDfn.getFactory();
					rft.tokenType = TokenType.operator;
					rft.precendence = opDfn.getOperatorPrecendence();
				}

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

					// try userformula first so scripts are never broken if a new formula is introduced with same name
					List<UserFormulaInternal> ufs = userFormulae.get(rft.token);
					if(ufs!=null){
						rft.identified = new UserFormulaPlaceholder(ufs);
					}

					// then try built in-function
					if(rft.identified == null){
						rft.identified = library.identify(rft.token, FunctionType.FUNCTION);						
					}
					
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
	
				// check for parameter before checking for variables
				if (rft.tokenType == null && userFormulaeParameters!=null && userFormulaeParameters.containsKey(rft.token)) {
					if (!okVariableName) {
						throwException(token);
					}
					
					// replace with the parameter
					rft = userFormulaeParameters.get(rft.token);
				}
				
				// must be a variable
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
		lib.buildStd();
		
		List<UserFormula> userFormulae = new ArrayList<UserFormula>();
	//	userFormulae.add(new UserFormula("add2(a,b) = a + b"));
		//userFormulae.add(new UserFormula("ten() = 10"));
		FormulaParser loader = new FormulaParser(null, lib, userFormulae);
		Function formula = loader.parse("\"hello \"world\"\"");
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
