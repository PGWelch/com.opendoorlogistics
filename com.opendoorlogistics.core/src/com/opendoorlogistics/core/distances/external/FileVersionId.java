package com.opendoorlogistics.core.distances.external;

import java.io.File;

public class FileVersionId {
	private final File file;
	private final long fileSizeInBytes;
	private final long lastModifiedTime;
	
	public FileVersionId(File file){
		this.file = file;
		fileSizeInBytes = file.length();
		lastModifiedTime = file.lastModified();
	}

	public long getFileSizeInBytes() {
		return fileSizeInBytes;
	}

	public long getLastModifiedTime() {
		return lastModifiedTime;
	}

	public File getFile() {
		return file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + (int) (fileSizeInBytes ^ (fileSizeInBytes >>> 32));
		result = prime * result + (int) (lastModifiedTime ^ (lastModifiedTime >>> 32));
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
		FileVersionId other = (FileVersionId) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (fileSizeInBytes != other.fileSizeInBytes)
			return false;
		if (lastModifiedTime != other.lastModifiedTime)
			return false;
		return true;
	}
	
	
	
	public static boolean isFileModified(FileVersionId versionId){
		if(!versionId.getFile().exists()){
			// very modified... no longer exists
			return true;
		}
		
		FileVersionId newVersion = new FileVersionId(versionId.getFile());
		return !newVersion.equals(versionId);
	}
}
