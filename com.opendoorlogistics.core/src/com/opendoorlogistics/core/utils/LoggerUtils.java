package com.opendoorlogistics.core.utils;

import java.time.LocalDateTime;
import java.util.LinkedList;

public class LoggerUtils {
	public static String prefix(){
		StringBuilder builder = new StringBuilder();
		builder.append(LocalDateTime.now().toString());
		builder.append(" - ");
		
		try {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			if(stackTraceElements!=null && stackTraceElements.length>2){
				StackTraceElement caller = stackTraceElements[2];
				
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
		return builder.toString();
	}

	
}
