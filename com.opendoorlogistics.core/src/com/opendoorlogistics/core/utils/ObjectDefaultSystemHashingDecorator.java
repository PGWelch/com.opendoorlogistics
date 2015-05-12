package com.opendoorlogistics.core.utils;

/**
 * Gives the decorated object the default (system) hashing behaviour
 * instead of its overridden equals and hashing behaviour. 
 * This assumes the object's hashcode never changes.
 * @author Phil
 *
 */
public class ObjectDefaultSystemHashingDecorator {
	private final Object decorated;
	private final int hashcode;

	public ObjectDefaultSystemHashingDecorator(Object decorated) {
		this.decorated = decorated;
		this.hashcode = decorated.hashCode();
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj || this.decorated == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectDefaultSystemHashingDecorator other = (ObjectDefaultSystemHashingDecorator) obj;
		
		// use == rather than equals to get system default behaviour
		return decorated == other.decorated;

	}

	
}
