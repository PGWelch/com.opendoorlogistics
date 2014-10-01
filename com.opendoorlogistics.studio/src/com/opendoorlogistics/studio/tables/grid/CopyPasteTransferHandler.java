/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
/*
 * Copyright 2013 Japplis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opendoorlogistics.studio.tables.grid;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

final class CopyPasteTransferHandler extends TransferHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GridTable spreadsheetTable;

	CopyPasteTransferHandler(GridTable spreadsheetTable) {
		super();
		this.spreadsheetTable = spreadsheetTable;
	}

	@Override
	public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
		Transferable c = createTransferable(comp);
		clip.setContents(c, null);
		exportDone(comp, c, action);
	}

	@Override
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		return true;
	}

	@Override
	public boolean importData(JComponent c, Transferable t) {
		if (canImport(new TransferHandler.TransferSupport(c, t))) {

			DataFlavor flavor = DataFlavor.stringFlavor;
			if (t.isDataFlavorSupported(flavor)) {
				try {
					Object data = t.getTransferData(flavor);
					if (data instanceof String) {
						spreadsheetTable.pasteTabbedText((String) data);
					}
				} catch (UnsupportedFlavorException | IOException ex) {
				}
			}

		}
		return false;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		return new StringSelection(spreadsheetTable.getSelectedAsTabbedText());
	}

}
