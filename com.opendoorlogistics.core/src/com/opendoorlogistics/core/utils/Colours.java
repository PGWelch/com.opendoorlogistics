/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.math.MathUtil;

final public class Colours {
	private Colours() {
	}

	private final static HashMap<String, Color> colourMap;

	private final static List<Color> predefinedList;
	
	public static String toHexString(Color c) {
		StringBuilder builder = new StringBuilder("#");

		if (c.getRed() < 16)
			builder.append('0');
		builder.append(Integer.toHexString(c.getRed()));

		if (c.getGreen() < 16)
			builder.append('0');
		builder.append(Integer.toHexString(c.getGreen()));

		if (c.getBlue() < 16)
			builder.append('0');
		builder.append(Integer.toHexString(c.getBlue()));

		return builder.toString();
	}
	

	static {

		colourMap = new HashMap<>();
		for (Field f : Color.class.getFields()) {
			if (f.getType() == Color.class) {
				try {
					Color c = (Color) f.get(null);
					colourMap.put(standardise(f.getName()), c);

				} catch (Throwable e) {
					// TODO: handle exception
				}
			}
		}
		
		predefinedList = new ArrayList<Color>();

		String[][] predefined = new String[][] { new String[] { "AliceBlue", "#F0F8FF" }, new String[] { "AntiqueWhite", "#FAEBD7" }, new String[] { "Aqua", "#00FFFF" }, new String[] { "Aquamarine", "#7FFFD4" }, new String[] { "Azure", "#F0FFFF" },
				new String[] { "Beige", "#F5F5DC" }, new String[] { "Bisque", "#FFE4C4" }, new String[] { "Black", "#000000" }, new String[] { "BlanchedAlmond", "#FFEBCD" }, new String[] { "Blue", "#0000FF" },
				new String[] { "BlueViolet", "#8A2BE2" }, new String[] { "Brown", "#A52A2A" }, new String[] { "BurlyWood", "#DEB887" }, new String[] { "CadetBlue", "#5F9EA0" }, new String[] { "Chartreuse", "#7FFF00" },
				new String[] { "Chocolate", "#D2691E" }, new String[] { "Coral", "#FF7F50" }, new String[] { "CornflowerBlue", "#6495ED" }, new String[] { "Cornsilk", "#FFF8DC" }, new String[] { "Crimson", "#DC143C" },
				new String[] { "Cyan", "#00FFFF" }, new String[] { "DarkBlue", "#00008B" }, new String[] { "DarkCyan", "#008B8B" }, new String[] { "DarkGoldenRod", "#B8860B" }, new String[] { "DarkGray", "#A9A9A9" },
				new String[] { "DarkGreen", "#006400" }, new String[] { "DarkKhaki", "#BDB76B" }, new String[] { "DarkMagenta", "#8B008B" }, new String[] { "DarkOliveGreen", "#556B2F" }, new String[] { "DarkOrange", "#FF8C00" },
				new String[] { "DarkOrchid", "#9932CC" }, new String[] { "DarkRed", "#8B0000" }, new String[] { "DarkSalmon", "#E9967A" }, new String[] { "DarkSeaGreen", "#8FBC8F" }, new String[] { "DarkSlateBlue", "#483D8B" },
				new String[] { "DarkSlateGray", "#2F4F4F" }, new String[] { "DarkTurquoise", "#00CED1" }, new String[] { "DarkViolet", "#9400D3" }, new String[] { "DeepPink", "#FF1493" }, new String[] { "DeepSkyBlue", "#00BFFF" },
				new String[] { "DimGray", "#696969" }, new String[] { "DodgerBlue", "#1E90FF" }, new String[] { "FireBrick", "#B22222" }, new String[] { "FloralWhite", "#FFFAF0" }, new String[] { "ForestGreen", "#228B22" },
				new String[] { "Fuchsia", "#FF00FF" }, new String[] { "Gainsboro", "#DCDCDC" }, new String[] { "GhostWhite", "#F8F8FF" }, new String[] { "Gold", "#FFD700" }, new String[] { "GoldenRod", "#DAA520" },
				new String[] { "Gray", "#808080" }, new String[] { "Green", "#008000" }, new String[] { "GreenYellow", "#ADFF2F" }, new String[] { "HoneyDew", "#F0FFF0" }, new String[] { "HotPink", "#FF69B4" },
				new String[] { "IndianRed ", "#CD5C5C" }, new String[] { "Indigo ", "#4B0082" }, new String[] { "Ivory", "#FFFFF0" }, new String[] { "Khaki", "#F0E68C" }, new String[] { "Lavender", "#E6E6FA" },
				new String[] { "LavenderBlush", "#FFF0F5" }, new String[] { "LawnGreen", "#7CFC00" }, new String[] { "LemonChiffon", "#FFFACD" }, new String[] { "LightBlue", "#ADD8E6" }, new String[] { "LightCoral", "#F08080" },
				new String[] { "LightCyan", "#E0FFFF" }, new String[] { "LightGoldenRodYellow", "#FAFAD2" }, new String[] { "LightGray", "#D3D3D3" }, new String[] { "LightGreen", "#90EE90" }, new String[] { "LightPink", "#FFB6C1" },
				new String[] { "LightSalmon", "#FFA07A" }, new String[] { "LightSeaGreen", "#20B2AA" }, new String[] { "LightSkyBlue", "#87CEFA" }, new String[] { "LightSlateGray", "#778899" }, new String[] { "LightSteelBlue", "#B0C4DE" },
				new String[] { "LightYellow", "#FFFFE0" }, new String[] { "Lime", "#00FF00" }, new String[] { "LimeGreen", "#32CD32" }, new String[] { "Linen", "#FAF0E6" }, new String[] { "Magenta", "#FF00FF" }, new String[] { "Maroon", "#800000" },
				new String[] { "MediumAquaMarine", "#66CDAA" }, new String[] { "MediumBlue", "#0000CD" }, new String[] { "MediumOrchid", "#BA55D3" }, new String[] { "MediumPurple", "#9370DB" }, new String[] { "MediumSeaGreen", "#3CB371" },
				new String[] { "MediumSlateBlue", "#7B68EE" }, new String[] { "MediumSpringGreen", "#00FA9A" }, new String[] { "MediumTurquoise", "#48D1CC" }, new String[] { "MediumVioletRed", "#C71585" }, new String[] { "MidnightBlue", "#191970" },
				new String[] { "MintCream", "#F5FFFA" }, new String[] { "MistyRose", "#FFE4E1" }, new String[] { "Moccasin", "#FFE4B5" }, new String[] { "NavajoWhite", "#FFDEAD" }, new String[] { "Navy", "#000080" },
				new String[] { "OldLace", "#FDF5E6" }, new String[] { "Olive", "#808000" }, new String[] { "OliveDrab", "#6B8E23" }, new String[] { "Orange", "#FFA500" }, new String[] { "OrangeRed", "#FF4500" }, new String[] { "Orchid", "#DA70D6" },
				new String[] { "PaleGoldenRod", "#EEE8AA" }, new String[] { "PaleGreen", "#98FB98" }, new String[] { "PaleTurquoise", "#AFEEEE" }, new String[] { "PaleVioletRed", "#DB7093" }, new String[] { "PapayaWhip", "#FFEFD5" },
				new String[] { "PeachPuff", "#FFDAB9" }, new String[] { "Peru", "#CD853F" }, new String[] { "Pink", "#FFC0CB" }, new String[] { "Plum", "#DDA0DD" }, new String[] { "PowderBlue", "#B0E0E6" }, new String[] { "Purple", "#800080" },
				new String[] { "Red", "#FF0000" }, new String[] { "RosyBrown", "#BC8F8F" }, new String[] { "RoyalBlue", "#4169E1" }, new String[] { "SaddleBrown", "#8B4513" }, new String[] { "Salmon", "#FA8072" },
				new String[] { "SandyBrown", "#F4A460" }, new String[] { "SeaGreen", "#2E8B57" }, new String[] { "SeaShell", "#FFF5EE" }, new String[] { "Sienna", "#A0522D" }, new String[] { "Silver", "#C0C0C0" },
				new String[] { "SkyBlue", "#87CEEB" }, new String[] { "SlateBlue", "#6A5ACD" }, new String[] { "SlateGray", "#708090" }, new String[] { "Snow", "#FFFAFA" }, new String[] { "SpringGreen", "#00FF7F" },
				new String[] { "SteelBlue", "#4682B4" }, new String[] { "Tan", "#D2B48C" }, new String[] { "Teal", "#008080" }, new String[] { "Thistle", "#D8BFD8" }, new String[] { "Tomato", "#FF6347" }, new String[] { "Turquoise", "#40E0D0" },
				new String[] { "Violet", "#EE82EE" }, new String[] { "Wheat", "#F5DEB3" }, new String[] { "White", "#FFFFFF" }, new String[] { "WhiteSmoke", "#F5F5F5" }, new String[] { "Yellow", "#FFFF00" }, new String[] { "YellowGreen", "#9ACD32" }

		};

		for (String[] pair : predefined) {
			Color col =  Color.decode(pair[1]);
			colourMap.put(standardise(pair[0]), col);
			predefinedList.add(col);
		}
	}

