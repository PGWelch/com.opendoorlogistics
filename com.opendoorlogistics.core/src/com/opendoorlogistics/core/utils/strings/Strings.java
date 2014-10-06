/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import com.opendoorlogistics.core.utils.Numbers;

final public class Strings {
	private static final HashSet<Character> unicodeSpeechMarks;

	private static Pattern nonAlphaNumericOrWhitespace = Pattern.compile("[^a-zA-Z\\d\\s\\-]");

	static {
		unicodeSpeechMarks = new HashSet<>();
		for (char c : new char[] { '\u0022', '\u201C', '\u201D', '\u201E', '\u201F', '\u301D', '\u301E', '\u301F', '\uFF02', '\u2033', '\u2036' }) {
			unicodeSpeechMarks.add(c);
		}
	}

	private Strings() {
	}

	public interface DoesStringExist {
		boolean isExisting(String s);
	}

	/**
	 * Check if the string s is already used and if so, add a number to the end of it (1, 2, 3...) to make it unique
	 * 
	 * @param s
	 * @return
	 */
	public static String makeUnique(String s, DoesStringExist cb) {
		long l = 0;
		while (l < Long.MAX_VALUE) {
			String ret = s + (l == 0 ? "" : Long.toString(l));
			if (cb.isExisting(ret) == false) {
				return ret;
			}
			l++;
		}
		return null;
	}

	public static String standardiseSpeechMarks(String s) {
		int n = s.length();
		StringBuilder builder = new StringBuilder(n);
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if (unicodeSpeechMarks.contains(c)) {
				builder.append("\"");
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}

	public static String toCommas(String... strs) {
		return toString(",", strs);
	}

	public static String toCommas(Collection<String> strs) {
		return toCommas(strs.toArray(new String[strs.size()]));
	}

	public static String toFirstLetterInWordCapitalised(String s) {
		int n = s.length();
		StringBuilder builder = new StringBuilder();
		boolean lastIsSpace = true;
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if (lastIsSpace) {
				builder.append(Character.toUpperCase(c));
			} else {
				builder.append(Character.toLowerCase(c));
			}
			lastIsSpace = Character.isWhitespace(c);
		}
		return builder.toString();
	}

	public static interface ToString<T> {
		String toString(T o);
	}

	public static <T> String toCommas(ToString<T> toString, Collection<T> objs) {
		return toString(toString, ",", objs);
	}

	public static <T> String toString(ToString<T> toString, String separator, Collection<T> objs) {
		int count = 0;
		StringBuilder builder = new StringBuilder();
		for (T obj : objs) {
			if (count > 0) {
				builder.append(separator);
			}
			builder.append(toString.toString(obj));
		}
		return builder.toString();
	}

	public static String toString(String separator, Collection<String> strs) {
		return toString(separator, strs.toArray(new String[strs.size()]));
	}

	/**
	 * Does the collection contain the input string when both are standardised?
	 * 
	 * @param find
	 * @param findIn
	 * @return
	 */
	public static boolean containsStandardised(String find, Iterable<String> findIn) {
		for (String test : findIn) {
			if (equalsStd(find, test)) {
				return true;
			}
		}
		return false;
	}

