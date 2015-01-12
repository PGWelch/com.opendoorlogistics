/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.gantt;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.chart.util.ParamChecks;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.GanntChart;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.Colours.CalculateAverageColour;
import com.opendoorlogistics.utils.ui.Icons;

public class GanttChartComponent implements ODLComponent, GanntChart {
	private static class MySubtask extends Task{
		private final GanttItem item;
		
		MySubtask(GanttItem item, Date start, Date end) {
			super("", start, end);
			this.item = item;
		}
		
		GanttItem getItem(){
			return item;
		}
	}
	
	@Override
	public String getId() {
		return "com.opendoorlogistics.components.gantt";
	}

	@Override
	public String getName() {
		return "Gantt chart";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		return GanttItem.beanMapping.getDefinition();
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(final ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {
		// Get items and sort by resource then date
		final StringConventions sc = api.getApi().stringConventions();
		List<GanttItem> items = GanttItem.beanMapping.getTableMapping(0).readObjectsFromTable(ioDs.getTableAt(0));
		
		// Rounding doubles to longs can create small errors where a start time is 1 millisecond after an end.
		// Set all start times to be <= end time
		for(GanttItem item:items){
			if(item.getStart()==null || item.getEnd()==null){
				throw new RuntimeException("Found Gannt item with null start or end time.");
			}
			
			if(item.getStart().getValue() > item.getEnd().getValue()){
				item.setStart(item.getEnd());
			}
		}
		
		Collections.sort(items, new Comparator<GanttItem>() {

			@Override
			public int compare(GanttItem o1, GanttItem o2) {
				int diff = sc.compareStandardised(o1.getResourceId(), o2.getResourceId());
				if (diff == 0) {
					diff = o1.getStart().compareTo(o2.getStart());
				}
				if (diff == 0) {
					diff = o1.getEnd().compareTo(o2.getEnd());
				}
				if (diff == 0) {
					diff = sc.compareStandardised(o1.getActivityId(), o2.getActivityId());
				}
				if (diff == 0) {
					diff = Colours.compare(o1.getColor(), o2.getColor());
				}
				return diff;
			}
		});

		// Filter any zero duration items
		Iterator<GanttItem> it = items.iterator();
		while (it.hasNext()) {
			GanttItem item = it.next();
			if (item.getStart().compareTo(item.getEnd()) == 0) {
				it.remove();
			}
		}

		// Get average colour for each activity type
		Map<String, CalculateAverageColour> calcColourMap = api.getApi().stringConventions().createStandardisedMap();
		for (GanttItem item : items) {
			CalculateAverageColour calc = calcColourMap.get(item.getActivityId());
			if (calc == null) {
				calc = new CalculateAverageColour();
				calcColourMap.put(item.getActivityId(), calc);
			}
			calc.add(item.getColor());
		}

		// Put into colour map
		Map<String, Color> colourMap = api.getApi().stringConventions().createStandardisedMap();
		for (Map.Entry<String, CalculateAverageColour> entry : calcColourMap.entrySet()) {
			colourMap.put(entry.getKey(), entry.getValue().getAverage());
		}

		// Split items by resource
		ArrayList<ArrayList<GanttItem>> splitByResource = new ArrayList<>();
		ArrayList<GanttItem> current = null;
		for (GanttItem item : items) {
			if (current == null || sc.compareStandardised(current.get(0).getResourceId(), item.getResourceId()) != 0) {
				current = new ArrayList<>();
				splitByResource.add(current);
			}
			current.add(item);
		}

		// put into jfreechart's task data structure
		TaskSeries ts = new TaskSeries("Resources");
		for (ArrayList<GanttItem> resource : splitByResource) {
			// get earliest and latest time (last time may not be in the last item)
			ODLTime earliest = resource.get(0).getStart();
			ODLTime latest = null;
			for (GanttItem item : resource) {
				if (latest == null || latest.compareTo(item.getEnd()) < 0) {
					latest = item.getEnd();
				}
			}

			Task task = new Task(resource.get(0).getResourceId(), new Date(earliest.getTotalMilliseconds()), new Date(latest.getTotalMilliseconds()));

			// add all items as subtasks
			for (GanttItem item : resource) {
				task.addSubtask(new MySubtask(item, new Date(item.getStart().getTotalMilliseconds()), new Date(item.getEnd().getTotalMilliseconds())));
			}

			ts.add(task);
		}
		TaskSeriesCollection collection = new TaskSeriesCollection();
		collection.add(ts);

		// Create the plot
		CategoryAxis categoryAxis = new CategoryAxis(null);
		DateAxis dateAxis = new DateAxis("Time");
		CategoryItemRenderer renderer = new MyRenderer(collection, colourMap);
		final CategoryPlot plot = new CategoryPlot(collection, categoryAxis, dateAxis, renderer);
		plot.setOrientation(PlotOrientation.HORIZONTAL);
		plot.getDomainAxis().setLabel(null);
	
		((DateAxis) plot.getRangeAxis()).setDateFormatOverride(new DateFormat() {

			@Override
			public Date parse(String source, ParsePosition pos) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
				toAppendTo.append(new ODLTime(date.getTime()).toString());
				return toAppendTo;
			}
		});

		// Create the chart and apply the standard theme without shadows to it
		api.submitControlLauncher(new ControlLauncherCallback() {

			@Override
			public void launchControls(ComponentControlLauncherApi launcherApi) {
				JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
				StandardChartTheme theme = new StandardChartTheme("standard theme", false);
				theme.setBarPainter(new StandardBarPainter());
				theme.apply(chart);

				class MyPanel extends ChartPanel implements Disposable {

					public MyPanel(JFreeChart chart) {
						super(chart);
					}

					@Override
					public void dispose() {
						// TODO Auto-generated method stub

					}

				}
				MyPanel chartPanel = new MyPanel(chart);
				launcherApi.registerPanel("Resource Gantt", null, chartPanel, true);
			}
		});

	}

	/** @see http://stackoverflow.com/questions/8938690 */
	private static class MyRenderer extends GanttRenderer {

		private static final int PASS = 1; // currently have one pass
		private final List<Color> colours = new ArrayList<Color>();
		private final List<String> tooltips = new ArrayList<String>();
		private final TaskSeriesCollection model;
		private final Map<String, Color> colourMap;
		private int row;
		private int col;
		private int index;

		public MyRenderer(TaskSeriesCollection model, Map<String, Color> colourMap) {
			this.model = model;
			this.colourMap = colourMap;

		}
		
		/**
		 * Override the method so we can set custom tooltips
		 */
		@Override
	    protected void addItemEntity(EntityCollection entities,
	            CategoryDataset dataset, int row, int column, Shape hotspot) {
	        ParamChecks.nullNotPermitted(hotspot, "hotspot");
	        if (!getItemCreateEntity(row, column)) {
	            return;
	        }
	        
			String tip = null;
			if((index-1) < tooltips.size()){
				tip = tooltips.get(index-1);
			}
			
	        String url = null;
	        CategoryURLGenerator urlster = getItemURLGenerator(row, column);
	        if (urlster != null) {
	            url = urlster.generateURL(dataset, row, column);
	        }
	        CategoryItemEntity entity = new CategoryItemEntity(hotspot, tip, url,
	                dataset, dataset.getRowKey(row), dataset.getColumnKey(column));
	        entities.add(entity);
	    }

		@Override
		public LegendItemCollection getLegendItems() {
			LegendItemCollection legendItemCollection = new LegendItemCollection();
			for (Map.Entry<String, Color> entry : colourMap.entrySet()) {
				legendItemCollection.add(new LegendItem(entry.getKey(), entry.getValue()));
			}
			return legendItemCollection;
		}

		@Override
		public Paint getItemPaint(int row, int col) {
			if (colours.isEmpty() || this.row != row || this.col != col) {
				initInfoPerResource(row, col);
				this.row = row;
				this.col = col;
				index = 0;
			}
			int colourIndex = index++ / PASS;
			// System.out.println(colourIndex + "/" + colours.size());
			return colours.get(colourIndex);
		}

		private void initInfoPerResource(int row, int col) {
			colours.clear();
			tooltips.clear();
			
			Color c = (Color) super.getItemPaint(row, col);
			float[] a = new float[3];
			Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), a);
			TaskSeries series = (TaskSeries) model.getRowKeys().get(row);
			List<Task> tasks = series.getTasks();
			Task resource = tasks.get(col);
			int taskCount = resource.getSubtaskCount();
			taskCount = Math.max(1, taskCount);
			for (int i = 0; i < taskCount; i++) {
				MySubtask subtask = (MySubtask)resource.getSubtask(i);
				Color colour = colourMap.get(subtask.getItem().getActivityId());
				colours.add(colour);
				tooltips.add(subtask.getItem().getName());
			}
		}
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING | ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("gantt.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("Gantt chart", "Gantt chart", "Gantt chart", new GanttChartComponent().getIODsDefinition(templatesApi.getApi(), null), (Serializable) null);
	}

	@Override
	public String activityIdColumnName() {
		return "activity-id";
	}

	@Override
	public String resourceIdColumnName() {
		return "resource-id";
	}

	@Override
	public String startTimeColumnName() {
		return "start-time";
	}

	@Override
	public String endTimeColumnName() {
		return "end-time";
	}

	@Override
	public String colourSourceColumnName() {
		return "colour";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition() {
		return getIODsDefinition(null, null);
	}

}
