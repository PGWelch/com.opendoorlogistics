/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opendoorlogistics.api.Factory;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.core.formulae.StringTokeniser;
import com.opendoorlogistics.core.formulae.StringTokeniser.StringToken;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

public class StringConventionsImpl implements StringConventions {

	@Override
	public String getVehicleName(String typeName, int numberOfVehiclesInType, int vehicleIndex) {
		if (typeName == null) {
			// names can be null
			typeName = "";
		}
		return getVehicleTypeString(typeName, numberOfVehiclesInType, vehicleIndex);
	}

	@Override
	public String getVehicleId(String typeId, int numberOfVehiclesInType, int vehicleIndex) {
		if (Strings.isEmpty(typeId)) {
			throw new RuntimeException("Vehicle id cannot be empty.");
		}
		return getVehicleTypeString(typeId, numberOfVehiclesInType, vehicleIndex);
	}

	private String getVehicleTypeString(String base, int numberOfVehiclesInType, int vehicleIndex) {
		// if(vehicleIndex >= numberOfVehiclesInType){
		// throw new RuntimeException("Illegal vehicle index. Cannot have more vehicles than the available number.");
		// }

		if (vehicleIndex < 0) {
			throw new RuntimeException("Illegal vehicle index. Cannot be negative.");
		}

		if (numberOfVehiclesInType < 2) {
			return base;
		}

		return base + "-" + (vehicleIndex + 1);
	}

	@Override
	public String getSpreadsheetAdapterId() {
		return ScriptConstants.EXTERNAL_DS_NAME;
	}

	@Override
	public boolean isEmptyString(String s) {
		return Strings.isEmpty(s);
	}

	@Override
	public String standardise(String s) {
		return Strings.std(s);
	}

	@Override
	public boolean equalStandardised(String a, String b) {
		return Strings.equalsStd(a, b);
	}

	@Override
	public <T> Map<String, T> createStandardisedMap() {
		return new StandardisedStringTreeMap<>(true);
	}

	@Override
	public Set<String> createStandardisedSet() {
		return new StandardisedStringSet(true);
	}

	@Override
	public int compareStandardised(String a, String b) {
		return Strings.compareStd(a, b, true);
	}

	@Override
	public Integer getVehicleIndex(String vehicleId, String vehicleTypeId) {
		vehicleId = standardise(vehicleId);
		vehicleTypeId = standardise(vehicleTypeId);

		if (vehicleId.startsWith(vehicleTypeId)) {
			String remaining = vehicleId.substring(vehicleTypeId.length(), vehicleId.length());
			if (remaining.length() == 0) {
				return 1;
			}

			remaining = remaining.trim();

			// remove the separator -
			if (remaining.length() > 0) {
				remaining = remaining.substring(1, remaining.length());
			}

			Integer value = null;
			try {
				value = Integer.parseInt(remaining);
				value--;
			} catch (Exception e) {
				// value will be null if exception occurs; this case is dealt with in the calling code
			}

			return value;
		}

		return null;
	}

	@Override
	public List<String> tokenise(String s) {
		ArrayList<String> ret = new ArrayList<String>();
		if (s != null) {
			for (StringToken token : StringTokeniser.tokenise(s)) {
				ret.add(token.getOriginal());
			}
		}
		return ret;
	}

	@Override
	public String getExceptionReport(Throwable t) {
		return Strings.getExceptionMessagesAsSingleStr(t);
	}

	@Override
	public <T> Map<String, T> createStandardisedMap(Factory<T> factory) {
		return new StandardisedStringTreeMap<>(true, factory);
	}

	@Override
	public boolean isEmptyStandardised(String s) {
		return Strings.isEmptyWhenStandardised(s);
	}

	/**
	 * Regexp taken from http://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
	 * 
	 * @param s
	 * @return
	 */
	@Override
	public List<String> splitCommas(String s) {
		String[] tokens = s.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
		return Arrays.asList(tokens);
	}

}
