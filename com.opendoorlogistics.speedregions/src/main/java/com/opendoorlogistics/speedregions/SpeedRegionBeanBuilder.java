package com.opendoorlogistics.speedregions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.speedregions.beans.QuadtreeNode;
import com.opendoorlogistics.speedregions.beans.RegionLookupBean;
import com.opendoorlogistics.speedregions.beans.SpeedRules;
import com.opendoorlogistics.speedregions.processor.ProcessorUtils;
import com.opendoorlogistics.speedregions.processor.SpeedRulesFilesProcesser;

public class SpeedRegionBeanBuilder {
	/**
	 * Build a lookup bean, which can be serialised to JSON
	 * @param files
	 * @param minCellLengthMetres
	 * @return
	 */
	public static RegionLookupBean buildBeanFromSpeedRulesObjs(List<SpeedRules> files, double minCellLengthMetres) {
		SpeedRulesFilesProcesser processer = new SpeedRulesFilesProcesser();
		final QuadtreeNode root=processer.buildQuadtree(files, ProcessorUtils.newGeomFactory(), minCellLengthMetres);
		RegionLookupBean built = new RegionLookupBean();
		built.setQuadtree(root);
		built.setRules(processer.validateRules(files));
		return built;
	}
	
	public static RegionLookupBean buildBeanFromSpeedRulesFiles(List<File> files ,double minCellLengthMetres){
		return buildBeanFromSpeedRulesObjs(loadSpeedRulesFiles(files), minCellLengthMetres);
	}
	
	public static RegionLookupBean loadBean(File file){
		return ProcessorUtils.fromJSON(file, RegionLookupBean.class);
	}
	
	public static void saveBean(RegionLookupBean bean,File file){
		ProcessorUtils.toJSONFile(bean, file);
	}

	private static List<SpeedRules> loadSpeedRulesFiles(List<File> files) {
		ArrayList<SpeedRules> objects = new ArrayList<SpeedRules>(files.size());
		for(File file : files){
			objects.add(ProcessorUtils.fromJSON(file, SpeedRules.class));
		}
		return objects;
	}
	
	

}
