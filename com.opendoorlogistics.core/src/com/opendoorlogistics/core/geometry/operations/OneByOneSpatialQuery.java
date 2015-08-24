package com.opendoorlogistics.core.geometry.operations;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableQuery.SpatialTableQuery;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Execute a spatial query on table by examining each row one-by-one.
 * @author Phil
 *
 */
public class OneByOneSpatialQuery {
	private final ODLApi api;
	
	public OneByOneSpatialQuery(ODLApi api) {
		this.api = api;
	}

	/**
	 * Execute a spatial query on table by examining each row one-by-one.
	 * Only a bounding-box query is performed by for geometry, which does 
	 * not guarantee the geometry is within the query box (but is much quicker than
	 * a complete geometry intersection query).
	 * @param table
	 * @param query
	 * @return
	 */
	public ODLTableReadOnly query(ODLTableReadOnly table, SpatialTableQuery query){
		Tables tables = api.tables();
		ODLTable ret = (ODLTable)tables.copyTableDefinition(table, tables.createAlterableDs());
		
		Envelope queryEnvelope = createQueryEnvelope(query);

		int nr = table.getRowCount();
		for(int i =0 ; i < nr ; i++){
			// has geom?
			ODLGeomImpl geom =null;
			boolean inBox=false;
			if(query.getGeomColumn()!=-1){
				geom =(ODLGeomImpl)ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, table.getValueAt(i, query.getGeomColumn()));
				if(geom!=null){
					Envelope envelope = geom.getWGSBounds();
					inBox = envelope!=null && queryEnvelope.intersects(envelope);
				}
			}
			
			// Test latitude and longitude if we didn't have a geometry
			if(!inBox && geom==null && query.getLatitudeColumn()!=-1 && query.getLongitudeColumn()!=-1){
				Long lng =(Long)ColumnValueProcessor.convertToMe(ODLColumnType.LONG, table.getValueAt(i, query.getLongitudeColumn()));
				Long lat =(Long)ColumnValueProcessor.convertToMe(ODLColumnType.LONG, table.getValueAt(i, query.getLatitudeColumn()));
				if(lng!=null && lat!=null){
					if(query.getMinimum()==null || (lng >= query.getMinimum().getLongitude() && lat>=query.getMinimum().getLatitude())){
						
						if(query.getMaximum()==null || (lng < query.getMaximum().getLongitude() && lat<query.getMaximum().getLatitude())){
							inBox = true;
						}	
					}
				}
			}
			
			if(inBox){
				tables.copyRow(table, i, ret);
			}
		}
		return ret;
	}

	private Envelope createQueryEnvelope(SpatialTableQuery query) {
		double x1,x2,y1,y2;
		if(query.getMinimum()!=null){
			x1 = query.getMinimum().getLongitude();
			y1 = query.getMinimum().getLatitude();
		}else{
			x1 = -Double.MAX_VALUE;
			y1 = -Double.MAX_VALUE;
		}
		
		if(query.getMaximum()!=null){
			x2 = query.getMaximum().getLongitude();
			y2 = query.getMaximum().getLatitude();
		}else{
			x2 = +Double.MAX_VALUE;
			y2 = +Double.MAX_VALUE;
		}
		Envelope queryEnvelope = new Envelope(x1, x2, y1, y2);
		return queryEnvelope;
	}
}
