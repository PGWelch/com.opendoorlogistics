/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.io;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

final public class WatchSingleDirectory implements Runnable {
	private static boolean DEBUG_LOG_TO_CONSOLE = false;
	private final File directory;
	private final DirectoryChangedListener listener;
	private final WatchService watchService;
	private final WatchKey watchKey;
	private final Thread thread;

	public interface DirectoryChangedListener {
		void onDirectoryChanged(File directory);
	}

	private WatchSingleDirectory(File directory, DirectoryChangedListener listener) {
		this.directory = directory;
		this.listener = listener;
		this.thread = new Thread(this);
		try {
			watchService = FileSystems.getDefault().newWatchService();
			Path path = Paths.get(directory.getAbsolutePath());
			watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static WatchSingleDirectory launch(File directory, DirectoryChangedListener listener) {
		
		if(DEBUG_LOG_TO_CONSOLE){
			System.out.println("" + Thread.currentThread().getId()+ " Launching");			
		}
		
		WatchSingleDirectory watcher = new WatchSingleDirectory(directory, listener);
		watcher.thread.start();
		return watcher;
	}

	public File getDirectory(){
		return directory;
	}
	
	public void run() {
		if (watchService == null || watchKey == null) {
			return;
		}
		
		if(DEBUG_LOG_TO_CONSOLE){
			System.out.println("" + Thread.currentThread().getId()+ " Started run method");			
		}

		try {
			do {
				WatchKey watchKey = watchService.take();
				List<WatchEvent<?>> events = watchKey.pollEvents();
				if (events != null && events.size() > 0) {
					if(DEBUG_LOG_TO_CONSOLE){
						System.out.println("" + Thread.currentThread().getId()+ " Received change event");			
					}
					
					// fire listener
					listener.onDirectoryChanged(directory);
				}
			} while (watchKey.reset());

		} catch (Throwable e) {
			// terminating
			if(DEBUG_LOG_TO_CONSOLE){
				System.out.println("" + Thread.currentThread().getId()+ " Exception: " + e.toString());			
			}
			
		}
	}

	public void shutdown() {
		try {
			if(DEBUG_LOG_TO_CONSOLE){
				System.out.println("" + Thread.currentThread().getId()+ " Calling shutdown");			
			}
			
			if (watchService != null) {
				watchService.close();
			}

			if(DEBUG_LOG_TO_CONSOLE){
				System.out.println("" + Thread.currentThread().getId()+ " Waiting for thread to finish");			
			}
			
			// wait until thread finished
			thread.join();
			
			if(DEBUG_LOG_TO_CONSOLE){
				System.out.println("" + Thread.currentThread().getId()+ " Thread has finished");			
			}
			
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
