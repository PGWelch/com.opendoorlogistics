
package com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.input;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.JXMapViewer;

/**
 * zooms using the mouse wheel on the view center
 * @author joshy
 */
public class ZoomMouseWheelListenerCenter implements MouseWheelListener
{
	private JXMapViewer viewer;
	
	/**
	 * @param viewer the jxmapviewer
	 */
	public ZoomMouseWheelListenerCenter(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		viewer.setZoom(viewer.getZoom() + e.getWheelRotation());
	}
}
