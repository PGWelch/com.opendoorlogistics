/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports.builder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRDefaultStyleProvider;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.engine.type.WhenResourceMissingTypeEnum;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.components.reports.ReportConstants;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ReportBuilderUtils {
	private ReportBuilderUtils() {
	}

//	static void addColumnHeaderSection(ODLTableDefinition table, JasperDesign ret) {
//		addColumnHeaderSection(table, getAvailableWidth(ret), ret);
//	}

	static void addColumnHeaderSection(ODLTableDefinition table, int elementWidth, JasperDesign ret) {
		// Add column header
		JRDesignBand chBand = new JRDesignBand();
		chBand.setHeight(20);

		addColumnHeaderToBand(table, elementWidth, chBand);

		ret.setColumnHeader(chBand);
	}

	private static void addColumnHeaderToBand(ODLTableDefinition table, int elementWidth, JRDesignBand chBand) {
		JRDesignStaticText back = new JRDesignStaticText();
		back.setBackcolor(new Color(230, 230, 230));
		back.setWidth(elementWidth);
		back.setHeight(20);
		back.setMode(ModeEnum.OPAQUE);
		chBand.addElement(back);

		List<Double> colWidths = getColumnWidths(table, elementWidth);
		int nc = table.getColumnCount();
		if (nc > 0) {
			double dx=0;
			for (int i = 0; i < nc; i++) {
				JRDesignStaticText text = new JRDesignStaticText();
				int x = (int) Math.round(dx);
				text.setX(x);
				text.setY(4);
				text.setWidth((int) Math.floor(colWidths.get(i)));
				text.setHeight(15);
				text.setText(table.getColumnName(i));
				text.setFontSize(11);
				// int fs = text.getFontSize();
				text.setForecolor(new Color(0, 0, 80));
				text.setBold(true);
				chBand.addElement(text);
				
				dx += colWidths.get(i);
			}
		}

		JRDesignLine line = new JRDesignLine();
		// line.setX(-ret.getLeftMargin());
		line.setY(19);
		line.setWidth(elementWidth);
		line.setHeight(0);
		line.setPositionType(PositionTypeEnum.FLOAT);
		chBand.addElement(line);
	}

	static int getAvailableWidth(JasperDesign design) {
		return design.getPageWidth() - design.getLeftMargin() - design.getRightMargin();
	}

	static int getAvailablePageHeight(JasperDesign design) {
		return design.getPageHeight() - design.getTopMargin() - design.getBottomMargin();
	}

	static JasperDesign createEmptyA4(String tablename, OrientationEnum orientation, boolean margins, int horizontalReduction) {

		switch (orientation) {
		case LANDSCAPE:
			return createEmpty(tablename, 842 - horizontalReduction, 595, margins);

		case PORTRAIT:
			return createEmpty(tablename, 595 - horizontalReduction, 842, margins);

		default:
			throw new RuntimeException();
		}

	}

	static JasperDesign createEmpty(String tablename, int pageWidth, int pageHeight, boolean margins) {
		JasperDesign ret = new JasperDesign();
		ret.setName(Strings.removeExportIllegalChars(AppConstants.ORG_NAME + " - " + tablename));
		ret.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
		ret.setWhenResourceMissingType(WhenResourceMissingTypeEnum.EMPTY);
		ret.setOrientation(OrientationEnum.LANDSCAPE);
		ret.setPageWidth(pageWidth);
		ret.setPageHeight(pageHeight);

		if (!margins) {
			ret.setLeftMargin(0);
			ret.setRightMargin(0);
			ret.setTopMargin(0);
			ret.setBottomMargin(0);
		}

		ret.setColumnWidth(Math.min(getAvailableWidth(ret), ret.getColumnWidth()));

		return ret;
	}

	static int addDetailBand(ODLTableDefinition table, int elementWidth, boolean isHeaderRowForSubreport, JasperDesign ret) {

		// add details
		JRDesignSection detailSection = (JRDesignSection) ret.getDetailSection();
		JRDesignBand band = new JRDesignBand();
		List<Double> colWidths = getColumnWidths(table, elementWidth);
		
		// decide on the row height.. set differently if have images; assume images are square
		int headerHeight=0;
		int rowHeight = 18;
		if (isHeaderRowForSubreport) {
			headerHeight = 22;
			rowHeight = 24;
			
			// repeat header for each master report element
			addColumnHeaderToBand(table, elementWidth, band);
		}
		
		// make row taller if we have an image, based on making the image square
		int nc = table.getColumnCount();
		for (int i = 0; i < nc; i++) {
			if (table.getColumnType(i) == ODLColumnType.IMAGE) {
				rowHeight = Math.max(rowHeight, (int)Math.ceil(colWidths.get(i)));
			}
		}
		
		// Add alternating row background BEFORE column data (so drawn behind)
		if (!isHeaderRowForSubreport) {
			addAlternativeRowBackground(elementWidth, rowHeight, band);
		}

		// Add column data
		if (nc > 0) {
			double dx=0;
			for (int i = 0; i < nc; i++) {
				int x = (int) Math.round(dx);

				JRDesignElement element;
				if (table.getColumnType(i) == ODLColumnType.IMAGE) {
					element = createImageField(table, i);

				} else {
					JRDesignTextField textField = createTextField(table, i);

					// make bigger if this is the title row for a subreport
					if (isHeaderRowForSubreport) {
						textField.setFontSize(16);
						textField.setBold(true);
					}

					element = textField;
				}

				element.setX(x);
				element.setY(headerHeight);
				element.setWidth((int) Math.floor(colWidths.get(i)));
				element.setHeight(rowHeight);

				if (isHeaderRowForSubreport) {
					//element.setY(0);
					element.setStretchType(StretchTypeEnum.NO_STRETCH);
				} else {
					element.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
				}

				band.addElement(element);
				dx += colWidths.get(i);
			}
		}

		band.setHeight(headerHeight + rowHeight);
		detailSection.addBand(band);
		
		return headerHeight + rowHeight;
	}

	private static List<Double> getColumnWidths(ODLTableDefinition table, int elementWidth) {
		// decide on column width; give more weight for images
		ArrayList<Double> colWidths =new ArrayList<>();
		double sum=0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			double val = 1;
			if (table.getColumnType(i) == ODLColumnType.IMAGE) {
				val = 2;
			}
			colWidths.add(val);
			sum += val;
		}
		for (int i = 0; i < table.getColumnCount(); i++) {
			colWidths.set(i, elementWidth * colWidths.get(i) / sum);
		}
		return colWidths;
	}

	private static JRDesignImage createImageField(ODLTableDefinition table, int i) {
		JRDesignImage image = new JRDesignImage(null);
		setImageBorder(image);
		image.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
		String fld = "$F{" + table.getColumnName(i) + "}";
		JRDesignExpression expression = new JRDesignExpression();
		expression.setText(fld);
		image.setExpression(expression);
		image.setPrintWhenDetailOverflows(false);
		return image;
	}

	private static JRDesignTextField createTextField(ODLTableDefinition table, int columnIndex) {
		JRDesignTextField textField = new JRDesignTextField();
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);

		JRDesignExpression expression = new JRDesignExpression();
		String fld = "$F{" + table.getColumnName(columnIndex) + "}";
		if (table.getColumnType(columnIndex) == ODLColumnType.DOUBLE) {
			String combined = fld + "!=null ? new DecimalFormat(\"###,###.###\").format(" + fld + "):\"\"";
			expression.setText(combined);
		} else {
			expression.setText(fld);
		}
		textField.setExpression(expression);
		textField.setPrintWhenDetailOverflows(false);
		return textField;
	}

	private static void addAlternativeRowBackground(int width, int height, JRDesignBand band) {
		JRDesignStaticText alt = new JRDesignStaticText();
		alt.setBackcolor(new Color(240, 240, 250));
		alt.setPrintWhenExpression(new JRDesignExpression("new java.lang.Boolean(($V{REPORT_COUNT}.intValue() % 2)==0)"));
		alt.setWidth(width);
		alt.setHeight(height);
		alt.setMode(ModeEnum.OPAQUE);
		alt.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
		band.addElement(alt);
	}

	static JRDesignSubreport createSubreport(int x, int y, int subheight, String subreportDesignExpression, String subreportDataExpression,
			JRDefaultStyleProvider defaultStyleProvider) {
		JRDesignSubreport sub = new JRDesignSubreport(defaultStyleProvider);
		sub.setHeight(subheight);
		sub.setX(x);
		sub.setY(y);
		sub.setPrintRepeatedValues(true);
		sub.setRemoveLineWhenBlank(true);
		
		//sub.setPrintInFirstWholeBand(true);
		
		if (subreportDataExpression != null) {
			sub.setDataSourceExpression(new JRDesignExpression(subreportDataExpression));
		}

		// expect the subreport template as a parameter to the main report - see
		// http://stackoverflow.com/questions/9785451/generate-jasper-report-with-subreport-from-java
		if (subreportDesignExpression != null) {
			JRDesignExpression subexpression = new JRDesignExpression(subreportDesignExpression);
			sub.setExpression(subexpression);
		}
		return sub;
	}

	static void addPageFooter( JasperDesign ret) {
		addPageFooter( getAvailableWidth(ret), ret);
	}

	static void addPageFooter( int elementWidth, JasperDesign ret) {
		JRDesignBand band = new JRDesignBand();
		band.setHeight(14);

		int height = 13;
		int joinPoint = 40;
		Color backCol = new Color(230, 230, 230);
		JRDesignTextField pageXOf = new JRDesignTextField();
		pageXOf.setWidth(elementWidth - joinPoint);
		pageXOf.setHeight(height);
		pageXOf.setBackcolor(backCol);
		pageXOf.setExpression(new JRDesignExpression("\"Page \"+$V{PAGE_NUMBER}+\" of\""));
		pageXOf.setMode(ModeEnum.OPAQUE);
		pageXOf.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		band.addElement(pageXOf);

		JRDesignTextField pageN = new JRDesignTextField();
		pageN.setWidth(joinPoint);
		pageN.setX(elementWidth - joinPoint);
		pageN.setHeight(height);
		pageN.setMode(ModeEnum.OPAQUE);
		pageN.setEvaluationTime(EvaluationTimeEnum.REPORT);
		pageN.setBackcolor(backCol);
		pageN.setHorizontalAlignment(HorizontalAlignEnum.LEFT);
		pageN.setExpression(new JRDesignExpression("\" \" +$V{PAGE_NUMBER}"));
		band.addElement(pageN);

		JRDesignTextField date = new JRDesignTextField();
		date.setHeight(height);
		date.setWidth(200);
		date.setPattern("EEEEE dd MMMMM yyyy");
		date.setExpression(new JRDesignExpression("new java.util.Date()"));
		band.addElement(date);

		JRDesignTextField odl = new JRDesignTextField();
		odl.setWidth(elementWidth);
		odl.setHeight(height);
		odl.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
		odl.setExpression(new JRDesignExpression("\"Created by ODL Studio with maps © OpenStreetMap\""));
		band.addElement(odl);

		ret.setPageFooter(band);
	}

	static void addTitle(String title,boolean hasHeaderMap,boolean hasDetails,  JasperDesign ret) {
		addTitle(title, getAvailableWidth(ret),hasHeaderMap,hasDetails, ret);
	}

	static void addTitle(String title, int elementWidth,boolean hasHeaderMap, boolean hasDetails,JasperDesign ret) {
		JRDesignBand band = new JRDesignBand();

		int titleHeight = 50;
		band.setHeight(titleHeight);

		JRDesignTextField textField = new JRDesignTextField();
		textField.setBlankWhenNull(true);
		textField.setX(0);
		textField.setY(10);
		textField.setWidth(elementWidth);
		textField.setHeight(38);
		textField.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
		textField.setFontSize(26);
		textField.setBold(true);
		JRDesignExpression expression = new JRDesignExpression();
		expression.setText("\"" + title + "\"");
		textField.setExpression(expression);
		band.addElement(textField);
		if(hasHeaderMap){
			double pictureWidthPoints = elementWidth;// / 10.0;
			double pictureXOffset = (elementWidth - pictureWidthPoints)/2.0; 
			double pictureWidthCM = pictureWidthPoints * ReportConstants.POINT_SIZE_IN_CM;
			
			// get picture height
			double pictureHeightCM = 10;
			if(hasDetails==false){
				// take whole page except for title
				double points = getAvailablePageHeight(ret) - titleHeight -40;
				pictureHeightCM = points * ReportConstants.POINT_SIZE_IN_CM;
			}
			else if (getAvailablePageHeight(ret) < getAvailableWidth(ret)){
				// landscape; make shorter
				pictureHeightCM = 6;
			}
			double pictureHeightPoints = pictureHeightCM / ReportConstants.POINT_SIZE_IN_CM;
			
			String imgExpression = "((" + ReportConstants.IMAGE_PROVIDER_INTERFACE + ")" + "$P{" + ReportConstants.HEADER_MAP_PROVIDER_PARAMETER + "})."
					+ ReportConstants.IMAGE_PROVIDER_INTERFACE_METHOD + "(" +pictureWidthCM +"," + pictureHeightCM + ",200)";
			JRDesignImage img = new JRDesignImage(null);
			
			img.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
			img.setExpression(new JRDesignExpression(imgExpression));
			img.setX((int)Math.round(pictureXOffset));
			img.setY(titleHeight);
			img.setWidth((int)Math.round(pictureWidthPoints));
			img.setHeight((int)Math.round(pictureHeightPoints));
			setImageBorder(img);
//			img.getLineBox().s
			band.setHeight(band.getHeight() + 10 +img.getHeight());
			band.addElement(img);
			
			//JRdesign
		}
		
		ret.setTitle(band);
	}

	static void setImageBorder(JRDesignImage img) {
		float bw = .5f;
		Color c = Color.BLACK;
		JRLineBox box = img.getLineBox();
		box.getLeftPen().setLineWidth(bw);
		box.getLeftPen().setLineColor(c);
		box.getRightPen().setLineWidth(bw);
		box.getRightPen().setLineColor(c);
		box.getBottomPen().setLineWidth(bw);
		box.getBottomPen().setLineColor(c);
		box.getTopPen().setLineWidth(bw);
		box.getTopPen().setLineColor(c);
	}

	static JRDesignField[] createFields(ODLTableDefinition table, boolean includeArtificialSubreportField) {
		int nc = table.getColumnCount();
		JRDesignField[] ret = new JRDesignField[nc + (includeArtificialSubreportField ? 1 : 0)];
		for (int i = 0; i < nc; i++) {
			ret[i] = new JRDesignField();
			ret[i].setName(table.getColumnName(i));
			ret[i].setValueClass(ColumnValueProcessor.getJavaClass(table.getColumnType(i)));
		}

		if (includeArtificialSubreportField) {
			ret[nc] = new JRDesignField();
			ret[nc].setName(ReportConstants.SUBREPORT_DATASTORE_FIELDNAME);
			ret[nc].setValueClass(net.sf.jasperreports.engine.JRRewindableDataSource.class);
		}
		return ret;
	}

	static void addFields(ODLTableDefinition table, boolean includeArtificialSubreportField, JasperDesign out) {
		for (JRDesignField field : createFields(table, includeArtificialSubreportField)) {
			try {
				out.addField(field);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

	}

}
