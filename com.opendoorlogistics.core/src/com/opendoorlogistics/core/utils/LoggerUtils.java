package com.opendoorlogistics.core.utils;

import java.time.LocalDateTime;

public class LoggerUtils {
	public static String addPrefix(String s){
		StringBuilder builder = new StringBuilder();
		prefix(builder,3);
		builder.append(s);
		return builder.toString();	
	}
	
	public static String prefix(){
		StringBuilder builder = new StringBuilder();
		prefix(builder,3);
		return builder.toString();
	}

	private static void prefix(StringBuilder builder, int stackDepth) {
		builder.append(LocalDateTime.now().toString());
		builder.append(" - ");
		
		try {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			if(stackTraceElements!=null && stackTraceElements.length>stackDepth){
				StackTraceElement caller = stackTraceElements[stackDepth];
				
				if(caller.getClassName()!=null){
					builder.append(caller.getClassName());
					builder.append(".");
				}
				if(caller.getMethodName()!=null){
					builder.append(caller.getMethodName());
				}
				

			//	builder.append(" - ");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	//	builder.append(c)
	}

	
}
