package com.opendoorlogistics.codefromweb;

import java.awt.*;
import javax.swing.border.AbstractBorder;

/**
 * Draws a line at the bottom only. Useful for making a separator in combo box, for example.
 * From http://stackoverflow.com/questions/138793/how-do-i-add-a-separator-to-a-jcombobox-in-java
 */
@SuppressWarnings("serial")
public class BottomLineBorder extends AbstractBorder {
	private int m_thickness;
	private Color m_color;

	public BottomLineBorder() {
		this(1, Color.BLACK);
	}

	BottomLineBorder(Color color) {
		this(1, color);
	}

	BottomLineBorder(int thickness, Color color) {
		m_thickness = thickness;
		m_color = color;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics copy = g.create();
		if (copy != null) {
			try {
				copy.translate(x, y);
				copy.setColor(m_color);
				copy.fillRect(0, height - m_thickness, width - 1, height - 1);
			} finally {
				copy.dispose();
			}
		}
	}

	@Override
	public boolean isBorderOpaque() {
		return true;
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(0, 0, m_thickness, 0);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets i) {
		i.left = i.top = i.right = 0;
		i.bottom = m_thickness;
		return i;
	}
}