/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map.plugins.snapshot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ProcessCreateImage {
	public static void process(BufferedImage image, ExportImageConfig config, ComponentControlLauncherApi controlLauncher) {

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
		
		if (config.isToFile() && Strings.isEmptyWhenStandardised(config.getFilename())==false) {
			File outputfile = new File(config.getFilename());
			try {
				ImageIO.write(image, config.getImageType().name().toLowerCase(), outputfile);
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}
		
		if(config.isToViewer() && controlLauncher!=null){
			String title = "Snapshot " + new ODLTime().toString();
			JPanel imgPanel = ImageUtils.createImagePanel(image, Color.WHITE);
			
			class MyPanel extends JPanel implements Disposable{

				@Override
				public void dispose() {
					// TODO Auto-generated method stub
					
				}
				
			}
			MyPanel panel = new MyPanel();
			panel.setLayout(new BorderLayout());
			panel.add(new JScrollPane(imgPanel), BorderLayout.CENTER);
			
			// don't make the panel too big if the image is huge
			int buffer =10;
			int maxSize = 400 + buffer;
			Dimension prefSize = new Dimension(Math.min(image.getWidth() + buffer, maxSize), Math.min(image.getHeight()+ buffer,maxSize));
			panel.setPreferredSize(prefSize);
			controlLauncher.registerPanel(UUID.randomUUID().toString(), title,panel , false);
		}

	}
}
