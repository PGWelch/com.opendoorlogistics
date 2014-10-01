/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Base class for any adapter that provides the list interface
 * @author Phil
 *
 * @param <T>
 */
public class ListAdapterBase<T> implements List<T>{

	@Override
	public int size() {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
        return size()==0;
	}

	@Override
	public boolean contains(Object o) {
        throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
        throw new UnsupportedOperationException();
	}

	@SuppressWarnings("hiding")
	@Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(T e) {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
        throw new UnsupportedOperationException();
	}

	@Override
	public T get(int index) {
        throw new UnsupportedOperationException();
	}

	@Override
	public T set(int index, T element) {
        throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, T element) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public T remove(int index) {
        throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
        throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException();
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
	}

}
