/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.Image;
import java.awt.image.BufferedImage;

import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.utils.images.CompressedImage;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.images.CompressedImage.CompressedType;

/**
 * Cache which stores only the recently used images
 * @author Phil
 *
 */
final public class RecentImageCache {
	public static final int DEFAULT_SIZE_IN_BYTES =64 * 1024 * 1024;
	private final RecentlyUsedCache lastUsedCache;
	private final ZipType zipType;
	
	public enum ZipType{
		PNG,
		LZ4,
		NONE
	}

	public RecentImageCache(ZipType zipType, int sizeInBytes){
		this.zipType = zipType;
		this.lastUsedCache = new RecentlyUsedCache("recent-image-cache", sizeInBytes);
	}
	
	public RecentImageCache(ZipType zipType){
		this(zipType, DEFAULT_SIZE_IN_BYTES);
	}
	

	public void put(Object objectKey, BufferedImage img){
		switch(zipType){
		case PNG:{
			CompressedImage ci= new CompressedImage(img, CompressedType.PNG);
			lastUsedCache.put(objectKey, ci, ci.getSizeBytes());
			break;			
		}
			
		case LZ4:{
			CompressedImage ci= new CompressedImage(img, CompressedType.LZ4);
			lastUsedCache.put(objectKey, ci, ci.getSizeBytes());
			break;			
		}
			
		case NONE:
			lastUsedCache.put(objectKey, img, img.getWidth() * img.getHeight() * 4);
			break;
		}
	}
	
	
	public Image get(Object key){
		Object val = lastUsedCache.get(key);
		if(val!=null){
			if(zipType==ZipType.NONE){
				return (Image)val;
			}else{
				return ((CompressedImage)val).get();
			}
		}
		return null;
	}
	
	public BufferedImage getBufferedImage(Object key){
		Image ret = get(key);
		if(ret!=null){
			return ImageUtils.toBufferedImage(ret);
		}
		return null;
	}
}
