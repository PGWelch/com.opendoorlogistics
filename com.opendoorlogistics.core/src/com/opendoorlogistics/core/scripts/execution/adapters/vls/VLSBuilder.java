package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.Factory;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.core.formulae.FormulaParser;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.gis.map.data.BackgroundImage;
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
import com.opendoorlogistics.core.scripts.formulae.rules.RuleNode.RuleFilter;
import com.opendoorlogistics.core.scripts.wizard.ColumnNameMatch;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.UnionDecorator;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

import gnu.trove.set.hash.TIntHashSet;

public class VLSBuilder {

	private static final BeanDatastoreMapping INPUT_VLS_ONLY;
	private static final int INPUT_VIEW_INDX;
	private static final int INPUT_LAYER_INDX;
	private static final int INPUT_STYLE_INDX;
	private static final int INPUT_EXTRA_FIELDS_INDX;
	private static final BeanTableMappingImpl SOURCE_TABLE;
	private static final Function[] APPEARANCE_KEY_ACCESSORS;

	private final ODLApi api;

	public VLSBuilder(ODLApi api) {
		this.api = api;
	}

	static {
		INPUT_VLS_ONLY = BeanMapping.buildDatastore(View.class, Layer.class, Style.class, ExtraFields.class);
		INPUT_VIEW_INDX = 0;
		INPUT_LAYER_INDX = 1;
		INPUT_STYLE_INDX = 2;
		INPUT_EXTRA_FIELDS_INDX = 3;

		SOURCE_TABLE = BeanMapping.buildTable(VLSSourceDrawables.class);

		APPEARANCE_KEY_ACCESSORS = new Function[4];
		for (int i = 0; i < Style.NB_RULE_KEYS; i++) {
			APPEARANCE_KEY_ACCESSORS[i] = new FmLocalElement(VLSSourceDrawables.COL_VLSKEY1 + i, "VLSKey" + (i + 1));
		}

	}

