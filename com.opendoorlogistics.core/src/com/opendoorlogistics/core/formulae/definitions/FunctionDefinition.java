/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae.definitions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionArgument;
import com.opendoorlogistics.core.utils.strings.Strings;

public final class FunctionDefinition implements Iterable<FunctionArgument> {
	private final String name;
	private final List<FunctionArgument> args = new ArrayList<>();
	private final FunctionType type;
	private FunctionFactory factory;
	private String description;
	private int operatorPrecendence=Integer.MAX_VALUE;
	private String group;
	
	public enum FunctionType {
		 OPERATOR,FUNCTION, CONSTANT
	}

	public enum ArgumentType {
		GENERAL, STRING_CONSTANT, TABLE_REFERENCE_CONSTANT
	}

	public FunctionDefinition(String name) {
		this(FunctionType.FUNCTION, name);
	}

	public FunctionDefinition(FunctionType type, String name) {
		this.name = name;
		this.type = type;
	}

	public static class FunctionArgument {
		private final String name;
		private final ArgumentType argType;
		private final String description;
		private final boolean varargs;

		public FunctionArgument(String name, String description) {
			this(name, ArgumentType.GENERAL, description, false);
		}
		
		public FunctionArgument(String name, ArgumentType argType, String description, boolean varArgs) {
			this.name = name;
			this.argType = argType;
			this.description = description;
			this.varargs = varArgs;
		}

		public String getName() {
			return name;
		}

		public boolean isConstantString() {
			return argType == ArgumentType.STRING_CONSTANT || argType == ArgumentType.TABLE_REFERENCE_CONSTANT;
		}

		public ArgumentType getArgumentType() {
			return argType;
		}

		public String getDescription() {
			return description;
		}

		public boolean isVarArgs() {
			return varargs;
		}
	}

	public int addArg(String name, String description) {
		return addArg(name, ArgumentType.GENERAL, description);
	}

	public int addArg(String name, ArgumentType argType) {
		return addArg(name, argType, null);
	}

	public int addArg(String name) {
		return addArg(name, ArgumentType.GENERAL, null);
	}

	public String getName() {
		return name;
	}

	public FunctionDefinition deepCopy() {
		FunctionDefinition ret = new FunctionDefinition(name);
		for (FunctionArgument arg : this) {
			ret.addArg(arg.getName(), arg.getArgumentType(), arg.getDescription(), arg.isVarArgs());
		}
		return ret;
	}

	public int addArg(String name, ArgumentType argType, String description) {
		return addArg(name, argType, description, false);
	}

	public int addVarArgs(String name, ArgumentType argType, String description) {
		return addArg(name, argType, description, true);
	}

	public boolean hasVarArgs() {
		for (FunctionArgument arg : args) {
			if (arg.isVarArgs()) {
				return true;
			}
		}
		return false;
	}

	private int addArg(String name, ArgumentType argType, String description, boolean isVarArgs) {
		return addArg(new FunctionArgument(name, argType, description, isVarArgs));
	}

	public int addArg(FunctionArgument arg){
		checkNameOk(arg.getName());	
		if (hasVarArgs()) {
			throw new RuntimeException("Varargs must be at the end of the function and we can only have one of them.");
		}
		args.add(arg);
		return args.size()-1;
	}
	
	private void checkNameOk(String name) {
		if (Strings.std(name).length() == 0) {
			throw new RuntimeException();
		}

		if (indexOf(name) != -1) {
			throw new RuntimeException();
		}
	}

	public int nbArgs() {
		return args.size();
	}

	public FunctionArgument getArg(int i) {
		return args.get(i);
	}

	public int indexOf(String s) {
		for (int i = 0; i < args.size(); i++) {
			if (Strings.equalsStd(s, getArg(i).getName())) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Iterator<FunctionArgument> iterator() {
		return args.iterator();
	}

	@Override
	public String toString() {
		return getSignature(false);
	}

	public String getSignature(boolean html) {
		StringBuilder builder = new StringBuilder();
		if(html){
			builder.append("<html><b>");
		}
		builder.append(getName());
		if(html){
			builder.append("</b>");
		}
		
		switch (type) {
		case FUNCTION:
			builder.append("(");
			for (int i = 0; i < nbArgs(); i++) {
				if (i > 0) {
					builder.append(", ");
				}
				FunctionArgument arg = getArg(i);
		
				builder.append(arg.getName());
				if (arg.isVarArgs()) {
					builder.append("...");
				}
			}
			builder.append(")");
			break;

		case CONSTANT:
			break;

		case OPERATOR:
			break;
		}
		if(html){
			builder.append("</html>");
		}
		return builder.toString();
	}

	public FunctionFactory getFactory() {
		return factory;
	}

	public void setFactory(FunctionFactory factory) {
		this.factory = factory;
	}

	public FunctionType getType() {
		return type;
	}

	public int getOperatorPrecendence() {
		return operatorPrecendence;
	}

	public void setOperatorPrecendence(int operatorPrecendence) {
		this.operatorPrecendence = operatorPrecendence;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

}
