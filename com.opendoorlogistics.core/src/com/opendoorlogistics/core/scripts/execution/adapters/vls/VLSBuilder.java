package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.loading.MLet;

import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.scripts.TargetIODsInterpreter;
import com.opendoorlogistics.core.scripts.elements.UserFormula;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilderUtils;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.Style.OutputFormula;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.scripts.formulae.TableParameters;
import com.opendoorlogistics.core.scripts.formulae.rules.RuleNode;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.UnionDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
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
		ODLTable source;
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
		BACKGROUND(PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND), ACTIVE(PredefinedTags.DRAWABLES), FOREGROUND(PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND);

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

			tables = api.stringConventions().createStandardisedMap();
			for (int i = 0; i < injector.getTableCount(); i++) {
				tables.put(injector.getTableName(i), i);
			}
		}

		ODLTable buildValidated(String name, ODLTableDefinition definition) {
			Integer indx = tables.get(name);
			if (indx == null) {
				report.setFailed("Cannot find table required bby view-layer-style adapter: " + name);
			} else {
				ODLTable table = injector.buildTable(indx);

				if (!report.isFailed()) {

					// Do simple name-based mapping to ensure all fields are present and in correct order, returning the mapped table
					ODLDatastoreImpl<ODLTableDefinition> tmpDfn = new ODLDatastoreImpl<ODLTableDefinition>(null);
					tmpDfn.addTable(definition);

					ODLDatastoreImpl<ODLTable> tmpData = new ODLDatastoreImpl<ODLTable>(null);
					tmpData.addTable(table);

					ODLDatastore<? extends ODLTable> mapped = new TargetIODsInterpreter(api).buildScriptExecutionAdapter(tmpData, tmpDfn, report);
					if (mapped != null && !report.isFailed()) {
						return mapped.getTableAt(0);
					}
				}

			}
			return null;
		}
	}

	public static ODLDatastore<? extends ODLTable> build(ODLApi api, VLSDependencyInjector injector, ExecutionReport report) {

		// make a lookup of table names
		StringConventions strings = api.stringConventions();

		TableFinder finder = new TableFinder(api, injector, report);

		// try getting built in tables
		ODLTableReadOnly viewTable = finder.buildValidated(View.TABLE_NAME, INPUT_VLS_ONLY.getTableMapping(INPUT_VIEW_INDX).getTableDefinition());
		ODLTableReadOnly layerTable = finder.buildValidated(Layer.TABLE_NAME, INPUT_VLS_ONLY.getTableMapping(INPUT_LAYER_INDX).getTableDefinition());
		ODLTableReadOnly styleTable = finder.buildValidated(Style.TABLE_NAME, INPUT_VLS_ONLY.getTableMapping(INPUT_STYLE_INDX).getTableDefinition());
		if (report.isFailed()) {
			return null;
		}

		// Get the first view we find
		if (viewTable.getRowCount() == 0) {
			report.setFailed("No view row provided in view table.");
			return null;
		}
		View view = INPUT_VLS_ONLY.getTableMapping(INPUT_VIEW_INDX).readObjectFromTableByRow(viewTable, 0);
		if (strings.isEmptyString(view.getId())) {
			report.setFailed("Empty or null ID found for view row.");
			return null;
		}

		// Get layers for this view
		List<Layer> layers = INPUT_VLS_ONLY.getTableMapping(INPUT_LAYER_INDX).readObjectsFromTable(layerTable);
		ArrayList<MatchedLayer> matchedLayers = new ArrayList<VLSBuilder.MatchedLayer>();
		Map<String, MatchedLayer> matchedLayersMap = strings.createStandardisedMap();
		Map<String, ODLTable> sourceTables = strings.createStandardisedMap();
		int activeCount = 0;
		for (Layer layer : layers) {
			if (strings.equalStandardised(view.getId(), layer.getViewId())) {

				// check layer id
				if (strings.isEmptyString(layer.getId())) {
					report.setFailed("Empty or null ID found for layer row.");
					return null;
				}
				if (matchedLayersMap.containsKey(layer.getId())) {
					report.setFailed("Duplicate layerid found: " + layer.getId());
					return null;
				}

				// create
				MatchedLayer ml = new MatchedLayer();
				ml.layer = layer;
				matchedLayers.add(ml);
				matchedLayersMap.put(layer.getId(), ml);

				// check active
				boolean isActive = layer.getActiveLayer() == 1;
				if (isActive) {
					if (activeCount > 1) {
						report.setFailed("Found more than one active layer in a view: " + layer.getId());
						return null;
					}
					ml.layerType = LayerType.ACTIVE;
				} else if (activeCount > 0) {
					ml.layerType = LayerType.FOREGROUND;
				} else {
					ml.layerType = LayerType.BACKGROUND;
				}

				// process source - note this could be a formula pointing to a shapefile or similar in the future
				if (strings.isEmptyString(layer.getSource())) {
					report.setFailed("Empty or null ID found data source in layer row, with layer id " + layer.getId());
					return null;
				}

				// see if source already processed, if not then process it
				ml.source = getLayerSourceTable(layer, sourceTables, finder, report);
				if(report.isFailed()){
					return null;
				}
			}
		}

		// Add styles to layers
		List<Style> styles = INPUT_VLS_ONLY.getTableMapping(INPUT_STYLE_INDX).readObjectsFromTable(styleTable);
		for (Style style : styles) {
			// check layer id
			if (strings.isEmptyString(style.getLayerId())) {
				report.setFailed("Empty or null layer ID found for style.");
				return null;
			}

			MatchedLayer ml = matchedLayersMap.get(style.getLayerId());
			if (ml != null) {
				ml.styles.add(new MatchedStyle(style));
			}
		}

		// Compile the styles into a lookup tree for each layer
		for (MatchedLayer layer : matchedLayers) {
			ArrayList<List<Object>> selectors = new ArrayList<List<Object>>();
			for (MatchedStyle style : layer.styles) {
				ArrayList<Object> values = new ArrayList<Object>();
				values.add(style.style.getKey1());
				values.add(style.style.getKey2());
				values.add(style.style.getKey3());
				values.add(style.style.getKey4());
				selectors.add(values);
			}

			layer.styleLookupTree = RuleNode.buildTree(selectors);
		}

		// Compile the formulae for the styles
		for (MatchedLayer layer : matchedLayers) {
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
							function = injector.buildFormula(styleFormula, layer.source);
							if (report.isFailed()) {
								report.setFailed("Failed to process view-layer-style tables.");
								return null;
							}
						} else {
							// Create constant formula with correct type
							function = new FmConst(ColumnValueProcessor.convertToMe(ft.outputType, styleValue));
						}
					}

					style.functions[ft.ordinal()] = function;
				}
			}
		}

		// Now process all layers into the adapter
		ArrayList<MatchedLayer> layersInType = new ArrayList<VLSBuilder.MatchedLayer>(matchedLayers.size());
		AdapterMapping mapping = AdapterMapping.createUnassignedMapping(DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_DS);
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

			ODLTableDefinition destinationTable = TableUtils.findTable(DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_DS, layerType.tablename);
			if (nl == 1) {
				// non-union - add directly
				MatchedLayer ml = layersInType.get(0);
				mapping.setTableSourceId(destinationTable.getImmutableId(), dsList.size(), ml.source.getImmutableId());
				setFieldMapping(ml, destinationTable.getImmutableId(), mapping);
				dsList.add(wrapTableInDs(ml.source));

			} else if (nl > 1) {
				// union - we build individual adapters, then place in a union decorator which we add to the final adapter

				// build adapters for each layer
				ArrayList<ODLDatastore<? extends ODLTable>> dsListToUnion = new ArrayList<ODLDatastore<? extends ODLTable>>(nl);
				for (MatchedLayer ml : layersInType) {
					AdapterMapping singleSourceMapping = AdapterMapping.createUnassignedMapping(destinationTable);
					singleSourceMapping.setTableSourceId(destinationTable.getImmutableId(), 0, ml.source.getImmutableId());
					setFieldMapping(ml, destinationTable.getImmutableId(), singleSourceMapping);
					dsListToUnion.add(new AdaptedDecorator<ODLTable>(singleSourceMapping, wrapTableInDs(ml.source)));
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

	private static ODLTable getLayerSourceTable(Layer layer, Map<String, ODLTable> sourceTables, TableFinder finder, ExecutionReport report) {
		ODLTable source = null;
		String layerFormula = AdapterBuilderUtils.getFormulaFromText(layer.getSource());
		if (layerFormula != null) {
			// Process formula
		} else {
			// Standard input
			String fullname = VLSSourceDrawables.SOURCE_PREFIX + layer.getSource();
			source = sourceTables.get(fullname);
			if (source == null) {
				source = finder.buildValidated(fullname, VLSSourceDrawables.BEAN_MAPPING.getTableDefinition());
				if (report.isFailed()) {
					return null;
				}
				sourceTables.put(fullname, source);
			}
		}
		return source;
	}

	private static void setFieldMapping(MatchedLayer ml, int destinationTableId, AdapterMapping mapping) {
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
		private final FmLocalElement defaultValue;

		StyleFunction(MatchedLayer layer, int drawableColumn) {
			this.layer = layer;

			// try to find a corresponding style formula type
			OutputFormula found = null;
			for (OutputFormula ft : OutputFormula.values()) {
				if (ft.drawablesColumn == drawableColumn) {
					found = ft;
					break;
				}
			}
			this.styleFormulaType = found;

			this.defaultValue = new FmLocalElement(drawableColumn, "");
		}

		@Override
		public Object execute(FunctionParameters parameters) {

			// Apply style rules if we can
			if (styleFormulaType != null) {
				MatchedStyle style = null;
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

				if (style != null) {
					if (styleFormulaType.booleanToFlag != -1) {
						// flag mapping logic
						return processBoolToFlag(parameters, style);
					} else {
						return style.functions[styleFormulaType.ordinal()].execute(parameters);
					}
				}
			}

			return defaultValue.execute(parameters);
		}

		private Object processBoolToFlag(FunctionParameters parameters, MatchedStyle style) {
			Object val = style.functions[styleFormulaType.ordinal()].execute(parameters);
			if (val == Functions.EXECUTION_ERROR) {
				return Functions.EXECUTION_ERROR;
			}

			Object defaultVal = defaultValue.execute(parameters);
			if (defaultVal == Functions.EXECUTION_ERROR) {
				return Functions.EXECUTION_ERROR;
			}
			Long lDefault = Numbers.toLong(defaultVal);
			long ret = lDefault != null ? lDefault : 0;

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

		@Override
		public Function deepCopy() {
			throw new UnsupportedOperationException();
		}

	}


}
