package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.scripts.TargetIODsInterpreter;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutionBlackboardImpl;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilderUtils;
import com.opendoorlogistics.core.scripts.execution.adapters.BuiltAdapters;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.Style.OutputFormula;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.scripts.formulae.TableParameters;
import com.opendoorlogistics.core.scripts.formulae.rules.RuleNode;
import com.opendoorlogistics.core.scripts.wizard.ColumnNameMatch;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.UnionDecorator;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Numbers;

public class VLSBuilder {

	private static final BeanDatastoreMapping INPUT_VLS_ONLY;
	private static final int INPUT_VIEW_INDX;
	private static final int INPUT_LAYER_INDX;
	private static final int INPUT_STYLE_INDX;
	private static final BeanTableMapping SOURCE_TABLE;
	private static final Function[] APPEARANCE_KEY_ACCESSORS;

	private final ODLApi api;

	public VLSBuilder(ODLApi api) {
		this.api = api;
	}

	static {
		INPUT_VLS_ONLY = BeanMapping.buildDatastore(View.class, Layer.class, Style.class);
		INPUT_VIEW_INDX = 0;
		INPUT_LAYER_INDX = 1;
		INPUT_STYLE_INDX = 2;

		SOURCE_TABLE = BeanMapping.buildTable(VLSSourceDrawables.class);

		APPEARANCE_KEY_ACCESSORS = new Function[4];
		for (int i = 0; i < Style.NB_RULE_KEYS; i++) {
			APPEARANCE_KEY_ACCESSORS[i] = new FmLocalElement(VLSSourceDrawables.COL_VLSKEY1 + i, "VLSKey" + (i + 1));
		}

	}

	public static class VLSConfig {
		private List<String> datasourceTableReferences = new ArrayList<String>();

		public List<String> getDatasourceTableReferences() {
			return datasourceTableReferences;
		}

		public void setDatasourceTableReferences(List<String> datasourceTableReferences) {
			this.datasourceTableReferences = datasourceTableReferences;
		}

	}

	/*
	 * Get view-layer-style tables
	 */
	public static ODLDatastore<? extends ODLTableDefinition> getVLSTableDefinitions() {
		return INPUT_VLS_ONLY.getDefinition();
	}

	public static ODLTableDefinition getSourceTableDefinition() {
		return SOURCE_TABLE.getTableDefinition();
	}

	public static interface VLSDependencyInjector {
		int getTableCount();

		String getTableName(int i);

		ODLTable buildTable(int i);

		ODLTable buildTableFormula(String s);

		Function buildFormula(String formula, ODLTableDefinition table);
	}

	private static class MatchedLayer {
		Layer layer;
		ArrayList<MatchedStyle> styles = new ArrayList<MatchedStyle>();
		SourceTable source;
		LayerType layerType;
		RuleNode styleLookupTree;
	}

	private static class MatchedStyle {

		MatchedStyle(Style style) {
			super();
			this.style = style;
		}

		final Style style;
		Function functions[] = new Function[Style.OutputFormula.values().length];
	}

	private enum LayerType {
		BACKGROUND_IMAGE(PredefinedTags.BACKGROUND_IMAGE), BACKGROUND(PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND), ACTIVE(PredefinedTags.DRAWABLES), FOREGROUND(PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND);

		final String tablename;

		private LayerType(String tablename) {
			this.tablename = tablename;
		}

	}

	/**
	 * Finds the input table and validates it to the table definition, returning a table mapped to have the correct fields in the correct order
	 * 
	 * @author Phil
	 *
	 */
	private static class TableFinder {
		final ODLApi api;
		final VLSDependencyInjector injector;
		final ExecutionReport report;
		final Map<String, Integer> tables;

		private TableFinder(ODLApi api, VLSDependencyInjector injector, ExecutionReport report) {
			this.api = api;
			this.injector = injector;
			this.report = report;

			// make a lookup of table names
			tables = api.stringConventions().createStandardisedMap();
			for (int i = 0; i < injector.getTableCount(); i++) {
				tables.put(injector.getTableName(i), i);
			}
		}

		ODLTable fetchRawSourceTable(String name) {
			Integer indx = tables.get(name);
			ODLTable ret = null;
			if (indx != null) {
				ret = injector.buildTable(indx);
			}

			if (ret == null) {
				report.setFailed("Cannot find table required by view-layer-style adapter: " + name);
			}

			return ret;
		}

