/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.tables.HasFlags;
import com.opendoorlogistics.api.tables.TableFlags;

@XmlRootElement
public class ODLHasFlags implements Serializable , HasFlags{
	protected long flags;
	
	public ODLHasFlags(){}
	
	public ODLHasFlags(long flags){
		this.flags = flags;
	}
	
	public ODLHasFlags(ODLHasFlags copyThis){
		this.flags = copyThis.flags;
	}
	
	protected void setFlag(boolean active, long flag) {
		if(active){
			flags |=  flag;
		}else{
			flags &= ~flag;
		}

	}
	
	protected boolean getFlag(long flag){
		return (flags & flag)==flag;
	}
	
	public long getFlags() {
		return flags;
	}

	public void setFlags(long flags) {
		this.flags = flags;

	}
	
	public boolean getIsOptional(){
		return getFlag(TableFlags.FLAG_IS_OPTIONAL);
	}
	
	@XmlAttribute
	public void setIsOptional(boolean optional){
		setFlag(optional, TableFlags.FLAG_IS_OPTIONAL);
	}

	public boolean getIsBatchKey(){
		return getFlag(TableFlags.FLAG_IS_BATCH_KEY);
	}
	
	@XmlAttribute
	public void setIsBatchKey(boolean batchKey){
		setFlag(batchKey, TableFlags.FLAG_IS_BATCH_KEY);
	}

	public boolean getIsReportKey(){
		return getFlag(TableFlags.FLAG_IS_REPORT_KEYFIELD);
	}
	
	@XmlAttribute
	public void setIsReportKey(boolean batchKey){
		setFlag(batchKey, TableFlags.FLAG_IS_REPORT_KEYFIELD);
	}

	public boolean getIsGroupBy(){
		return getFlag(TableFlags.FLAG_IS_GROUP_BY_FIELD);
	}
	
	@XmlAttribute
	public void setIsGroupBy(boolean groupBy){
		setFlag(groupBy, TableFlags.FLAG_IS_GROUP_BY_FIELD);
	}

	
}
