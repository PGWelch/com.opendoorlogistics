package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.UserRenderFlags;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.annotations.ODLIgnore;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(Style.TABLE_NAME)
public class Style extends BeanMappedRowImpl {
	public static final int NB_RULE_KEYS=4;
	public static final String TABLE_NAME = "Styles";
	private String layerId;
	private String key1;
	private String key2;
	private String key3;
	private String key4;
	private String colour;
	private String width; // filter width zero??
	private String symbol;
	private String outlined;
	private String opaque;
	private String label;
	private String labelAlwaysVisible;
	private String labelColour;
	private String labelPosition; 
	private String labelSize;
	private String legendKey;

	public enum OutputFormula{
		COLOUR(DrawableObjectImpl.COL_COLOUR, ODLColumnType.COLOUR),
		WIDTH(DrawableObjectImpl.COL_PIXEL_WIDTH,ODLColumnType.LONG),
		SYMBOL(DrawableObjectImpl.COL_SYMBOL,ODLColumnType.STRING),
		OUTLINED(DrawableObjectImpl.COL_OUTLINE,ODLColumnType.LONG),
		OPAQUE(DrawableObjectImpl.COL_OPAQUE,ODLColumnType.DOUBLE),
		LABEL(DrawableObjectImpl.COL_LABEL,ODLColumnType.STRING),
		LABELALWAYSVISIBLE(DrawableObjectImpl.COL_FLAGS,ODLColumnType.LONG, UserRenderFlags.ALWAYS_SHOW_LABEL),
		LABELCOLOUR(DrawableObjectImpl.COL_LABEL_COLOUR,ODLColumnType.COLOUR),
		LABELPOSITION(DrawableObjectImpl.COL_LPO,ODLColumnType.STRING),
		LABELSIZE(DrawableObjectImpl.COL_FONT_SIZE,ODLColumnType.LONG),
		LEGENDKEY(DrawableObjectImpl.COL_LEGEND_KEY,ODLColumnType.STRING);	
		
		final int drawablesColumn;
		final ODLColumnType outputType;
		final long booleanToFlag;

		private OutputFormula(int drawablesColumn, ODLColumnType outputType) {
			this(drawablesColumn, outputType,-1);
		}
		
		private OutputFormula(int drawablesColumn, ODLColumnType outputType, long mappedToFlag) {
			this.drawablesColumn = drawablesColumn;
			this.outputType = outputType;
			this.booleanToFlag = mappedToFlag;
		}
		
	}
	
	public String getRuleKey(int indx){
		if(indx<0 || indx>=NB_RULE_KEYS){
			throw new IllegalArgumentException();
		}
		switch (indx) {
		case 0:
			return key1;

		case 1:
			return key2;
			
		case 2:
			return key3;
			
		case 3:
			return key4;			

		}
		
		return null;
	}
	
	@ODLIgnore
	public String getFormula(OutputFormula type){
		switch(type){
		case COLOUR:
			return getColour();
			
		case WIDTH:
			return getWidth();
			
		case SYMBOL:
			return getSymbol();
			
		case OUTLINED:
			return getOutlined();
			
		case OPAQUE:
			return getOpaque();
			
		case LABELALWAYSVISIBLE:
			return getLabelAlwaysVisible();
			
		case LABELCOLOUR:
			return getLabelColour();
			
		case LABEL:
			return getLabel();
			
		case LABELPOSITION:
			return getLabelPosition();
			
		case LABELSIZE:
			return getLabelSize();
			
		default:
			throw new IllegalArgumentException();
		}
		
	}
	
	public String getLayerId() {
		return layerId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public String getKey1() {
		return key1;
	}

	public void setKey1(String key1) {
		this.key1 = key1;
	}

	public String getKey2() {
		return key2;
	}

	public void setKey2(String key2) {
		this.key2 = key2;
	}

	public String getKey3() {
		return key3;
	}

	public void setKey3(String key3) {
		this.key3 = key3;
	}

	public String getKey4() {
		return key4;
	}

	public void setKey4(String key4) {
		this.key4 = key4;
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getOutlined() {
		return outlined;
	}

	public void setOutlined(String outlined) {
		this.outlined = outlined;
	}

	public String getOpaque() {
		return opaque;
	}

	public void setOpaque(String opaque) {
		this.opaque = opaque;
	}

	public String getLabelColour() {
		return labelColour;
	}

	public void setLabelColour(String labelColour) {
		this.labelColour = labelColour;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabelSize() {
		return labelSize;
	}

	public void setLabelSize(String labelSize) {
		this.labelSize = labelSize;
	}

	public String getLabelAlwaysVisible() {
		return labelAlwaysVisible;
	}

	public void setLabelAlwaysVisible(String labelAlwaysVisible) {
		this.labelAlwaysVisible = labelAlwaysVisible;
	}

	public String getLabelPosition() {
		return labelPosition;
	}

	public void setLabelPosition(String labelPosition) {
		this.labelPosition = labelPosition;
	}

	public String getLegendKey() {
		return legendKey;
	}

	public void setLegendKey(String legendKey) {
		this.legendKey = legendKey;
	}

	
	
}