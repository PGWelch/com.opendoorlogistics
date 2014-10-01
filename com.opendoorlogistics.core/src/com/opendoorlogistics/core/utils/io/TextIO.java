/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.io;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

import au.com.bytecode.opencsv.CSVReader;

final public class TextIO {
	private final static String DEFAULT_COLUMN_NAME = "Column";

	public static ODLDatastoreAlterable<ODLTableAlterable> importCSV(File file) {
		try {
			CSVReader reader = new CSVReader(new FileReader(file));
			return importFile(reader, getTableName(file));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	private static String getTableName(File file) {
		String tableName = FilenameUtils.getBaseName(file.getAbsolutePath());
		if (Strings.isEmptyWhenStandardised(tableName)) {
			tableName = "Table";
		}
		return tableName;
	}

	public static ODLDatastoreAlterable<ODLTableAlterable> importTabbed(File file) {
		try {
			CSVReader reader = new CSVReader(new FileReader(file), '\t');
			return importFile(reader, getTableName(file));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	private static ODLDatastoreAlterable<ODLTableAlterable> importFile(CSVReader reader, String tableName) {
		try {
			List<String[]> list = reader.readAll();
			int n = list.size();
			if (n > 0) {
				ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
				ODLTableAlterable table = ret.createTable(tableName, -1);

				// create all columns
				for (String col : list.get(0)) {
					// give default name if invalid
					if (col == null || Strings.isEmptyWhenStandardised(col) || TableUtils.findColumnIndx(table, col, true) != -1) {
						col = TableUtils.getUniqueNumberedColumnName(DEFAULT_COLUMN_NAME, table);
					}

					table.addColumn(-1,col, ODLColumnType.STRING, 0);
				}

				for (int line = 1; line < n; line++) {
					String[] row = list.get(line);
					if (row.length != table.getColumnCount()) {
						throw new RuntimeException("Line found with different number of columns to header line: " + line);
					}

					int rowIndx = table.createEmptyRow(line);
					for (int col = 0; col < row.length; col++) {
						table.setValueAt(row[col], rowIndx, col);
					}
				}
				return ret;
			}
			return null;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
