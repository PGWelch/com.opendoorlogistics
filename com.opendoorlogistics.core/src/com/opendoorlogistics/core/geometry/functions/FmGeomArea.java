package com.opendoorlogistics.core.geometry.functions;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.operations.GridTransforms;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.vividsolutions.jts.geom.Geometry;

public class FmGeomArea extends FunctionImpl {


	public FmGeomArea(Function geom, Function EPSGCodeEqualArea) {
		super(geom,EPSGCodeEqualArea);
	}
	@Override
	public Object execute(FunctionParameters parameters) {
		
		Object [] childExe = executeChildFormulae(parameters, true);
		if(childExe==null){
			return null;
		}
		
		ODLGeom geometry = (ODLGeom)ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, childExe[0]);
		String  epsg =(String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, childExe[1]);
		
		Geometry jtsGeom= ((ODLGeomImpl)geometry).getJTSGeometry();
		
		GridTransforms transforms = GridTransforms.getAndCache(epsg);
		Geometry projected = transforms.wgs84ToGrid(jtsGeom);
		
		// See http://osgeo-org.1560.x6.nabble.com/how-to-calculate-area-of-a-polygon-by-coordinates-td4318327.html
		// We ignore polygon densification at the moment (is it needed before projecting).
		double area = projected.getArea();
		return area;
	}

	@Override
	public Function deepCopy() {
		return new FmGeomArea(child(0).deepCopy(),child(1).deepCopy());
	}

}
