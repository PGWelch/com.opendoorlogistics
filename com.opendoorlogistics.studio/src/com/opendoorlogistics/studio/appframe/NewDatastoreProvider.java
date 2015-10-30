package com.opendoorlogistics.studio.appframe;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.tables.io.TableIOUtils;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;

/**
 * An interface used by the client app to present the user with new datastores they can create
 * @author Phil
 *
 */
public interface NewDatastoreProvider {
	String name();
	ODLDatastoreAlterable<? extends ODLTableAlterable> create(ODLApi api);
	
	public static List<NewDatastoreProvider> createDefaults(){
		ArrayList<NewDatastoreProvider> ret = new ArrayList<NewDatastoreProvider>();
		
		ret.add(new NewDatastoreProvider() {
			
			@Override
			public String name() {
				return "Create empty datastore";
			}
			
			@Override
			public ODLDatastoreAlterable<? extends ODLTableAlterable> create(ODLApi api) {
				return ODLDatastoreImpl.alterableFactory.create();
			}
		});
		
		for (final String exampleDs : new String[] { "Customers"
		// , "Sales territories" // disable sales territories for the moment as it takes 30 seconds to load!
		}) {
			ret.add(new NewDatastoreProvider() {
				
				@Override
				public String name() {
					return "Create example " + exampleDs + " datastore";
				}
				
				@Override
				public ODLDatastoreAlterable<? extends ODLTableAlterable> create(ODLApi api) {
					return TableIOUtils.importExampleDatastore(exampleDs + ".xlsx", null);
				}
			});
		}
		
		return ret;
	}
}
