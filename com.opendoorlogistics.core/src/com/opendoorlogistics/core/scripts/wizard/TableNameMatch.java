/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.wizard;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.utils.strings.Strings;

public class TableNameMatch<T extends ODLTableDefinition> {
	//private final List<Pair<T, T>> matched = new ArrayList<>();
	private final TIntObjectHashMap<T> matchedByA = new TIntObjectHashMap<>();
	private final TIntObjectHashMap<T> matchedByB = new TIntObjectHashMap<>();
	private final List<T> unmatchedInA = new ArrayList<>();
	private final List<T> unmatchedInB = new ArrayList<>();

	public TableNameMatch(ODLDatastore<? extends T> a, ODLDatastore<? extends T> b, boolean allowSingleCombinationMatch) {
		if (allowSingleCombinationMatch && a.getTableCount() == 1 && b.getTableCount() == 1) {
			matchedByA.put(a.getTableAt(0).getImmutableId(), b.getTableAt(0));
			matchedByB.put(b.getTableAt(0).getImmutableId(), a.getTableAt(0));
			
		} else {

			int na = a.getTableCount();
			for (int i = 0; i < na; i++) {
				unmatchedInA.add(a.getTableAt(i));
			}

			int nb = b.getTableCount();
			for (int i = 0; i < nb; i++) {
				unmatchedInB.add(b.getTableAt(i));
			}

			Iterator<T> itA = unmatchedInA.iterator();
			while (itA.hasNext()) {
				T ta = itA.next();
				Iterator<T> itB = unmatchedInB.iterator();
				while (itB.hasNext()) {
					T tb = itB.next();
					if (Strings.equalsStd(ta.getName(), tb.getName())) {
						//matched.add(new Pair<T, T>(ta, tb));
						matchedByA.put(ta.getImmutableId(), tb);
						matchedByB.put(tb.getImmutableId(), ta);
						itA.remove();
						itB.remove();
						break;
					}
				}

			}
		}
	}

//	public List<Pair<T, T>> getMatched() {
//		return matched;
//	}

	public List<T> getUnmatchedInA() {
		return unmatchedInA;
	}

	public List<T> getUnmatchedInB() {
		return unmatchedInB;
	}

	public T getMatchForTableA(ODLTableDefinition tableA) {
		return matchedByA.get(tableA.getImmutableId());
	}

	public T getMatchForTableB(ODLTableDefinition tableB) {
		return matchedByB.get(tableB.getImmutableId());
	}
}