		SourceTable fetchValidatedSourceTable(String name, ODLTableDefinition definition) {
			ODLTable raw = fetchRawSourceTable(name);
			if (report.isFailed()) {
				return null;
			}

			SourceTable ret = new SourceTable();
			ret.raw = raw;

			// Do simple name-based mapping to ensure all fields are present and in correct order, returning the mapped table
			ODLDatastoreImpl<ODLTableDefinition> tmpDfn = new ODLDatastoreImpl<ODLTableDefinition>(null);
			tmpDfn.addTable(definition);

			ODLDatastoreImpl<ODLTable> tmpData = new ODLDatastoreImpl<ODLTable>(null);
			tmpData.addTable(ret.raw);

			ODLDatastore<? extends ODLTable> mapped = new TargetIODsInterpreter(api).buildScriptExecutionAdapter(tmpData, tmpDfn, report);

			if (mapped != null && !report.isFailed()) {
				ret.validated = mapped.getTableAt(0);
				return ret;
			}

			if (report.isFailed()) {
				report.setFailed("Error reading view-layer-style table: " + name);
			}

			return null;
		}
	}

	public ODLDatastore<? extends ODLTable> build(VLSDependencyInjector injector, ExecutionReport report) {

		// Try getting built in tables
		TableFinder finder = new TableFinder(api, injector, report);
		SourceTable viewTable = finder.fetchValidatedSourceTable(View.TABLE_NAME, INPUT_VLS_ONLY.getTableMapping(INPUT_VIEW_INDX).getTableDefinition());
		SourceTable layerTable = finder.fetchValidatedSourceTable(Layer.TABLE_NAME, INPUT_VLS_ONLY.getTableMapping(INPUT_LAYER_INDX).getTableDefinition());
		SourceTable styleTable = finder.fetchValidatedSourceTable(Style.TABLE_NAME, INPUT_VLS_ONLY.getTableMapping(INPUT_STYLE_INDX).getTableDefinition());
		if (report.isFailed()) {
			return null;
		}

		View view = findView(viewTable, report);
		if (report.isFailed()) {
			return null;
		}

		// Get layers for this view including their data source tables
		ArrayList<MatchedLayer> matchedLayers = new ArrayList<VLSBuilder.MatchedLayer>();
		StringConventions strings = api.stringConventions();
		Map<String, MatchedLayer> matchedLayersMap = strings.createStandardisedMap();
		readLayers(finder, layerTable, view, matchedLayers, matchedLayersMap, report);
		if (report.isFailed()) {
			return null;
		}

		// Add styles objects to layer objects
		addStylesToLayers(styleTable, matchedLayersMap, report);
		if (report.isFailed()) {
			return null;
		}

		compileStyles(injector, matchedLayers, report);
		if (report.isFailed()) {
			return null;
		}

		AdaptedDecorator<ODLTable> ret = createAdapter(matchedLayers, report);
		return ret;
	}

