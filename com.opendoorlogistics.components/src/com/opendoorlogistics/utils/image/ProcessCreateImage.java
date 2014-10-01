/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.image;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

final public class ProcessCreateImage {
	public static void process(BufferedImage image, ExportImageConfig config) {

		if (config.isToClipboard()) {
			class ImageTransferable implements Transferable {
				private Image image;

				public ImageTransferable(Image image) {
					this.image = image;
				}

				public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
					if (isDataFlavorSupported(flavor)) {
						return image;
					} else {
						throw new UnsupportedFlavorException(flavor);
					}
				}

				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return flavor == DataFlavor.imageFlavor;
				}

				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[] { DataFlavor.imageFlavor };
				}
			}

			ImageTransferable transferable = new ImageTransferable( image );
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);

		}
		
		if (config.isToFile()) {
			File outputfile = new File(config.getFilename());
			try {
				ImageIO.write(image, config.getImageType().name().toLowerCase(), outputfile);
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}

	}
}
