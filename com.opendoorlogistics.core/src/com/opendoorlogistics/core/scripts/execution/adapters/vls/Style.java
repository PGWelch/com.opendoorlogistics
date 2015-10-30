package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.UserRenderFlags;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

@ODLTableName(Style.TABLE_NAME)
public class Style extends BeanMappedRowImpl {
	private static final int COL_LAYERID=0;
	private static final int COL_KEY1=COL_LAYERID+1;
	private static final int COL_KEY2=COL_KEY1+1;
	private static final int COL_KEY3=COL_KEY2+1;
	private static final int COL_KEY4=COL_KEY3+1;
	private static final int COL_FILTER=COL_KEY4+1;
	private static final int COL_COLOUR=COL_FILTER+1;
	private static final int COL_WIDTH =COL_COLOUR+1;
	private static final int COL_SYMBOL=COL_WIDTH +1;
	private static final int COL_OUTLINED=COL_SYMBOL+1;
	private static final int COL_OPAQUE=COL_OUTLINED+1;
	private static final int COL_LABEL=COL_OPAQUE+1;
	private static final int COL_LABELALWAYSVISIBLE=COL_LABEL+1;
	private static final int COL_LABELCOLOUR=COL_LABELALWAYSVISIBLE+1;
	private static final int COL_LABELPOSITION =COL_LABELCOLOUR+1;
	private static final int COL_LABELPRIORITY =COL_LABELPOSITION+1;
	private static final int COL_LABELSIZE=COL_LABELPRIORITY +1;
	private static final int COL_LEGENDKEY=COL_LABELSIZE+1;
	private static final int COL_DESCRIPTION=COL_LEGENDKEY+1;
	
	public static final int NB_RULE_KEYS=4;
	public static final String TABLE_NAME = "Styles";
	private String layerId;
	private String key1;
	private String key2;
	private String key3;
	private String key4;
	private String filter;
	private String colour;
	private String width; // filter width zero??
	private String symbol;
	private String outlined;
	private String opaque;
	private String label;
	private String labelAlwaysVisible;
	private String labelColour;
	private String labelPosition; 
	private String labelPriority; 
	private String labelSize;
	private String legendKey;
	private String description;

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
		LABELPRIORITY(DrawableObjectImpl.COL_LABEL_PRIORITY,ODLColumnType.LONG),
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
			
		case LABELPRIORITY:
			return getLabelPriority();
			
		case LABELSIZE:
			return getLabelSize();
			
		case LEGENDKEY:
			return getLegendKey();
			
		default:
			throw new IllegalArgumentException();
		}
		
	}
	
	public String getLayerId() {
		return layerId;
	}

	@ODLColumnOrder(COL_LAYERID)
	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public String getKey1() {
		return key1;
	}

	@ODLColumnOrder(COL_KEY1)
	@ODLNullAllowed
	public void setKey1(String key1) {
		this.key1 = key1;
	}

	public String getKey2() {
		return key2;
	}

	@ODLColumnOrder(COL_KEY2)
	@ODLNullAllowed	
	public void setKey2(String key2) {
		this.key2 = key2;
	}

	public String getKey3() {
		return key3;
	}

	@ODLColumnOrder(COL_KEY3)
	@ODLNullAllowed	
	public void setKey3(String key3) {
		this.key3 = key3;
	}

	public String getKey4() {
		return key4;
	}

	@ODLColumnOrder(COL_KEY4)
	@ODLNullAllowed	
	public void setKey4(String key4) {
		this.key4 = key4;
	}

	public String getColour() {
		return colour;
	}

	@ODLColumnOrder(COL_COLOUR)	
	@ODLNullAllowed	
	public void setColour(String colour) {
		this.colour = colour;
	}

	public String getWidth() {
		return width;
	}
	
	@ODLColumnOrder(COL_WIDTH)
	@ODLNullAllowed	
	public void setWidth(String width) {
		this.width = width;
	}

	public String getSymbol() {
		return symbol;
	}

	@ODLColumnOrder(COL_SYMBOL)
	@ODLNullAllowed	
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getOutlined() {
		return outlined;
	}

	@ODLColumnOrder(COL_OUTLINED)	
	@ODLNullAllowed	
	public void setOutlined(String outlined) {
		this.outlined = outlined;
	}

	public String getOpaque() {
		return opaque;
	}

	@ODLColumnOrder(COL_OPAQUE)
	@ODLNullAllowed	
	public void setOpaque(String opaque) {
		this.opaque = opaque;
	}

	public String getLabelColour() {
		return labelColour;
	}

	@ODLColumnOrder(COL_LABELCOLOUR)
	@ODLNullAllowed	
	public void setLabelColour(String labelColour) {
		this.labelColour = labelColour;
	}

	public String getLabel() {
		return label;
	}

	
	@ODLColumnOrder(COL_LABEL)
	@ODLNullAllowed	
	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabelSize() {
		return labelSize;
	}

	
	@ODLColumnOrder(COL_LABELSIZE)
	@ODLNullAllowed	
	public void setLabelSize(String labelSize) {
		this.labelSize = labelSize;
	}

	public String getLabelAlwaysVisible() {
		return labelAlwaysVisible;
	}
	
	@ODLColumnOrder(COL_LABELALWAYSVISIBLE)
	@ODLNullAllowed	
	public void setLabelAlwaysVisible(String labelAlwaysVisible) {
		this.labelAlwaysVisible = labelAlwaysVisible;
	}

	public String getLabelPosition() {
		return labelPosition;
	}

	@ODLColumnOrder(COL_LABELPOSITION)
	@ODLNullAllowed	
	public void setLabelPosition(String labelPosition) {
		this.labelPosition = labelPosition;
	}

	public String getLegendKey() {
		return legendKey;
	}

	@ODLColumnOrder(COL_LEGENDKEY)
	@ODLNullAllowed	
	public void setLegendKey(String legendKey) {
		this.legendKey = legendKey;
	}

	public String getLabelPriority() {
		return labelPriority;
	}

	
	@ODLColumnOrder(COL_LABELPRIORITY)
	@ODLNullAllowed	
	public void setLabelPriority(String labelPriority) {
		this.labelPriority = labelPriority;
	}

	public String getFilter() {
		return filter;
	}

	@ODLColumnOrder(COL_FILTER)
	@ODLNullAllowed	
	public void setFilter(String filter) {
		this.filter = filter;
	}

	public String getDescription() {
		return description;
	}

	@ODLColumnOrder(COL_DESCRIPTION)
	@ODLNullAllowed
	public void setDescription(String description) {
		this.description = description;
	}

	
	
}