	public static String toString(String separator, String... strs) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < strs.length; i++) {
			builder.append(strs[i]);
			if (i != strs.length - 1) {
				builder.append(separator);
			}
		}
		return builder.toString();
	}

	public static <T> String[] toStringArray(T[] objs) {
		String[] ret = new String[objs.length];
		for (int i = 0; i < objs.length; i++) {
			ret[i] = objs[i].toString();
		}
		return ret;
	}

	/**
	 * See http://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars
	 * 
	 * @param s
	 * @return
	 */
	public static String toFileSafeString(String s) {
		return s.replaceAll("[^a-zA-Z0-9\\._]+", "_");
	}

	public static String[] toLowerCaseArray(String[] arr) {
		String[] ret = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ret[i] = arr[i].toLowerCase();
		}
		return ret;
	}

	public static TreeSet<String> stdTreeSet(Iterable<String> iterable) {
		TreeSet<String> ret = new TreeSet<>();
		if (iterable != null) {
			for (String s : iterable) {
				if (s != null) {
					String std = std(s);
					if (std.length() > 0) {
						ret.add(std(s));
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Standardised version of a string value
	 * 
	 * @param s
	 * @return
	 */
	public static String std(String s) {
		if(s==null){
			return "";
		}
		
		// trim whitespace at start and end and convert to lowercase
		s = s.trim().toLowerCase();

		// ensure only have single spaces
		s = s.replaceAll("  ", " ");
		return s;
	}

	/**
	 * Find the index of the string in the array or return -1 if not found. An exact match is favoured over a standardised match.
	 * 
	 * @param find
	 * @param strs
	 * @return
	 */
	public static int indexOfStd(String find, String[] strs) {
		for (int i = 0; i < strs.length; i++) {
			if (find.equals(strs[i])) {
				return i;
			}
		}

		for (int i = 0; i < strs.length; i++) {
			if (equalsStd(find, strs[i])) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Case-sensitive equals which will return true if both a and b are null
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equals(String a, String b) {
		if ((a == null) != (b == null)) {
			return false;
		}

		if (a == null) {
			// b must also be null
			return true;
		}

		return a.equals(b);
	}

	public static boolean equalsStd(String a, String b) {
		if (isEmpty(a) && isEmpty(b)) {
			return true;
		}

		if (a == null || b == null) {
			return false;
		}
		return std(a).equals(std(b));
	}

	private static class StdStringComparer {
		final private String notNumberGroup = "([^0-9]*)";
		final private String number = "(\\d+)";
		final private Pattern pattern = Pattern.compile(notNumberGroup + number + notNumberGroup);
		final private static StdStringComparer singleton = new StdStringComparer();

		private class StringComponents implements Comparable<StringComponents> {
			String original;
			String nonNumber;
			Long number;
			String secondNonNumber;

			public StringComponents(String s) {
				original = s;
				s = Strings.std(s);
				nonNumber = s;
				number = Long.MIN_VALUE;
				secondNonNumber = "";
				Matcher matcher = pattern.matcher(s);
				if (matcher.matches()) {
					Long l = Numbers.toLong(matcher.group(2));
					if (l != null) {
						nonNumber = Strings.std(matcher.group(1));
						number = l;
						secondNonNumber = Strings.std(matcher.group(3));
					}
				}
			}

			@Override
			public int compareTo(StringComponents o) {
				int diff = nonNumber.compareTo(o.nonNumber);
				if (diff == 0) {
					diff = Long.compare(number, o.number);
				}
				if (diff == 0) {
					diff = secondNonNumber.compareTo(o.secondNonNumber);
				}
				return diff;
			}

			@Override
			public String toString() {
				return original;
			}
		}

		private StdStringComparer() {
		}

		public int compare(String a, String b) {
			int diff = Boolean.compare(isEmpty(a), isEmpty(b));
			if (diff == 0 && a == null) {
				// must both be empty...
				return 0;
			}
			StringComponents ca = new StringComponents(a);
			StringComponents cb = new StringComponents(b);
			diff = ca.compareTo(cb);
			return diff;
		}

		public static StdStringComparer singleton() {
			return singleton;
		}
	}

	public static String[] addToArray(String [] arr, String s){
		String []ret = new String[arr.length+1];
		System.arraycopy(arr, 0, ret, 0, arr.length);
		ret[arr.length] = s;
		return ret;
	}
	
	/**
	 * Standardised comparison of two strings. 
	 * The comparison compares the standardised version of the two strings.
	 * It also handles the situation where you have a word followed by a number,
	 * e.g. "vehicle 9", "vehicle 11", and applies numeric sorting to the number part. 
	 * @param a
	 * @param b
	 * @return
	 */
	public static int compareStd(String a, String b) {
		return StdStringComparer.singleton().compare(a, b);
	}

	public static boolean isTrue(String s) {
		if (s == null) {
			return false;
		}
		s = std(s);
		return s.equals("1") || s.equals("t") || s.equals("true") || s.equals("y") || s.equals("yes");
	}

	public static boolean hasWhiteSpace(String s) {
		return hasWhitespace.matcher(s).matches();
	}

	private final static Pattern hasWhitespace = Pattern.compile("\\S*\\s+.*");

	private final static Pattern isNumber = Pattern.compile("\\s*-?\\d+\\.?\\d*\\s*");
	private final static Pattern isIntNumber = Pattern.compile("\\s*-?\\d+\\s*$");

	/**
	 * Tests if the input string is an integer or decimal number. Leading and trailing whitespace is ignored.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isNumber(String s) {
		if (Strings.isEmpty(s)) {
			return false;
		}
		return isNumber.matcher(s).matches();
	}

	private final static Pattern isEmail = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

	/**
	 * Validates an email using the regular expression from
	 * http://examples.javacodegeeks.com/core-java/util/regex/matcher/validate-email-address-with-java-regular-expression-example/
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isEmailAddress(String s) {
		return isEmail.matcher(s).matches();
	}

	public static boolean isIntNumber(String s) {
		if (Strings.isEmpty(s)) {
			return false;
		}
		return isIntNumber.matcher(s).matches();
	}

	public static boolean isEnclosedBySpeechMarks(String s) {
		return s.length() >= 2 && s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"';
	}

	/**
	 * Parse integer without throwing exception
	 * 
	 * @param s
	 * @return
	 */
	public static Long parseLong(String s) {
		if (isIntNumber(s)) {
			try {
				return Long.parseLong(s);
			} catch (Throwable e) {
				// TODO: handle exception
			}
		}
		return null;
	}

	public static String getTabs(int nbTabs) {
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < nbTabs; i++) {
			ret.append('\t');
		}
		return ret.toString();
	}

	/**
	 * Adds additional tabs to the start of each line
	 * 
	 * @param s
	 * @param nbTabs
	 * @return
	 */
	public static String getTabIndented(String s, int nbTabs) {
		String tabs = getTabs(nbTabs);

		StringBuilder ret = new StringBuilder();
		int len = s.length();

		if (len > 0) {
			ret.append(tabs);
		}
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			ret.append(c);
			if (c == '\n' && i < len - 1) {
				ret.append(tabs);
			}
		}
		return ret.toString();
	}

	public static boolean isEmpty(Object o) {
		return o == null || isEmpty(o.toString());
	}

	public static boolean isEmptyWhenStandardised(Object o) {
		return o == null || isEmptyWhenStandardised(o.toString());
	}

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}

	public static boolean isEmptyWhenStandardised(String s) {
		return isEmpty(std(s));
	}

	public static void writeToFile(String s, File file) {
		try {
			PrintWriter out = new PrintWriter(file);
			out.append(s);
			out.close();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static List<String> readFileAsLines(String path) {
		BufferedReader br = null;
		ArrayList<String> ret = new ArrayList<>();
		try {
			br = new BufferedReader(new FileReader(path));
			String line = br.readLine();
			while (line != null) {
				ret.add(line);
				line = br.readLine();
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Throwable e2) {
					throw new RuntimeException(e2);
				}
			}
		}
		return ret;
	}

	public static String readUTF8Resource(String name) {
		InputStream is = Object.class.getResourceAsStream(name);
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(is, writer, Charsets.UTF_8);
			is.close();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		return writer.toString();
	}

	public static String readFile(String path) {
		return readFile(path, Charset.defaultCharset());
	}

	public static String readFile(String path, Charset encoding) {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return encoding.decode(ByteBuffer.wrap(encoded)).toString();

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts text to a more-display friendly format.
	 * 
	 * @param text
	 * @return
	 */
	public static String convertEnumToDisplayFriendly(String text) {
		text = text.replaceAll("_", " ");
		text = text.replaceAll("  ", " ");
		text = text.trim();
		text = text.toLowerCase();
		if (text.length() > 0) {
			text = text.substring(0, 1).toUpperCase() + text.substring(1);
		}
		return text;
	}

	/**
	 * Converts text to a more-display friendly format.
	 * 
	 * @param text
	 * @return
	 */
	public static String convertEnumToDisplayFriendly(Enum<?> en) {
		return convertEnumToDisplayFriendly(en.name());
	}

	public static String getStackTrace(Throwable e) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		e.printStackTrace(printWriter);
		return writer.toString();
	}

	public static String getFiltered(String s, char... acceptChars) {
		TreeSet<Character> set = new TreeSet<>();
		for (char c : acceptChars) {
			set.add(c);
		}
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (set.contains(s.charAt(i))) {
				ret.append(s.charAt(i));
			}
		}
		return ret.toString();
	}

	public static String toString(String separator, int... ints) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < ints.length; i++) {
			if (i > 0) {
				builder.append(separator);
			}
			builder.append(ints[i]);
		}
		return builder.toString();
	}

	public static String toCommas(int... ints) {
		return toString(",", ints);
	}

	/**
	 * Remove chars from the string which could prove problematic when exporting. Currently this contains chars not allowed in workbook sheet names.
	 * 
	 * @param s
	 * @return
	 */
	public static String removeExportIllegalChars(String s) {
		char[] illegals = new char[] { '/', '\\', '?', '*', ']', '[', ':' };

		StringBuilder builder = new StringBuilder();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			boolean found = false;
			for (char illegal : illegals) {
				if (illegal == c) {
					found = true;
					break;
				}
			}

			if (!found) {
				builder.append(c);
			}
		}

		return builder.toString();
	}

	public static String getLeftWithoutWordSplitting(String s, int maxNbChars) {
		int n = s.length();
		if (n <= maxNbChars) {
			return s;
		}

		// work out the max char to take
		int nbInclude = 0;
		boolean onNonBreak = true;
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			int nbChar = i + 1;

			if (onNonBreak && (c == ',' || c == ' ')) {
				if (nbChar <= maxNbChars || nbInclude == 0) {
					nbInclude = nbChar - 1;
				}
				if (nbChar > maxNbChars) {
					break;
				}

				onNonBreak = false;
			} else {
				onNonBreak = true;
			}
		}

		if (nbInclude == 0) {
			return "";
		}
		return s.substring(0, nbInclude);
	}

	public static void main(String[] args) {

		ArrayList<String> list = new ArrayList<>();
		for (String s : new String[] { "vehicle 9", "vehicle", "vehicle 01", "vehicle 10", "vehicle    23b", "vehicle 23", "artic 1" }) {
			list.add(s);
		}
		Collections.sort(list, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return compareStd(o1, o2);
			}
		});
		System.out.println(list);
	}

	public static TreeSet<String> toTreeSet(String... strs) {
		TreeSet<String> ret = new TreeSet<>();
		for (String s : strs) {
			ret.add(s);
		}
		return ret;
	}

	/**
	 * Gets the list of all messages from the exception and and the ancestor exception(s) that caused it. The list is returned in chronological order.
	 * 
	 * @param e
	 * @return
	 */
	public static List<String> getExceptionMessages(Throwable e) {
		ArrayList<String> ret = new ArrayList<>();
		while (e != null) {
			if (isEmpty(e.getMessage()) == false) {
				// only print the exception type if it gives the user some information
				if (e.getClass() != Exception.class && e.getClass() != RuntimeException.class) {
					ret.add("Exception of type \"" + e.getClass().getSimpleName() + "\" : " + e.getMessage());
				} else {
					ret.add(e.getMessage());
				}
			}
			e = e.getCause();
		}
		Collections.reverse(ret);
		return ret;
	}

	public static interface LineCB {
		boolean lineCB(String line);
	}

	public static void parseFileAsLines(File file, LineCB cb) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line = br.readLine();

			while (line != null) {
				if (!cb.lineCB(line)) {
					break;
				}
				line = br.readLine();
			}

		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			try {
				br.close();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Returns true if the string is empty, or contains solely alphanumeric characters or whitespaces or - or :
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isEmptyAlphaNumericWhitespaceOrDash(String s) {
		return nonAlphaNumericOrWhitespace.matcher(s).find() == false;
	}
}
