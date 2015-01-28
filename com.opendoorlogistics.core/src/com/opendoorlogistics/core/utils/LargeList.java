/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A list which uses multiple memory blocks to store its contents and therefore doesn't require a single contiguous memory block for the whole list.
 * The size of the list is still limited by Integer.MaxValue and the total available memory in the system but it is no longer limited by the the total
 * contiguous memory available.
 * 
 * @author Phil
 * 
 * @param <T>
 */
final public class LargeList<T> extends AbstractList<T> {
	public final static int DEFAULT_BLOCK_SIZE_BYTES = 1024 * 16; // 16 kb blocks
	public final static int DEFAULT_INITIAL_CAPACITY = 10; 
	private final int blockSize;
	private final ArrayList<T> firstBlock = new ArrayList<>();
	private final ArrayList<Object[]> blocks = new ArrayList<>();
	
	//private final ArrayList<ArrayList<T>> blocks = new ArrayList<>();
	private long size;

	public LargeList() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_BLOCK_SIZE_BYTES);
	}

	public LargeList(int initialCapacity){
		this(initialCapacity, DEFAULT_BLOCK_SIZE_BYTES);
	}
	
	public LargeList(int initialCapacity,int blockSize){
		this.blockSize = blockSize;
		ensureCapacity(initialCapacity);
	}
	
	public LargeList(Collection<T> collection){
		this(collection.size());
		for(T o : collection){
			add(o);
		}
	}

	@Override
    public void clear() {
		size=0;
		blocks.clear();
		firstBlock.clear();
	}

    public T remove(long index) {
		if(index<0 || index>=size){
			throw new IndexOutOfBoundsException();
		}
		
		T val = get(index);
		
		if(index==size-1){
			// special case... removing the end		
			if(index < blockSize){
				firstBlock.remove((int)index);
			}else{
				set(index, null);				
			}
			size--;
		}else{
			// copy everything down
			for(long i = index ; i<size-1; i++){
				set(i, get(i+1));
			}
			
			// then remove from the end
			remove(size-1);
		}
		return val;
    }

	@Override
    public T remove(int index) {
		return remove((long)index);
    }

	private int indexInBlock(long indx) {
		return (int) (indx % blockSize);
	}

	@SuppressWarnings("unchecked")
	public T get(long index){
		if(index < blockSize){
			return firstBlock.get((int)index);
		}	
		index -= blockSize;
		return (T) blocks.get((int)(index/blockSize))[indexInBlock(index)];
	}
	
	@Override
	public T get(int index) {
		return get((long)index);
	}

	@Override
	public int size() {
		if(size >Integer.MAX_VALUE){
			throw new RuntimeException("Cannot use size() method if list if greater than Integer.MaxValue, use longsize() instead");
		}
		return (int)size;
	}

	public long longSize(){
		return size;
	}
	
	@Override
	public T set(int index, T element) {
		return set((long)index, element);
	}

	public T set(long index, T element) {
		if(index < blockSize){
			firstBlock.set((int)index, element);
		}
		else{
			index -= blockSize;
			blocks.get((int)(index/blockSize))[indexInBlock(index)] = element;			
		}
		return element;
	}
	
    public void ensureCapacity(long minCapacity) {
    	if(minCapacity <= blockSize){
    		// small list.. just increase capacity on the first block
    		firstBlock.ensureCapacity((int)minCapacity);
    		return;
    	}
    	
    	// allocate blocks as needed
    	long capacity = (long)blocks.size() * blockSize;
    	while(capacity < minCapacity){
    		blocks.add(new Object[blockSize]);
    		capacity += blockSize;
    	}
    }
    
    @Override
    public void add(int index, T element) {
    	add((long)index,element);
    }
    
    public void add(long index, T element) {
    	if(index<0 || index>size){
    		throw new IndexOutOfBoundsException();
    	}
    	
    	ensureCapacity(size+1);
    	if(index==size){
    		// adding to the end... just add to the correct block

    		if(index < blockSize){
    			// still adding to first (variable length) block
    			firstBlock.add(element);
    		}else{
    			// adding to fixed list blocks
    			index -= blockSize;	
    			blocks.get((int)(index/blockSize))[indexInBlock(index)]=element;
    		}
    		size++;
    	}
    	else{
    		// Inserting - do this the *slow* way currently for simplicity
    		// If performance is needed, replace with arraycopy....
    		
    		// add empty position at the end and copy everything by 1 position
    		add(size, null);
    		for(long i = size-1; i> index ;i--){
    			set(i, get(i-1));
    		}
    		set(index, element);
    	}
    	

    }
    
    public void trimToSize() {
    	// trim blocks first
    	long nbBlocksNeeded = (size / blockSize) + 1;
    	while(blocks.size() > nbBlocksNeeded){
    		blocks.remove(blocks.size()-1);
    	}
   
    	if(size< blockSize){
    		firstBlock.trimToSize();
    	}
    }
    
    public int getBlockSize(){
    	return blockSize;
    }
    
	public static void main(String[] args)throws Exception {
	//	long nbMB = 5000;
		
		int size = 100;
		
		while(true){
			System.out.println("Size=" + size);
			int nbFails=0;
			
			try {
				new ArrayList<>(size);				
			} catch (OutOfMemoryError e) {
				nbFails++;
				System.out.println("Size " + size  + " - arraylist failed");
			}

			try {
				 new LargeList<>(size);				
			} catch (OutOfMemoryError e) {
				nbFails++;
				System.out.println("Size " + size  + " - largelist failed");
			}

			
			if(nbFails==2){
				break;
			}
			size *= 1.1;
		}
		
//		long elementSizeBytes = 4;
//		long nbKb = nbMB * 1024;
//		long nbBytes = nbKb * 1024;
//		long nbElems = nbBytes / elementSizeBytes;
//		System.out.println("NbElems = " + nbElems + " IntMax=" + Integer.MAX_VALUE);
//		if(nbElems < Integer.MAX_VALUE){
//			int[] arr = new int[nbElems];
//			for(int i =0; i < arr.length ; i++){
//				arr[i] = i;
//			}
//			System.out.println("Allocated!!!" + arr.length);	
//		}

	}

}
