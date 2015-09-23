/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog.builder;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FilenameUtils;
import org.geotools.geometry.jts.JTS;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.OSMTileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.util.GeoUtil;
import com.opendoorlogistics.core.geometry.ImportShapefile;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.geometry.rog.ODLRenderOptimisedGeom;
import com.opendoorlogistics.core.geometry.rog.QuadLoader;
import com.opendoorlogistics.core.geometry.rog.RogReaderUtils;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreenImpl;
import com.opendoorlogistics.core.gis.map.transforms.TransformGeomToWorldBitmap;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.LargeList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class ROGBuilder {
	private final int nbThreads;
	private final File shapefile;
	private final File outfile;
	private final File tmpFile;
	private final double pixelTol;
	private final boolean isNOLPL;
	private final TileFactoryInfo tileFactoryInfo = new OSMTileFactoryInfo();
	private ProcessingApi processingApi = new ProcessingApi() {
		
		@Override
		public ODLApi getApi() {
			return null;
		}
		
		@Override
		public boolean isFinishNow() {
			return false;
		}
		
		@Override
		public boolean isCancelled() {
			return false;
		}
		
		@Override
		public void postStatusMessage(String s) {
			System.out.println(s);
		}
		
		@Override
		public void logWarning(String warning) {
			System.err.println(warning);
		}
	};
	
	// private final LZ4Compressor compressor;

	public ROGBuilder(File shapefile,boolean isNOLPL, double pixelTol, int nbThreads, ProcessingApi processingApi) {
		this(shapefile,
			new File(FilenameUtils.removeExtension(shapefile.getPath()) + "." + RogReaderUtils.RENDER_GEOMETRY_FILE_EXT),
			isNOLPL,
			pixelTol, nbThreads,processingApi);
	}

	private ROGBuilder(File shapefile, File outfile,boolean isNOLPL, double pixelTol, int nbThreads, ProcessingApi processingApi) {
		this.shapefile = shapefile;
		this.outfile = outfile;
		this.pixelTol = pixelTol;
		this.tmpFile = new File(outfile.getAbsolutePath() + ".tmp");
		this.nbThreads = nbThreads;
		this.isNOLPL =isNOLPL;
		// this.compressor = LZ4Factory.unsafeInstance().fastCompressor();
		Spatial.initSpatial();
		
		if(processingApi!=null){
			this.processingApi = processingApi;
		}

	}

	
	private class RowAllocator {
		private final int nRows;
		private volatile int next = 0;

		public RowAllocator(int nRows) {
			super();
			this.nRows = nRows;
			this.next = next;
		}

		/*
		 * Allocate a row or return -1 if non available
		 */
		synchronized int allocateRow() {
			int ret = -1;
			if (next < nRows) {
				ret = next;
				next++;
			}
			return ret;
		}
	}

	private synchronized void postStatusMessage(String s){
		processingApi.postStatusMessage(s);	
	}
	
	private class RowProcessor implements Callable<Void> {
		final ODLTableReadOnly table;
		final int geomCol;
		final LargeList<ShapeIndex> indices;
		final int zoom;
		TransformGeomToWorldBitmap mathTransform;
		final RowAllocator allocator;
		final List<PendingWrite> resultsList;
		final WKBWriter geomWriter = new WKBWriter();
		final String baseMessage;
		
		RowProcessor(ODLTableReadOnly table, int geomCol, LargeList<ShapeIndex> indices, int zoom, TransformGeomToWorldBitmap mathTransform, RowAllocator allocator,String baseMessage, List<PendingWrite> resultsList) {
			this.table = table;
			this.geomCol = geomCol;
			this.indices = indices;
			this.zoom = zoom;
			this.mathTransform = mathTransform;
			this.allocator = allocator;
			this.resultsList = resultsList;
			this.baseMessage = baseMessage;
		}

		@Override
		public Void call() throws Exception {
			while (true) {
				int row = allocator.allocateRow();
				if (row == -1) {
					return null;
				}

				PendingWrite pending = processRow(row);
				resultsList.set(row, pending);

				if (row % 1000 == 0 && row>0) {
					postStatusMessage(baseMessage  + " - processed " + row + " geometries");
				}
			}
		}

		private PendingWrite processRow(int row) {
			ODLGeomImpl geom = (ODLGeomImpl) table.getValueAt(row, geomCol);
			if (geom == null) {
				return null;
			}

			Geometry wgs = geom.getJTSGeometry();
			try {
				Geometry transformed = JTS.transform(wgs, mathTransform);
				Envelope bb = transformed.getEnvelopeInternal();

				byte[] bytes = null;
				int nbPoints = 0;
				if (bb.getWidth() >= pixelTol || bb.getHeight() >= pixelTol) {
					// simplify
					transformed = TopologyPreservingSimplifier.simplify(transformed, pixelTol);

					// geometry can become empty is its too small ..
					nbPoints = transformed.getNumPoints();
					if (nbPoints > 0) {
						bytes = geomWriter.write(transformed);
					}
				}

				ShapeIndex index = indices.get(row);
				if (index.rowNb != row) {
					throw new RuntimeException();
				}
				if (bytes != null) {
					boolean writeBytes = true;
					int lastDefinedLevel = index.findLastDefinedLevel(tileFactoryInfo.getMinimumZoomLevel(), zoom);
					if (lastDefinedLevel != -1) {
						int lastNbPoints = index.nbPoints[lastDefinedLevel];
						int target = (int) Math.round(lastNbPoints * ROGConstants.MINIMUM_REDUCTION_FRACTION_FOR_NEXT_LEVEL);
						if (nbPoints >= target) {
							// Use last defined level geometry as current level is not much more simplified
							index.blockNb[zoom] = ODLRenderOptimisedGeom.USE_LAST_LEVEL;
							writeBytes = false;
						}
					}

					if (writeBytes) {
						index.nbPoints[zoom] = nbPoints;
						return new PendingWrite(index, bytes);
					}
				} else {
					index.blockNb[zoom] = ODLRenderOptimisedGeom.SUBPIXEL;
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			return null;
		}

	}

	@SuppressWarnings("resource")
	public void build() {

		try {

			// Load the shapefile
			ODLDatastoreAlterable<ODLTableAlterable> ds = ODLDatastoreImpl.alterableFactory.create();
			ImportShapefile.importShapefile(shapefile, false, ds, false);
			ODLTableReadOnly table = ds.getTableAt(0);
			if (table.getRowCount() == 0 || processingApi.isCancelled()) {
				return;
			}
			int geomCol = TableUtils.findColumnIndx(table, ODLColumnType.GEOM);
			int nrows = table.getRowCount();

			// Create map to store positions in the output file
			LargeList<ShapeIndex> indices = new LargeList<>();
			for (int row = 0; row < nrows; row++) {
				indices.add(new ShapeIndex(row, tileFactoryInfo.getMaximumZoomLevel(), (ODLGeom) table.getValueAt(row, geomCol)));
			}

			// Get full WGS84 geometries into collection of PendingWrites
			WKBWriter geomWriter = new WKBWriter();
			LargeList<PendingWrite> pws = new LargeList<>();
			for (int row = 0; row < nrows; row++) {
				Geometry g = ((ODLGeomImpl) table.getValueAt(row, geomCol)).getJTSGeometry();
				byte[] bytes = geomWriter.write(g);
				pws.add(new PendingWrite(indices.get(row), bytes));
			}
			
			if(processingApi.isCancelled()){
				return;
			}

			// write full geometries
			QuadWriter quadWriter = new QuadWriter(tmpFile);
			quadWriter.add(pws, null, -1);

			
			if(processingApi.isCancelled()){
				return;
			}

			// Create executor service
			ExecutorService executorService = Executors.newFixedThreadPool(nbThreads);

			// Loop over zoom levels
			for (int zoom = tileFactoryInfo.getMinimumZoomLevel(); zoom <= tileFactoryInfo.getMaximumZoomLevel(); zoom++) {
				postStatusMessage("ODLRG builder - processing zoom level " + zoom + " with " + ((long)tileFactoryInfo.getLongitudeDegreeWidthInPixels(zoom)) + " pixels/degree");

				// Create converter for this zoom level
				TransformGeomToWorldBitmap mathTransform = createTransform(zoom);

				// Get a list and size it to store the results
				LargeList<PendingWrite> pendingWrites = new LargeList<>();
				for (int row = 0; row < nrows; row++) {
					pendingWrites.add(null);
				}

				// create a per-thread processor and then invoke all
				RowAllocator allocator = new RowAllocator(nrows);
				ArrayList<RowProcessor> processors = new ArrayList<>();
				for (int i = 0; i < nbThreads; i++) {
					processors.add(new RowProcessor(table, geomCol, indices, zoom, mathTransform, allocator,"ODLRG builder - processing zoom level " + zoom + "", pendingWrites));
				}
				List<Future<Void>> futures = executorService.invokeAll(processors);
				for (Future<Void> future : futures) {
					future.get();
				}

				// Process all pending writes
				LargeList<PendingWrite> nonNulls = new LargeList<>();
				for (PendingWrite pw : pendingWrites) {
					if (pw != null) {
						nonNulls.add(pw);
					}
				}
				quadWriter.add(nonNulls, tileFactoryInfo, zoom);
	
				
				if(processingApi.isCancelled()){
					return;
				}

			}

			// shutdown executor service
			executorService.shutdown();

			// create final file
			quadWriter.finish(isNOLPL,indices, outfile);

			// try loading it
			validateFinalFile();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	static class PendingWrite implements Comparable<PendingWrite> {
		final ShapeIndex index;
		byte[]bjsonBytes;
		final byte[] geomBytes;
		Point centroid;

		PendingWrite(ShapeIndex index, byte[] bytes) {
			this.index = index;
			this.geomBytes = bytes;

		}

		@Override
		public int compareTo(PendingWrite o) {
			return Integer.compare(index.rowNb, o.index.rowNb);
		}

	}

	// /**
	// * @param indices
	// * @throws FileNotFoundException
	// * @throws IOException
	// */
	// private void createMergedFile(LargeList<ShapeIndex> indices) throws FileNotFoundException, IOException {
	//
	// // Create header file
	// FileOutputStream outFos = new FileOutputStream(outfile);
	// FileChannel outChannel = outFos.getChannel();
	// writeIndex(indices, outChannel);
	//
	// // check predicted size against file position, remembering to flush the buffer
	// long predictedHeaderSize = getPredictedHeaderIndexSize(indices);
	// if(outChannel.position() != predictedHeaderSize){
	// throw new RuntimeException("Incorrect header size written");
	// }
	//
	// // Now append other file, validating the positions as we append
	// FileInputStream fis = new FileInputStream(tmpFile);
	// //FileChannel inChannel = fis.getChannel();
	// BufferedInputStream bis = new BufferedInputStream(fis);
	// DataInputStream dis = new DataInputStream(bis);
	//
	// while(true){
	// // get output current position
	// long currentOutPos = outChannel.position();
	//
	// // read first byte to see if we have an entry left
	// int b1 =bis.read();
	// if(b1==-1){
	// break;
	// }
	//
	// // read the next 7 bytes of the id and turn into the long
	// ByteBuffer buffer = ByteBuffer.allocate(8);
	// buffer.put((byte)b1);
	// for(int i =0 ; i < 7 ; i++){
	// buffer.put((byte)bis.read());
	// }
	// buffer.flip();
	// long id = buffer.getLong();
	//
	// // check id is valid
	// if(id >= indices.size() || id<0){
	// throw new RuntimeException("Invalid id");
	// }
	//
	// // check this position is known in the index
	// ShapeIndex indx = indices.get(id);
	// boolean found= currentOutPos == indx.originalWGS84GeomPosition + predictedHeaderSize;
	// for(int zoom=0 ; zoom < indx.positions.length && !found ; zoom++){
	// found = currentOutPos == indx.positions[zoom] + predictedHeaderSize;
	// }
	//
	// if(!found){
	// throw new RuntimeException("Position not found in geometry");
	// }
	//
	// // read the geometry bytes in
	// int nbytes = dis.readInt();
	// byte [] bytes = new byte[nbytes];
	// dis.read(bytes);
	//
	// // write back out again
	// ROGUtils.writeGeom(id, bytes, outChannel);
	// }
	//
	//
	// // close files
	// dis.close();
	// outFos.flush();
	// outFos.close();
	//
	// }

	/**
	 * @throws IOException
	 */
	public void validateFinalFile() {
		final QuadLoader loader = new QuadLoader(outfile);
		try {
//			System.out.println("Validating final output file");
			List<ODLRenderOptimisedGeom> geoms = loader.readObjects();
			for (int i = 0; i < geoms.size(); i++) {
				
				ODLRenderOptimisedGeom geom = geoms.get(i);
				for (int zoom = -1; zoom <= tileFactoryInfo.getMaximumZoomLevel(); zoom++) {

					int[] pos = geom.getFilePosition(zoom);
					if (pos != null && pos[0] >= 0) {
						loader.loadGeometry(i, pos[0], pos[1]);
					}
				}
				
				if(i%100==0){
					postStatusMessage("ODLRG builder - validated " + (i+1) + " object(s) across all zoom levels");
				}
				
				
				if(processingApi.isCancelled()){
					loader.close();
					return;
				}

			}
		} catch (Exception e) {		
			throw new RuntimeException(e);
		}
		finally{
			try {
				loader.close();				
			} catch (Exception e2) {
				throw new RuntimeException(e2);
			}
		}

	}

//	/**
//	 * @param indices
//	 * @param out
//	 * @throws IOException
//	 */
//	private void writeIndex(LargeList<ShapeIndex> indices, FileChannel outChannel) throws IOException {
//		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(outChannel)));
//
//		// Get header size
//		long predictedHeaderSize = getPredictedHeaderIndexSize(indices);
//
//		// Write version and number of entries to header
//		long checkHeaderSize = 0;
//		out.writeInt(AppConstants.RENDER_GEOMETRY_FILE_VERSION);
//		checkHeaderSize += 4;
//		out.writeInt(indices.size());
//		checkHeaderSize += 4;
//
//		// Write each entry to header
//		for (int i = 0; i < indices.size(); i++) {
//
//			// write row number / id
//			ShapeIndex indx = indices.get(i);
//			out.writeLong(indx.rowNb);
//			checkHeaderSize += 8;
//
//			// write total points count
//			out.writeInt(indx.nbPointsFullGeometry);
//			checkHeaderSize += 4;
//
//			// write count by shape
//			out.writeInt(indx.pointsCount);
//			out.writeInt(indx.linestringsCount);
//			out.writeInt(indx.polysCount);
//			checkHeaderSize += 3 * 4;
//
//			// write bounds
//			out.writeDouble(indx.getWgsBounds().getMinX());
//			out.writeDouble(indx.getWgsBounds().getMinY());
//			out.writeDouble(indx.getWgsBounds().getWidth());
//			out.writeDouble(indx.getWgsBounds().getHeight());
//			checkHeaderSize += 4 * 8;
//
//			// write latitude
//			out.writeDouble(indx.getWgsCentroid().getLongitude());
//			out.writeDouble(indx.getWgsCentroid().getLatitude());
//			checkHeaderSize += 2 * 8;
//
//			// write full geometry position
//			out.writeLong(predictedHeaderSize + indx.getOriginalWGS84GeomPosition());
//			checkHeaderSize += 8;
//
//			// write array size
//			out.writeByte(indx.positions.length);
//			checkHeaderSize++;
//
//			// write position array
//			for (int j = 0; j < indx.positions.length; j++) {
//				long val = indx.positions[j];
//				if (val >= 0) {
//					val += predictedHeaderSize;
//				}
//				out.writeLong(val);
//				checkHeaderSize += 8;
//			}
//		}
//		if (checkHeaderSize != predictedHeaderSize) {
//			out.close();
//			throw new RuntimeException("Incorrect header size");
//		}
//
//		if (checkHeaderSize < Integer.MAX_VALUE && checkHeaderSize != out.size()) {
//			out.close();
//			throw new RuntimeException("Incorrect header size");
//		}
//
//		// ensure whole header is written to physical file
//		out.flush();
//	}
//
//	/**
//	 * @param indices
//	 * @return
//	 */
//	private long getPredictedHeaderIndexSize(LargeList<ShapeIndex> indices) {
//		int entrySize = 8 + 4 + (3 * 4) + (4 * 8) + (2 * 8) + 8 + 1 + indices.get(0).positions.length * 8;
//		long predictedHeaderSize = 4L + 4 + entrySize * indices.size();
//		return predictedHeaderSize;
//	}

	// /**
	// * @param rf
	// * @param pos
	// */
	// private void testReadGeometry(RandomAccessFile rf, long pos) {
	// try {
	// final InputStream in = Channels.newInputStream(rf.getChannel().position(pos));
	// Geometry g = new WKBReader().read(new InStream() {
	//
	// @Override
	// public void read(byte[] buf) throws IOException {
	// in.read(buf);
	// }
	// });
	// if(g==null){
	// throw new RuntimeException();
	// }
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// }
	// }

	private TransformGeomToWorldBitmap createTransform(final int zoom) {
		LatLongToScreen converter = new LatLongToScreenImpl() {

			@Override
			public Rectangle2D getViewportWorldBitmapScreenPosition() {
				return null;
			}

			@Override
			public Point2D getWorldBitmapPixelPosition(LatLong latLong) {
				Point2D point = GeoUtil.getBitmapCoordinate(new GeoPosition(latLong.getLatitude(), latLong.getLongitude()), zoom, tileFactoryInfo);
				return point;
			}

			@Override
			public LatLong getLongLat(double pixelX, double pixelY) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object getZoomHashmapKey() {
				return zoom;
			}

			@Override
			public int getZoomForObjectFiltering() {
				return zoom;
			}

		};

		TransformGeomToWorldBitmap mathTransform = new TransformGeomToWorldBitmap(converter);
		return mathTransform;
	}

}
