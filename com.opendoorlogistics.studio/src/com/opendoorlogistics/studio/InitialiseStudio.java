/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.app.ODLApp;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ODLStudioLoader;
import com.opendoorlogistics.api.standardcomponents.map.MapSelectionList;
import com.opendoorlogistics.components.InitialiseComponents;
import com.opendoorlogistics.core.InitialiseCore;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.execution.OptionsSubpath;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutionBlackboardImpl;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutor;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.AbstractDependencyInjector;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.DependencyInjector;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.DependencyInjectorDecorator;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.ProcessingApiDecorator;
import com.opendoorlogistics.studio.appframe.AppFrame;
import com.opendoorlogistics.studio.components.geocoder.component.NominatimGeocoderComponent;
import com.opendoorlogistics.studio.components.map.GlobalMapPluginManager;
import com.opendoorlogistics.studio.components.map.MapApiImpl;
import com.opendoorlogistics.studio.components.map.MapConfig;
import com.opendoorlogistics.studio.components.map.SuggestedFillValuesManager;
import com.opendoorlogistics.studio.components.tables.TableControlComponent;
import com.opendoorlogistics.studio.scripts.editor.ScriptEditor;
import com.opendoorlogistics.studio.scripts.editor.adapters.AdaptedTableControl;
import com.opendoorlogistics.studio.scripts.editor.adapters.AdapterTableDefinitionGrid;
import com.opendoorlogistics.studio.scripts.editor.adapters.AdapterTablesTabControl;
import com.opendoorlogistics.studio.scripts.editor.adapters.QueryAvailableData;
import com.opendoorlogistics.studio.scripts.editor.wizardgenerated.ScriptEditorWizardGenerated;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManagerImpl;
import com.opendoorlogistics.studio.scripts.execution.ScriptsRunner;

final public class InitialiseStudio implements ODLStudioLoader{
	private static volatile boolean isInit = false;

	public InitialiseStudio(){
		
	}
	
	public static synchronized ODLApi initialise(boolean preloadClasses) {
		if (!isInit) {
			// Check the classname is as expected by the API (as its used with reflection)
			Class<?> cls = InitialiseStudio.class;
			String className =cls.getName(); 
			if(!className.equals(ODLStudioLoader.LOADER_IMPLEMENTATION_NAME)){
				throw new RuntimeException("Loader class has been renamed and no longer equals " + ODLStudioLoader.LOADER_IMPLEMENTATION_NAME + ". This will break code using it by reflection.");
			}
			if(!ODLStudioLoader.class.isAssignableFrom(cls)){
				throw new RuntimeException("Loader class no longer implements ODLStudioLoader . This will break code using it by reflection.");				
			}
			
			initLookAndFeel();
			ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

			// run app initialisation
			InitialiseCore.initialise();
			InitialiseComponents.initialise();
			ODLGlobalComponents.register(new NominatimGeocoderComponent());
			MapApiImpl.registerComponent();		
			ODLGlobalComponents.register(new TableControlComponent());
			

			// hack .. any classes which cause a noticeable pause in the UI when
			// first loaded are given dummy calls here to put make the loading
			// occur once-off when the app starts.
			if (preloadClasses) {
				preloadClasses();
			}


			isInit = true;
		}

		return new ODLApiImpl();
	}

