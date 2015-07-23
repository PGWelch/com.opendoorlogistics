package com.opendoorlogistics.studio;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.appframe.AppFrame;

public class DropFileImporterListener  implements DropTargetListener {
	private final AppFrame appFrame;
	
    public DropFileImporterListener(AppFrame appFrame) {
		this.appFrame = appFrame;
	}

	@Override
    public void drop(DropTargetDropEvent event) {

        // Accept copy drops
        event.acceptDrop(DnDConstants.ACTION_COPY);

        // Get the transfer which can provide the dropped item data
        Transferable transferable = event.getTransferable();

        // Get the data formats of the dropped item
        DataFlavor[] flavors = transferable.getTransferDataFlavors();

        // Loop through the flavors
        for (DataFlavor flavor : flavors) {

            try {

                // If the drop items are files
                if (flavor.isFlavorJavaFileListType()) {

                    // Get all of the dropped files
                    List<File> files = (List<File>) transferable.getTransferData(flavor);

                    // Loop them through
                    for (File file : files) {

                    	// just import first file.. multiple not supported at the moment
                    	String ext = FilenameUtils.getExtension(file.getName());
                    	if(Strings.equalsStd(ext, "xls") || Strings.equalsStd(ext, "xlsx")){
                    		if(appFrame.getLoadedDatastore()==null){
                        		appFrame.openFile(file);
                    		}else{
                        		appFrame.importFile(file, ImportFileType.EXCEL);                    			
                    		}
                        	break;                    		
                    	}
                    	else if(Strings.equalsStd(ext, "csv")) {
                    		appFrame.importFile(file, ImportFileType.CSV);                    		
                    	}
                    }

                }

            } catch (Exception e) {

                // Print out the error stack
                e.printStackTrace();

            }
        }

        // Inform that the drop is complete
        event.dropComplete(true);

    }

    @Override
    public void dragEnter(DropTargetDragEvent event) {
    }

    @Override
    public void dragExit(DropTargetEvent event) {
    }

    @Override
    public void dragOver(DropTargetDragEvent event) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
    }

}