	// public static class VLSConfig {
	// private List<String> datasourceTableReferences = new ArrayList<String>();
	//
	// public List<String> getDatasourceTableReferences() {
	// return datasourceTableReferences;
	// }
	//
	// public void setDatasourceTableReferences(List<String>
	// datasourceTableReferences) {
	// this.datasourceTableReferences = datasourceTableReferences;
	// }
	//
	// }

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
		MapTileProvider mapTileProvider;
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
		Function filter;
	}

	private enum LayerType {
		BACKGROUND_IMAGE(false, PredefinedTags.BACKGROUND_IMAGE, PredefinedTags.BACKGROUND_IMAGE), BACKGROUND(true, PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND,
				PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND, "background"), ACTIVE(true, PredefinedTags.DRAWABLES, "active"), FOREGROUND(true, PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND,
						PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND, "foreground");

		final String tablename;
		final String[] keywords;
		final boolean drawable;

		private LayerType(boolean drawable, String tablename, String... keywords) {
			this.drawable = drawable;
			this.tablename = tablename;
			this.keywords = keywords;
		}

		private static final StandardisedStringTreeMap<LayerType> KEYWORD_MAP = new StandardisedStringTreeMap<VLSBuilder.LayerType>(false);

		static {
			for (LayerType type : LayerType.values()) {
				for (String keyword : type.keywords) {
					KEYWORD_MAP.put(keyword, type);
				}
			}
		}

		public static LayerType identify(String s) {
			return KEYWORD_MAP.get(s);
		}

		public static String orderedCommaSeparatedKeywords() {
			StringBuilder builder = new StringBuilder();
			for (LayerType type : values()) {
				if (builder.length() > 0) {
					builder.append(", ");
				}
				builder.append(type.keywords[0]);
			}
			return builder.toString();
		}
	}

	/**
	 * Finds the input table and validates it to the table definition, returning
	 * a table mapped to have the correct fields in the correct order
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

		ODLTable fetchRawSourceTable(String name, boolean isOptional) {
			Integer indx = tables.get(name);
			ODLTable ret = null;
			if (indx != null) {
				ret = injector.buildTable(indx);
			}

			if (ret == null && !isOptional) {
				report.setFailed("Cannot find table required by view-layer-style adapter: " + name);
			}

			return ret;
		}

		SourceTable fetchValidatedSourceTable(String name, boolean isOptional, ODLTableDefinition definition) {
			ODLTable raw = fetchRawSourceTable(name, isOptional);
			if (report.isFailed() || raw == null) {
				return null;
			}

			SourceTable ret = new SourceTable();
			ret.raw = raw;

			// Do simple name-based mapping to ensure all fields are present and
			// in correct order, returning the mapped table
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

	private class ViewHierarchy {
		List<View> background = new ArrayList<>();
		List<View> active = new ArrayList<>();
		List<View> foreground = new ArrayList<>();
		View current;
	}

	private ViewHierarchy readViewHierarchy(SourceTable viewTable, ExecutionReport report) {
		ViewHierarchy ret = new ViewHierarchy();

		// Read views table and turn into map
		List<View> views = readViewTable(viewTable, report);
		StringConventions strings = api.stringConventions();
		Map<String, View> viewById = strings.createStandardisedMap();
		for (View view : views) {
			if (strings.isEmptyString(view.getId())) {
				report.setFailed("Empty or null ID found for view row.");
				return null;
			}

			if (viewById.containsKey(view.getId())) {
				report.setFailed("Found duplicate view id: " + view.getId());
				return null;
			}

			viewById.put(view.getId(), view);
		}

		// Find the main view
		ret.current = findView(views, report);
		if (report.isFailed()) {
			return null;
		}

		Set<String> includedViews = strings.createStandardisedSet();
		class Helper {
			List<View> getNewViews(String ids, int maxAllowed) {
				if (ids == null) {
					return new ArrayList<>(0);
				}
				String[] split = ids.split(",");
				if (split.length > maxAllowed) {
					report.setFailed("Found activeViewId field in views table containing more than one view id.");
					return new ArrayList<>(0);
				}

				// ArrayList<String> tmp = new ArrayList<>(split.length);
				ArrayList<View> ret = new ArrayList<>();
				for (String s : split) {
					s = strings.standardise(s);
					if (!strings.isEmptyString(s) && !includedViews.contains(s)) {
						includedViews.add(s);

						View view = viewById.get(s);
						if (view == null) {
							report.setFailed("Unknown view id referenced in include field in the views table:" + s);
							return new ArrayList<>(0);
						}

						// add recursion
						ret.addAll(recurse(view));

						// add myself
						ret.add(view);
					}
				}

				return ret;
			}

			List<View> recurse(View view) {
				ArrayList<View> ret = new ArrayList<>();
				ret.addAll(getNewViews(view.getBackgroundViewIds(), Integer.MAX_VALUE));
				ret.addAll(getNewViews(view.getActiveViewId(), 1));
				ret.addAll(getNewViews(view.getForegroundViewIds(), Integer.MAX_VALUE));
				return ret;
			}
		}

		// Build the view hierarchy, including each view only once
		includedViews.add(ret.current.getId());
		Helper helper = new Helper();
		ret.background = helper.getNewViews(ret.current.getBackgroundViewIds(), Integer.MAX_VALUE);
		ret.active = helper.getNewViews(ret.current.getActiveViewId(), 1);
		ret.foreground = helper.getNewViews(ret.current.getForegroundViewIds(), Integer.MAX_VALUE);

		return ret;
	}

	public ODLDatastore<? extends ODLTable> build(VLSDependencyInjector injector, ExecutionReport report) {

		// Try getting built in tables
		TableFinder finder = new TableFinder(api, injector, report);
		SourceTable viewTable = finder.fetchValidatedSourceTable(View.TABLE_NAME, false, INPUT_VLS_ONLY.getTableMapping(INPUT_VIEW_INDX).getTableDefinition());
		SourceTable layerTable = finder.fetchValidatedSourceTable(Layer.TABLE_NAME, false, INPUT_VLS_ONLY.getTableMapping(INPUT_LAYER_INDX).getTableDefinition());
		SourceTable styleTable = finder.fetchValidatedSourceTable(Style.TABLE_NAME, false, INPUT_VLS_ONLY.getTableMapping(INPUT_STYLE_INDX).getTableDefinition());
		SourceTable extraFieldDfnsTable = finder.fetchValidatedSourceTable(ExtraFields.TABLE_NAME, true, INPUT_VLS_ONLY.getTableMapping(INPUT_EXTRA_FIELDS_INDX).getTableDefinition());
		if (report.isFailed()) {
			return null;
		}

		ViewHierarchy viewHierarchy = readViewHierarchy(viewTable, report);
		if (report.isFailed()) {
			return null;
		}

		// Read the additional field definitions
		ODLTableDefinition extraFldsDfns = readExtraFields(extraFieldDfnsTable, report);
		if(report.isFailed()){
			return null;
		}
		
		// Get layers for this view including their data source tables
		ArrayList<MatchedLayer> matchedLayers = new ArrayList<VLSBuilder.MatchedLayer>();
		StringConventions strings = api.stringConventions();
		Map<String, MatchedLayer> matchedLayersMap = strings.createStandardisedMap();
		readLayers(finder, layerTable, viewHierarchy,extraFldsDfns, matchedLayers, matchedLayersMap, report);
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

		AdaptedDecorator<ODLTable> ret = createAdapter(matchedLayers, extraFldsDfns,report);
		return ret;
	}

	private ODLTableDefinition readExtraFields(SourceTable extraFieldDfnsTable, ExecutionReport report) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds = api.tables().createDefinitionDs();
		ODLTableDefinitionAlterable table = ds.createTable(ExtraFields.TABLE_NAME, -1);
		if (extraFieldDfnsTable == null) {
			return table;
		}

		// get list of all field names
		Set<String> allFields = api.stringConventions().createStandardisedSet();
		for(int i =0 ; i < DrawableObjectImpl.DRAWABLES_ONLY_DS.getTableCount();i++){
			ODLTableDefinition drawables = DrawableObjectImpl.DRAWABLES_ONLY_DS.getTableAt(i);
			int nc = drawables.getColumnCount();
			for(int j =0 ; j < nc ; j++){
				allFields.add(drawables.getColumnName(j));
			}
		}
		
		List<ExtraFields> rows = INPUT_VLS_ONLY.getTableMapping(INPUT_EXTRA_FIELDS_INDX).readObjectsFromTable(extraFieldDfnsTable.validated, report);
		if (report.isFailed()) {
			return null;
		}
		
		for (ExtraFields fld : rows) {
			ODLColumnType type = api.tables().getColumnType(fld.getType());
			if (type == null) {
				report.setFailed("Unidentified column type in extra fields table in VLS adapter: " + fld.getType());
				return null;
			}

			String name = fld.getName();
			if (api.stringConventions().isEmptyStandardised(name)) {
				report.setFailed("Empty name found for field in extra fields table in VLS adapter.");
				return null;
			}

			// check not already used
			if(allFields.contains(name)){
				report.setFailed("Found field in extra fields table in VLS adapter which has been included twice: " + name);
				return null;				
			}
			
			table.addColumn(-1, name, type, 0);
			allFields.add(name);
		}

		return table;
	}

	private AdaptedDecorator<ODLTable> createAdapter(List<MatchedLayer> matchedLayers,ODLTableDefinition extraFields, ExecutionReport report) {
		// Now process all layers into the datastore adapter
		ArrayList<MatchedLayer> layersInType = new ArrayList<VLSBuilder.MatchedLayer>(matchedLayers.size());
		AdapterMapping mapping = AdapterMapping.createUnassignedMapping(DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS,true);
		ArrayList<ODLDatastore<? extends ODLTable>> dsList = new ArrayList<ODLDatastore<? extends ODLTable>>(matchedLayers.size());
		for (LayerType layerType : LayerType.values()) {

			// Get all the layers for this layertype
			layersInType.clear();
			for (MatchedLayer ml : matchedLayers) {
				if (ml.layerType == layerType) {
					layersInType.add(ml);
				}
			}

			ODLTableDefinition destinationTable = TableUtils.findTable(DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS, layerType.tablename);
			int nl = layersInType.size();
			if (nl == 0) {
				if (layerType != LayerType.BACKGROUND_IMAGE) {
					// add the extra fields (even though they're empty)
					int nbExtra = extraFields!=null? extraFields.getColumnCount() : 0;
					for(int i =0 ; i < nbExtra;i++){
						mapping.addMappedField(destinationTable.getImmutableId(), extraFields.getColumnName(i), extraFields.getColumnType(i), -1);
					}
				}
				
				// nothing to do...
				continue;
			}

			// Get the destination table
			Tables tables = api.tables();
			
			if (layerType == LayerType.BACKGROUND_IMAGE) {

				// Create a table containing all the map tile providers in-order
				ODLDatastoreAlterable<? extends ODLTableAlterable> imgDs = tables.createAlterableDs();
				ODLTable table = (ODLTable) tables.copyTableDefinition(BackgroundImage.BEAN_MAPPING.getTableDefinition(), imgDs);
				mapping.setTableSourceId(destinationTable.getImmutableId(), dsList.size(), table.getImmutableId());
				dsList.add(imgDs);
				for (int i = 0; i < table.getColumnCount(); i++) {
					mapping.setFieldSourceIndx(destinationTable.getImmutableId(), i, i);
				}
				for (MatchedLayer ml : layersInType) {
					if (ml.mapTileProvider != null) {
						BackgroundImage bi = new BackgroundImage();
						bi.setTileProvider(ml.mapTileProvider);
						BackgroundImage.BEAN_MAPPING.writeObjectToTable(bi, table);
					}
				}
			} else {

//				// Add the extra fields to the destination table if needed
//				if(extraFields!=null && extraFields.getColumnCount()>0){
//					destinationTable =tables.copyTableDefinition(destinationTable, tables.createAlterableDs());
//					tables.copyTableDefinition(extraFields, (ODLTableDefinitionAlterable)destinationTable);
//				}
				
				if (nl == 1) {
					// non-union - add directly
					MatchedLayer ml = layersInType.get(0);
					mapping.setTableSourceId(destinationTable.getImmutableId(), dsList.size(), ml.source.validated.getImmutableId());
					setFieldMapping(ml, destinationTable.getImmutableId(),extraFields, mapping);
					dsList.add(wrapTableInDs(ml.source.validated));

				} else {
					// union - we build individual adapters, then place in a
					// union decorator which we add to the final adapter

					// build adapters for each layer
					ArrayList<ODLDatastore<? extends ODLTable>> dsListToUnion = new ArrayList<ODLDatastore<? extends ODLTable>>(nl);
					for (MatchedLayer ml : layersInType) {
						AdapterMapping singleSourceMapping = AdapterMapping.createUnassignedMapping(destinationTable,true);
						singleSourceMapping.setTableSourceId(destinationTable.getImmutableId(), 0, ml.source.validated.getImmutableId());
						setFieldMapping(ml, destinationTable.getImmutableId(),extraFields, singleSourceMapping);
						AdaptedDecorator<ODLTable> singleLayerDecorator = new AdaptedDecorator<ODLTable>(singleSourceMapping, wrapTableInDs(ml.source.validated));
						dsListToUnion.add(singleLayerDecorator);
					}

					// union them together
					UnionDecorator<ODLTable> union = new UnionDecorator<ODLTable>(dsListToUnion);

					// set the mapping to point towards the union
					mapping.setTableSourceId(destinationTable.getImmutableId(), dsList.size(), union.getTableAt(0).getImmutableId());
					for (int i = 0; i <= DrawableObjectImpl.COL_MAX; i++) {
						mapping.setFieldSourceIndx(destinationTable.getImmutableId(), i, i);
					}
					
					// also add the extra fields to the additional mapping
					int nbExtra = extraFields!=null? extraFields.getColumnCount() : 0;
					for(int i =0 ; i < nbExtra;i++){
						mapping.addMappedField(destinationTable.getImmutableId(), extraFields.getColumnName(i), extraFields.getColumnType(i), DrawableObjectImpl.COL_MAX+ 1 + i);
					}
					
					dsList.add(union);
				}
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
			layer.styleLookupTree = RuleNode.buildTree(selectors, true);

			// Compile the formulae for the styles
			for (MatchedStyle style : layer.styles) {

				// build output formula
				for (OutputFormula ft : Style.OutputFormula.values()) {
					String styleValue = style.style.getFormula(ft);
					Function function = null;
					if (strings.isEmptyString(styleValue)) {
						function = null;
					} else {

						// Test if this value is actually a formula (starts with
						// :=)
						String styleFormula = AdapterBuilderUtils.getFormulaFromText(styleValue);
						if (styleFormula != null) {
							// Build the formula against the raw table (with the
							// additional fields),
							// not the validated one where additional fields are
							// stripped
							function = injector.buildFormula(styleFormula, layer.source.raw);
							if (report.isFailed()) {
								report.setFailed("Failed to build style formula " + ft.name() + " in styles table: " + styleFormula);
								return;
							}
						} else {
							// Create constant formula with correct type
							Object value = ColumnValueProcessor.convertToMe(ft.outputType, styleValue);
							if (value == null) {
								report.setFailed("Could not convert value \"" + styleValue + "\" for style formula " + ft.name() + " to required type " + ft.outputType.name() + ".");
							} else {
								function = new FmConst(value);
							}
						}
					}

					// Save the compiled function
					style.functions[ft.ordinal()] = function;
				}

				// build filter formula
				String filterFormula = AdapterBuilderUtils.getFormulaFromText(style.style.getFilter());
				if (!strings.isEmptyString(style.style.getFilter()) && filterFormula == null) {
					report.setFailed("Found a non-empty filter formulae in styles table that did not begin with := (the symbol which indicates a formula).");
					return;
				}
				if (filterFormula != null) {
					style.filter = injector.buildFormula(filterFormula, layer.source.raw);
					if (report.isFailed()) {
						report.setFailed("Failed to build filter formula in styles table:" + filterFormula);
						return;
					}

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

	private void readLayers(TableFinder finder, SourceTable layerTable, ViewHierarchy viewHierarchy, ODLTableDefinition additionalFields,List<MatchedLayer> matchedLayers, Map<String, MatchedLayer> matchedLayersMap,
			ExecutionReport report) {
		StringConventions strings = api.stringConventions();
		Map<String, ODLTable> sourceTables = strings.createStandardisedMap();

		// Read layer objects
		List<Layer> layers = INPUT_VLS_ONLY.getTableMapping(INPUT_LAYER_INDX).readObjectsFromTable(layerTable.validated, report);
		if (layers.size() < layerTable.validated.getRowCount() || report.isFailed()) {
			report.setFailed("Failed to read one or more layer records correctly.");
		}

		// Collate by view
		Map<String, LinkedList<Layer>> layersByView = strings.createStandardisedMap(new Factory<LinkedList<Layer>>() {

			@Override
			public LinkedList<Layer> create() {
				return new LinkedList<>();
			}
		});
		for (Layer layer : layers) {
			layersByView.get(layer.getViewId()).add(layer);
		}

		int[] countByLayerType = new int[LayerType.values().length];
		class Helper {
			void addView(View view, LayerType lt) {
				// Loop over each layer object within the view
				LinkedList<Layer> layers4View = layersByView.get(view.getId());
				if (layers4View != null) {
					for (Layer layer : layers4View) {

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

						// identify layer type, using position of the active layer to
						// identify background and foreground if string is empty
						if (lt == null && Strings.isEmpty(layer.getLayerType())) {
							report.setFailed("Layer with id " + layer.getId() + " has empty layer type.");
							return;
						}

						ml.layerType = lt;
						if (ml.layerType == null) {
							ml.layerType = LayerType.identify(layer.getLayerType());
						}
						if (ml.layerType == null) {
							report.setFailed("Unidentified layer type: " + layer.getLayerType());
							return;
						}

						// validate layer type order
						if (ml.layerType == LayerType.ACTIVE && countByLayerType[ml.layerType.ordinal()] > 0) {
							report.setFailed("Found more than one active layer in a view: " + ml.layer.getId());
							return;
						}
						for (int i = 0; i < countByLayerType.length; i++) {
							int count = countByLayerType[i];
							if (count > 0 && ml.layerType.ordinal() < i) {
								report.setFailed("Found layer type " + ml.layerType.keywords[0] + " defined after layer type " + LayerType.values()[i].keywords[0]
										+ ". Layers must be defined in order " + LayerType.orderedCommaSeparatedKeywords() + ".");
								return;
							}
						}
						countByLayerType[ml.layerType.ordinal()]++;
					}
				}

			}

			void addViews(Iterable<View> views, LayerType lt) {
				for (View view : views) {
					addView(view, lt);
				}
			}
		}

		Helper helper = new Helper();
		helper.addViews(viewHierarchy.background, LayerType.BACKGROUND);
		helper.addViews(viewHierarchy.active, LayerType.ACTIVE);
		helper.addView(viewHierarchy.current, null);
		helper.addViews(viewHierarchy.foreground, LayerType.FOREGROUND);

		// Process matched layer sources
		for (MatchedLayer ml : matchedLayers) {
			Layer layer = ml.layer;

			// process source - note this could be a formula pointing to a
			// shapefile or similar in the future
			if (strings.isEmptyString(layer.getSource())) {
				report.setFailed("Empty or null source value found for the data source for row in layers table with layer id " + layer.getId() + ".");
				return;
			}

			// see if source already processed, if not then process it
			if (ml.layerType == LayerType.BACKGROUND_IMAGE) {
				ml.mapTileProvider = fetchMapTileProvider(layer, report);
			} else {
				ml.source = fetchLayerSourceTable(layer, sourceTables, additionalFields, finder, report);
			}
			if (report.isFailed()) {
				return;
			}

		}
	}

	private MapTileProvider fetchMapTileProvider(Layer layer, ExecutionReport report) {
		String source = layer.getSource();
		String sFormula = AdapterBuilderUtils.getFormulaFromText(source);
		if (sFormula == null) {
			report.setFailed("Background image layer with id " + layer.getId() + " found without a source formula; formula values begin with :=");
			return null;
		}

		Function function = null;
		if (sFormula != null) {
			FormulaParser loader = new FormulaParser(null, FunctionDefinitionLibrary.DEFAULT_LIB, null);
			try {
				function = loader.parse(sFormula);
			} catch (Exception e) {
				report.setFailed(e);
			}
		}

		if (function == null || report.isFailed()) {
			report.setFailed("Background image layer with id " + layer.getId() + " - failed to compile source formula.");
			return null;
		}

		MapTileProvider provider = null;
		boolean wrongType = false;
		try {
			Object exec = function.execute(null);
			if (exec != Functions.EXECUTION_ERROR) {
				if (exec != null && exec instanceof MapTileProvider) {
					provider = (MapTileProvider) exec;
				} else {
					wrongType = true;
				}
			}
		} catch (Exception e) {
		}

		if (provider == null) {
			// report.setFailed("Layer with type " +
			// LayerType.BACKGROUND_IMAGE.keywords[0] + " had a problem
			// executing the formula: " + source);
			report.setFailed("Background image layer with id " + layer.getId() + " - failed to execute source formula."
					+ (wrongType ? " Result must be of type " + ODLColumnType.MAP_TILE_PROVIDER.name() + "." : ""));
		}

		return provider;

	}

	private List<View> readViewTable(SourceTable viewTable, ExecutionReport report) {
		return INPUT_VLS_ONLY.getTableMapping(INPUT_VIEW_INDX).readObjectsFromTable(viewTable.validated, report);
	}

	private View findView(List<View> views, ExecutionReport report) {
		if (views.size() == 0) {
			report.setFailed("No view row provided in view table.");
			return null;
		}
		// Get the first view we find witrh isdefault=1
		for (View view : views) {
			if (view.getIsDefault() == 1) {
				return view;
			}
		}

		// If not found just take the first
		return views.get(0);
	}

	private static class SourceTable {
		ODLTable raw;
		ODLTable validated;
		int []extraFieldIndicesInRaw;
	}

	private SourceTable fetchLayerSourceTable(Layer layer, Map<String, ODLTable> rawSourceTables, ODLTableDefinition additionalFields, TableFinder finder, ExecutionReport report) {

		// Get the output table definition
		ODLTableDefinition outTableDfn = VLSSourceDrawables.BEAN_MAPPING.getTableDefinition();

		// Get the raw table, trying (1) cache, (2) table formula and (3) input table
		SourceTable ret = new SourceTable();
		ret.raw = rawSourceTables.get(layer.getSource());
		if (ret.raw == null) {
			String layerFormula = AdapterBuilderUtils.getFormulaFromText(layer.getSource());
			if (layerFormula != null) {
				ret.raw = finder.injector.buildTableFormula(layer.getSource());
			} else {
				ret.raw = finder.fetchRawSourceTable(VLSSourceDrawables.SOURCE_PREFIX + layer.getSource(), false);
			}
		}

		if (report.isFailed()) {
			return null;
		}
		
		// Cache the raw source table
		rawSourceTables.put(layer.getSource(), ret.raw);

		// Apply filtering to the raw table if we have it...
		if (!api.stringConventions().isEmptyString(layer.getFilter())) {
			String filterFormula = AdapterBuilderUtils.getFormulaFromText(layer.getFilter());
			if (filterFormula != null) {
				String dummyRawID = "RawDSID";

				// Create a dummy adapter for the filtering
				AdapterConfig dummyAdapter = new AdapterConfig("DummyID");
				AdaptedTableConfig dummyTable = new AdaptedTableConfig();
				dummyTable.setName(ret.raw.getName());
				dummyAdapter.getTables().add(dummyTable);
				int nc = ret.raw.getColumnCount();
				for (int i = 0; i < nc; i++) {
					dummyTable.addMappedColumn(ret.raw.getColumnName(i), ret.raw.getColumnName(i), ret.raw.getColumnType(i), ret.raw.getColumnFlags(i));
				}
				dummyTable.setFilterFormula(filterFormula);
				dummyTable.setFrom(dummyRawID, ret.raw.getName());

				ScriptExecutionBlackboardImpl bb = new ScriptExecutionBlackboardImpl(false);
				BuiltAdapters builtAdapters = new BuiltAdapters();
				builtAdapters.addAdapter(dummyRawID, wrapTableInDs(ret.raw));
				AdapterBuilder builder = new AdapterBuilder(dummyAdapter, null, bb, null, builtAdapters);
				ODLDatastore<? extends ODLTable> built = builder.build();
				if (bb.isFailed()) {
					report.add(bb);
					report.setFailed("Failed to process filter formula in layer: " + filterFormula);
					return null;
				}

				ret.raw = built.getTableAt(0);
			} else {
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
		AdapterMapping mapping = AdapterMapping.createUnassignedMapping(outTableDfn,true);
		mapping.setTableSourceId(outTableDfn.getImmutableId(), 0, ret.raw.getImmutableId());
		int ndc = outTableDfn.getColumnCount();
		for (int i = 0; i < ndc; i++) {
			mapping.setFieldSourceIndx(outTableDfn.getImmutableId(), i, columnNameMatch.getMatchForTableB(i));
		}

		// Ensure the raw has the extra fields and map them to the validated
		if(additionalFields!=null && additionalFields.getColumnCount()>0){
			int nbExtra = additionalFields.getColumnCount();
			ret.extraFieldIndicesInRaw = new int[nbExtra];
			Map<String, Integer> map = api.tables().getColumnNamesMap(ret.raw);
			for(int col =0 ; col < nbExtra;col++){
				String name = additionalFields.getColumnName(col);
				Integer indx = map.get(name);
				if(indx==null){
					report.setFailed("Additional field " + additionalFields.getColumnName(col) + " is missing from source table " + layer.getSource() + ".");
					return null;
				}
				ret.extraFieldIndicesInRaw[col] = indx;
				mapping.addMappedField(outTableDfn.getImmutableId(), name, additionalFields.getColumnType(col), indx);

			}
		}else{
			ret.extraFieldIndicesInRaw = new int[0];
		}
		
		// Create an adapter
		AdaptedDecorator<ODLTable> adapter = new AdaptedDecorator<ODLTable>(mapping, ret.raw);
		ret.validated = adapter.getTableAt(0);

		return ret;
	}

	/**
	 * Set mapping which defines a drawables table. Latitude, longitude and
	 * geometry and non-overlapping layer come directly from the input source
	 * table, everything else comes from a style function
	 * 
	 * @param ml
	 * @param destinationTableId
	 * @param mapping
	 */
	private void setFieldMapping(MatchedLayer ml, int destinationTableId,ODLTableDefinition extraFields, AdapterMapping mapping) {
		// get set of columns read directly from validated source and never
		// controlled by styles
		TIntHashSet fromSource = new TIntHashSet();
		fromSource.add(DrawableObjectImpl.COL_LATITUDE);
		fromSource.add(DrawableObjectImpl.COL_LONGITUDE);
		fromSource.add(DrawableObjectImpl.COL_GEOMETRY);
		fromSource.add(DrawableObjectImpl.COL_NOPL_GROUP_KEY);

		// now set the source for each column as appropriate
		for (int drawablesCol = 0; drawablesCol <= DrawableObjectImpl.COL_MAX; drawablesCol++) {
			if (fromSource.contains(drawablesCol)) {
				// Read directly from validated source table
				mapping.setFieldSourceIndx(destinationTableId, drawablesCol, drawablesCol);
			} else {
				// Style formula based on unvalidated raw source table,
				// defaulting to validated when formula unavailable empty
				mapping.setFieldFormula(destinationTableId, drawablesCol, new StyleFunction(ml, drawablesCol));
			}
		}
				
		// additional fields come from the raw...
		int []extraSrcs = ml.source.extraFieldIndicesInRaw;
		if(extraSrcs!=null && extraSrcs.length>0){
			if(extraFields.getColumnCount()!= extraSrcs.length){
				throw new RuntimeException();
			}
			for(int i =0 ; i < extraSrcs.length; i++){
				final int finalI=i;
				mapping.addMappedFormula(destinationTableId, extraFields.getColumnName(i), extraFields.getColumnType(i), new FunctionImpl() {
					
					@Override
					public Object execute(FunctionParameters parameters) {
						TableParameters tp = (TableParameters)parameters;
						return ml.source.raw.getValueById(tp.getRowId(), extraSrcs[finalI]);
					}
					
					@Override
					public Function deepCopy() {
						throw new IllegalArgumentException();
					}
				});
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
		private final List<ODLDatastore<? extends ODLTable>> rawTableWrappedInDsList;
		private final int targetDrawableColumn;
		private final RuleFilter ruleFilter = new RuleFilter() {

			@Override
			public boolean filter(Object filterData, Object[] values, int ruleNb) {
				TableParameters rawTP = (TableParameters) filterData;
				MatchedStyle style = layer.styles.get(ruleNb);
				if (style.filter != null) {
					Long l = Numbers.toLong(style.filter.execute(rawTP));
					if (l != null && l == 1) {
						return true;
					} else {
						return false;
					}
				}
				return true;
			}
		};

		StyleFunction(MatchedLayer layer, int targetDrawableColumn) {
			this.layer = layer;
			this.rawTableWrappedInDsList = Arrays.asList(wrapTableInDs(layer.source.raw));
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

			// Get table parameters for the raw input table
			TableParameters rawTP = new TableParameters(rawTableWrappedInDsList, 0, rawTableWrappedInDsList.get(0).getTableAt(0).getImmutableId(), tp.getRowId(), tp.hasRowNb()?tp.getRowNb():-1, null);

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

					int rule = layer.styleLookupTree.findRuleNumber(keys, ruleFilter, rawTP);
					if (rule != -1) {
						style = layer.styles.get(rule);
					}
				}

			}

			// use the style
			if (style != null) {

				// if we don't have a function then use the source table
				if (style.functions[styleFormulaType.ordinal()] == null) {
					if (targetDrawableColumn != -1) {
						return layer.source.validated.getValueAt(tp.getRowNb(), targetDrawableColumn);
					}
					return null;
				}

				// Styles are executed against the raw table
				Object val = style.functions[styleFormulaType.ordinal()].execute(rawTP);
				if (val == Functions.EXECUTION_ERROR) {
					return Functions.EXECUTION_ERROR;
				}

				// Just return the executed value if we're not doing flag
				// mapping logic
				if (styleFormulaType.booleanToFlag == -1) {
					return val;
				}

				// Otherwise process flag mapping. First get the base value from
				// the validated table
				Object baseValue = getValidatedTableValue(tp);
				if (baseValue == Functions.EXECUTION_ERROR) {
					return Functions.EXECUTION_ERROR;
				}

				// Then transform the base value to long
				Long lBase = Numbers.toLong(baseValue);
				long ret = lBase != null ? lBase : 0;

				// Then transform the executed style value to a bool and add /
				// remove the flag
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

			// take min and max zoom from the layers if set
			if (targetDrawableColumn == DrawableObjectImpl.COL_MIN_ZOOM && layer.layer.getMinZoom() != null) {
				return layer.layer.getMinZoom();
			}

			if (targetDrawableColumn == DrawableObjectImpl.COL_MAX_ZOOM && layer.layer.getMaxZoom() != null) {
				return layer.layer.getMaxZoom();
			}

			// If we have no default style we take the corresponding value from
			// the validated table (which extends drawable)
			return getValidatedTableValue(tp);
		}

		private Object getValidatedTableValue(TableParameters tp) {
			if (tp.hasRowNb()) {
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
