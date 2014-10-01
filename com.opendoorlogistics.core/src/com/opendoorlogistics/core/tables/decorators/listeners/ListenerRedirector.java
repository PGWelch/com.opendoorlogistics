/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.listeners;

import java.util.HashMap;

import com.opendoorlogistics.api.tables.ODLHasListeners;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLListener.ODLListenerType;

final public class ListenerRedirector implements ODLHasListeners {
	private final ODLHasListeners eventSource;
	private final HashMap<ODLListener, RedirectableListener> listeners = new HashMap<>();
	private final boolean useRowRange;

	// private boolean isEnabled=true;

	public ListenerRedirector(ODLHasListeners eventSource, boolean useRowRange) {
		this.eventSource = eventSource;
		this.useRowRange = useRowRange;
	}

	protected class RedirectableListener implements ODLListener {
		protected final ODLListener destination;
		protected final boolean useRowRange;

		public RedirectableListener(ODLListener destination, boolean useRowRange) {
			this.destination = destination;
			this.useRowRange = useRowRange;
		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			// if(isEnabled){
			if (useRowRange) {
				destination.tableChanged(tableId, firstRow, lastRow);
			} else {
				destination.tableChanged(tableId, 0, Integer.MAX_VALUE);
			}
			// }
		}

		@Override
		public void datastoreStructureChanged() {
			// if(isEnabled){
			destination.datastoreStructureChanged();
			// }
		}

		@Override
		public ODLListenerType getType() {
			return destination.getType();
		}

	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {
		RedirectableListener redirectable = new RedirectableListener(tml, useRowRange);
		listeners.put(tml, redirectable);
		eventSource.addListener(redirectable, tableIds);
	}

	@Override
	public void removeListener(ODLListener tml) {
		RedirectableListener redirectable = listeners.remove(tml);
		eventSource.removeListener(redirectable);
	}

	@Override
	public void disableListeners() {
		eventSource.disableListeners();
	}

	@Override
	public void enableListeners() {
		eventSource.enableListeners();
	}

	public void fireTableSetChanged() {
		for (ODLListener listener : listeners.keySet()) {
			if (listener.getType() == ODLListenerType.DATASTORE_STRUCTURE_CHANGED) {
				listener.datastoreStructureChanged();
			}
		}
	}

	public void fireTableChanged(int tableId, int firstRow, int lastRow) {
		for (ODLListener listener : listeners.keySet()) {
			if (listener.getType() == ODLListenerType.TABLE_CHANGED) {
				listener.tableChanged(tableId, firstRow, lastRow);
			}
		}
	}
}
