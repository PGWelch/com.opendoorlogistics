/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

/**
 * An interface defining an object which can deep copy
 * other objects of the generic type.
 * @author Phil
 *
 * @param <T>
 */
public interface DeepCopier<T> {
	T deepCopy(T obj);
}
