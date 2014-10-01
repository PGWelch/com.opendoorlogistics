
package com.opendoorlogistics.codefromweb;

import java.awt.Rectangle;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

public class TileInternalFrames{

	public static void tile(JDesktopPane desktopPane) {
		tile(desktopPane, desktopPane.getAllFrames());
	}
	
	public static void tile(JDesktopPane desktopPane, JInternalFrame[] frames) {
		if(frames.length==0){
			return;
		}
		
		Rectangle dBounds = desktopPane.getBounds();

		int rows = (int) Math.sqrt(frames.length);
		int cols = (int) (Math.ceil(((double) frames.length) / rows));
		int lastRow = frames.length - cols * (rows - 1);
		int width;
		int height;

		if (lastRow == 0) {
			rows--;
			height = dBounds.height / rows;
		} else {
			height = dBounds.height / rows;
			if (lastRow < cols) {
				rows--;
				width = dBounds.width / lastRow;
				for (int i = 0; i < lastRow; i++) {
					frames[cols * rows + i].setBounds(i * width, rows * height, width, height);
				}
			}
		}

		width = dBounds.width / cols;
		for (int j = 0; j < rows; j++) {
			for (int i = 0; i < cols; i++) {
				int frameIndx = i + j * cols;
				try {
					if (frames[frameIndx].isMaximum()) {
						frames[frameIndx].setMaximum(false);
					}
					frames[frameIndx].setIcon(false);
				} catch (Throwable e) {
					// TODO: handle exception
				}

				frames[frameIndx].setBounds(i * width, j * height, width, height);
			}
		}
	}

}