	/**
	 * Preload classes so pauses due to class loading happen at start-up only.
	 */
	public static void preloadClasses() {
		for (Class<?> cls : new Class<?>[] {  MapApiImpl.class, MapConfig.class,GlobalMapPluginManager.class,
				MapSelectionList.class, SuggestedFillValuesManager.class,
				 MouseMotionListener.class, ScriptEditor.class, ScriptEditorWizardGenerated.class, AdaptedTableControl.class,
				AdapterTableDefinitionGrid.class, AdapterTablesTabControl.class, QueryAvailableData.class, ScriptsRunner.class, ScriptUIManager.class, ScriptUIManagerImpl.class,
				ScriptExecutor.class, ScriptExecutionBlackboardImpl.class, ExecutionReportImpl.class, AdapterBuilder.class, FunctionsBuilder.class, OptionsSubpath.class,
				AbstractDependencyInjector.class, DependencyInjector.class, DependencyInjectorDecorator.class, ProcessingApiDecorator.class,
				com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStateListener.class, com.opendoorlogistics.api.distances.ODLCostMatrix.class,
				com.opendoorlogistics.codefromweb.BlockingLifoQueue.class, com.opendoorlogistics.codefromweb.DesktopScrollPane.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.DesktopPaneMapViewer.DesktopPanePanMouseInputListener.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.DesktopPaneMapViewer.DesktopPaneZoomMouseWheelListenerCursor.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.beans.AbstractBean.class, com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.AbstractTileFactory.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.empty.EmptyTileFactory.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition.class, com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile.Priority.class, com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileCache.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileListener.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.util.GeoUtil.class, com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.OSMTileFactoryInfo.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.painter.AbstractPainter.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.painter.AbstractPainter.Interpolation.class,
				com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.painter.CompoundPainter.class, com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.painter.Painter.class,
				com.opendoorlogistics.core.api.impl.GeometryImpl.class, com.opendoorlogistics.core.cache.ApplicationCache.class, com.opendoorlogistics.core.cache.RecentlyUsedCache.class,
				com.opendoorlogistics.core.distances.DistancesSingleton.class, com.opendoorlogistics.core.distances.functions.FmAbstractDrivingCost.class,
				com.opendoorlogistics.core.distances.functions.FmAbstractTravelCost.class, com.opendoorlogistics.core.distances.functions.FmDistance.class,
				com.opendoorlogistics.core.distances.functions.FmDistance.Km.class, com.opendoorlogistics.core.distances.functions.FmDistance.Metres.class,
				com.opendoorlogistics.core.distances.functions.FmDistance.Miles.class, com.opendoorlogistics.core.distances.functions.FmDrivingDistance.class,
				com.opendoorlogistics.core.distances.functions.FmDrivingDistance.Km.class, com.opendoorlogistics.core.distances.functions.FmDrivingDistance.Metres.class,
				com.opendoorlogistics.core.distances.functions.FmDrivingDistance.Miles.class, com.opendoorlogistics.core.distances.functions.FmDrivingRouteGeom.class,
				com.opendoorlogistics.core.distances.functions.FmDrivingRouteGeomFromLine.class, com.opendoorlogistics.core.distances.functions.FmDrivingTime.class,
				com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.class,
				com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType.class,
				com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionArgument.class,
				com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionType.class, com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary.class,
				com.opendoorlogistics.core.formulae.FormulaParser.class, com.opendoorlogistics.core.formulae.FunctionFactory.class, com.opendoorlogistics.core.formulae.FunctionImpl.class,
				com.opendoorlogistics.core.formulae.Functions.class, com.opendoorlogistics.core.formulae.Functions.Fm1DoubleParam.class,
				com.opendoorlogistics.core.formulae.Functions.Fm1GeometryParam.class, com.opendoorlogistics.core.formulae.Functions.FmAbs.class,
				com.opendoorlogistics.core.formulae.Functions.FmAcos.class, com.opendoorlogistics.core.formulae.Functions.FmAnd.class,
				com.opendoorlogistics.core.formulae.Functions.FmAsin.class, com.opendoorlogistics.core.formulae.Functions.FmAtan.class,
				com.opendoorlogistics.core.formulae.Functions.FmCeil.class, com.opendoorlogistics.core.formulae.Functions.FmColour.class,
				com.opendoorlogistics.core.formulae.Functions.FmColourImage.class, com.opendoorlogistics.core.formulae.Functions.FmColourMultiply.class,
				com.opendoorlogistics.core.formulae.Functions.FmRelativeComparisonBase.class, com.opendoorlogistics.core.formulae.Functions.FmConcatenate.class,
				com.opendoorlogistics.core.formulae.Functions.FmConst.class, com.opendoorlogistics.core.formulae.Functions.FmContains.class,
				com.opendoorlogistics.core.formulae.Functions.FmCos.class, com.opendoorlogistics.core.formulae.Functions.FmDecimalFormat.class,
				com.opendoorlogistics.core.formulae.Functions.FmDivide.class, com.opendoorlogistics.core.formulae.Functions.FmEquals.class,
				com.opendoorlogistics.core.formulae.Functions.FmFadeImage.class, com.opendoorlogistics.core.formulae.Functions.FmFloor.class,
				com.opendoorlogistics.core.formulae.Functions.FmGreaterThan.class, com.opendoorlogistics.core.formulae.Functions.FmGreaterThanEqualTo.class,
				com.opendoorlogistics.core.formulae.Functions.FmIfThenElse.class, com.opendoorlogistics.core.formulae.Functions.FmImageFilter.class,
				com.opendoorlogistics.core.formulae.Functions.FmIndexOf.class, com.opendoorlogistics.core.formulae.Functions.FmLeft.class,
				com.opendoorlogistics.core.formulae.Functions.FmLen.class, com.opendoorlogistics.core.formulae.Functions.FmLerp.class,
				com.opendoorlogistics.core.formulae.Functions.FmLessThan.class, com.opendoorlogistics.core.formulae.Functions.FmLessThanEqualTo.class,
				com.opendoorlogistics.core.formulae.Functions.FmLn.class, com.opendoorlogistics.core.formulae.Functions.FmLog10.class,
				com.opendoorlogistics.core.formulae.Functions.FmLower.class, com.opendoorlogistics.core.formulae.Functions.FmMax.class,
				com.opendoorlogistics.core.formulae.Functions.FmMin.class, com.opendoorlogistics.core.formulae.Functions.FmMod.class,
				com.opendoorlogistics.core.formulae.Functions.FmMultiply.class, com.opendoorlogistics.core.formulae.Functions.FmOr.class,
				com.opendoorlogistics.core.formulae.Functions.FmPostcodeUk.class, com.opendoorlogistics.core.formulae.Functions.FmRand.class,
				com.opendoorlogistics.core.formulae.Functions.FmRandColour.class, com.opendoorlogistics.core.formulae.Functions.FmRandomSymbol.class,
				com.opendoorlogistics.core.formulae.Functions.FmReplace.class, com.opendoorlogistics.core.formulae.Functions.FmRound.class,
				com.opendoorlogistics.core.formulae.Functions.FmRound2Second.class, com.opendoorlogistics.core.formulae.Functions.FmSin.class,
				com.opendoorlogistics.core.formulae.Functions.FmSingleString.class, com.opendoorlogistics.core.formulae.Functions.FmSqrt.class,
				com.opendoorlogistics.core.formulae.Functions.FmSubtract.class, com.opendoorlogistics.core.formulae.Functions.FmSum.class,
				com.opendoorlogistics.core.formulae.Functions.FmSwitch.class, com.opendoorlogistics.core.formulae.Functions.FmTan.class,
				com.opendoorlogistics.core.formulae.Functions.FmTemperatureColours.class, com.opendoorlogistics.core.formulae.Functions.FmTime.class,
				com.opendoorlogistics.core.formulae.Functions.FmUpper.class, com.opendoorlogistics.core.formulae.FunctionUtils.class,
				com.opendoorlogistics.core.formulae.StringTokeniser.class, com.opendoorlogistics.core.formulae.StringTokeniser.StringToken.class,
				com.opendoorlogistics.core.geometry.functions.FmCentroid.class, com.opendoorlogistics.core.geometry.functions.FmGeom.GeomType.class,
				com.opendoorlogistics.core.geometry.functions.FmGeomBorder.class, com.opendoorlogistics.core.geometry.functions.FmLatitude.class,
				com.opendoorlogistics.core.geometry.functions.FmLongitude.class, com.opendoorlogistics.core.geometry.functions.FmShapefileLookup.class,
				com.opendoorlogistics.core.geometry.GreateCircle.class, com.opendoorlogistics.core.geometry.Spatial.class,
				com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton.class, com.opendoorlogistics.core.gis.map.CachedGeomImageRenderer.class,
				com.opendoorlogistics.core.gis.map.data.LatLongBoundingBox.class, com.opendoorlogistics.core.gis.map.DatastoreRenderer.class,
				com.opendoorlogistics.core.gis.map.JXMapUtils.class, com.opendoorlogistics.core.gis.map.ObjectRenderer.class, com.opendoorlogistics.core.gis.map.RecentImageCache.class,
				com.opendoorlogistics.core.gis.map.RecentImageCache.ZipType.class, com.opendoorlogistics.core.gis.map.RenderProperties.class, com.opendoorlogistics.core.gis.map.Symbols.class,
				com.opendoorlogistics.core.gis.map.Symbols.SymbolType.class, com.opendoorlogistics.core.gis.map.tiled.ChangedObjectsCalculator.class,
				com.opendoorlogistics.core.gis.map.tiled.NOPLManager.class, com.opendoorlogistics.core.gis.map.tiled.TileCacheRenderer.class,
				com.opendoorlogistics.core.gis.map.transforms.LatLongToScreenImpl.class, com.opendoorlogistics.core.gis.postcodes.UKPostcodes.UKPostcodeLevel.class,
				com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ProcessedLookupReferences.class,
				com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ToProcessLookupReferences.class, com.opendoorlogistics.core.scripts.formulae.FmAbstractLookup.class,
				com.opendoorlogistics.core.scripts.formulae.image.FmImage.Mode.class, com.opendoorlogistics.core.scripts.formulae.FmIsSelectedInMap.class,
				com.opendoorlogistics.core.scripts.formulae.FmLocalElement.class, com.opendoorlogistics.core.scripts.formulae.FmLookup.class,
				com.opendoorlogistics.core.scripts.formulae.FmLookup.LookupType.class, com.opendoorlogistics.core.scripts.formulae.FmLookupGeomUnion.class,
				com.opendoorlogistics.core.scripts.formulae.FmLookupNearest.class, com.opendoorlogistics.core.scripts.formulae.FmLookupWeightedCentroid.class,
				com.opendoorlogistics.core.scripts.formulae.FmRow.class, com.opendoorlogistics.core.scripts.formulae.FmRowId.class,
				com.opendoorlogistics.core.scripts.formulae.TableParameters.class, com.opendoorlogistics.core.scripts.TableReference.class,
				com.opendoorlogistics.core.tables.concurrency.WriteRecorderDecorator.class, com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator.class,
				com.opendoorlogistics.core.tables.decorators.datastores.UnionDecorator.class, com.opendoorlogistics.core.tables.decorators.listeners.ListenerRedirector.class,
				com.opendoorlogistics.core.utils.NullComparer.class, com.opendoorlogistics.core.utils.SimpleSoftReferenceMap.class,
				com.opendoorlogistics.core.utils.strings.StandardisedCache.class, com.opendoorlogistics.core.utils.strings.StringKeyValue.class,
				com.opendoorlogistics.core.utils.strings.Strings.ToString.class, com.opendoorlogistics.core.utils.ui.SwingUtils.class,
				com.opendoorlogistics.studio.GlobalMapSelectedRowsManager.GlobalSelectionChangedCB.class, com.opendoorlogistics.studio.internalframes.ProgressFrame.class,
				com.opendoorlogistics.studio.scripts.execution.ScriptsRunner.class, com.opendoorlogistics.studio.components.map.plugins.snapshot.CreateImageConfig.class,
				com.opendoorlogistics.studio.components.map.plugins.snapshot.ExportImageConfig.class,

		}) {
			cls.toString();
		}
	}

