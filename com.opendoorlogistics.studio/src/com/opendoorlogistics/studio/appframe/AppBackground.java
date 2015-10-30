/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.appframe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import javax.imageio.ImageIO;

import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

final public class AppBackground {
	// private static final BasicStroke INNER_TEXT_STROKE = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
	private final Random random = new Random(123);
	private final int NB_POSITIONING_ATTEMPTS = 5000;
	private final HashMap<Pair<String, Integer>, Pair<TextLayout, Rectangle2D>> layouts = new HashMap<>();
	private final int width;
	private final int height;
	private final Quadtree quadtree = new Quadtree();
	private final Rectangle2D screen;
	private final BufferedImage image ;
	private final String[] strs = new String[] { "consultancy", "integration", "help & advice", "software development", "support", "training",
			"implementation", "bespoke solutions",
			// "territory design",
			"www.opendoorlogistics.com", "www.opendoorlogistics.com", "www.opendoorlogistics.com" };

	private int nbConsecutiveFails = 0;
	private int step=0;
	private Graphics2D g;

	public static final Color BACKGROUND_COLOUR = new Color(230, 230, 230);

	public AppBackground(){
		this(1000, 1000);
	}
	
	public AppBackground(int width, int height) {
		this.width = width;
		this.height = height;
		screen = new Rectangle2D.Double(0, 0, width, height);
		image = ImageUtils.createBlankImage(width, height, BACKGROUND_COLOUR);
	}

	public static void main(String[] args) throws IOException {
		BufferedImage img = new AppBackground(1000,1600).create();
		
		ImageUtils.createImageFrame(img).setVisible(true);
		File outputfile = new File("c:\\temp\\background.png");
		ImageIO.write(img, "png", outputfile);
		// File outputfile = new File("c:\\temp\\outputimage.png");
		// ImageIO.write(img, "png", outputfile);
	}

	public BufferedImage create() {
		start();

		while (nbConsecutiveFails < 100) {
			doStep();
		}

		finish();

		return image;
	}

	public int getNbConsecutiveFails() {
		return nbConsecutiveFails;
	}

	public void doStep() {
		String s = strs[random.nextInt(strs.length)];
		int fontSize = 10 + random.nextInt(20);
		int buffer = 200;
		int x = 0;
		int y = 0;
		boolean okPos = false;
		// Shape shape = null;
		TextLayout textLayout = null;
		Rectangle2D textBounds = null;
		Rectangle2D untranslatedBounds = null;
		for (int j = 0; j < NB_POSITIONING_ATTEMPTS && okPos == false; j++) {
			// reduce font size if we're having trouble fitting in
			if (j > 0 && j % 50 == 0 && fontSize >= 8) {
				fontSize--;
				textLayout = null;
			}

			if (textLayout == null) {
				Pair<TextLayout, Rectangle2D> pair = getLayout(g, s, fontSize);
				textLayout = pair.getFirst();
				untranslatedBounds = pair.getSecond();
			}

			x = random.nextInt(width + buffer) - buffer;
			y = random.nextInt(height + buffer) - buffer;

			// get bounds by getting the shape and translating to screen position
			// AffineTransform affineTransform = new AffineTransform();
			// affineTransform.translate(x, y);
			// shape = textLayout.getOutline(affineTransform);

			Rectangle2D initial = new Rectangle2D.Double(untranslatedBounds.getX() + x, untranslatedBounds.getY() + y, untranslatedBounds.getWidth(),
					untranslatedBounds.getHeight());
			textBounds = new Rectangle2D.Double(initial.getX() - initial.getWidth() / 8, initial.getY() - initial.getHeight() / 8,
					initial.getWidth() * 1.25, initial.getHeight() * 1.25);
			okPos = true;
			if (!screen.contains(textBounds)) {
				okPos = false;
			} else {
				for (Object o : quadtree.query(toEnvelope(textBounds))) {
					Rectangle2D rect = (Rectangle2D) o;
					if (textBounds.intersects(rect)) {
						okPos = false;
						break;
					}
				}
			}
		}

		if (okPos) {
			Color col = new Color(fontCol(), fontCol(), fontCol(), 40 + random.nextInt(80));
			// Color outlineCol = new Color(255, 255, 255, 100);
			// g.setStroke(INNER_TEXT_STROKE);
			// g.setColor(outlineCol);
			// g.draw(shape);
			g.setColor(col);
			textLayout.draw(g, x, y);
			quadtree.insert(toEnvelope(textBounds), textBounds);
			nbConsecutiveFails = 0;
		} else {
			nbConsecutiveFails++;
		}

		
		//System.out.println("Step=" +step + " rendered=" + quadtree.size() + " nbConsecutiveFails="+ nbConsecutiveFails + " - " + okPos);
		step++;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void finish() {
		g.dispose();
	}

	public void start() {
		g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}

	private Envelope toEnvelope(Rectangle2D r) {
		return new Envelope(r.getMinX(), r.getMaxX(), r.getMinY(), r.getMaxY());
	}

	private Pair<TextLayout, Rectangle2D> getLayout(Graphics2D g, String s, int fontSize) {
		Pair<String, Integer> key = new Pair<String, Integer>(s, fontSize);
		Pair<TextLayout, Rectangle2D> ret = layouts.get(key);

		if (ret == null) {
			Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
			TextLayout tl = new TextLayout(s, font, g.getFontRenderContext());
			ret = new Pair<TextLayout, Rectangle2D>(tl, tl.getBounds());
			layouts.put(key, ret);
		}
		return ret;
	}

	private int fontCol() {
		return 180 + random.nextInt(20);
	}

	public int getNbRendered(){
		return quadtree.size();
	}
	
	public static void paintBackground(Component c,Graphics g,BufferedImage background){
		Graphics2D g2d = (Graphics2D) g;
		if (background != null) {
			TexturePaint paint = new TexturePaint(background, new Rectangle(0, 0, background.getWidth(), background.getHeight()));

			if (paint != null) {
				g2d.setPaint(paint);
				g2d.fill(g2d.getClip());
			}
		} else {
			g2d.setColor(AppBackground.BACKGROUND_COLOUR);
			g2d.fillRect(0, 0, (int) c.getSize().getWidth(), (int) c.getSize().getHeight());
		}
	}
}
