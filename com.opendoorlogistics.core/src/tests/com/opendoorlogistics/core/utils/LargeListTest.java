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
import com.opendoorlogistics.core.utils.LargeList;
import com.opendoorlogistics.core.utils.TreeList;

public class LargeListTest {
	private static final int[] TEST_BLOCK_SIZES = new int[] { 10, 20, 100, 500 };

	/**
	 * Test simple add and removal
	 */
	@Test
	public void testAppending() {
		// test simple adding to the list
		for (int blockSize : TEST_BLOCK_SIZES) {
			int n = 10 * blockSize;
			LargeList<Integer> list = new LargeList<>(LargeList.DEFAULT_INITIAL_CAPACITY, blockSize);
			fillList(n, list);

			for (int i = 0; i < n; i++) {
				assertEquals((int) list.get(i), i);

			}

			assertEquals(list.size(), n);
		}

	}

	@Test
	public void testClearing() {
		// test simple adding to the list
		for (int blockSize : TEST_BLOCK_SIZES) {
			int n = 10 * blockSize;
			LargeList<Integer> list = new LargeList<>(LargeList.DEFAULT_INITIAL_CAPACITY, blockSize);
			fillList(n, list);
			list.clear();
			assertEquals(list.size(), 0);
		}

	}

	@Test
	public void testSetting() {
		for (int blockSize : TEST_BLOCK_SIZES) {
			int n = 10 * blockSize;
			LargeList<Integer> list = new LargeList<>(LargeList.DEFAULT_INITIAL_CAPACITY, blockSize);
			fillList(n, list);

			for (int i = 0; i < n; i++) {
				list.set(i, i * 2);
			}

			for (int i = 0; i < n; i++) {
				if ((int) list.get(i) != 2 * i) {
					System.out.println(i);
					assertEquals((int) list.get(i), 2 * i);

				}

			}

		}

	}

	@Test
	public void testInserting() {
		for (int blockSize : TEST_BLOCK_SIZES) {
			int n = 10 * blockSize;
			LargeList<Integer> llist = new LargeList<>(LargeList.DEFAULT_INITIAL_CAPACITY, blockSize);
			ArrayList<Integer> alist = new ArrayList<>();
			fillList(n, llist);
			fillList(n, alist);
			assertListsEqual(llist, alist);

			Random random = new Random(123);
			for (int i = 0; i < n; i++) {
				int index = random.nextInt(llist.size());
				llist.add(index, i);
				alist.add(index, i);
				// System.out.println(llist);
				// System.out.println(alist);
				assertListsEqual(llist, alist);
			}
		}
	}

	@Test
	public void testRemoving() {
		for (int blockSize : TEST_BLOCK_SIZES) {
			int n = 10 * blockSize;
			LargeList<Integer> llist = new LargeList<>(LargeList.DEFAULT_INITIAL_CAPACITY, blockSize);
			ArrayList<Integer> alist = new ArrayList<>();
			fillList(n, llist);
			fillList(n, alist);
			assertListsEqual(llist, alist);

			Random random = new Random(123);
			while (llist.size() > 0) {
				int index = random.nextInt(llist.size());
				llist.remove(index);
				alist.remove(index);
				// System.out.println(llist);
				// System.out.println(alist);
				assertListsEqual(llist, alist);
			}
		}
	}

	private static void fillList(int n, List<Integer> list) {
		for (int i = 0; i < n; i++) {
			list.add(i);
		}
	}

	private static void assertListsEqual(List<Integer> listA, List<Integer> listB) {
		assertEquals(listA.size(), listB.size());
		for (int i = 0; i < listA.size(); i++) {
			assertTrue(listA.get(i).equals(listB.get(i)));
		}
	}

	@Test
	public void testSequentialOperations() {
		Random random = new Random(123);
		for (int blockSize : new int[] { 1, 2, 4, 8, 16 }) {
			LargeList<Integer> llist = new LargeList<>(LargeList.DEFAULT_INITIAL_CAPACITY, blockSize);
			ArrayList<Integer> alist = new ArrayList<>();

			for (int i = 0; i < 10000; i++) {
				int value = random.nextInt(100);

				if (llist.size() == 0) {
					llist.add(value);
					alist.add(value);
				} else {

					int index = random.nextInt(llist.size());
					switch (random.nextInt(13)) {
					case 0:
					case 1:
					case 2:
					case 3:
					case 4:
						llist.add(value);
						alist.add(value);
						break;

					case 5:
					case 6:
					case 7:
						llist.remove(index);
						alist.remove(index);
						break;

					case 8:
					case 9:
						llist.add(index, value);
						alist.add(index, value);
						break;

					case 10:
					case 11:
						llist.set(index, value);
						alist.set(index, value);
						break;

					case 12:
						llist.clear();
						alist.clear();

					default:
						break;
					}
				}

//				String prefix = "BL=" + blockSize + " STEP=" + (i+1) + " ";
//				System.out.println(prefix + "L: " + llist);
//				System.out.println(prefix + "A: " + alist);
				assertListsEqual(llist, alist);
			}
		}
	}

}
