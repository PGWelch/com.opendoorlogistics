package com.opendoorlogistics.codefromweb;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * See http://stackoverflow.com/questions/10331129/jscrollpane-resize-containing-jpanel-when-scrollbars-appear
 * Use like this scrollPane = new JScrollPane(new ButtonContainerHost(buttonContainer));
 * @author 
 *
 */
public class ButtonContainerHost extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;

    private final JPanel buttonContainer;

    public ButtonContainerHost(JPanel buttonContainer) {
        super(new BorderLayout());
        this.buttonContainer = buttonContainer;
        add(buttonContainer);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Dimension preferredSize = buttonContainer.getPreferredSize();
        if (getParent() instanceof JViewport) {
            preferredSize.width += ((JScrollPane) getParent().getParent()).getVerticalScrollBar()
                    .getPreferredSize().width;
        }
        return preferredSize;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? Math.max(visibleRect.width * 9 / 10, 1)
                : Math.max(visibleRect.height * 9 / 10, 1);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            JViewport viewport = (JViewport) getParent();
            return getPreferredSize().height < viewport.getHeight();
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? Math.max(visibleRect.width / 10, 1)
                : Math.max(visibleRect.height / 10, 1);
    }
}