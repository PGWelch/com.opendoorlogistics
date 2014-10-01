/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.util.UUID;

import com.opendoorlogistics.core.utils.strings.Strings;

class ReporterFrameIdentifier{
	private final String panelId;
	private final String instructionId;
	private final String scriptId;
	
	public ReporterFrameIdentifier(String scriptId, String instructionId, String panelId) {
		this.panelId = Strings.std(panelId);
		this.instructionId = Strings.std(instructionId);
		this.scriptId = Strings.std(scriptId);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((instructionId == null) ? 0 : instructionId.hashCode());
		result = prime * result + ((panelId == null) ? 0 : panelId.hashCode());
		result = prime * result + ((scriptId == null) ? 0 : scriptId.hashCode());
		return result;
	}
	
	public String getInstructionId(){
		return instructionId;
	}
	
	public String getScriptId(){
		return scriptId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReporterFrameIdentifier other = (ReporterFrameIdentifier) obj;
		if (instructionId == null) {
			if (other.instructionId != null)
				return false;
		} else if (!instructionId.equals(other.instructionId))
			return false;
		if (panelId == null) {
			if (other.panelId != null)
				return false;
		} else if (!panelId.equals(other.panelId))
			return false;
		if (scriptId == null) {
			if (other.scriptId != null)
				return false;
		} else if (!scriptId.equals(other.scriptId))
			return false;
		return true;
	}
	
	public String getCombinedId(){
		StringBuilder builder = new StringBuilder();
		builder.append(scriptId);
		builder.append("-");
		builder.append(instructionId);
		builder.append("-");
		builder.append(panelId);
		UUID ret = UUID.nameUUIDFromBytes(builder.toString().getBytes());
		return ret.toString();
	}
}
