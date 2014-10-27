/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog;

import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.OSMTileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.utils.LargeList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import de.undercouch.bson4jackson.BsonFactory;

public class QuadLoader implements Closeable, RogFileInformation{
	private final LargeList<Long> quadPositions = new LargeList<>();
	private final RandomAccessFile rf; 	
	private final File file;
	private final FileChannel channel;
	private final TileFactoryInfo info = new OSMTileFactoryInfo();
	private final Point2D [] mapCentresAtZoom ;
	private boolean isNOLP;
	
	public QuadLoader(File file ) {
		this(file, null);
	}
	
	public QuadLoader(File file , List<ODLRenderOptimisedGeom> readObjects) {
		this.file = file;
		
		// pre-fetch info for zooms to prevent unneccessary object allocation
		int nz = info.getMaximumZoomLevel()+1;
		mapCentresAtZoom = new Point2D[nz];
		for(int zoom = info.getMinimumZoomLevel() ; zoom <= info.getMaximumZoomLevel() ; zoom++){
			mapCentresAtZoom[zoom] = info.getMapCenterInPixelsAtZoom(zoom);
		}
		
		try {
			rf = new RandomAccessFile(file, "r");
			channel = rf.getChannel();
			
			DataInputStream dis = createDIS();	
			readObjectsFromFileStart(dis, readObjects);

			// read quad positions		
			long n = dis.readLong();
			for(long l= 0 ; l < n ; l++){
				quadPositions.add(dis.readLong());
			}	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	private DataInputStream createDIS() {
		DataInputStream dis = new DataInputStream(new BufferedInputStream(Channels.newInputStream(channel)));
		return dis;
	}
	
	public List<ODLRenderOptimisedGeom> readObjects(){
		LargeList<ODLRenderOptimisedGeom> ret = new LargeList<>();				
		readObjects( ret);
		return ret;
	}

	private void readObjects( List<ODLRenderOptimisedGeom> list) {
		try {
			channel.position(0);
			DataInputStream dis = createDIS();
			readObjectsFromFileStart(dis, list);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Read the objects assuming the input DataInputStream is positioned at the start of the file
	 * @param dis
	 * @param list
	 */
	private void readObjectsFromFileStart(DataInputStream dis, List<ODLRenderOptimisedGeom> list) {
		try {

			// Read header
		    readFileHeader(dis);
			
			// read number of objects
			int nbObjs = dis.readInt();
			for(int i =0 ; i<nbObjs ; i++){
				ODLRenderOptimisedGeom geom = new ODLRenderOptimisedGeom(dis, this);
				if(list!=null){
					list.add(geom);
				}
			}			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		

	}

	/**
	 * @param dis
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 */
	private void readFileHeader(DataInputStream dis) throws IOException, JsonParseException, JsonMappingException {
		BsonFactory factory = new BsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		JsonNode rootNode = mapper.readValue(RogReaderUtils.readBytesArray(dis), JsonNode.class);
		JsonNode version = rootNode.findValue(RogReaderUtils.VERSION_KEY);
		int rogversion =version.asInt();
		if(rogversion > RogReaderUtils.RENDER_GEOMETRY_FILE_VERSION){
			throw new RuntimeException(RogReaderUtils.RENDER_GEOMETRY_FILE_EXT + " cannot be read as it is made for a newer version of ODL Studio.");
		}
		
		JsonNode isNOLP = rootNode.findValue(RogReaderUtils.IS_NOPL_KEY);
		if(isNOLP!=null){
			this.isNOLP = isNOLP.asBoolean();
		}else{
			this.isNOLP = false;
		}
	}
	
	private static class CacheKey{
		final File file;
		final int blockNb;
		
		CacheKey(File file, int blockNb) {
			this.file = file;
			this.blockNb = blockNb;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + blockNb;
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (blockNb != other.blockNb)
				return false;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			return true;
		}
		
	}
	
	private static class CachedQuadBlock{
		private final byte[] quadBinaryData;
		private final long [] geomIds;
		private final byte[][] bjsonBytes;
		private final byte[][] geomBytes;
		private long sizeBytes=0;
		
		public CachedQuadBlock(DataInputStream dis, int blockNb) {
			try {
				
				// read block nb
				int readBlockNb = dis.readInt();
				if(readBlockNb!=blockNb){
					throw new RuntimeException("Corrupt quadtree file.");
				}

				// read bjson
				quadBinaryData = RogReaderUtils.readBytesArray(dis);
				
				// read number of leaves
				int n =dis.readInt();
				
				// allocate arrays to hold the data, incrementing the object size
				geomIds = new long[n];
				sizeBytes += n*8;

				sizeBytes += n*8 + 8;
				bjsonBytes = new byte[n][];

				sizeBytes += n*8 + 8;
				geomBytes = new byte[n][];
						
				// skip past the position of all leaves relative to block start...
				for(int i =0 ; i<n ; i++){
					dis.readInt();
				}
				
				// read the leaves themselves
				for(int i =0 ; i<n ; i++){
					
					// read geom id
					geomIds[i] = dis.readLong();
	
					// read bjson array
					bjsonBytes[i] = RogReaderUtils.readBytesArray(dis);
					addToSize(bjsonBytes[i]);
					
					// read geom array
					geomBytes[i] = RogReaderUtils.readBytesArray(dis);
					addToSize(geomBytes[i]);
	
				}
	
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}
		
		private void addToSize(byte[]bytes){
			if(bytes!=null){
				sizeBytes += bytes.length;
			}
		}
		
		public long getSizeInBytes(){
			return sizeBytes;
		}
	}
	
	public synchronized Geometry loadGeometry(long geomId, int blockNb, int geomNbInBlock){
		// try fetching the block from the cache
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.ROG_QUADTREE_BLOCKS);
		CacheKey cacheKey = new CacheKey(file, blockNb);
		CachedQuadBlock block = (CachedQuadBlock)cache.get(cacheKey);
		
		// load block and cache it if we didn't find it
		if(block==null){
			try {
				channel.position(quadPositions.get(blockNb));
				block = new CachedQuadBlock(createDIS(),blockNb);
				cache.put(cacheKey, block, block.getSizeInBytes());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}	
		}
		
		// read the geometry
		if(block.geomIds[geomNbInBlock]!=geomId){
			throw new RuntimeException("Invalid quadtree file; read incorrect geometry id");
		}
		
		try {
			WKBReader reader = new WKBReader();
			Geometry g = reader.read(block.geomBytes[geomNbInBlock]);
			return g;			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	


	public Geometry loadTransformedGeometry(long geomId,int blockNb, int geomNbInBlock,final int sourceZoom,final int targetZoom) {
		Geometry g = loadGeometry(geomId,blockNb, geomNbInBlock);
		if(g==null){
			return null;
		}
		
		MathTransform transform = new Abstract2dMathTransform(){
			@Override
			public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
				for(int i =0 ; i< numPts ;i++){
					int srcIndex = srcOff + i*2;
			
					// The transform from lat long to x and y works as:
					// x = mcx_z + lng * pixelsPerLngDegree_z
					// y = mcy_z + fn(lat) * pixelsPerLngRadian_z
				
					// get longitude and fn(lat) which is zoom-independent
					double lng = (srcPts[srcIndex] - mapCentresAtZoom[sourceZoom].getX()) / info.getLongitudeDegreeWidthInPixels(sourceZoom);
					double fnLat = (srcPts[srcIndex+1] - mapCentresAtZoom[sourceZoom].getY()) / info.getLongitudeRadianWidthInPixels(sourceZoom);
					
					// now get x and y in the target zoom
					double x = mapCentresAtZoom[targetZoom].getX() + lng * info.getLongitudeDegreeWidthInPixels(targetZoom);
					double y = mapCentresAtZoom[targetZoom].getY() + fnLat * info.getLongitudeRadianWidthInPixels(targetZoom);
	
					int destIndex = dstOff + i*2;
					dstPts[destIndex] = x;
					dstPts[destIndex+1] = y;
				}
			}	
		};

		try {
			Geometry gTrans = JTS.transform(g, transform);
			return gTrans;			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		rf.close();
	}
	
	@Override
	public boolean getIsNOLPL(){
		return isNOLP;
	}
	
	@Override
	public File getFile(){
		return file;
	}
}
