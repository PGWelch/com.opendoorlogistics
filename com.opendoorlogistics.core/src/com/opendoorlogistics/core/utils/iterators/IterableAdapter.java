/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.iterators;

import java.util.Iterator;

import com.opendoorlogistics.core.utils.ObjectConverter;

final public class IterableAdapter<From,To> implements Iterable<To> {
	final private Iterable<From> from;
	final private ObjectConverter<From, To> converter;
	
	public IterableAdapter(Iterable<From> from, ObjectConverter<From, To> converter) {
		this.from = from;
		this.converter = converter;
	}

	@Override
	public Iterator<To> iterator() {
		return new IteratorAdapter<>(from.iterator(), converter);
	}

}
