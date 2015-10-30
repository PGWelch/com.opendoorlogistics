package com.opendoorlogistics.studio.appframe;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;

import com.opendoorlogistics.api.app.AppDisposedListener;
import com.opendoorlogistics.api.app.ODLApp;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.codefromweb.DesktopScrollPane;
import com.opendoorlogistics.codefromweb.TileInternalFrames;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.PreferencesManager;
import com.opendoorlogistics.studio.controls.ODLScrollableToolbar;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame.FramesChangedListener;
import com.opendoorlogistics.studio.internalframes.ProgressFrame;
import com.opendoorlogistics.studio.scripts.editor.ScriptEditor;
import com.opendoorlogistics.studio.scripts.execution.ReporterFrame;
import com.opendoorlogistics.studio.tables.custom.CustomTableEditorFrame;
import com.opendoorlogistics.studio.tables.grid.GridFrame;
import com.opendoorlogistics.studio.tables.schema.TableSchemaEditor;
import com.opendoorlogistics.studio.utils.WindowState;
import com.opendoorlogistics.utils.ui.Icons;

/**
 * Appframe which implements internal frame management
 * @author Phil
 *
 */
public abstract class DesktopAppFrame extends AbstractAppFrame {
	private volatile BufferedImage background;
	private final DesktopScrollPane desktopScrollPane;
	private final ODLScrollableToolbar windowToolBar;	
	private final HashSet<AppDisposedListener> appDisposedListeners = new HashSet<AppDisposedListener>();
	private final JDesktopPane desktopPane = new JDesktopPane() {

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			AppBackground.paintBackground(this, g, background);
//			Graphics2D g2d = (Graphics2D) g;
//			if (background != null) {
//				TexturePaint paint = new TexturePaint(background, new Rectangle(0, 0, background.getWidth(), background.getHeight()));
//
//				if (paint != null) {
//					g2d.setPaint(paint);
//					g2d.fill(g2d.getClip());
//				}
//			} else {
//				g2d.setColor(AppBackground.BACKGROUND_COLOUR);
//				g2d.fillRect(0, 0, (int) getSize().getWidth(), (int) getSize().getHeight());
//			}

			// g.drawImage(image, 0, 0, this);
		}
	};

	public DesktopAppFrame(){
		Container con = getContentPane();
		con.setLayout(new BorderLayout());

		initWindowPosition();
		
		initBackgroundImage();
		
		desktopScrollPane = new DesktopScrollPane(desktopPane);

		windowToolBar = new ODLScrollableToolbar();

	}
	
	@Override
	public BufferedImage getBackgroundImage(){
		return background;
	}


	protected void initBackgroundImage() {
		SwingWorker<Void, Void> createBackground = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				// background = new AppBackground().create();
				AppBackground ab = new AppBackground();

				ab.start();
				long lastTime = System.currentTimeMillis();
				int lastRendered = 0;
				while (ab.getNbConsecutiveFails() < 100) {
					ab.doStep();
					long current = System.currentTimeMillis();
					if (current - lastTime > 100 && lastRendered != ab.getNbRendered()) {
						setBackgroundImage( ImageUtils.deepCopy(ab.getImage()));
						lastTime = current;
						lastRendered = ab.getNbRendered();
					}
				}

				ab.finish();
				setBackgroundImage( ab.getImage());				
				return null;
			}
		};
		createBackground.execute();
	}
	
	@Override
	public JInternalFrame[] getInternalFrames() {
		return desktopPane.getAllFrames();
	}
	
	/**
	 * Based on http://www.javalobby.org/forums/thread.jspa?threadID=15690&tstart=0
	 */
	public void cascadeWindows() {
		JInternalFrame[] frames = getInternalFrames();
		Rectangle dBounds = desktopPane.getBounds();
		int separation = 40;

		// make standard size which is 2/3 of available width
		int width = Math.max(100, 2 * dBounds.width / 3);
		int height = Math.max(100, 2 * dBounds.width / 3);
		for (int i = 0; i < frames.length; i++) {
			try {
				frames[i].setIcon(false);
			} catch (PropertyVetoException e) {
			}
			frames[i].setBounds(i * separation, i * separation, width, height);
			frames[i].toFront();
		}
	}
	
	// Based on http://www.javalobby.org/java/forums/t15696.html
	public void tileWindows() {
		JInternalFrame[] frames = getInternalFrames();
		if (frames.length == 0) {
			return;
		}

		TileInternalFrames.tile(desktopPane, frames);
	}
	

	@Override
	public void addInternalFrame(JInternalFrame frame, FramePlacement placement) {
		desktopPane.add(frame);
		frame.pack();
		frame.setVisible(true);

		// if(ScriptEditor.class.isInstance(frame)){
		// try {
		// frame.setMaximum(true);
		// } catch (PropertyVetoException e) {
		// }
		// }
		// else{

		// WindowState state = PreferencesManager.getSingleton().getWindowState(frame)
		if (placement == FramePlacement.AUTOMATIC) {
			boolean placed = false;
			if (ODLInternalFrame.class.isInstance(frame)) {
				ODLInternalFrame odlFrame = (ODLInternalFrame) frame;
				placed = odlFrame.placeInLastPosition(desktopScrollPane.getViewport().getBounds());
			}

			if (!placed) {
				LayoutUtils.placeInternalFrame(desktopPane, frame);
			}
		} else if (placement == FramePlacement.CENTRAL) {
			Dimension desktopSize = desktopPane.getSize();
			Dimension frameSize = frame.getSize();
			int x = (desktopSize.width - frameSize.width) / 2;
			int y = (desktopSize.height - frameSize.height) / 2;
			frame.setLocation(x, y);
		} else if (placement == FramePlacement.CENTRAL_RANDOMISED) {
			Dimension desktopSize = desktopPane.getSize();
			Dimension frameSize = frame.getSize();
			Dimension remaining = new Dimension(Math.max(0, desktopSize.width - frameSize.width), Math.max(0, desktopSize.height - frameSize.height));
			Dimension halfRemaining = new Dimension(remaining.width / 2, remaining.height / 2);
			Random random = new Random();
			int x = remaining.width / 4 + (halfRemaining.width>0 ?random.nextInt(halfRemaining.width):0);
			int y = remaining.height / 4 + (halfRemaining.height>0?random.nextInt(halfRemaining.height):0);
			frame.setLocation(x, y);
		}
		
		
		if(ODLInternalFrame.class.isInstance(frame)){
			ODLInternalFrame odlf = (ODLInternalFrame)frame;
			odlf.setChangedListener(new FramesChangedListener() {
				
				@Override
				public void internalFrameChange(ODLInternalFrame f) {
					updateWindowsToolbar();
				}
			});
		}
		
		frame.toFront();
		updateWindowsToolbar();
	}


	protected void updateWindowsToolbar(){
		windowToolBar.getToolBar().removeAll();
		
		// get all internal frames, adding progress frames first
		List<ODLInternalFrame> frames =new ArrayList<ODLInternalFrame>();
		boolean hasProgress=false;
		for(JInternalFrame frame : getInternalFrames()){
			if(ODLInternalFrame.class.isInstance(frame)){
				if(ProgressFrame.class.isInstance(frame)){
					frames.add(0,(ODLInternalFrame)frame);
					hasProgress = true;
				}else{
					frames.add((ODLInternalFrame)frame);					
				}
			}
		}
		
		for (final ODLInternalFrame frame : frames) {
	
			// get the title
			String title = frame.getTitle();
			if(ScriptEditor.class.isInstance(frame)){
				File file = ((ScriptEditor)frame).getFile();
				if(file!=null){
					title = file.getName();
					title = Strings.caseInsensitiveReplace(title,"."+ ScriptConstants.FILE_EXT, "");
				}
			}
			if(title!=null){
				int maxchar = 20;
				if(title.length()>maxchar){
					title = title.substring(0, maxchar) + "...";						
				}
			}
			
			// get an icon if we can
			Icon icon = null;
			if(ReporterFrame.class.isInstance(frame)){
				ReporterFrame<?> rf = (ReporterFrame<?>)frame;
				if(rf.getComponent()!=null){
					icon = rf.getComponent().getIcon(getApi(), ODLComponent.MODE_DEFAULT);
				}
			}
			else if(GridFrame.class.isInstance(frame)||CustomTableEditorFrame.class.isInstance(frame)){
				icon = Icons.loadFromStandardPath("table-window-toolbar-icon.png");
			}
			else if(TableSchemaEditor.class.isInstance(frame)){
				icon = Icons.loadFromStandardPath("table-edit.png");
			}
			else if (ScriptEditor.class.isInstance(frame)){
				icon = Icons.loadFromStandardPath("script-window-toolbar.png");
			}else if (ProgressFrame.class.isInstance(frame)){
				icon = ProgressFrame.ANIMATED_ICON;
			}
			
			// create the button
			JButton button =null;
			if(icon!=null){
				button = new JButton(title, icon);
			}else{
				button = new JButton(title);
			}
			button.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if(frame.isIcon()){
						try {
							frame.setIcon(false);
						} catch (PropertyVetoException e1) {
						
						}
					}
					frame.toFront();
				}
			});
			button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED), BorderFactory.createEmptyBorder(2, 2, 2, 2))) ;
			windowToolBar.getToolBar().add(button);
		
		}
		
		
		windowToolBar.repaint();
		
		// need updateUI here otherwise toolbar sometimes disappears!
		windowToolBar.updateUI();
		
		if(hasProgress){
			// ensure progress are shown
			windowToolBar.setScrollViewToInitialPosition();
		}
	}

	public void tileVisibleFrames(JInternalFrame [] frames){
		TileInternalFrames.tile(desktopPane, frames);		
	}

	protected ODLScrollableToolbar getWindowToolBar() {
		return windowToolBar;
	}

	protected JDesktopPane getDesktopPane() {
		return desktopPane;
	}
	
	private void initWindowPosition() {
		WindowState screenState = PreferencesManager.getSingleton().getScreenState();
		boolean boundsSet = false;
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if (screenState != null) {
			setExtendedState(screenState.getExtendedState());
			int safety = 20;
			int screenWidth = gd.getDisplayMode().getWidth();
			int screenHeight = gd.getDisplayMode().getHeight();
			if (getExtendedState() == JFrame.NORMAL && screenState.getX() < (screenWidth - safety) && (screenState.getY() < screenHeight - safety && screenState.getWidth() <= screenWidth && screenState.getHeight() <= screenHeight)) {
				boundsSet = true;
				setBounds(screenState.getX(), screenState.getY(), screenState.getWidth(), screenState.getHeight());
			}
		}

		// make a fraction of the screen size by default
		if (!boundsSet && gd != null && getExtendedState() == JFrame.NORMAL) {
			int screenWidth = gd.getDisplayMode().getWidth();
			int screenHeight = gd.getDisplayMode().getHeight();
			setSize(3 * screenWidth / 4, 3 * screenHeight / 4);
		}
	}

	public void closeWindows() {
		for (JInternalFrame frame :getInternalFrames()) {
			if (ScriptEditor.class.isInstance(frame)) {
				((ScriptEditor) frame).disposeWithSavePrompt();
			} else {
				frame.dispose();
			}
		}
	}

	public void minimiseWindows() {
		for (JInternalFrame frame : getInternalFrames()) {
			try {
				frame.setIcon(true);
			} catch (PropertyVetoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	protected void setBackgroundImage(BufferedImage img){
		this.background = img;
		SwingUtils.invokeLaterOnEDT(new Runnable() {
			
			@Override
			public void run() {
				repaint();
			}
		});
	}
	
	public void addOnDisposedListener(AppDisposedListener listener){
		appDisposedListeners.add(listener);
	}

	@Override
	public void dispose() {
		
		for(AppDisposedListener adl : appDisposedListeners){
			adl.onAppDisposed(this);
		}
		
		super.dispose();
	}
}
