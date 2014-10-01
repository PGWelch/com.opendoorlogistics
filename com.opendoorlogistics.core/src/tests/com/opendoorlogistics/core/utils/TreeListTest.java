/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package tests.com.opendoorlogistics.core.utils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.opendoorlogistics.core.utils.DeepCopier;
import com.opendoorlogistics.core.utils.TreeList;

public class TreeListTest {
	private final int length = 100;
	private final int nbRepeats=10;

	/**
	 * Test simple add and removal
	 */
	@Test
	public void testSimpleAddRemove() {
		TreeList<Integer> list = new TreeList<>(false);
		list.setDebugChecks(true);

		// add remove root with no child
		list.add(10);
		list.removeAt(0);
		assertEquals(list.size(), 0);
		
		// remove root with one child
		list = new TreeList<>(false);
		list.setDebugChecks(true);
		
		list.insert(0, 10);
		list.insert(1, 20);
		list.removeAt(0);
		assertEquals(20, (int)list.get(0).getValue());
		
		// remove root with two children
		list = new TreeList<>(false);
		list.setDebugChecks(true);
		
		list.insert(0, 10);
		list.insert(1, 20);
		list.insert(0, 5);
		list.removeAt(1);
		assertEquals(5, (int)list.get(0).getValue());	
		assertEquals(20, (int)list.get(1).getValue());
	}
	
	/**
	 * Test that indices fuction correctly in an unsorted list
	 */
	@Test
	public void testUnsortedIndices() {
			
		// insert n integers in random positions
		Random random = new Random(123);
		for(int j =0 ; j < nbRepeats ; j++){
			TreeList<Integer> list = createUnsorted(random);
			assertEquals(length, list.size());
			assertTrue(isIndicesCorrect(list));		
		}
	}
	
	/**
	 * Retrieve each node by index and check the node's index equals the
	 * retrieval index
	 * @param list
	 * @return
	 */
	private static boolean isIndicesCorrect(TreeList<Integer> list ){
		for(int i =0 ;i< list.size() ; i++){
			TreeList<Integer>.TreeListNode node = list.get(i);
			if(i!= node.getIndex()){
				return false;
			}
		}	
		return true;
	}
	
	/**
	 * Test that extracting the contents via an iterator or via
	 * the indices gives the same results
	 */
	@Test
	public void testIterator(){
		Random random = new Random(123);
		for(int j =0 ; j < nbRepeats ; j++){
			TreeList<Integer> list = createUnsorted(random);
			
			ArrayList<Integer> fromIt = extractListUsingIterator(list);
			
			ArrayList<Integer> fromRandAccess = new ArrayList<>();
			for(int i = 0 ; i < list.size() ; i++){
				fromRandAccess.add(list.get(i).getValue());
			}
			
			// check lists retrieved by different methods are equal
			assertListsEqual(fromIt, fromRandAccess);
		}		
	}

	private void assertListsEqual(List<Integer> listA, List<Integer> listB) {
		assertEquals(listA.size(), listB.size());
		for(int i =0 ; i<listA.size() ; i++){
			assertTrue(listA.get(i).equals(listB.get(i)));
		}
	}

	private ArrayList<Integer> extractListUsingIterator(TreeList<Integer> list) {
		ArrayList<Integer> fromIt = new ArrayList<>();
		for(TreeList<Integer>.TreeListNode node : list){
			fromIt.add(node.getValue());
		}
		return fromIt;
	}

	/**
	 * Create an unsorted list
	 * @param random
	 * @return
	 */
	private TreeList<Integer> createUnsorted(Random random) {
		TreeList<Integer> list = new TreeList<>(false);
		list.setDebugChecks(true);
		
		for(int i =0 ;i< length ; i++){
			int index = random.nextInt(list.size()+1);
			list.insert(index, random.nextInt());
		}
		return list;
	}


	/**
	 * Test the input list is correctly sorted numerically
	 * @param list
	 * @return
	 */
	static boolean isSorted(TreeList<Integer> list ){
		for(int j =1 ; j < list.size() ; j++){
			if(list.get(j-1).getValue() > list.get(j).getValue()){
				return false;
			}
		}				
		return true;
	}

	/**
	 * Create a sorted list and check it is sorted
	 */
	@Test
	public void testSorted() {
		Random random = new Random(3456);
		for(int i =0 ; i < nbRepeats ; i++){
			// create sorted and check it is sorted
			TreeList<Integer> list = createSorted(random);		
			assertTrue(isSorted(list));
			assertTrue(isIndicesCorrect(list));
		}
	}


