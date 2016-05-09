package com.opendoorlogistics.graphhopper;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.CmdArgs;

public class BuildGraphhopperFile {

	public static void main(String[] strArgs) throws Exception{
//		GHServer.main(strArgs);
		CmdArgs args = CmdArgs.read(strArgs);
        args = CmdArgs.readFromConfigAndMerge(args, "config", "graphhopper.config");
        GraphHopper hopper = new GraphHopper().forDesktop().init(args);
        hopper.importOrLoad();
        hopper.close();
	}

}
