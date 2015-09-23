package com.opendoorlogistics.studio.controls;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import com.opendoorlogistics.utils.ui.Icons;

public class ODLScrollableToolbar extends JPanel {
	/**
	 * This timer is static and shared between all instances of this class, as the internal thread
	 * only gets disposed on garbage collection otherwise.
	 */
	private static final Timer MOVEMENT_TIMER = new Timer();

	private final JToolBar toolBar;
	private final JScrollPane scrollPane;
	private boolean isNeedsScroll = false;
	private final JButton leftButton;
	private final JButton rightButton;

	public ODLScrollableToolbar(){
		this(JToolBar.HORIZONTAL);
	}
	
	public ODLScrollableToolbar(int toolbarOrientation) {
		this(Icons.loadFromStandardPath("window-toolbar-left.png"),  Icons.loadFromStandardPath("window-toolbar-right.png"),toolbarOrientation);
	}

	public ODLScrollableToolbar(Icon leftIcon, Icon rightIcon, int toolbarOrientation) {
		toolBar = new JToolBar(toolbarOrientation);
		toolBar.setFloatable(false);

		setLayout(new BorderLayout());

		scrollPane = new JScrollPane(toolBar);
		scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.getViewport().addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				updateScrollState();
			}
		});

		add(scrollPane, BorderLayout.CENTER);
		leftButton =leftIcon!=null? new JButton(leftIcon): new JButton("<");
		leftButton.setVisible(false);
		leftButton.addMouseListener(createMouseListener(true));
		add(leftButton, BorderLayout.WEST);

		rightButton = rightIcon!=null ? new JButton(rightIcon) : new JButton(">");
		rightButton.setVisible(false);
		rightButton.addMouseListener(createMouseListener(false));
		add(rightButton, BorderLayout.EAST);
		
		addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				updateScrollState();
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				updateScrollState();
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				updateScrollState();
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				updateScrollState();
			}
		});

//		addAncestorListener(new AncestorListener() {
//			
//			@Override
//			public void ancestorRemoved(AncestorEvent event) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void ancestorMoved(AncestorEvent event) {
//				updateScrollState();
//			}
//			
//			@Override
//			public void ancestorAdded(AncestorEvent event) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
	}

	private MouseListener createMouseListener(final boolean isLeft) {
		return new MouseListener() {

			TimerTask task;

			@Override
			public void mouseReleased(MouseEvent e) {
				if (task != null) {
					task.cancel();
					task = null;
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				final long startTime = System.currentTimeMillis();
				task = new TimerTask() {

					@Override
					public void run() {
						long duration = System.currentTimeMillis() - startTime;
						long increment = 1 + duration / 150;
						increment = Math.min(increment, 6);

						scroll((int) (isLeft ? -increment : +increment));
					}
				};
				MOVEMENT_TIMER.scheduleAtFixedRate(task, 0, 10);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// task.cancel();
			}

			@Override
			public void mouseEntered(MouseEvent e) {

			}

			@Override
			public void mouseClicked(MouseEvent e) {

			}
		};
	}

	public JToolBar getToolBar() {
		return toolBar;
	}

	private void scroll(int value) {
		Point pos = scrollPane.getViewport().getViewPosition();
		pos = new Point(pos.x + value, pos.y);
		if (pos.x < 0) {
			pos.x = 0;
		}

		int max = toolBar.getWidth() - scrollPane.getWidth();
		max = Math.max(0, max);
		if (pos.x > max) {
			pos.x = max;
		}

		setViewportPosition(pos);
	}

	private void setViewportPosition(Point pos) {
		scrollPane.getViewport().setViewPosition(pos);
		scrollPane.repaint();
	}

	public void setScrollViewToInitialPosition(){
		Point pos = scrollPane.getViewport().getViewPosition();	
		setViewportPosition(new Point(0, pos.y));
	}
	
	private boolean isHorizontal(){
		return toolBar.getOrientation() == JToolBar.HORIZONTAL;
	}
	
	private void updateScrollState() {
		boolean newNeedsScroll = isHorizontal()? scrollPane.getWidth() < toolBar.getWidth() : scrollPane.getHeight() < toolBar.getHeight();
		if (isNeedsScroll != newNeedsScroll) {
			leftButton.setVisible(newNeedsScroll);
			rightButton.setVisible(newNeedsScroll);
			isNeedsScroll = newNeedsScroll;
		}

	}
	

	public static void main(String[] args) {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JFrame frame = new JFrame();
		frame.setContentPane(mainPanel);

		ODLScrollableToolbar sPanel = new ODLScrollableToolbar();
		for (int i = 1; i < 100; i++) {
			sPanel.getToolBar().add(new JButton("hello" + i));
		}
		frame.add(sPanel, BorderLayout.SOUTH);

		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

}