	private AdaptedDecorator<ODLTable> createAdapter(List<MatchedLayer> matchedLayers, ExecutionReport report) {
		// Now process all layers into the datastore adapter
		ArrayList<MatchedLayer> layersInType = new ArrayList<VLSBuilder.MatchedLayer>(matchedLayers.size());
		AdapterMapping mapping = AdapterMapping.createUnassignedMapping(DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS);
		ArrayList<ODLDatastore<? extends ODLTable>> dsList = new ArrayList<ODLDatastore<? extends ODLTable>>(matchedLayers.size());
		for (LayerType layerType : LayerType.values()) {

			// Get all the layers for this layertype
			layersInType.clear();
			for (MatchedLayer ml : matchedLayers) {
				if (ml.layerType == layerType) {
					layersInType.add(ml);
				}
			}

			int nl = layersInType.size();
			if (nl == 0) {
				// nothing to do...
				continue;
			}

			ODLTableDefinition destinationTable = TableUtils.findTable(DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS, layerType.tablename);
			if (nl == 1) {
				// non-union - add directly
				MatchedLayer ml = layersInType.get(0);
				mapping.setTableSourceId(destinationTable.getImmutableId(), dsList.size(), ml.source.validated.getImmutableId());
				setFieldMapping(ml, destinationTable.getImmutableId(), mapping);
				dsList.add(wrapTableInDs(ml.source.validated));

			} else {
				// union - we build individual adapters, then place in a union decorator which we add to the final adapter

				// build adapters for each layer
				ArrayList<ODLDatastore<? extends ODLTable>> dsListToUnion = new ArrayList<ODLDatastore<? extends ODLTable>>(nl);
				for (MatchedLayer ml : layersInType) {
					AdapterMapping singleSourceMapping = AdapterMapping.createUnassignedMapping(destinationTable);
					singleSourceMapping.setTableSourceId(destinationTable.getImmutableId(), 0, ml.source.validated.getImmutableId());
					setFieldMapping(ml, destinationTable.getImmutableId(), singleSourceMapping);
					dsListToUnion.add(new AdaptedDecorator<ODLTable>(singleSourceMapping, wrapTableInDs(ml.source.validated)));
				}

				// union them together
				UnionDecorator<ODLTable> union = new UnionDecorator<ODLTable>(dsListToUnion);

				// set the mapping to point towards the union
				mapping.setTableSourceId(destinationTable.getImmutableId(), dsList.size(), union.getTableAt(0).getImmutableId());
				for (int i = 0; i <= DrawableObjectImpl.COL_MAX; i++) {
					mapping.setFieldSourceIndx(destinationTable.getImmutableId(), i, i);
				}
				dsList.add(union);
			}
		}

		// Finally return the decorator
		AdaptedDecorator<ODLTable> ret = new AdaptedDecorator<ODLTable>(mapping, dsList);
		return ret;
	}

	private void compileStyles(VLSDependencyInjector injector, ArrayList<MatchedLayer> matchedLayers, ExecutionReport report) {
		StringConventions strings = api.stringConventions();

		for (MatchedLayer layer : matchedLayers) {

			// Get the values which need to match for each style in this layer
			ArrayList<List<Object>> selectors = new ArrayList<List<Object>>();
			for (MatchedStyle style : layer.styles) {
				ArrayList<Object> values = new ArrayList<Object>();
				for (int i = 0; i < Style.NB_RULE_KEYS; i++) {
					String key = style.style.getRuleKey(i);
					if (AdapterBuilderUtils.getFormulaFromText(key) != null) {
						report.setFailed("Functions are not allowed for style appearance key fields: " + key);
						return;
					}
					values.add(key);
				}
				selectors.add(values);
			}

			// Compile the style keys into a lookup tree
			layer.styleLookupTree = RuleNode.buildTree(selectors);

			// Compile the formulae for the styles
			for (MatchedStyle style : layer.styles) {
				for (OutputFormula ft : Style.OutputFormula.values()) {
					String styleValue = style.style.getFormula(ft);
					Function function = null;
					if (strings.isEmptyString(styleValue)) {
						function = FmConst.NULL;
					} else {

						// Test if this value is actually a formula (starts with :=)
						String styleFormula = AdapterBuilderUtils.getFormulaFromText(styleValue);
						if (styleFormula != null) {
							// Build the formula against the raw table (with the additional fields),
							// not the validated one where additional fields are stripped
							function = injector.buildFormula(styleFormula, layer.source.raw);
							if (report.isFailed()) {
								report.setFailed("Failed to process view-layer-style tables.");
								return;
							}
						} else {
							// Create constant formula with correct type
							Object value = ColumnValueProcessor.convertToMe(ft.outputType, styleValue);
							if (value == null) {
								report.setFailed("Could not convert value \"" + styleValue + "\" for style formula " + ft.name() + " to required type "
										+ ft.outputType.name() + ".");
							} else {
								function = new FmConst(value);
							}
						}
					}

					// Save the compiled function
					style.functions[ft.ordinal()] = function;
				}
			}
		}

	}

