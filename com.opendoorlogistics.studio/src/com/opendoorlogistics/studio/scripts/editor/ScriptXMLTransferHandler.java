/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.w3c.dom.Document;

import com.opendoorlogistics.core.scripts.io.XMLConversionHandler;
import com.opendoorlogistics.core.utils.XMLUtils;

public abstract class ScriptXMLTransferHandler extends TransferHandler{

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

	protected abstract XMLConversionHandler conversionHandler();

	protected abstract Object getSelected();

	protected abstract void pasteItem(Object o);

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		return conversionHandler()!=null;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public boolean importData(JComponent c, Transferable t) {
		if(conversionHandler()!=null){
			if (canImport(new TransferHandler.TransferSupport(c, t))) {

				DataFlavor flavor = DataFlavor.stringFlavor;
				if (t.isDataFlavorSupported(flavor)) {
					try {
						Object data = t.getTransferData(flavor);
						if (data instanceof String) {
							Document doc = XMLUtils.parse((String)data);
							if(doc!=null){
								List<Object> list = conversionHandler().fromXML(doc);
								if(list!=null){
									for(Object obj:list){
										pasteItem(obj);
									}
								}
							}
						}
					} catch (Throwable e) {
					}
				}

			}	
		}

		return false;
	}

	@Override
	public Transferable createTransferable(JComponent c) {
		if(conversionHandler()!=null && getSelected()!=null){
			Document doc = conversionHandler().toXML(getSelected());
			if(doc!=null){
				String s= XMLUtils.toString(doc, XMLUtils.getPrettyPrintFormat());
				return new StringSelection(s);					
			}
		}
		return null;
	}
}
