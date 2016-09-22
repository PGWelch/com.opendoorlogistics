package com.opendoorlogistics.speedregions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.opendoorlogistics.speedregions.beans.SpatialTreeNode;
import com.opendoorlogistics.speedregions.beans.RegionLookupBean;
import com.opendoorlogistics.speedregions.beans.SpeedRules;
import com.opendoorlogistics.speedregions.processor.SpatialTreeStats;
import com.opendoorlogistics.speedregions.processor.RegionProcessorUtils;
import com.opendoorlogistics.speedregions.processor.SpeedRulesFilesProcesser;

public class SpeedRegionBeanBuilder {
	private static final Logger LOGGER = Logger.getLogger(SpeedRegionBeanBuilder.class.getName());

	/**
	 * Build a lookup bean, which can be serialised to JSON
	 * @param files
	 * @param minDiagonalLengthMetres
	 * @return
	 */
	public static RegionLookupBean buildBeanFromSpeedRulesObjs(List<SpeedRules> files, double minDiagonalLengthMetres) {
		SpeedRulesFilesProcesser processer = new SpeedRulesFilesProcesser();
		final SpatialTreeNode root=processer.buildQuadtree(files, RegionProcessorUtils.newGeomFactory(), minDiagonalLengthMetres);
		LOGGER.info("Built quadtree: " + SpatialTreeStats.build(root).toString());
		
		RegionLookupBean built = new RegionLookupBean();
		built.setQuadtree(root);
		built.setRules(processer.validateApplyCountryCodes(files));
		return built;
	}
	
	public static RegionLookupBean buildBeanFromSpeedRulesFiles(List<File> files ,double minDiagonalLengthMetres){
		return buildBeanFromSpeedRulesObjs(loadSpeedRulesFiles(files), minDiagonalLengthMetres);
	}
	
	public static RegionLookupBean loadBean(File file){
		return RegionProcessorUtils.fromJSON(file, RegionLookupBean.class);
	}
	
	public static void saveBean(RegionLookupBean bean,File file){
		RegionProcessorUtils.toJSONFile(bean, file);
	}

	private static List<SpeedRules> loadSpeedRulesFiles(List<File> files) {
		ArrayList<SpeedRules> objects = new ArrayList<SpeedRules>(files.size());
		for(File file : files){
			objects.add(RegionProcessorUtils.fromJSON(file, SpeedRules.class));
		}
		return objects;
	}
	
	

}
