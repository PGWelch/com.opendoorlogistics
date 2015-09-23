package com.opendoorlogistics.core.scripts.formulae.rules;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.scripts.TableReference;
import com.opendoorlogistics.core.scripts.execution.adapters.IndexedDatastores;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedCache;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * A rule lookup is built from an input table which is comprised of one or more
 * selector fields and one result field. Each row in the table is a rule and
 * lower-index rules have higher priority. An input list of values (one per
 * selector field) is given to the rule lookup. If a selector field is null it
 * matches to anything. If a selector field is not null then the corresponding
 * input value must match it. The return value from the lowest index rule is
 * then selected for the input list. This function assumes the input table is
 * not modified after building.
 * 
 * A rule lookup object is therefore built to return results from only a single
 * column in a rule lookup table.
 * @author Phil
 *
 */
public class FmRuleLookup extends FunctionImpl {
	private final Object[] results;
	private final int nc;
	private final String[] selectorFieldNames;
	
	private final RuleNode tree;

	private FmRuleLookup(Object[] results, int nc, String[] selectorFieldNames,RuleNode tree) {
		this.results = results;
		this.nc = nc;
		this.tree = tree;
		this.selectorFieldNames = selectorFieldNames;
	}
	
	public FmRuleLookup(Function[] inputSelectors, String[] selectorFieldNames, ODLTableReadOnly table, String returnField) {
		super(inputSelectors);
		this.selectorFieldNames = selectorFieldNames;

		// Match the columns
		nc = selectorFieldNames.length;
		int[] cols = new int[nc];
		for (int i = 0; i < nc; i++) {
			cols[i] = identifyFieldname(table, selectorFieldNames[i]);
		}

		// Also for results
		int resultCol = identifyFieldname(table, returnField);

		// get matrix of selector values
		int n = table.getRowCount();
		List<List<Object>> selectorsMatrix = new ArrayList<List<Object>>();
		for (int rule = 0; rule < n; rule++) {
			List<Object> selectors4Rule = new ArrayList<Object>();
			selectorsMatrix.add(selectors4Rule);
			for (int col = 0; col < nc; col++) {
				Object s = table.getValueAt(rule, cols[col]);
				selectors4Rule.add(s);
			}
		}
		
		// Build a lookup tree
		results = new Object[n];
		RuleNode baseNode =RuleNode.buildTree(selectorsMatrix);
		
		tree = baseNode;
		
		// save results
		for (int rule = 0; rule < n; rule++) {
			results[rule] = table.getValueAt(rule, resultCol);
		}
	}



	private int identifyFieldname(ODLTableReadOnly table, String fieldname) {
		int col = TableUtils.findColumnIndx(table, fieldname);
		if (col == -1) {
			throw new RuntimeException("Cannot identify column " + fieldname + " in table " + table.getName() + " needed by cascading lookup.");
		}
		return col;
	}



	@Override
	public Object execute(FunctionParameters parameters) {
		Object[] childexe = executeChildFormulae(parameters, false);
		if (childexe == null) {
			return Functions.EXECUTION_ERROR;
		}

		int ruleNb = tree.findRuleNumber(childexe); 
		if (ruleNb != -1) {
			return results[ruleNb];
		}

		return null;

	}



	@Override
	public Function deepCopy() {
		// Return a new object giving it the immutable internal data 
		return new FmRuleLookup(results, nc,selectorFieldNames, tree);
	}

	public static void buildRuleLookup(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTable> datastores,final int defaultDatastoreIndex, final ExecutionReport result) {
		for (int lookupsize = 1; lookupsize <= 7; lookupsize++) {
			FunctionDefinition dfn = new FunctionDefinition("rulelookup");
			dfn.setGroup("rulelookup");
			dfn.setDescription("A rule lookup is built from an input table which is comprised of one or more selector fields and one result field."
					+ " Each row in the table is a rule and lower-index rules have higher priority."
					+ " An input list of values (one per selector field) is given to the rule lookup." + "If a selector field is null it matches to anything. "
					+ " If a selector field is not null then the corresponding input value must match it."
					+ " The return value from the lowest index rule is then selected for the input list.");

			for (int i = 0; i < lookupsize; i++) {
				dfn.addArg("SearchValue" + (i + 1));
			}
			final int tr = dfn.addArg("TableReference", ArgumentType.TABLE_REFERENCE_CONSTANT);

			final int[] lookupFieldNames = new int[lookupsize];
			for (int i = 0; i < lookupsize; i++) {
				lookupFieldNames[i] = dfn.addArg("LookupFieldName" + (i + 1), ArgumentType.STRING_CONSTANT);
			}

			final int retField = dfn.addArg("ReturnField", ArgumentType.STRING_CONSTANT);

			dfn.setFactory(new FunctionFactory() {

				@Override
				public Function createFunction(Function... children) {
					// parse the table reference.. (format is already validated)
					String sTableRef = FunctionUtils.getConstantString(children[tr]);
					TableReference tableRef = TableReference.create(sTableRef, result);
					if (tableRef == null) {
						result.setFailed("Error reading table reference in formula rulelookup.");
						return null;
					}

					// find the datastore index ...
					int dsIndx = defaultDatastoreIndex;
					if (Strings.isEmpty(tableRef.getDatastoreName()) == false) {	
						dsIndx = datastores.getIndex(tableRef.getDatastoreName());
						if (dsIndx == -1) {
							result.setFailed("Error getting datastore " + tableRef.getDatastoreName() + " used in formula rulelookup.");
							return null;
						}				
					}
					
					// Then the datastore
					ODLDatastore<? extends ODLTable> ds = datastores.getDatastore(dsIndx);
					if (ds == null || result.isFailed()) {
						result.setFailed("Error getting datastore " + tableRef + " used in formula rulelookup.");
						return null;
					}
					
					// Then the table
					ODLTableReadOnly table = TableUtils.findTable(ds, tableRef.getTableName());
					if (table == null) {
						result.setFailed("Error getting table " + tableRef + " used in formula rulelookup.");
						return null;
					}

					// get column names and input functions
					String[] colNames = new String[lookupFieldNames.length];
					Function[] funcs = new Function[lookupFieldNames.length];
					for (int i = 0; i < colNames.length; i++) {
						colNames[i] = FunctionUtils.getConstantString(children[lookupFieldNames[i]]);
						funcs[i] = children[i];
					}

					// get return field name
					String retName = FunctionUtils.getConstantString(children[retField]);

					// get functions in an array
					return new FmRuleLookup(funcs, colNames, table, retName);
				}
			});

			library.add(dfn);
		}
	}
}
