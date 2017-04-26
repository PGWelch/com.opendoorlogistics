package com.opendoorlogistics.components.jsprit;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.components.jsprit.AlgorithmConfig.JSpritStrategyWeight;

import com.graphhopper.jsprit.core.algorithm.box.Jsprit.Strategy;

public class AlgorithmConfigReflectionUtils {
	public static class StrategyWeightGetterSetter{
		final Strategy strategy;
		final Method getter;
		final Method setter;
		
		private StrategyWeightGetterSetter(Strategy strategy, Method getter, Method setter) {
			this.strategy = strategy;
			this.getter = getter;
			this.setter = setter;
		}
		
		
		double read(AlgorithmConfig config){
			Double val;
			try {
				val = (Double) getter.invoke(config);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			return val;
		}
		
		void write(Double value,AlgorithmConfig config){
			try {
				setter.invoke(config, value);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}	
		}
	}
	
	public static List<StrategyWeightGetterSetter> getStrategyWeights(){
		ArrayList<StrategyWeightGetterSetter> ret = new ArrayList<>();
		BeanInfo beanInfo = null;
		try {
			beanInfo = java.beans.Introspector.getBeanInfo(AlgorithmConfig.class);
			for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
				if (property.getWriteMethod() != null && property.getReadMethod() != null) {
					JSpritStrategyWeight annot = property.getWriteMethod().getAnnotation(JSpritStrategyWeight.class);
					if (annot == null) {
						annot = property.getReadMethod().getAnnotation(JSpritStrategyWeight.class);
					}
					
					if(annot!=null){
						StrategyWeightGetterSetter getterSetter = new StrategyWeightGetterSetter(annot.value(), property.getReadMethod(), property.getWriteMethod());
						ret.add(getterSetter);						
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return ret;
	}
}
