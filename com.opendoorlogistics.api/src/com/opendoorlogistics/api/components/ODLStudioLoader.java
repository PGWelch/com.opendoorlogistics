package com.opendoorlogistics.api.components;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.JOptionPane;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.app.ODLApp;

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
public interface ODLStudioLoader {


//	public static void load(File odlstudiojarfile, ODLComponent... components)  {
//		try {
//			findLoader(odlstudiojarfile).startStudio(components);		
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}

	/**
	 * Start ODL Studio
	 * @param components Additional components to add.
	 * @return
	 */
	public ODLApp startStudio(ODLComponent ... components);
	
	/**
	 * Create an ODL Api
	 * @return
	 */
	public ODLApi createApi();
	
	public static final String LOADER_IMPLEMENTATION_NAME = "com.opendoorlogistics.studio.InitialiseStudio";
	

	
	/**
	 * Find the loader implementation in the input jar file using reflection.
	 * @param odlStudioJar Jar file containing ODL Studio
	 * @return
	 * @throws ClassNotFoundException
	 * @throws MalformedURLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static ODLStudioLoader findLoader(File odlStudioJar) throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException{
		URL url = odlStudioJar.toURI().toURL();
		URL[] urls = new URL[] { url };
		@SuppressWarnings("resource")
		ClassLoader cl = new URLClassLoader(urls);

		// find appframe class
		Class<?> loaderCls = cl.loadClass(LOADER_IMPLEMENTATION_NAME);
		return (ODLStudioLoader)loaderCls.newInstance();
	}
	
	/*
	 * Find the loader using the current classpath
	 */
	public static ODLStudioLoader findLoader() throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		Class<?> loaderCls = ODLStudioLoader.class.getClassLoader().loadClass(LOADER_IMPLEMENTATION_NAME);
		return (ODLStudioLoader)loaderCls.newInstance();
	}
}
