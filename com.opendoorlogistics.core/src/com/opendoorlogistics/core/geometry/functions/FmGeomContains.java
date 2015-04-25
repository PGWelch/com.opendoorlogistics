package com.opendoorlogistics.core.geometry.functions;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.operations.GeomContains;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Function to test if a geometry contains a lat long
 * @author Phil
 *
 */
public class FmGeomContains extends FunctionImpl{
	public FmGeomContains(Function geometry, Function latitude, Function longitude, Function epsg){
		super(geometry,latitude,longitude,epsg);
	}
	
	public FmGeomContains(Function geometry, Function latitude, Function longitude){
		super(geometry,latitude,longitude);
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		FmGeomContainsParameters pms = readParameters(parameters);
		if(pms==null){
			return Functions.EXECUTION_ERROR;
		}
		
		switch(execute(pms)){
		case ERROR:
			return Functions.EXECUTION_ERROR;
			
		case TRUE:
			return 1L;
			
		default:
		case FALSE:
			return 0L;
			
		}
	}

	public enum ContainsResult{
		TRUE,
		FALSE,
		ERROR
	}
	
	public ContainsResult execute(FmGeomContainsParameters pms) {
		boolean contains = false;
		
		if(pms.geometry!=null){
			Geometry g = ((ODLGeomImpl)pms.geometry).getJTSGeometry();
			if(g==null){
				return ContainsResult.ERROR;							
			}
			
			contains = GeomContains.containsPoint(g, pms.latitude, pms.longitude, pms.epsg);
		}
		return contains? ContainsResult.TRUE : ContainsResult.FALSE;
	}
	
	public FmGeomContainsParameters readParameters(FunctionParameters parameters){
		FmGeomContainsParameters pms = new FmGeomContainsParameters();
		
		Object [] childExe = executeChildFormulae(parameters, false);
		if(childExe==null){
			return null;
		}
		
		pms.geometry = (ODLGeom)ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, childExe[0]);
		if(childExe[0]!=null && pms.geometry==null){
			return null;
		}
		
		pms.latitude = (Double)ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, childExe[1]);
		pms.longitude =(Double) ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, childExe[2]);
		if(pms.latitude==null || pms.longitude == null){
			return null;			
		}
		
		// null epsg is valid, it just means calculate in lat long space
		if(isProjected()){
			pms.epsg =(String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, childExe[3]);
		}
		
		return pms;
	}

	public Function geometry(){
		return child(0);
	}
	
	public Function latitude(){
		return child(1);
	}
	
	public Function longitude(){
		return child(2);
	}
	
	public boolean isProjected(){
		return nbChildren()>3;
	}
	
	public static class FmGeomContainsParameters{
		public ODLGeom geometry;
		public Double latitude;
		public Double longitude;
		public String epsg;
	}
	
	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}
}