	/**
	 * Add the style objects to the layer objects, matching on layerid field
	 * 
	 * @param styleTable
	 * @param matchedLayersMap
	 * @param report
	 */
	private void addStylesToLayers(SourceTable styleTable, Map<String, MatchedLayer> matchedLayersMap, ExecutionReport report) {
		List<Style> styles = INPUT_VLS_ONLY.getTableMapping(INPUT_STYLE_INDX).readObjectsFromTable(styleTable.validated, report);
		if (styles.size() < styleTable.validated.getRowCount()) {
			report.setFailed("Failed to read one or more style records correctly.");
		}
		if (report.isFailed()) {
			return;
		}

		for (Style style : styles) {
			// check layer id
			if (api.stringConventions().isEmptyString(style.getLayerId())) {
				report.setFailed("Empty or null layer ID found for style.");
				return;
			}

			MatchedLayer ml = matchedLayersMap.get(style.getLayerId());
			if (ml != null) {
				ml.styles.add(new MatchedStyle(style));
			}
		}
	}

	private void readLayers(TableFinder finder, SourceTable layerTable, View view, ArrayList<MatchedLayer> matchedLayers,
			Map<String, MatchedLayer> matchedLayersMap, ExecutionReport report) {
		StringConventions strings = api.stringConventions();
		Map<String, ODLTable> sourceTables = strings.createStandardisedMap();

		// Read layer objects
		List<Layer> layers = INPUT_VLS_ONLY.getTableMapping(INPUT_LAYER_INDX).readObjectsFromTable(layerTable.validated, report);
		if (layers.size() < layerTable.validated.getRowCount() || report.isFailed()) {
			report.setFailed("Failed to read one or more layer records correctly.");
		}

		// Loop over each layer object within the view
		int activeCount = 0;
		for (Layer layer : layers) {
			if (strings.equalStandardised(view.getId(), layer.getViewId())) {

				// check layer id
				if (strings.isEmptyString(layer.getId())) {
					report.setFailed("Empty or null ID found for layer row.");
					return;
				}
				if (matchedLayersMap.containsKey(layer.getId())) {
					report.setFailed("Duplicate layerid found: " + layer.getId());
					return;
				}

				// create matched layer object
				MatchedLayer ml = new MatchedLayer();
				ml.layer = layer;
				matchedLayers.add(ml);
				matchedLayersMap.put(layer.getId(), ml);

				// check active
				boolean isActive = layer.getActiveLayer() == 1;
				if (isActive) {
					if (activeCount > 1) {
						report.setFailed("Found more than one active layer in a view: " + layer.getId());
						return;
					}
					ml.layerType = LayerType.ACTIVE;
					activeCount++;
				} else if (activeCount > 0) {
					ml.layerType = LayerType.FOREGROUND;
				} else {
					ml.layerType = LayerType.BACKGROUND;
				}

				// process source - note this could be a formula pointing to a shapefile or similar in the future
				if (strings.isEmptyString(layer.getSource())) {
					report.setFailed("Empty or null ID found for the data source for row in layers table with layer id " + layer.getId() + ".");
					return;
				}

				// see if source already processed, if not then process it
				ml.source = fetchLayerSourceTable(layer, sourceTables, finder, report);
				if (report.isFailed()) {
					return;
				}
			}
		}
	}

	private View findView(SourceTable viewTable, ExecutionReport report) {
		// Get the first view we find
		if (viewTable.validated.getRowCount() == 0) {
			report.setFailed("No view row provided in view table.");
			return null;
		}
		View view = INPUT_VLS_ONLY.getTableMapping(INPUT_VIEW_INDX).readObjectFromTableByRow(viewTable.validated, 0, report);
		if (view == null || report.isFailed()) {
			report.setFailed("Failed to view record correctly.");
		}
		if (api.stringConventions().isEmptyString(view.getId())) {
			report.setFailed("Empty or null ID found for view row.");
			return null;
		}
		return view;
	}

	private static class SourceTable {
		ODLTable raw;
		ODLTable validated;
	}