	public static Map<String, Color> getStandardColoursMap(){
		return colourMap;
	}
	
	private static String standardise(String s) {
		s = Strings.std(s);
		s = s.replace(" ", "");
		return s;
	}

	public static Color getColourByName(String s) {
		Color ret = colourMap.get(standardise(s));
		return ret;
	}

	public static Color toColour(Object o) {
		Color col = null;
		if ((Color.class.isInstance(o))) {
			col = (Color) o;
		} else {
			col = Colours.getColourByString(o.toString());
		}
		return col;
	}
	
	public static Color toGrey(Color c){
		double av = (c.getRed() + c.getBlue() + c.getGreen() ) / (255.0 * 3.0);
		int val = to0To255Int(av);
		Color greyCol = new Color(val, val, val, c.getAlpha());
		return greyCol;
	}

	public static Color getColourByString(String s) {
		Color ret = getColourByName(s);
		if (ret == null) {
			try {
				// try hex (this is the canonical string representation support by ODLColumnType)
				ret = Color.decode(s);
			} catch (Throwable e) {
			}
		}

		if (ret == null) {
			// treat as random
			ret = getRandomColour(s);
		}

		return ret;
	}

	private static final Color[] avoidSelectingColours = new Color[] { new Color(181, 208, 208), // openstreetmap sea!
	};

