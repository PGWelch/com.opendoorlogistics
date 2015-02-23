package com.opendoorlogistics.core.scripts.formulae.image;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionType;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.annotations.ImageFormulaKey;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.scripts.TableReference;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilderUtils;
import com.opendoorlogistics.core.scripts.execution.adapters.IndexedDatastores;
import com.opendoorlogistics.core.scripts.formulae.image.FmImageWithView.IWVMode;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ImageFormulaeCreator {

	private static class ArgIndices {
		int lookupVal = -1;
		int tableToDrawRef = -1;
		int tableForZoomDrawRef = -1;
		int mode = -1;
		int height = -1;
		int width = -1;
		int dpCM = -1;
		int renderProp = -1;
	}

	public static void buildImageFormulae(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTable> datastores, final ExecutionReport result) {

		for (final boolean withView : new boolean[] { false, true }) {
			for (final boolean printable : new boolean[] { false, true }) {
				for (final boolean includeProperties : new boolean[] { false, true }) {

					// construct the definition
					final ArgIndices argIndices = new ArgIndices();
					FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, getFormulaName(withView, printable));

					if (withView) {
						dfn.setDescription("Draw an image using all objects in the to-draw-table, taking the zoom and view centre from the objects in the to-zoom-table whose image formula key matches to-zoom-lookup-value. "
								+ "Both tables must contain the standard drawable fields.");

						argIndices.tableToDrawRef = dfn.addArg("to-draw-table-reference", ArgumentType.TABLE_REFERENCE_CONSTANT,
								"Reference to the drawable table including the datastore name - e.g. \"external, drawabletable\".");

						argIndices.lookupVal = dfn.addArg("to-zoom-lookup-value", "Filter objects in the to-zoom table whose image formula key field has this value.");

						argIndices.tableForZoomDrawRef = dfn.addArg("to-zoom-table-table-reference", ArgumentType.TABLE_REFERENCE_CONSTANT,
								"Reference to the to-zoom-table including the datastore name - e.g. \"external, drawabletable\".");
						
					} else {
						dfn.setDescription("Draw an image using the referenced drawable-table, filtering the objects based on the row in current table.");
						argIndices.lookupVal = dfn.addArg("lookup_value", "Filter objects in the drawable table whose image formula key field has this value.");
						argIndices.tableToDrawRef = dfn.addArg("drawable-table-reference", ArgumentType.TABLE_REFERENCE_CONSTANT,
								"Reference to the drawable table including the datastore name - e.g. \"external, drawabletable\".");						
					}

					addStandardArguments(withView,printable, includeProperties, argIndices, dfn);

					dfn.setFactory(new FunctionFactory() {

						@Override
						public Function createFunction(Function... children) {
							// parse the table reference for the drawables table
							ODLTableReadOnly pointsTable = getDrawablesTable(children[argIndices.tableToDrawRef], datastores, result);

							// get the to-zoom table if we have one
							ODLTableReadOnly zoomTable= null;
							if(argIndices.tableForZoomDrawRef!=-1){
								zoomTable = getDrawablesTable(children[argIndices.tableForZoomDrawRef], datastores, result);								
							}
							
							// get properties
							RenderProperties properties = getRenderProperties(argIndices, children);

							if (withView) {			
								return createFmImageWithViewFormula(argIndices, pointsTable,zoomTable, properties, children);
							} else {
								return createFmImageFormula(argIndices, pointsTable, properties, children);
							}
						}
					});
					library.add(dfn);

				}
			}
		}
	}

	private static String getFormulaName(boolean withView, boolean printable) {

		return (printable ? "printableImage" : "image") + (withView ? "WithView" : "");
	}

	private static void addStandardArguments(final boolean withView,final boolean isPrintable, final boolean includeProperties, final ArgIndices argIndices, FunctionDefinition dfn) {
		StringBuilder builder = new StringBuilder();
		
		if(withView){
			for (FmImageWithView.IWVMode mode : FmImageWithView.IWVMode.values()) {
				builder.append(" " + mode.getKeyword() + " = " + mode.getDescription());
			}
		}else{
			for (FmImage.Mode mode : FmImage.Mode.values()) {
				builder.append(" " + mode.getKeyword() + " = " + mode.getDescription());
			}			
		}
		argIndices.mode = dfn.addArg("Mode", ArgumentType.STRING_CONSTANT, "Create image mode." + builder.toString());

		if (isPrintable) {
			argIndices.width = dfn.addArg("width", "Image width in centimeters.");
			argIndices.height = dfn.addArg("height", "Image height in centimeters.");
			argIndices.dpCM = dfn.addArg("dots_per_cm", "Dots per centimeter");
		} else {
			argIndices.width = dfn.addArg("width", "Image width in pixels.");
			argIndices.height = dfn.addArg("height", "Image height in pixels.");
		}

		if (includeProperties) {
			argIndices.renderProp = dfn.addArg("render-properties", ArgumentType.STRING_CONSTANT, "A string containing key value pairs, for example \"legend=topleft\".");
		}
	}

	private static ODLTableReadOnly getDrawablesTable(Function oTableRef, final IndexedDatastores<? extends ODLTable> datastores, final ExecutionReport result) {
		String sTableRef = FunctionUtils.getConstantString(oTableRef);
		
		TableReference tableRef = TableReference.create(sTableRef, result);
		if (tableRef == null) {
			throw new RuntimeException("Error reading table reference in an image formula.");
		}

		// find the drawable table ...
		int dsIndx = datastores.getIndex(tableRef.getDatastoreName());
		if (dsIndx == -1) {
			throw new RuntimeException("Error getting datastore " + tableRef.getDatastoreName() + " used in an image formula.");
		}
		ODLDatastore<? extends ODLTable> ds = datastores.getDatastore(dsIndx);
		if (ds == null || result.isFailed()) {
			throw new RuntimeException("Error getting datastore " + tableRef + " used  in an image formula.");
		}

		// do simple adaption of table to drawable datastore definition to ensure table format is exact
		ODLDatastore<? extends ODLTableDefinition> definition = DrawableObjectImpl.getBeanMapping().getDefinition();
		AdapterConfig adapterConfig = AdapterConfig.createSameNameMapper(definition);
		adapterConfig.getTables().get(0).setFromTable(tableRef.getTableName());
		ODLDatastore<ODLTable> simpleAdapted = AdapterBuilderUtils.createSimpleAdapter(ds, adapterConfig, result);
		if (simpleAdapted == null || result.isFailed()) {
			throw new RuntimeException("Error matching table " + tableRef + " used in formula image to the table expected by the map renderer.");
		}
		ODLTableReadOnly pointsTable = simpleAdapted.getTableAt(0);
		return pointsTable;
	}

	private static RenderProperties getRenderProperties(final ArgIndices argIndices, Function... children) {
		// get flags
		RenderProperties properties = new RenderProperties();
		if (argIndices.renderProp != -1) {
			String s = FunctionUtils.getConstantString(children[argIndices.renderProp]);
			properties = new RenderProperties(s);
		}
		// add default flags
		properties.addFlags(RenderProperties.SHOW_ALL);
		return properties;
	}

	private static Function createFmImageWithViewFormula(final ArgIndices argIndices, ODLTableReadOnly pointsTable,ODLTableReadOnly zoomTable, RenderProperties properties,
			Function... children) {
		
		// find the group key field
		int groupKeyColumnIndex = getImageFilterColumnIndex();

		// get the mode
		FmImageWithView.IWVMode mode = null;
		String sMode = FunctionUtils.getConstantString(children[argIndices.mode]);
		for (FmImageWithView.IWVMode m : FmImageWithView.IWVMode.values()) {
			if (Strings.equalsStd(m.getKeyword(), sMode)) {
				mode = m;
			}
		}
		
		if (mode == null) {
			throw new RuntimeException("Unknown image function mode: " + sMode);
		}


		return new 	FmImageWithView(pointsTable,zoomTable, groupKeyColumnIndex,children[argIndices.lookupVal], mode, children[argIndices.width],
				children[argIndices.height], argIndices.dpCM!=-1? children[argIndices.dpCM]:null,properties);
	}
	
	private static Function createFmImageFormula(final ArgIndices argIndices, ODLTableReadOnly pointsTable, RenderProperties properties,
			Function... children) {
		
		// find the group key field
		int groupKeyColumnIndex = getImageFilterColumnIndex();
		
		// get the mode
		FmImage.Mode mode = null;
		String sMode = FunctionUtils.getConstantString(children[argIndices.mode]);
		for (FmImage.Mode m : FmImage.Mode.values()) {
			if (Strings.equalsStd(m.getKeyword(), sMode)) {
				mode = m;
			}
		}
		if (mode == null) {
			throw new RuntimeException("Unknown image function mode: " + sMode);
		}

		if (argIndices.dpCM != -1) {
			return FmImage.createFixedPhysicalSize(children[argIndices.lookupVal], pointsTable, groupKeyColumnIndex, mode, children[argIndices.width], children[argIndices.height],
					children[argIndices.dpCM], properties);
		} else {
			return FmImage.createFixedPixelSize(children[argIndices.lookupVal], pointsTable, groupKeyColumnIndex, mode, children[argIndices.width], children[argIndices.height],
					properties);
		}
	}

	private static int getImageFilterColumnIndex() {
		BeanDatastoreMapping beanMap = DrawableObjectImpl.getBeanMapping();
		int groupKeyColumnIndex = beanMap.getTableMapping(0).indexOfAnnotation(ImageFormulaKey.class);
		if (groupKeyColumnIndex == -1) {
			throw new RuntimeException("Could not find group key field in drawable lat long table, used in image formula.");
		}
		return groupKeyColumnIndex;
	}
}
