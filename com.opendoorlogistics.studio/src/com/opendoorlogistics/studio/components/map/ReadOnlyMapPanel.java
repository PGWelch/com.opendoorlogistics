/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.SynchronousRenderer;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.utils.image.ExportImageConfig;
import com.opendoorlogistics.utils.image.ExportImagePanel;
import com.opendoorlogistics.utils.image.ProcessCreateImage;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class ReadOnlyMapPanel extends JPanel implements Disposable {
	/**
	 * We keep a static reference to the last image created configuration so this is pre-populated the next time the user runs it...
	 * 
	 */
	private static ExportImageConfig lastCreateImageConfig;
	protected final ReadOnlyMapControl map;
	protected final List<Action> actions;
	
	public void setDrawables(Iterable<? extends DrawableObject> pnts){
		map.setDrawables(pnts);
	}
	
	public void zoomBestFit() {
		map.zoomBestFit();
	}
	
	public ReadOnlyMapPanel(MapConfig config,List<? extends DrawableObject> pnts) {
		this.map = new ReadOnlyMapControl(config);
		map.setDrawables(pnts);
		actions = createActions();
		initLayout();
	}
	
	public ReadOnlyMapControl getMapControl(){
		return map;
	}

	private void initLayout() {
		setLayout(new BorderLayout());
		add(createToolbar(), BorderLayout.NORTH);
		add(map, BorderLayout.CENTER);
		createPopupMenu();
		
		// hack ... read only map panel is appearing with zero size
		// so we set a preferred size..
		map.setPreferredSize( new Dimension(400, 400));
	}

	protected void createPopupMenu() {
		// right-click popup menu
		final JPopupMenu popup = new JPopupMenu();
		for (Action action : actions) {
			if (action != null) {
				popup.add(action);
			} else {
				popup.addSeparator();
			}
		}
		map.addMouseListener(new PopupMenuMouseAdapter() {
			
			@Override
			protected void launchMenu(MouseEvent me) {
				popup.show(me.getComponent(), me.getX(), me.getY());	
			}
		});
	}

//	private void setDefaultSize() {
//		setPreferredSize(new Dimension(600, 600));
//	}

	public ReadOnlyMapPanel(ReadOnlyMapControl map,boolean initLayout){
		this.map = map;
		actions = createActions();
		if(initLayout){
			initLayout();
		}
	}
	
	
	protected JToolBar createToolbar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		for(Action action : actions){
			if(action!=null){
				toolBar.add(action);				
			}else{
				toolBar.addSeparator();
			}
		}

		class RenderBox extends JCheckBox{

			public RenderBox(String name, String tooltip,final long flag) {
				super(name);
				setToolTipText(tooltip);
				setSelected(map.getRenderFlags().hasFlag(flag));
				addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						map.getRenderFlags().setFlag(flag, isSelected());
						map.repaint();
					}
				});
			}
		}
		
		toolBar.addSeparator();
		toolBar.add(new RenderBox("Text", "Toggle drawing of text on and off", RenderProperties.SHOW_TEXT));
		toolBar.addSeparator();
		toolBar.add(new RenderBox("Map", "Toggle drawing of background map on and off", RenderProperties.SHOW_BACKGROUND));
		return toolBar;
	}

	protected List<Action> createActions(){
		List<Action> ret = new ArrayList<>();	
		createZoomActions(ret);
		createPanActions(ret);
		createPictureAction(ret);	
		createLegendAction(ret);
		return ret;
	}

	protected void createLegendAction(Collection<Action> ret) {
		ret.add(new SimpleAction("Show legend", "Show legend", "legend-16x16.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				map.showLegend();
				//	JOptionPane.showMessageDialog(ReadOnlyMapPanel.this, "No legend available. Configure the legend by setting the legend key field in the map adapter.");					
				
			}

		});
	}

	protected void createPictureAction(Collection<Action> ret) {
		ret.add(new SimpleAction("Take picture", "Take picture", "camera.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				ExportImageConfig config = ExportImagePanel.showModal(ReadOnlyMapPanel.this, lastCreateImageConfig);
				if (config != null) {
					try {
						lastCreateImageConfig = config.deepCopy();
						BufferedImage image = SynchronousRenderer.singleton().drawAtBitmapCoordCentre(map.getCenter(), config.getWidth(), config.getHeight(),
								map.getZoom(),map.getRenderFlags().getFlags(), map.getDrawables()).getFirst();
						ProcessCreateImage.process(image, config);

					} catch (Throwable e2) {
						JOptionPane.showMessageDialog(ReadOnlyMapPanel.this, "An error occurred when creating or saving the image");
					}
				}

			}

		});
	}

	protected void createZoomActions(Collection<Action> ret) {
		ret.add( new SimpleAction("Zoom to all", "Zoom to all", "small_zoom_best_fit.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				map.zoomBestFit();
			}
		});

		Action zoomIn= new SimpleAction("Zoom in", "Zoom in", "zoom-in-3.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				map.setZoom(map.getZoom() - 1);
			}
		};
		//registerActionKeyEvent(zoomIn, KeyEvent.VK_PLUS);
		ret.add(zoomIn);
		
		Action zoomOut = new SimpleAction("Zoom out", "Zoom out", "zoom-out-3.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				map.setZoom(map.getZoom() + 1);
			}
		};
	//	registerActionKeyEvent(zoomOut, KeyEvent.VK_MINUS);				
		ret.add(zoomOut);
	}

	protected void createPanActions(Collection<Action> ret) {
		class PanAction extends SimpleAction{
			double x;
			double y;
			
			PanAction(String name, String tooltip, String smallIconPng, double x, double y, int keyEvent) {
				super(name, tooltip, smallIconPng);
				this.x = x;
				this.y = y;
				
				// register myself in action map
				registerActionKeyEvent(this, keyEvent);
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Rectangle bounds = map.getViewportBounds();
				double dx =x * bounds.width;
				double dy =y * bounds.height;
				Point2D centre = map.getCenter();
				centre = new Point2D.Double(centre.getX() + dx, centre.getY() + dy);
				map.setCenter(centre);
			}
		}
		final double pan = 0.25;
		ret.add( new PanAction("Pan left", "Pan left", "arrow-left-3-16x16.png" , -pan,0, KeyEvent.VK_LEFT)) ;
		ret.add( new PanAction("Pan up", "Pan up", "arrow-up-3-16x16.png" ,0 ,-pan, KeyEvent.VK_UP)) ;
		ret.add( new PanAction("Pan down", "Pan down", "arrow-down-3-16x16.png" ,0 ,pan, KeyEvent.VK_DOWN)) ;
		ret.add( new PanAction("Pan right", "Pan right", "arrow-right-3-16x16.png" , pan,0,KeyEvent.VK_RIGHT)) ;
	}

	@Override
	public void dispose() {
		map.dispose();
	}

	protected void registerActionKeyEvent(Action action, int keyEvent) {
		if(map!=null){
			String actionName = action.getValue(Action.NAME).toString();
			for (int imap : new int[] { JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_FOCUSED, JComponent.WHEN_IN_FOCUSED_WINDOW }) {
				KeyStroke stroke = KeyStroke.getKeyStroke(keyEvent, 0);
				map.getInputMap(imap).put(stroke, actionName);
			}
			map.getActionMap().put(actionName, action);
		}
	}
}
