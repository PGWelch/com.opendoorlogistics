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
import java.util.concurrent.Callable;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.SynchronousRenderer;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.studio.components.map.snapshot.CreateImageConfig.CaptureMode;
import com.opendoorlogistics.studio.components.map.snapshot.ExportImageConfig;
import com.opendoorlogistics.studio.components.map.snapshot.ExportImagePanel;
import com.opendoorlogistics.studio.components.map.snapshot.ProcessCreateImage;
import com.opendoorlogistics.studio.controls.ODLScrollableToolbar;
import com.opendoorlogistics.studio.dialogs.ProgressDialog;
import com.opendoorlogistics.studio.dialogs.ProgressDialog.OnFinishedSwingThreadCB;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class ReadOnlyMapPanel extends JPanel implements Disposable {
	/**
	 * We keep a static reference to the last image created configuration so
	 * this is pre-populated the next time the user runs it...
	 * 
	 */
	private static ExportImageConfig lastCreateImageConfig;
	protected final ReadOnlyMapControl map;
	protected final List<Action> actions;
	protected final ComponentControlLauncherApi hasInternalFrames;

	public void setDrawables(LayeredDrawables pnts) {
		map.setDrawables(pnts);
	}

	public void zoomBestFit() {
		map.zoomBestFit();
	}

	public ReadOnlyMapPanel(MapConfig config, MapModePermissions permissions, LayeredDrawables pnts, ComponentControlLauncherApi hasInternalFrames) {
		this.map = new ReadOnlyMapControl(config, permissions);
		this.hasInternalFrames = hasInternalFrames;
		map.setDrawables(pnts);
		actions = createActions();
		initLayout();
	}

	public ReadOnlyMapControl getMapControl() {
		return map;
	}

	private void initLayout() {
		setLayout(new BorderLayout());
		add(createToolbar(), BorderLayout.NORTH);
		add(map, BorderLayout.CENTER);
		createPopupMenu();

		// hack ... read only map panel is appearing with zero size
		// so we set a preferred size..
		map.setPreferredSize(new Dimension(400, 400));
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

	// private void setDefaultSize() {
	// setPreferredSize(new Dimension(600, 600));
	// }

	public ReadOnlyMapPanel(ReadOnlyMapControl map, boolean initLayout, ComponentControlLauncherApi hasInternalFrames) {
		this.map = map;
		this.hasInternalFrames = hasInternalFrames;
		actions = createActions();
		if (initLayout) {
			initLayout();
		}
	}

	protected JToolBar createToolbar() {
		JToolBar toolBar = new ODLScrollableToolbar().getToolBar();
		toolBar.setFloatable(false);

		for (Action action : actions) {
			if (action != null) {
				toolBar.add(action);
			} else {
				toolBar.addSeparator();
			}
		}

		class RenderBox extends JCheckBox {

			public RenderBox(String name, String tooltip, final long flag) {
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

	protected List<Action> createActions() {
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
				// JOptionPane.showMessageDialog(ReadOnlyMapPanel.this,
				// "No legend available. Configure the legend by setting the legend key field in the map adapter.");

			}

		});
	}

	protected void createPictureAction(Collection<Action> ret) {
		ret.add(new SimpleAction("Take picture", "Take picture", "camera.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				ExportImageConfig config = ExportImagePanel.showModal(ReadOnlyMapPanel.this, lastCreateImageConfig, map.getSize());
				if (config != null) {

					lastCreateImageConfig = config.deepCopy();

					final int width;
					final int height;
					final int zoom;
					final Point2D centre;
					CaptureMode mode = config.getCaptureMode();
					if (!mode.isCustomSize) {
						int izoom = map.getZoom() - mode.zoomDiff;
						izoom = Integer.min(izoom, map.getTileFactory().getInfo().getMaximumZoomLevel());
						izoom = Integer.max(izoom, map.getTileFactory().getInfo().getMinimumZoomLevel());
						zoom = izoom;
						int zoomDiff = map.getZoom() - zoom;
						int diff = (int) Math.pow(2, zoomDiff);
						width = map.getSize().width * diff;
						height = map.getSize().height * diff;

						// adjust bitmap centre to match same lat-long with new
						// zoom
						if (zoomDiff != 0) {
							LatLongToScreen converter = map.createImmutableConverter();
							LatLong ll = converter.getLongLat(map.getWidth() / 2, map.getHeight() / 2);
							centre = map.getTileFactory().geoToPixel(new GeoPosition(ll.getLatitude(), ll.getLongitude()), zoom);
						} else {
							centre = map.getCenter();
						}
					} else {
						centre = map.getCenter();
						width = config.getWidth();
						height = config.getHeight();
						zoom = map.getZoom();
					}

					final ProgressDialog<BufferedImage> pd = new ProgressDialog<>((JFrame) SwingUtilities.getWindowAncestor(ReadOnlyMapPanel.this), "Creating snapshot...", false, false);
					pd.setLocationRelativeTo(ReadOnlyMapPanel.this);
					// pd.setText("Creating snapshot...");
					pd.start(new Callable<BufferedImage>() {

						@Override
						public BufferedImage call() throws Exception {
							BufferedImage image = SynchronousRenderer.singleton()
									.drawAtBitmapCoordCentre(centre, width, height, zoom, map.getRenderFlags().getFlags(), map.getVisibleDrawables(true, true, true)).getFirst();

							return image;
						}
					}, new OnFinishedSwingThreadCB<BufferedImage>() {

						@Override
						public void onFinished(BufferedImage result, boolean userCancelled, boolean userFinishedNow) {
							try {
								ProcessCreateImage.process(result, config, hasInternalFrames);
							} catch (Exception e2) {
								JOptionPane.showMessageDialog(ReadOnlyMapPanel.this, "An error occurred when creating or saving the image");
							}

						}
					});

				}

			}

		});
	}

	protected void createZoomActions(Collection<Action> ret) {
		ret.add(new SimpleAction("Zoom to all", "Zoom to all", "small_zoom_best_fit.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				map.zoomBestFit();
			}
		});

		Action zoomIn = new SimpleAction("Zoom in", "Zoom in", "zoom-in-3.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				map.setZoom(map.getZoom() - 1);
			}
		};
		// registerActionKeyEvent(zoomIn, KeyEvent.VK_PLUS);
		ret.add(zoomIn);

		Action zoomOut = new SimpleAction("Zoom out", "Zoom out", "zoom-out-3.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				map.setZoom(map.getZoom() + 1);
			}
		};
		// registerActionKeyEvent(zoomOut, KeyEvent.VK_MINUS);
		ret.add(zoomOut);
	}

	protected void createPanActions(Collection<Action> ret) {
		class PanAction extends SimpleAction {
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
				double dx = x * bounds.width;
				double dy = y * bounds.height;
				Point2D centre = map.getCenter();
				centre = new Point2D.Double(centre.getX() + dx, centre.getY() + dy);
				map.setCenter(centre);
			}
		}
		final double pan = 0.25;
		ret.add(new PanAction("Pan left", "Pan left", "arrow-left-3-16x16.png", -pan, 0, KeyEvent.VK_LEFT));
		ret.add(new PanAction("Pan up", "Pan up", "arrow-up-3-16x16.png", 0, -pan, KeyEvent.VK_UP));
		ret.add(new PanAction("Pan down", "Pan down", "arrow-down-3-16x16.png", 0, pan, KeyEvent.VK_DOWN));
		ret.add(new PanAction("Pan right", "Pan right", "arrow-right-3-16x16.png", pan, 0, KeyEvent.VK_RIGHT));
	}

	@Override
	public void dispose() {
		map.dispose();
	}

	protected void registerActionKeyEvent(Action action, int keyEvent) {
		if (map != null) {
			String actionName = action.getValue(Action.NAME).toString();
			for (int imap : new int[] { JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_FOCUSED, JComponent.WHEN_IN_FOCUSED_WINDOW }) {
				KeyStroke stroke = KeyStroke.getKeyStroke(keyEvent, 0);
				map.getInputMap(imap).put(stroke, actionName);
			}
			map.getActionMap().put(actionName, action);
		}
	}

	public MapModePermissions getPermissions() {
		return map.getPermissions();
	}
}