	private static void initLookAndFeel() {
		// get the defaults to keep
		HashMap<Object, Object> defaultsToKeep = new HashMap<>();
		for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
			// keep progress bar string defaults (as nimbus looks rubbish)
			boolean isStringKey = entry.getKey().getClass() == String.class ;
			String key = isStringKey ? ((String) entry.getKey()):"";
			
			boolean keep =key.startsWith("ProgressBar");
			
			// keep tabbed pane defaults as nimbus to prevent re-ordering http://stackoverflow.com/questions/7481991/jtabbedpane-avoid-automatic-re-ordering-tabs-if-stacked-nimbus
			if(!keep){
				// see list of properties here http://www.java2s.com/Tutorial/Java/0240__Swing/CustomizingaJTabbedPaneLookandFeel.htm
				keep = key.startsWith("TabbedPane");
			}
			
			if (keep) {
				defaultsToKeep.put(entry.getKey(), entry.getValue());
			}
		}

		// set look and feel
		try {
			boolean set = false;
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
	//				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

					UIDefaults defaults = UIManager.getLookAndFeelDefaults();
					defaults.put("Table.gridColor", new Color(214, 217, 223));
					defaults.put("Table.disabled", false);
					defaults.put("Table.showGrid", true);
					defaults.put("Table.intercellSpacing", new Dimension(1, 1));
					defaults.put("nimbusOrange", defaults.get("nimbusBase"));
					set = true;
					break;
				}
			}

			if (!set) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		// copy back progress bar defaults
		for (Map.Entry<Object, Object> entry : defaultsToKeep.entrySet()) {
			UIManager.getDefaults().put(entry.getKey(), entry.getValue());
		}

		JFrame.setDefaultLookAndFeelDecorated(true);
	}

	@Override
	public ODLApp startStudio(ODLComponent... components) {
		// ensure classes are preloaded, even if we've done a no-preload init before  
		initialise(true);
		preloadClasses();
	
		return new AppFrame();
	}

	@Override
	public ODLApi createApi() {
		// do a lazy (no preloading) initialisation
		initialise(false);
		
		return new ODLApiImpl();
	}


}
