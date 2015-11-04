package com.opendoorlogistics.components.geocode.postcodes;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.cache.ObjectCache;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCConstants;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCGeocodeFile;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCRecord.StrField;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.operations.FastContainedPointsQuadtree;
import com.opendoorlogistics.core.geometry.operations.FastContainedPointsQuadtree.Builder.InsertedListener;
import com.opendoorlogistics.core.utils.LargeList;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.Icons;
import com.sun.management.GcInfo;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.hash.TLongHashSet;

public class PCSpatialQueryComponent implements ODLComponent{
	public static class BaseIOClass implements BeanMappedRow{
		private long globalRowId;
		private String polygonId;
		
		@Override
		public long getGlobalRowId() {
			return globalRowId;
		}

		@Override
		@ODLIgnore
		public void setGlobalRowId(long globalRowId) {
			this.globalRowId = globalRowId;
		}

		public String getPolygonId() {
			return polygonId;
		}

		@ODLNullAllowed
		public void setPolygonId(String polygonId) {
			this.polygonId = polygonId;
		}		
	}
	
	@ODLTableName("Polygons")
	public static class Input extends BaseIOClass{
		private ODLGeom geom;

		public ODLGeom getGeom() {
			return geom;
		}

		@ODLNullAllowed
		public void setGeom(ODLGeom geom) {
			this.geom = geom;
		}
		
	}
	
	@ODLTableName("Postcodes")
	public static class Output extends BaseIOClass implements Comparable<Output>{
		private String postcode;

		public String getPostcode() {
			return postcode;
		}

		public void setPostcode(String postcode) {
			this.postcode = postcode;
		}

		@Override
		public int compareTo(Output o) {
			int diff = Strings.compareStd(getPolygonId(), o.getPolygonId(), false);
			if(diff==0){
				diff = Strings.compareStd(getPostcode(), o.getPostcode(), false);
			}
			return diff;
		}

	}
	

	@Override
	public String getId() {
		return "com.opendoorlogistics.components.geocode.postcodes.PCSpatialQueryComponent";
	}

	@Override
	public String getName() {
		return "Query for postcodes within polygons";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		return getClsDefn(api, Input.class);
	}