	private static class RandomColours {
		private static ArrayList<Color> chosen = new ArrayList<>();
		private static int NB_TESTS = 100;
		private static int LIST_SIZE = 1000;

		private static int distance(Color a, Color b) {
			int ret = 0;
			ret += Math.abs(a.getRed() - b.getRed());
			ret += Math.abs(a.getGreen() - b.getGreen());
			ret += Math.abs(a.getBlue() - b.getBlue());
			return ret;
		}

		private static int minimum(Color c, Iterable<Color> colours) {
			int ret = Integer.MAX_VALUE;
			for (Color color : colours) {
				int diff = distance(c, color);
				if (diff < ret) {
					ret = diff;
				}
			}
			return ret;
		}

		private static int minimum(Color c, Color... colours) {
			int ret = Integer.MAX_VALUE;
			for (Color color : colours) {
				int diff = distance(c, color);
				if (diff < ret) {
					ret = diff;
				}
			}
			return ret;
		}

		static {
			Random random = new Random(123);

			// generate a spaced random pallet
			chosen = new ArrayList<>();
			for (int i = 0; i < LIST_SIZE; i++) {
				Color bestCol = null;
				int bestMin = 0;
				for (int j = 0; j < NB_TESTS; j++) {
					Color col = internalRandom(random);

					// get min to already chosen
					int min = minimum(col, chosen);

					// also check colours to avoid
					int minAvoid = minimum(col, avoidSelectingColours);
					min = Math.min(minAvoid, min);

					if (min > bestMin) {
						bestMin = min;
						bestCol = col;
					}
				}

				chosen.add(bestCol);
			}
		}

		private static Color internalRandom(Random random) {
			while (true) {
				int red = random.nextBoolean() ? random.nextInt(255) : 0;
				int blue = random.nextBoolean() ? random.nextInt(255) : 0;
				int green = random.nextBoolean() ? random.nextInt(255) : 0;
				int sum = red + blue + green;
				if (sum > 150 && sum < (240 * 3)) {
					return new Color(red, blue, green);
				}
			}
			// Color col = new Color(0.1f + 0.8f * random.nextFloat(), 0.1f + 0.8f * random.nextFloat(), 0.1f + 0.8f * random.nextFloat());
			// return col;
		}

	}

	public static Color getRandomColour(String s) {
		if (s == null || s.length() == 0) {
			return DrawableObject.DEFAULT_COLOUR;
		}
		Long l = Strings.parseLong(s);
		if (l != null) {
			return getRandomColour(l);
		}
		return getRandomColour(Strings.std(s).hashCode());
	}

	private static int lerpComponent(int a, int b, double fraction) {
		if (fraction <= 0) {
			return a;
		}

		if (fraction >= 1) {
			return b;
		}

		double ret = a * (1.0 - fraction) + b * fraction;
		return to0To255Int(ret);
	}

