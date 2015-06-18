package com.opendoorlogistics.core.gis.map.background;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContent;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.ContrastMethod;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;

public class ImageFileTileFactory extends TileFactory {

	protected ImageFileTileFactory(TileFactoryInfo info) {
		super(info);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Tile getTile(int x, int y, int zoom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void startLoading(Tile tile) {
		// TODO Auto-generated method stub

	}

	@Override
	public BufferedImage renderSynchronously(int x, int y, int zoom) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRenderedOffline() {
		// TODO Auto-generated method stub
		return false;
	}

	private void loadFile(File rasterFile) {
		try {
			AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
			AbstractGridCoverage2DReader reader = format.getReader(rasterFile);

			CRSAuthorityFactory crsFac = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
			CoordinateReferenceSystem webMercator = crsFac.createCoordinateReferenceSystem("3857");
			DefaultMapContext context = new DefaultMapContext(webMercator);
			context.addLayer(reader, createRGBStyle(reader));
			
			StreamingRenderer renderer = new StreamingRenderer();
			renderer.setMapContent(context);
			
//			   renderer.paint(destinationGraphics,
//                       deviceArea,
//                       worldArea,
//                       worldToScreenTransform);
			   
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
    private Style createRGBStyle(AbstractGridCoverage2DReader reader) {
        GridCoverage2D cov = null;
        try {
            cov = reader.read(null);
        } catch (IOException giveUp) {
            throw new RuntimeException(giveUp);
        }
        // We need at least three bands to create an RGB style
        int numBands = cov.getNumSampleDimensions();
        if (numBands < 3) {
            return null;
        }
        // Get the names of the bands
        String[] sampleDimensionNames = new String[numBands];
        for (int i = 0; i < numBands; i++) {
            GridSampleDimension dim = cov.getSampleDimension(i);
            sampleDimensionNames[i] = dim.getDescription().toString();
        }
        final int RED = 0, GREEN = 1, BLUE = 2;
        int[] channelNum = { -1, -1, -1 };
        // We examine the band names looking for "red...", "green...", "blue...".
        // Note that the channel numbers we record are indexed from 1, not 0.
        for (int i = 0; i < numBands; i++) {
            String name = sampleDimensionNames[i].toLowerCase();
            if (name != null) {
                if (name.matches("red.*")) {
                    channelNum[RED] = i + 1;
                } else if (name.matches("green.*")) {
                    channelNum[GREEN] = i + 1;
                } else if (name.matches("blue.*")) {
                    channelNum[BLUE] = i + 1;
                }
            }
        }
        // If we didn't find named bands "red...", "green...", "blue..."
        // we fall back to using the first three bands in order
        if (channelNum[RED] < 0 || channelNum[GREEN] < 0 || channelNum[BLUE] < 0) {
            channelNum[RED] = 1;
            channelNum[GREEN] = 2;
            channelNum[BLUE] = 3;
        }
        // Now we create a RasterSymbolizer using the selected channels
        SelectedChannelType[] sct = new SelectedChannelType[cov.getNumSampleDimensions()];
        StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);        
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
        for (int i = 0; i < 3; i++) {
            sct[i] = sf.createSelectedChannelType(String.valueOf(channelNum[i]), ce);
        }
        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        ChannelSelection sel = sf.channelSelection(sct[RED], sct[GREEN], sct[BLUE]);
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }

}
