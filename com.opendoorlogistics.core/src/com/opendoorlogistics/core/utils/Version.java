/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A handy class to store version numbers according to the GNU standard of major, minor and revision
 * @author Phil
 *
 */

@XmlRootElement(name = "Version")
final public class Version implements Comparable<Version>, Serializable{
	private int major;
	private int minor;
	private int revision;
	
	public int getMajor() {
		return major;
	}
	
	@XmlAttribute
	public void setMajor(int major) {
		this.major = major;
	}
	public int getMinor() {
		return minor;
	}
	
	@XmlAttribute
	public void setMinor(int minor) {
		this.minor = minor;
	}
	public int getRevision() {
		return revision;
	}
	
	@XmlAttribute
	public void setRevision(int revision) {
		this.revision = revision;
	}
	
	@Override
	public String toString() {
		return "" + major + "." + minor + "." + revision;
	}
	
	public Version(){}
	
	public Version(int major, int minor, int revision) {
		super();
		this.major = major;
		this.minor = minor;
		this.revision = revision;
	}
	
	public Version(String s){
		String [] split = s.split("\\.");
		if(split.length!=3){
			throw new RuntimeException("Invalid version number string: " + s);
		}
		major = Integer.parseInt(split[0]);
		minor = Integer.parseInt(split[1]);
		revision = Integer.parseInt(split[2]);
	}
	
	@Override
	public int compareTo(Version o) {
		int diff = Integer.compare(major, o.major);
		if(diff==0){
			diff = Integer.compare(minor, o.minor);
		}
		if(diff==0){
			diff = Integer.compare(revision, o.revision);
		}
		return diff;
	}

}
