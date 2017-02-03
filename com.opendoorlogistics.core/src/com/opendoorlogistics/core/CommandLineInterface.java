package com.opendoorlogistics.core;

import java.util.Arrays;

import com.opendoorlogistics.core.formulae.StringTokeniser;
import com.opendoorlogistics.core.geometry.Shapefile2TextCommand;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

public class CommandLineInterface {
	private static final StandardisedStringTreeMap<Command> COMMANDS = new StandardisedStringTreeMap<CommandLineInterface.Command>(true);
	
	public interface Command {

		public String[] getKeywords();

		public String getDescription();

		public boolean execute(String[] args);
	}

	public static boolean process(String [] args){
		if(args!=null && args.length>0){
			String keyword=null;
			for(String minus:StringTokeniser.minuses){
				if(args[0].startsWith(minus)){
					args[0] = args[0].substring(1, args[0].length());
					keyword = args[0];
				}
			}
			
			if(keyword!=null){
				System.out.println("Processing command line: " + Strings.toString(" ", args));
				Command command = COMMANDS.get(keyword);
				if(command==null){
					System.out.println("Could not find command: " + command);
				}else{
					args = Arrays.copyOfRange(args, 1, args.length);
					for(int i =0 ; i < args.length ; i++){
						args[i] = trimSpeechMarks(args[i]);
					}
					command.execute(args);
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public static void registerCommand(Command command){
		for(String keyword : command.getKeywords()){
			if(COMMANDS.containsKey(keyword)){
				throw new RuntimeException("Registering a command twice: " + keyword);
			}
			COMMANDS.put(keyword, command);
		}
	}
	
	public static Iterable<Command> getCommands(){
		return COMMANDS.values();
	}
	
	private static String trimSpeechMarks(String s){
		// remove start and end speech marks
		if (s.length() > 0 && s.startsWith("\"")) {
			s = s.substring(1, s.length());
		}
		if (s.length() > 0 && s.endsWith("\"")) {
			s= s.substring(0, s.length() - 1);
		}
		return s;
	}
	
	static{
		registerCommand(new Shapefile2TextCommand());
		registerCommand(new Shapefile2TextCommand.Shapefile2TextCommandDir());
		
	}
}
