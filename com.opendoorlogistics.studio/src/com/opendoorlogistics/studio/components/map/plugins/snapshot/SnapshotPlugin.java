package com.opendoorlogistics.studio.components.map.plugins.snapshot;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.standardcomponents.map.MapActionFactory;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.core.gis.map.SynchronousRenderer;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.studio.components.map.plugins.snapshot.CreateImageConfig.CaptureMode;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;
import com.opendoorlogistics.studio.dialogs.ProgressDialog;
import com.opendoorlogistics.studio.dialogs.ProgressDialog.OnFinishedSwingThreadCB;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class SnapshotPlugin implements MapPlugin {
	private static ExportImageConfig lastCreateImageConfig;
	@Override
	public void initMap(final MapApi api) {
		PluginUtils.registerActionFactory(api, new MapActionFactory() {
			
			@Override
			public Action create(MapApi api) {
				return createAction(api);
			}
		}, StandardMapMenuOrdering.SNAPSHOT,"snapshot");
	}

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.SnapshotPlugin";
	}

	
	private static Action createAction(final MapApi api) {
		return new SimpleAction("Take picture", "Take picture", "camera.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				Component mapComponent = api.getMapWindowComponent();
				Dimension mapSize = api.getMapWindowComponent().getSize();
				ExportImageConfig config = ExportImagePanel.showModal(mapComponent, lastCreateImageConfig, mapSize);
				if (config != null) {

					lastCreateImageConfig = config.deepCopy();

					final int width;
					final int height;
					final int zoom;
					final Point2D centre;
					CaptureMode mode = config.getCaptureMode();
					if (!mode.isCustomSize) {
						int izoom = api.getZoom() - mode.zoomDiff;
						izoom = Integer.min(izoom, api.getMaxZoom());
						izoom = Integer.max(izoom,api.getMinZoom());
						zoom = izoom;
						int zoomDiff = api.getZoom() - zoom;
						int diff = (int) Math.pow(2, zoomDiff);
						width = mapSize.width * diff;
						height = mapSize.height * diff;

						// adjust bitmap centre to match same lat-long with new zoom
						if (zoomDiff != 0) {
							LatLongToScreen converter = api.createImmutableConverter();
							LatLong ll = converter.getLongLat(mapSize.width/ 2, mapSize.height / 2);
							centre = api.getWorldBitmapPosition(ll, zoom);
						} else {
							centre = api.getWorldBitmapMapCentre();
						}
					} else {
						centre =api.getWorldBitmapMapCentre();
						width = config.getWidth();
						height = config.getHeight();
						zoom =api.getZoom();
					}

					final ProgressDialog<BufferedImage> pd = new ProgressDialog<>((JFrame) SwingUtilities.getWindowAncestor(mapComponent), "Creating snapshot...", false, false);
					pd.setLocationRelativeTo(mapComponent);
					// pd.setText("Creating snapshot...");
					pd.start(new Callable<BufferedImage>() {

						@Override
						public BufferedImage call() throws Exception {
							LinkedList<DrawableObject> list = PluginUtils.getVisibleDrawables(api);
							BufferedImage image = SynchronousRenderer.singleton()
									.drawAtBitmapCoordCentre(centre, width, height, zoom, api.getRenderFlags(),list).getFirst();

							return image;
						}
					}, new OnFinishedSwingThreadCB<BufferedImage>() {

						@Override
						public void onFinished(BufferedImage result, boolean userCancelled, boolean userFinishedNow) {
							try {
								ProcessCreateImage.process(result, config, api.getControlLauncherApi());
							} catch (Exception e2) {
								JOptionPane.showMessageDialog(mapComponent, "An error occurred when creating or saving the image");
							}

						}
					});

				}

			}

		};
	}


}