	/**
	 * Create a sorted list and then remove from it in random
	 * order ensuring the list is still sorted.
	 */
	@Test
	public void testSortedWithRemovals() {
		Random random = new Random(3456);
		for(int i =0 ; i < nbRepeats ; i++){
			// create sorted, keep on removing and checking still sorted
			TreeList<Integer> list = createSorted(random);		
			while(list.size() > 0){
				removeFromRandomPosition(random, list);
				assertTrue(isSorted(list));		
				assertTrue(isIndicesCorrect(list));
			}			
		}
	}

	private void removeFromRandomPosition(Random random, TreeList<Integer> list) {
		int index = random.nextInt(list.size());
		Object atIndex = list.get(index);
		Object removed = list.removeAt(index);
		assertEquals(atIndex, removed);
	}

	/**
	 * Create a sorted list and then remove from it in random
	 * order ensuring the list is still sorted.
	 */
	@Test
	public void testSortedWithRemovalsAndInserts() {
		Random random = new Random(3456);
		for(int i =0 ; i < nbRepeats ; i++){
			TreeList<Integer> list  = new TreeList<>(false);
			list.setDebugChecks(true);

			for(int j =0 ;j< length ; j++){
				if(random.nextBoolean() && list.size()>0){
					removeFromRandomPosition(random, list);
				}else{
					int val = random.nextInt();
					insertInSortedPosition(list, val);
				}

				assertTrue(isSorted(list));		
				assertTrue(isIndicesCorrect(list));
			}		
		}
	}

	
	/**
	 * Create a sorted list
	 * @param random
	 * @return
	 */
	private TreeList<Integer> createSorted(Random random) {
		TreeList<Integer> list = new TreeList<>(false);
		list.setDebugChecks(true);
		
		for(int i =0 ;i< length ; i++){
			int value = random.nextInt();			
			insertInSortedPosition(list, value);
		}
		return list;
	}
	
	@Test
	public void testDeepCopy() {
		Random random = new Random(123);
		TreeList<Integer> original = createSorted(random);
		DeepCopier<Integer> valueCopier = new DeepCopier<Integer>() {
			
			@Override
			public Integer deepCopy(Integer obj) {
				return obj;
			}
		};
		
		TreeList<Integer> copy = original.deepCopy(valueCopier);
		List<Integer> originalList = extractListUsingIterator(original);
		List<Integer> copyList = extractListUsingIterator(copy);
		isIndicesCorrect(copy);
		assertListsEqual(originalList, copyList);
	
	}

	private void insertInSortedPosition(TreeList<Integer> list, int value) {
		int found=-1;
		for(int j =0 ; j < list.size() ; j++){
			if(list.get(j).getValue() >= value){
				found = j;
				break;
			}
		}
		
		if(found == -1){
			// add at end
			found = list.size();
		}
		
		list.insert(found, value);
	}
	
	@Test
	public void testSimpleRebuild(){
		Random random = new Random(123);
		for(int i =0 ; i < nbRepeats ; i++){
			TreeList<Integer> list = createSorted(random);
			ArrayList<Integer> before = list.toArrayList();
			list.rebuild();
			assertTrue(isSorted(list));		
			assertTrue(isIndicesCorrect(list));
			ArrayList<Integer> after = list.toArrayList();
			assertTrue(before.equals(after));			
		}
	}
	
	@Test
	public void testComplexRebuild(){
		Random random = new Random(123);
		for(int i =0 ; i < nbRepeats ; i++){
			TreeList<Integer> list  = new TreeList<>(false);
			list.setDebugChecks(true);

			for(int j =0 ;j< length ; j++){
				if(random.nextBoolean() && list.size()>0){
					removeFromRandomPosition(random, list);
				}else{
					int val = random.nextInt();
					insertInSortedPosition(list, val);
				}

				// test rebuild
				ArrayList<Integer> before = list.toArrayList();
				list.rebuild();
				assertTrue(isSorted(list));		
				assertTrue(isIndicesCorrect(list));
				ArrayList<Integer> after = list.toArrayList();
				assertTrue(before.equals(after));							
			}		
		}
	}
	
	@Test
	public void testAutomaticRebuild(){
		Random random = new Random(123);
		for(int i =0 ; i < nbRepeats ; i++){
			TreeList<Integer> list  = new TreeList<>(true);
			list.setDebugChecks(true);

			for(int j =0 ;j< length ; j++){
				if(random.nextBoolean() && list.size()>0){
					removeFromRandomPosition(random, list);
				}else{
					int val = random.nextInt();
					insertInSortedPosition(list, val);
				}

				assertTrue(isSorted(list));		
				assertTrue(isIndicesCorrect(list));
						
			}		
		}
	}
}