	private SourceTable fetchLayerSourceTable(Layer layer, Map<String, ODLTable> rawSourceTables, TableFinder finder, ExecutionReport report) {

		// Get the output table definition
		ODLTableDefinition outTableDfn = VLSSourceDrawables.BEAN_MAPPING.getTableDefinition();

		// Get the raw table, trying (1) cache, (2) table formula and (3) input table
		SourceTable ret = new SourceTable();
		ret.raw = rawSourceTables.get(layer.getSource());
		if(ret.raw==null){
			String layerFormula = AdapterBuilderUtils.getFormulaFromText(layer.getSource());
			if (layerFormula != null) {
				ret.raw =  finder.injector.buildTableFormula(layer.getSource());
			}else{
				ret.raw = finder.fetchRawSourceTable(VLSSourceDrawables.SOURCE_PREFIX + layer.getSource());
			}		
		}

		if(report.isFailed()){
			return null;
		}
			
		// Cache the raw source table
		rawSourceTables.put(layer.getSource(), ret.raw);

		// Apply filtering to the raw table if we have it...
		if(!api.stringConventions().isEmptyString(layer.getFilter())){
			String filterFormula = AdapterBuilderUtils.getFormulaFromText(layer.getFilter());
			if(filterFormula!=null){
				String dummyRawID = "RawDSID";
				
				// Create a dummy adapter for the filtering
				AdapterConfig dummyAdapter = new AdapterConfig("DummyID");
				AdaptedTableConfig dummyTable = new AdaptedTableConfig();
				dummyTable.setName(ret.raw.getName());
				dummyAdapter.getTables().add(dummyTable);
				int nc = ret.raw.getColumnCount();
				for(int i =0 ; i < nc ; i++){
					dummyTable.addMappedColumn(ret.raw.getColumnName(i), ret.raw.getColumnName(i), ret.raw.getColumnType(i), ret.raw.getColumnFlags(i));
				}
				dummyTable.setFilterFormula(filterFormula);	
				dummyTable.setFrom(dummyRawID, ret.raw.getName());
				
				ScriptExecutionBlackboardImpl bb = new ScriptExecutionBlackboardImpl(false);
				BuiltAdapters builtAdapters = new BuiltAdapters();
				builtAdapters.addAdapter(dummyRawID, wrapTableInDs(ret.raw));
				AdapterBuilder builder = new AdapterBuilder(dummyAdapter, null, bb, null, builtAdapters);
				ODLDatastore<? extends ODLTable> built = builder.build();
				if(bb.isFailed()){
					report.add(bb);
					report.setFailed("Failed to process filter formula in layer: " + filterFormula );
					return null;
				}
				
				ret.raw = built.getTableAt(0);
			}else{
				report.setFailed("Failed to parse filter \"" + layer.getFilter() + "\" in layer table.");
			}
		}

		// Match columns based on name and also match by geom type if not already found
		ColumnNameMatch columnNameMatch = new ColumnNameMatch(ret.raw, outTableDfn);
		if (columnNameMatch.getMatchForTableB(VLSSourceDrawables.COL_GEOMETRY) == -1) {
			columnNameMatch.setMatchForTableB(VLSSourceDrawables.COL_GEOMETRY, TableUtils.findColumnIndx(ret.raw, ODLColumnType.GEOM));
		}

		// Check either latitude and longitude or geometry have mapped
		boolean geomMatch = columnNameMatch.getMatchForTableB(VLSSourceDrawables.COL_GEOMETRY) != -1;
		boolean latMatch = columnNameMatch.getMatchForTableB(VLSSourceDrawables.COL_LATITUDE) != -1;
		boolean lngMatch = columnNameMatch.getMatchForTableB(VLSSourceDrawables.COL_LONGITUDE) != -1;
		if (!geomMatch && (!latMatch || !lngMatch)) {
			report.setFailed("Failed to map either geometry column or latitude and longitudes columns in input VLS table: " + layer.getSource());
			return null;
		}

		// Apply the field mapping to object defining the field mapping
		AdapterMapping mapping = AdapterMapping.createUnassignedMapping(outTableDfn);
		mapping.setTableSourceId(outTableDfn.getImmutableId(), 0, ret.raw.getImmutableId());
		int ndc = outTableDfn.getColumnCount();
		for (int i = 0; i < ndc; i++) {
			mapping.setFieldSourceIndx(outTableDfn.getImmutableId(), i, columnNameMatch.getMatchForTableB(i));
		}

		// Create an adapter
		AdaptedDecorator<ODLTable> adapter = new AdaptedDecorator<ODLTable>(mapping, ret.raw);
		ret.validated = adapter.getTableAt(0);
		
		return ret;
	}

