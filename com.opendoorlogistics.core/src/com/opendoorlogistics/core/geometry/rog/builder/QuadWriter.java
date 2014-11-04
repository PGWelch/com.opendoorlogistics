/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog.builder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.geometry.rog.RogReaderUtils;
import com.opendoorlogistics.core.geometry.rog.builder.QuadBlockBuilder.QuadBlock;
import com.opendoorlogistics.core.geometry.rog.builder.ROGBuilder.PendingWrite;
import com.opendoorlogistics.core.utils.LargeList;

import de.undercouch.bson4jackson.BsonFactory;

class QuadWriter {
	final private FileChannel tmpChannel;
	final private LargeList<Long> quadPositions = new LargeList<>();
	final private File tmpFile;
	
	QuadWriter(File tmpFile) {
		try {
			this.tmpFile = tmpFile;
			if(tmpFile.exists()){
				tmpFile.delete();
			}
			@SuppressWarnings("resource")
			RandomAccessFile rf = new RandomAccessFile(tmpFile, "rw");
			tmpChannel = rf.getChannel();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void finish(boolean isNOLPL, List<ShapeIndex> shapes,File outFile){
		try {
			FileOutputStream outFos = new FileOutputStream(outFile);
			FileChannel outChannel = outFos.getChannel();
			
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(outChannel)));
			writeObjectIndex(isNOLPL,shapes, dos);
			
			writeQuadData(outChannel,dos);
			
			outFos.close();	
			tmpChannel.close();
			tmpFile.delete();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	long getTmpSizeBytes(){
		try {
			return tmpChannel.position();			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeQuadData(FileChannel outChannel, DataOutputStream dos){
		try {
			// calculate the start position of the quad data
			dos.flush();
			long n = quadPositions.size();
			long indexSizeBytes = 8 + n * 8;
			long quadDataStart = outChannel.position() + indexSizeBytes;
			
			// write the number of quad positions
			dos.writeLong(n);
			
			// write the index of quad positions
			for(long i=0; i < n ; i++){
				dos.writeLong(quadDataStart + quadPositions.get(i));
			}
			dos.flush();
			
			// now write all quad data, copying from the temp file
			tmpChannel.position(0);
			BufferedInputStream bis = new  BufferedInputStream(Channels.newInputStream(tmpChannel));
			int nextByte = bis.read();
			while(nextByte!=-1){
				dos.write(nextByte);
				nextByte = bis.read();
			}
			dos.flush();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	void add(Iterable<PendingWrite> writes, TileFactoryInfo info, int zoom){
		QuadBlockBuilder builder = new QuadBlockBuilder();
		QuadBlock root = builder.build(writes, info, zoom);
		add(root, zoom);
//		try {
//			System.out.println(quadPositions.size() + " blocks written to tmp file in " + (tmpChannel.position()/1024) + " KB");			
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
	}
	
	void add(QuadBlock block, int zoom){
		try {
			if(block.getNbLeaves()>0){
				// allocate new block by saving its position
				int blockNb = quadPositions.size();
				long pos = tmpChannel.position();
				quadPositions.add(pos);
				
				List<PendingWrite> leaves = block.getLeaves();
				
				// write all to a temporary byte array
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				
				// write block nb
				int headerSize =0;
				dos.writeInt(blockNb);
				headerSize+=4;
				
				// write bjson
				headerSize += ROGWriterUtils.writeByteArray(block.getBjson(), dos);
				
				// write number of leaves
				int n = leaves.size(); 
				dos.writeInt(n);
				headerSize+=4;
				
				// write position of all leaves relative to block start
				headerSize += 4*n;
				int leafPos = headerSize;
				for(int i =0 ; i<n ; i++){
					dos.writeInt(leafPos);
					PendingWrite pw = leaves.get(i);
					pw.index.setBlock(blockNb, i, zoom);
					leafPos+= ROGWriterUtils.getWrittenGeomSize(pw.bjsonBytes, pw.geomBytes);
				}
				
				// now write the leaves themselves				
				for(int i =0 ; i<n ; i++){
					PendingWrite pw = leaves.get(i);
					ROGWriterUtils.writeGeom(pw.index.rowNb,pw.bjsonBytes, pw.geomBytes, dos);
				}
				
				// now write the whole block to disk
				tmpChannel.write(ByteBuffer.wrap(baos.toByteArray()));
			}
			
			for(int i =0 ; i<block.getNbChildren();i++){
				add(block.getChild(i),zoom);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);			
		}
	}

	private void writeBJSONFileHeader(boolean isNOLPL,DataOutputStream dos){
		try {
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    BsonFactory factory = new BsonFactory();
		    JsonGenerator gen = factory.createJsonGenerator(baos);

		    gen.writeStartObject();
		    gen.writeFieldName(RogReaderUtils.VERSION_KEY);
		    gen.writeNumber(RogReaderUtils.RENDER_GEOMETRY_FILE_VERSION);
	
		    gen.writeFieldName(RogReaderUtils.IS_NOPL_KEY);
		    gen.writeBoolean(isNOLPL);
		    
		    gen.writeEndObject();
		    gen.close();
		   
		    ROGWriterUtils.writeByteArray(baos.toByteArray(), dos);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	
	private void writeObjectIndex(boolean isNOLPL,List<ShapeIndex> indices,DataOutputStream out) throws IOException {

		writeBJSONFileHeader(isNOLPL, out);
		
		out.writeInt(indices.size());

		// Write each entry to header
		for (int i = 0; i < indices.size(); i++) {
			
			// write row number / id
			ShapeIndex indx = indices.get(i);
			out.writeLong(indx.rowNb);
			
			// write total points count
			out.writeInt(indx.nbPointsFullGeometry);
			
			// write count by shape 
			out.writeInt(indx.pointsCount);
			out.writeInt(indx.linestringsCount);
			out.writeInt(indx.polysCount);
			
			// write bounds
			out.writeDouble(indx.getWgsBounds().getMinX());
			out.writeDouble(indx.getWgsBounds().getMinY());
			out.writeDouble(indx.getWgsBounds().getWidth());
			out.writeDouble(indx.getWgsBounds().getHeight());
			
			// write latitude
			out.writeDouble(indx.getWgsCentroid().getLongitude());
			out.writeDouble(indx.getWgsCentroid().getLatitude());
			
			// write full geometry position
			out.writeInt(indx.originalWGS84BlockNb);
			out.writeInt(indx.originalWGS84GeomNbInBlock);
			
			// write position array size
			out.writeByte(indx.blockNb.length);
			
			// write position array
			for (int j = 0; j < indx.blockNb.length; j++) {
				out.writeInt(indx.blockNb[j]);
			}
			for (int j = 0; j < indx.blockNb.length; j++) {
				out.writeInt(indx.geomNbInBlock[j]);
			}

			// write binary json data if we have it
			ROGWriterUtils.writeByteArray(indx.binaryJSONData, out);
		}

		// ensure whole header is written to physical file
		out.flush();
	}

}