	private ODLDatastore<? extends ODLTableDefinition> getClsDefn(ODLApi api,Class<? extends BeanMappedRow> cls){
		Tables tables = api.tables();
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = tables.createAlterableDs();
		tables.copyTableDefinition(tables.mapBeanToTable(cls).getTableDefinition(), ret);
		return ret;	
	}
	
	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		return getClsDefn(api, Output.class);
	}
	
	private static class CachedTree{
		FastContainedPointsQuadtree tree ;	
		LargeList<String> list;
		
		long sizeInBytes(){
			long ret=tree.getEstimatedSizeInBytes();
			for(String s:list){
				if(s!=null){
					ret +=s.length()*2;
				}
			}
			return ret;
		}
	}
	
	private static CachedTree createQuadtree(ComponentExecutionApi api, File file, int level){
		// do import and add to builder
		api.postStatusMessage("Loading postcodes from file " + file.getName());
		
		PCGeocodeFile pc =null;
		try {
			// load all the postcodes
			pc = new PCGeocodeFile(file);		
			FastContainedPointsQuadtree.Builder builder = new FastContainedPointsQuadtree.Builder();
			CachedTree ret = new CachedTree();
			ret.list= new LargeList<>();
			long count=0;
			List<PCRecord> postcodes = pc.getPostcodes(level, api);
			if(api.isCancelled()){
				return null;
			}
			
			// add them all to the builder
			for (PCRecord record : postcodes) {
				long id = ret.list.longSize();
				builder.add(new Coordinate(record.getLongitude().doubleValue(), record.getLatitude().doubleValue()), id);
				ret.list.add(record.getField(StrField.POSTAL_CODE));
				
				count++;
				if(count%25000==0){
					api.postStatusMessage("Building postcode lookup tree step 1: processed " + count + " postcodes.");
				}
			}
			
			pc.close();
			pc = null;
			
			if(api.isCancelled()){
				return null;
			}
			
			// create tree
			api.postStatusMessage("Building postcode lookup tree");
			GeometryFactory geometryFactory = new GeometryFactory();
			ret.tree = builder.build(geometryFactory, new InsertedListener() {
				
				@Override
				public void inserted(Coordinate c, long count) {
					if(count%25000==0){
						api.postStatusMessage("Building postcode lookup tree step 2: processed " + count + " postcodes.");
					}
				}
			});
			return ret;
		} catch (Exception e) {
			throw e;
		}finally {
			if(pc!=null){
				pc.close();
			}
		}

	}

	private synchronized ObjectCache initCache(ODLApi api){
		String id = getId() + " - treecache";
		ObjectCache ret = api.cache().get(id);
		if(ret==null){
			ret = api.cache().create(id, 1024 * 1024 * 256);
		}
		return ret;
	}
	
	/**
	 * Query for postcodes contained within the input polygons. The query doesn't do any projection, so it won't work
	 * for any input polygons which cross the 180th meridian, so it won't work for a small part of North Eastern Russia 
	 * (see https://en.wikipedia.org/wiki/180th_meridian) or a polygon spanning the poles (e.g. Antartica).
	 */
	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {

		// Get the filename
		PCImporterConfig config = (PCImporterConfig)configuration;
		File file = PCConstants.resolvePostcodeFile(api.getApi(), new File(config.getGeocoderDbFilename()));
		
		// Get a cache key including characters which should be illegal under any file system (windows, unix)
		String cacheKey = file.getAbsolutePath()  + " \\/?/\\ " + config.getLevel();

		// see if we have a quadtree cached for these postcodes...
		ObjectCache cache = initCache(api.getApi());
		CachedTree tree = (CachedTree)cache.get(cacheKey);
		
		// load tree if needed, an exception will be thrown if loaded failed...
		if(tree==null){
			tree = createQuadtree(api, file, config.getLevel());
			if(api.isCancelled()){
				return;
			}
			cache.put(cacheKey, tree, tree.sizeInBytes());
			Runtime.getRuntime().gc();
		}

		Tables tables = api.getApi().tables();
		BeanTableMapping inputMapping = tables.mapBeanToTable(Input.class);
		List<Input> polygons = inputMapping.readObjectsFromTable(ioDs.getTableAt(0));

		BeanTableMapping outputMapping = tables.mapBeanToTable(Output.class);
		TLongHashSet ids = new TLongHashSet();
		int polyCount=0;
		for(Input polygon: polygons){
			if(api.isCancelled() || api.isFinishNow()){
				return;
			}
			
			api.postStatusMessage("Querying for polygon " + (polyCount+1));
			
			if(polygon.getGeom()!=null){
				Geometry geometry = ((ODLGeomImpl)polygon.getGeom()).getJTSGeometry();
				if(geometry!=null){
					ids.clear();
					tree.tree.query(geometry, ids);
					CachedTree finalTree = tree;
					
					// get list of output objects
					LargeList<Output> outputList = new LargeList<>();
					ids.forEach(new TLongProcedure() {
						
						@Override
						public boolean execute(long value) {
								
							Output output = new Output();
							output.setPolygonId(polygon.getPolygonId());
							output.setPostcode(finalTree.list.get(value));
							outputList.add(output);
							return true;
						}
					});
					
					// sort list
					Collections.sort(outputList);
					
					// write to table
					for(Output output : outputList){
						if(api.isCancelled() || api.isFinishNow()){
							return;
						}	
						outputMapping.writeObjectToTable(output, outputDs.getTableAt(0));
					}
				}
			}
			
			polyCount++;
		}
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return PCImporterConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		return PCImporterConfig.createConfigEditorPanel(api,(PCImporterConfig) config, "Query");
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING |ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("postcode-spatial-query.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate(getName(), getName(), "Query for postcodes contained within a set of input polygons.",
				getIODsDefinition(templatesApi.getApi(), new PCImporterConfig()),				
				new PCImporterConfig());
	}

}
