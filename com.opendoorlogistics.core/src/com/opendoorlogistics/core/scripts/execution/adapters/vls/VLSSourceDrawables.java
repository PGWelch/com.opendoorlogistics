package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import java.util.Set;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;

@ODLTableName(VLSSourceDrawables.SOURCE_PREFIX + "Your-Source-Name")

/**
 * Source drawables can take extra columns because additional columns required by the label formula such
 * as quantities are passed this way. The table is optional because (a) it should be possible to define
 * a VLS without sources (i.e. reading shapefiles instead) and (b) source tables names will be include their source name,
 * and hence be different with only a common prefix.
 * @author Phil
 *
 */
@ODLTableFlags(TableFlags.FLAG_COLUMN_WILDCARD|TableFlags.FLAG_IS_OPTIONAL|TableFlags.FLAG_TABLE_NAME_WILDCARD)
public class VLSSourceDrawables extends DrawableObjectImpl{

	
	public static final String SOURCE_PREFIX = "Source-";
	
	public static final BeanTableMappingImpl BEAN_MAPPING =BeanMapping.buildDatastore(VLSSourceDrawables.class).getTableMapping(0); 

	public static final Set<String> STD_COLUMN_NAMES;
	static{
		STD_COLUMN_NAMES = new StandardisedStringSet(false);
		ODLTableDefinition dfn = BEAN_MAPPING.getTableDefinition();
		for(int i =0 ;i < dfn.getColumnCount() ; i++){
			STD_COLUMN_NAMES.add(dfn.getColumnName(i));
		}
	}
	
	private String VLSKey1;
	private String VLSKey2;
	private String VLSKey3;
	private String VLSKey4;
	
	public static final int COL_VLSKEY1 = DrawableObjectImpl.COL_MAX+1;
	public static final int COL_VLSKEY2 = COL_VLSKEY1+1;
	public static final int COL_VLSKEY3 = COL_VLSKEY2+1;
	public static final int COL_VLSKEY4 = COL_VLSKEY3+1;
	
	public String getVLSKey1() {
		return VLSKey1;
	}
	
	@ODLColumnOrder(COL_VLSKEY1)
	@ODLColumnName("VLSKey1")
	@ODLNullAllowed
	public void setVLSKey1(String vLSKey1) {
		VLSKey1 = vLSKey1;
	}
	public String getVLSKey2() {
		return VLSKey2;
	}
	
	@ODLColumnName("VLSKey2")
	@ODLColumnOrder(COL_VLSKEY2)
	@ODLNullAllowed
	public void setVLSKey2(String vLSKey2) {
		VLSKey2 = vLSKey2;
	}
	public String getVLSKey3() {
		return VLSKey3;
	}
	
	@ODLColumnName("VLSKey3")
	@ODLNullAllowed
	@ODLColumnOrder(COL_VLSKEY3)
	public void setVLSKey3(String vLSKey3) {
		VLSKey3 = vLSKey3;
	}
	public String getVLSKey4() {
		return VLSKey4;
	}
	
	@ODLColumnName("VLSKey4")
	@ODLNullAllowed
	@ODLColumnOrder(COL_VLSKEY4)
	public void setVLSKey4(String vLSKey4) {
		VLSKey4 = vLSKey4;
	}
	
	
}