	public static Color lerp(Color a, Color b, double fraction) {
		if (fraction <= 0) {
			return a;
		}

		if (fraction >= 1) {
			return b;
		}

		return new Color(lerpComponent(a.getRed(), b.getRed(), fraction), lerpComponent(a.getGreen(), b.getGreen(), fraction), lerpComponent(a.getBlue(), b.getBlue(), fraction), lerpComponent(a.getAlpha(), b.getAlpha(), fraction));
	}

	public static Color setAlpha(Color col, int alpha) {
		return new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha);
	}

	public static Color getRandomColour(long indx) {
		if (indx < RandomColours.chosen.size() && indx >= 0) {
			return RandomColours.chosen.get((int) indx);
		}
		return RandomColours.internalRandom(new Random(indx));
	}

	public static void main(String[] args) {
		// for (int i = 0; i <= 255; i++) {
		// Color col = new Color(255, 255, 255, i);
		//
		// System.out.println("" + i + " -> " + getAlpha(col.getRGB()));
		// }
	}

	public static Color getRandomColorFromPredefinedPallet(Random r){
		int i = r.nextInt(predefinedList.size());
		return predefinedList.get(i);
	}
	
	public static int compare(Color col1, Color col2) {
		int diff = NullComparer.compare(col1, col2);

		if(diff==0 && col1==null){
			// no further comparison possible
			return 0;
		}
		
		if (diff == 0) {
			diff = Integer.compare(col1.getRGB(), col2.getRGB());
		}

		if (diff == 0) {
			diff = Integer.compare(col1.getAlpha(), col2.getAlpha());
		}
		return diff;
	}

	public static class CalculateAverageColour {
		private double r;
		private double g;
		private double b;
		private int count;

		public void add(Color col) {
			count++;
			r += col.getRed();
			g += col.getGreen();
			b += col.getBlue();
		}

		private int calc(double sum) {
			int ret = (int) Math.round(sum / count);
			if (ret > 255)
				ret = 255;
			return ret;
		}

		public Color getAverage() {
			if (count > 0) {
				return new Color(calc(r), calc(g), calc(b));
			}
			return Color.BLACK;
		}
	}

	public static int ensureRange(int val) {
		if (val < 0) {
			return 0;
		}
		if (val > 255) {
			return 255;
		}
		return val;
	}

	public static int to0To255Int(double v) {
		return ensureRange((int) Math.round(v));
	}

	/**
	 * Multiple all red, green and blue but not alpha by the factor
	 * 
	 * @param col
	 * @param factor
	 * @return
	 */
	public static Color multiplyNonAlpha(Color col, float factor) {
		return new Color(to0To255Int(col.getRed() * factor), to0To255Int(col.getGreen() * factor), to0To255Int(col.getBlue() * factor), col.getAlpha());
	}

	private static final Color[] _old_temperatureCols = { new Color(41, 10, 216), new Color(38, 77, 255), new Color(63, 160, 255), new Color(114, 217, 255), new Color(170, 247, 255), new Color(224, 255, 255), new Color(255, 255, 191),
			new Color(255, 224, 153), new Color(255, 173, 114), new Color(247, 109, 94), new Color(216, 38, 50), new Color(165, 0, 33) };

	private static final Color[] temperatureCols = {
	new Color(0,0,127),
	new Color(0,0,255),
	new Color(0,127,255),
	new Color(0,255,255),
	new Color(127,255,127),
	new Color(255,255,0),
	new Color(255,127,0),
	new Color(255,0,0),
	new Color(127,0,0)};

	
	public static Color temperature(double fraction) {
		fraction = MathUtil.clamp(fraction, 0, 1);
		int n = temperatureCols.length;
		double index = fraction * (n - 1);
		int low = (int) Math.floor(index);
		int high = (int) Math.ceil(index);
		double ifrac = index - low;
		Color l = temperatureCols[low];
		Color h = temperatureCols[high];
		Color ret = new Color(lerp(l.getRed(), h.getRed(), ifrac), lerp(l.getGreen(), h.getGreen(), ifrac), lerp(l.getBlue(), h.getBlue(), ifrac));
		return ret;
	}

	private static int lerp(int low, int high, double fraction) {
		fraction = MathUtil.clamp(fraction, 0, 1);
		double val = low * (1.0 - fraction) + high * fraction;
		return ensureRange((int) Math.round(val));
	}
}
