package com.opendoorlogistics.api.app;

import com.opendoorlogistics.api.app.ui.ActionBuilder;

/**
 * Listener called when initialising the app
 * @author Phil
 *
 */
public interface ODLAppInitListener {
	void onBuildFileActions(ODLApp app,ActionBuilder builder);
	void onBuildEditActions(ODLApp app,ActionBuilder builder);

}