	private void setFieldMapping(MatchedLayer ml, int destinationTableId, AdapterMapping mapping) {
		for (int i = 0; i <= DrawableObjectImpl.COL_MAX; i++) {
			if (i != DrawableObjectImpl.COL_LATITUDE && i != DrawableObjectImpl.COL_LONGITUDE && i != DrawableObjectImpl.COL_GEOMETRY) {
				mapping.setFieldFormula(destinationTableId, i, new StyleFunction(ml, i));
			} else {
				mapping.setFieldSourceIndx(destinationTableId, i, i);
			}
		}
	}

	private static ODLDatastoreImpl<ODLTable> wrapTableInDs(ODLTable source) {
		ODLDatastoreImpl<ODLTable> tmpDatastore = new ODLDatastoreImpl<ODLTable>(null);
		tmpDatastore.addTable(source);
		return tmpDatastore;
	}

	private static class StyleFunction extends FunctionImpl {
		private final MatchedLayer layer;
		private final Style.OutputFormula styleFormulaType;
		private final List<ODLDatastore<? extends ODLTable>> rawTableInDsList;
		private final int targetDrawableColumn;

		StyleFunction(MatchedLayer layer, int targetDrawableColumn) {
			this.layer = layer;
			this.rawTableInDsList = Arrays.asList(wrapTableInDs(layer.source.raw));
			this.targetDrawableColumn = targetDrawableColumn;

			// try to find a corresponding style formula type
			OutputFormula found = null;
			for (OutputFormula ft : OutputFormula.values()) {
				if (ft.drawablesColumn == targetDrawableColumn) {
					found = ft;
					break;
				}
			}
			this.styleFormulaType = found;

		}

		@Override
		public Object execute(FunctionParameters parameters) {
			TableParameters tp = (TableParameters) parameters;

			// Try to find a matching style
			MatchedStyle style = null;
			if (styleFormulaType != null) {
				if (layer.styles.size() > 0) {
					Object[] keys = new Object[Style.NB_RULE_KEYS];
					for (int i = 0; i < Style.NB_RULE_KEYS; i++) {
						keys[i] = APPEARANCE_KEY_ACCESSORS[i].execute(parameters);
						if (keys[i] == Functions.EXECUTION_ERROR) {
							return Functions.EXECUTION_ERROR;
						}
					}

					int rule = layer.styleLookupTree.findRuleNumber(keys);
					if (rule != -1) {
						style = layer.styles.get(rule);
					}
				}

			}

			// use the style
			if (style != null) {

				// Styles are executed against the raw table
				TableParameters rawTP = new TableParameters(rawTableInDsList, 0, rawTableInDsList.get(0).getTableAt(0).getImmutableId(), tp.getRowId(),
						tp.getRowNb(), null);
				Object val = style.functions[styleFormulaType.ordinal()].execute(rawTP);
				if (val == Functions.EXECUTION_ERROR) {
					return Functions.EXECUTION_ERROR;
				}

				// Just return the executed value if we're not doing flag mapping logic
				if (styleFormulaType.booleanToFlag == -1) {
					return val;
				}

				// Otherwise process flag mapping. First get the base value from the validated table
				Object baseValue = getValidatedTableValue(tp);
				if (baseValue == Functions.EXECUTION_ERROR) {
					return Functions.EXECUTION_ERROR;
				}

				// Then transform the base value to long
				Long lBase = Numbers.toLong(baseValue);
				long ret = lBase != null ? lBase : 0;

				// Then transform the executed style value to a bool and add / remove the flag
				Long lBool = Numbers.toLong(val);
				if (lBool != null) {
					if (lBool == 1) {
						// add flag
						ret |= styleFormulaType.booleanToFlag;
					} else {
						// remove flag
						ret &= ~styleFormulaType.booleanToFlag;
					}
				}

				return ret;
			}

			// If we have no default style we take the corresponding value from the validated table (which extends drawable)
			return getValidatedTableValue(tp);
		}

		private Object getValidatedTableValue(TableParameters tp) {
			if (tp.getRowNb() != -1) {
				return layer.source.validated.getValueAt(tp.getRowNb(), targetDrawableColumn);
			} else if (tp.getRowId() != -1) {
				return layer.source.validated.getValueById(tp.getRowId(), targetDrawableColumn);
			}
			return null;
		}

		@Override
		public Function deepCopy() {
			throw new UnsupportedOperationException();
		}

	}

}
