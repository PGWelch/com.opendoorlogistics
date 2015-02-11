package com.opendoorlogistics.core.scripts.formulae;

import java.util.ArrayList;

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
 * @author Phil
 *
 */
public class FmRuleLookup extends FunctionImpl {
	private final Object[] results;
	private final int nc;
	private final StandardisedCache standardisedCache = new StandardisedCache();
	private final String[] selectorFieldNames;
	
	private class CascadeNode {
		final int ruleNb;
		final int colIndx;
		final Object value;
		ArrayList<CascadeNode> children;
				
		CascadeNode(int ruleNb, int colIndx, Object value) {
			this.ruleNb = ruleNb;
			this.colIndx = colIndx;
			this.value = value;
		}

		@Override
		public String toString(){
			StringBuilder b = new StringBuilder();
			toString(b);
			return b.toString();
		}
		
		private void toString(StringBuilder b){
			if(colIndx>=0){
				b.append(Strings.repeat("\t", colIndx) + selectorFieldNames[colIndx] +" = " + value + (colIndx == nc-1? " (rule #" + ruleNb + ")":"") + System.lineSeparator());				
			}
			if(children!=null){
				for(CascadeNode n:children){
					n.toString(b);
				}
			}
		}
	}

	private final CascadeNode tree;

	private FmRuleLookup(Object[] results, int nc, String[] selectorFieldNames,CascadeNode tree) {
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

		// Build a lookup tree
		int n = table.getRowCount();
		results = new Object[n];
		tree = new CascadeNode(-1,-1,null);
		tree.children = new ArrayList<CascadeNode>();
		for (int rule = 0; rule < n; rule++) {

			CascadeNode parent = tree;
			for (int col = 0; col < nc; col++) {

				// get selector value and ensure zero length is treated as null
				Object s = table.getValueAt(rule, cols[col]);
				if (s != null && s.toString().length() == 0) {
					s = null;
				}

				// try to find the pre-existing node with this value
				int nc = parent.children != null ? parent.children.size() : 0;
				CascadeNode nextParent = null;
				for (int k = 0; k < nc; k++) {
					CascadeNode child = parent.children.get(k);
					if (ColumnValueProcessor.isEqual(s, child.value, standardisedCache)) {
						nextParent = child;
						break;
					}
				}

				// make a node if none exists, marking the rule number
				if (nextParent == null) {
					nextParent = new CascadeNode(rule, col, s);
					
					// add to parent
					if (parent.children == null) {
						parent.children = new ArrayList<CascadeNode>(3);
					}
					parent.children.add(nextParent);
				}

				parent = nextParent;
			}

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

	private void recurseMatch(Object[] values, CascadeNode node, int[] lowestRuleNumber) {

		// record the rule if we're on the last one and its lower than the
		// current lowest
		if (node.colIndx == nc - 1) {
			lowestRuleNumber[0] = Math.min(lowestRuleNumber[0], node.ruleNb);
		} else {
			// recurse to the next level if we find a null selector (which
			// matches all) or we have a match
			if (node.children != null) {
				int nchildren = node.children.size();
				for (int i = 0; i < nchildren; i++) {
					CascadeNode childNode = node.children.get(i);
					boolean recurse = childNode.value == null;
					if (!recurse) {
						recurse = ColumnValueProcessor.isEqual(values[childNode.colIndx], childNode.value, standardisedCache);
					}

					if (recurse) {
						recurseMatch(values,  childNode, lowestRuleNumber);
					}
				}
			}
		}
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		Object[] childexe = executeChildFormulae(parameters, false);
		if (childexe == null) {
			return Functions.EXECUTION_ERROR;
		}

		// Find all matching rules
		int[] lowestRuleNumber = new int[1];
		lowestRuleNumber[0] = Integer.MAX_VALUE;

		// Find the lowest matching rule number
		recurseMatch(childexe, tree, lowestRuleNumber);
		if (lowestRuleNumber[0] != Integer.MAX_VALUE) {
			return results[lowestRuleNumber[0]];
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
