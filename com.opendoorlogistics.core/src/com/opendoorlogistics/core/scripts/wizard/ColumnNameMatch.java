/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.wizard;

import gnu.trove.impl.Constants;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ColumnNameMatch {
	private final List<Pair<Integer, Integer>> matched = new ArrayList<>();
	private final TIntIntHashMap matchedByA = new TIntIntHashMap(10, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
	private final TIntIntHashMap matchedByB = new TIntIntHashMap(10, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
	private final TIntArrayList unmatchedInA = new TIntArrayList();
	private final TIntArrayList unmatchedInB = new TIntArrayList();

	public ColumnNameMatch(ODLTableDefinition ta, ODLTableDefinition tb) {
		int na = ta.getColumnCount();
		for (int i = 0; i < na; i++) {
			unmatchedInA.add(i);
		}

		int nb = tb.getColumnCount();
		for (int i = 0; i < nb; i++) {
			unmatchedInB.add(i);
		}

		int i = 0;
		while (i < unmatchedInA.size()) {

			// search in b for a
			int a = unmatchedInA.get(i);
			boolean isMatched = false;
			nb = unmatchedInB.size();
			for (int j = 0; j < nb; j++) {
				int b = unmatchedInB.get(j);
				if (Strings.equalsStd(ta.getColumnName(a), tb.getColumnName(b))) {
					matched.add(new Pair<Integer, Integer>(a, b));
					matchedByA.put(a, b);
					matchedByB.put(b, a);
					unmatchedInA.removeAt(i);
					unmatchedInB.removeAt(j);
					isMatched = true;
					break;
				}
			}

			if (!isMatched) {
				i++;
			}
		}

	}

	public List<Pair<Integer, Integer>> getMatched() {
		return matched;
	}

	public TIntArrayList getUnmatchedInA() {
		return unmatchedInA;
	}

	public TIntArrayList getUnmatchedInB() {
		return unmatchedInB;
	}

	public int getMatchForA(int columnIndexInA) {
		return matchedByA.get(columnIndexInA);
	}

	public int getMatchForTableB(int columnIndexInB) {
		return matchedByB.get(columnIndexInB);
	}
	
	public void setMatchForTableB(int columnIndexInB, int matchingColumnIndexInA){
		matchedByB.put(columnIndexInB, matchingColumnIndexInA);
		matchedByA.put(matchingColumnIndexInA, columnIndexInB);
	}
	
}
