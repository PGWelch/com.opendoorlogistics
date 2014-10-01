/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.iterators;

import java.util.Iterator;

import com.opendoorlogistics.core.utils.ObjectConverter;

final public class IteratorAdapter<From,To> implements Iterator<To>{
	final private Iterator<From> fromIt;
	final private ObjectConverter<From, To> converter;
	
	public IteratorAdapter(Iterator<From> fromIt, ObjectConverter<From, To> converter){
		this.fromIt = fromIt;
		this.converter = converter;
	}

	@Override
	public boolean hasNext() {
		return fromIt.hasNext();
	}

	@Override
	public To next() {
		return converter.convert(fromIt.next());
	}

	@Override
	public void remove() {
		fromIt.remove();
	}
	
	
}
