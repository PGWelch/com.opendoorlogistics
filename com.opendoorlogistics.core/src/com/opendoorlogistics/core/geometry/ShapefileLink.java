/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.core.formulae.StringTokeniser;
import com.opendoorlogistics.core.formulae.StringTokeniser.StringToken;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * Represents a link to an object in a shapefile. As the file could be relative or absolute, and may in the future be the name of an entry in some
 * type of data library, it is represented by String not File.
 * 
 * Hashcode and equals for a ShapefileLink use standardised strings (case insensitive with leading/trailing spaces ignored).
 * 
 * @author Phil
 * 
 */
public final class ShapefileLink {
	static final String SHAPEFILELINK_KEYWORD = "shapefilelink";
	private final String filename;
	private final String typename;
	private final String featureId;

	private final String filenameStd;
	private final String typenameStd;
	private final String featureIdStd;

	public ShapefileLink(String filename, String typename, String featureId) {
		this.filename = FilenameUtils.normalize(filename);
		this.typename = typename;
		this.featureId = featureId;

		// Also save standardised version for the hash lookup, including standardising slashes in the filename
		// to stop issues with shapefilelink generated in linux and used in windows (or vice-versa).
		filenameStd = Strings.std(this.filename).replace("/", "\\");
		typenameStd = Strings.std(typename);
		featureIdStd = Strings.std(featureId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((featureIdStd == null) ? 0 : featureIdStd.hashCode());
		result = prime * result + ((filenameStd == null) ? 0 : filenameStd.hashCode());
		result = prime * result + ((typenameStd == null) ? 0 : typenameStd.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShapefileLink other = (ShapefileLink) obj;
		if (featureIdStd == null) {
			if (other.featureIdStd != null)
				return false;
		} else if (!featureIdStd.equals(other.featureIdStd))
			return false;
		if (filenameStd == null) {
			if (other.filenameStd != null)
				return false;
		} else if (!filenameStd.equals(other.filenameStd))
			return false;
		if (typenameStd == null) {
			if (other.typenameStd != null)
				return false;
		} else if (!typenameStd.equals(other.typenameStd))
			return false;
		return true;
	}

	public String getFile() {
		return filename;
	}
	
	public String getTypename() {
		return typename;
	}


	public String getFeatureId() {
		return featureId;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(SHAPEFILELINK_KEYWORD);
		ret.append("(\"");
		ret.append(filename);
		ret.append("\",\"");
		ret.append(typename);
		ret.append("\",\"");
		ret.append(featureId);
		ret.append("\")");
		return ret.toString();
	}

	public static ShapefileLink parse(String s) {
		List<StringToken> tokens = StringTokeniser.tokenise(s);
		// 0 = keyword
		// 1 = (
		// 2 = filename
		// 3 = ,
		// 4 = typename
		// 5 = ,
		// 6 = id
		// 7 = )
		if (tokens.size() != 8) {
			return null;
		}

		// check keyword
		if (!Strings.equalsStd(tokens.get(0).getOriginal(), SHAPEFILELINK_KEYWORD)) {
			return null;
		}

		// check correct ( , )
		if (tokens.get(1).getOriginal().equals("(") == false || tokens.get(3).getOriginal().equals(",") == false
				|| tokens.get(5).getOriginal().equals(",") == false || tokens.get(7).getOriginal().equals(")") == false) {
			return null;
		}

		String filename = tokens.get(2).getOriginal().replace("\"", "");
		String typename = tokens.get(4).getOriginal().replace("\"", "");
		String id = tokens.get(6).getOriginal().replace("\"", "");
		return new ShapefileLink(filename, typename, id);
	}

	public static void main(String[] args) {
		for (String s : new String[] { "shapefilelink( \"states.shp\" , \"states\", \"23\")" }) {
			System.out.println(s + " ->" + parse(s));
		}
	}
}
