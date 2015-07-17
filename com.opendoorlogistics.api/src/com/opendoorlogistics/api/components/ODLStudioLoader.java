package com.opendoorlogistics.api.components;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.JOptionPane;

/**
 * A class which allows a component to be debugged without adding ODL Studio to its project as a library
 * (which can cause jar-hell type problems). ODL Studio is loaded by reflection so fewer incompatible
 * jar version problems should be encountered.
 * 
 * Note that when a plugin component is loaded into ODL Studio, ODL Studio loads it by reflection instead,
 * so the classloaders are the other way round, which can cause differences in behaviour if jar version
 * problems are present.
 * @author Phil
 *
 */
public class ODLStudioLoader {
	public static void load(String[] commandLineArguments, ODLComponent ...components)  {
		if(commandLineArguments.length==0){
			JOptionPane.showMessageDialog(null, "Expected a command line argument with path to com.opendoorlogistics.studio.jar");
		}
		
		// load the ODL Studio jar by reflection
		String trimmed = commandLineArguments[0].replaceAll("\"", "");
		File file = new File(trimmed);
		load(file, components);
	}

	public static void load(File odlstudiojarfile, ODLComponent... components)  {
		try {
			URL url = odlstudiojarfile.toURI().toURL();
			URL[] urls = new URL[] { url };
			@SuppressWarnings("resource")
			ClassLoader cl = new URLClassLoader(urls);

			// find appframe class
			Class<?> appFrameCls = cl.loadClass("com.opendoorlogistics.studio.appframe.AppFrame");
			
			// and start appframe giving it the vehicle routing component
			Method start = appFrameCls.getMethod("startWithComponents", ODLComponent[].class);
			start.invoke(null, new Object[]{ components});			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
