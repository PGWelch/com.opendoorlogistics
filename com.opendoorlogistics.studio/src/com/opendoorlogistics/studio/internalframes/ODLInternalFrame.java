/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.internalframes;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.PreferencesManager;
import com.opendoorlogistics.studio.utils.WindowState;

public class ODLInternalFrame extends JInternalFrame {
	private final String positioningId;
	private boolean isDisposed=false;
	private FramesChangedListener framesChangedListener;
	
	public ODLInternalFrame(String positioningId) {
		this.positioningId = positioningId;
		setIconifiable(true);
		setMaximizable(true);
		setResizable(true);
		setClosable(true);
	}

	@Override
	public void dispose(){
		if(!isDisposed){
			isDisposed = true;
			super.dispose();
			
			// save positioning information
			if(Strings.isEmpty(positioningId)==false && isIcon()==false){
				Rectangle bounds = getBounds();
				PreferencesManager.getSingleton().setWindowState(positioningId, new WindowState(bounds, -1));
			}	
			
			fireChangedListener();
		}

	}
	
	@Override
	public void setTitle(String s){
		super.setTitle(s);
		
		fireChangedListener();
		
	}

	private void fireChangedListener() {
		if(framesChangedListener!=null){
			framesChangedListener.internalFrameChange(this);
		}
	}
	
	@Override
    public void show() {
		super.show();
		fireChangedListener();
    }
    
	public boolean isDisposed(){
		return isDisposed;
	}
	
	public boolean placeInLastPosition(Rectangle viewportBounds){
		if(Strings.isEmpty(positioningId)==false){
			WindowState state = PreferencesManager.getSingleton().getWindowState(positioningId);
			if(state!=null){
				Rectangle bounds = state.getBounds();
				if(viewportBounds.intersects(bounds)){
					setBounds(bounds);
					return true;
				}
			}
		}
		return false;
	}
	
	


	public static void showInDummyDesktopPane(final ODLInternalFrame frame) {
		SwingUtils.invokeLaterOnEDT(new Runnable() {
			public void run() {
				try {
					InitialiseStudio.initialise(false);
					JFrame outer = new JFrame();
					outer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					JDesktopPane pane = new JDesktopPane();
					pane.add(frame);
					frame.setVisible(true);
					outer.setContentPane(pane);
					outer.setVisible(true);
					frame.toFront();
					outer.setMinimumSize(new Dimension(600, 600));
					outer.pack();

					// frame doesn't appear without a set bounds...
					frame.setBounds(50, 50, 200,200);
					// frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
					// frame.getFrame().setVisible(true);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public interface FramesChangedListener{
		void internalFrameChange(ODLInternalFrame f);
	}
	
	public void setChangedListener(FramesChangedListener listener){
		this.framesChangedListener = listener;
	}
	
}
