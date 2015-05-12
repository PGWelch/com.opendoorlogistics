package com.opendoorlogistics.core.utils;

/**
 * Simple class to help with basic code timing tasks
 * @author Phil
 *
 */
public class SimpleCodeTimer {
	private long start = System.currentTimeMillis();
	
	public SimpleCodeTimer(){
		
	}
	
	public void print(String message){
		long now = System.currentTimeMillis();
		System.out.println(message + ": " + (now - start) + " milliseconds");
		start = now;
	}
	
}
