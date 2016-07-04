package com.opendoorlogistics.api.distances;

import java.io.Serializable;

public class ExternalMatrixFileConfiguration implements Serializable {
	private boolean useDefaultFile = true;
	private String nonDefaultFilename = "";

	public boolean isUseDefaultFile() {
		return useDefaultFile;
	}

	public void setUseDefaultFile(boolean useDefaultFile) {
		this.useDefaultFile = useDefaultFile;
	}

	public String getNonDefaultFilename() {
		return nonDefaultFilename;
	}

	public void setNonDefaultFilename(String nonDefaultFilename) {
		this.nonDefaultFilename = nonDefaultFilename;
	}
	
	public ExternalMatrixFileConfiguration deepCopy(){
		ExternalMatrixFileConfiguration ret = new ExternalMatrixFileConfiguration();
		ret.setUseDefaultFile(isUseDefaultFile());
		ret.setNonDefaultFilename(getNonDefaultFilename());
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nonDefaultFilename == null) ? 0 : nonDefaultFilename.hashCode());
		result = prime * result + (useDefaultFile ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExternalMatrixFileConfiguration other = (ExternalMatrixFileConfiguration) obj;
		if (nonDefaultFilename == null) {
			if (other.nonDefaultFilename != null)
				return false;
		} else if (!nonDefaultFilename.equals(other.nonDefaultFilename))
			return false;
		if (useDefaultFile != other.useDefaultFile)
			return false;
		return true;
	}

}
