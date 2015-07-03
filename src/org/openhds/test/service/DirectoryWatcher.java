package org.openhds.test.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhds.test.service.interfaces.DirectoryWatcherListener;

public class DirectoryWatcher extends Thread{
	
	private WatchService watcher = null;
	private Map<WatchKey,Path> keys = null;
	private List<DirectoryWatcherListener> listeners;
	
	public DirectoryWatcher(){
		System.out.println("Started DirectoryWatcher...");
		listeners = new ArrayList<DirectoryWatcherListener>();
		
		try{
			this.watcher = FileSystems.getDefault().newWatchService();
			this.keys = new HashMap<WatchKey,Path>();
			
			Path dir = Paths.get("data/");
			WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			keys.put(key, dir);
			
			Path dir2 = Paths.get("data/submissions");
			WatchKey key2 = dir2.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			keys.put(key2, dir2);
//			processEvents();
			start();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public void addListener(DirectoryWatcherListener listener){
		if(!listeners.contains(listener)){
			listeners.add(listener);
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		processEvents();
	}
	
	void processEvents() {
		for(;;){
			// wait for key to be signaled
		    WatchKey key;
		    try {
		        key = watcher.take();
		    } catch (InterruptedException x) {
		        return;
		    }
		    
            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }
            
		    for (WatchEvent<?> event: key.pollEvents()) {
		        WatchEvent.Kind<?> kind = event.kind();

//		        System.out.println("Event: " + kind.name());
		        
		        // This key is registered only
		        // for ENTRY_CREATE events,
		        // but an OVERFLOW event can
		        // occur regardless if events
		        // are lost or discarded.
		        if (kind == StandardWatchEventKinds.OVERFLOW) {
		        	System.out.println("Overflow");
		            continue;
		        }
		        else if(kind == StandardWatchEventKinds.ENTRY_CREATE){
			        // The filename is the
			        // context of the event.
			        WatchEvent<Path> ev = (WatchEvent<Path>)event;
			        Path filename = ev.context();

	//                // Context for directory entry event is the file name of entry
	//                WatchEvent<Path> ev = cast(event);
	//                Path name = ev.context();
	                Path child = dir.resolve(filename);
	
	                // print out event
//	                System.out.format("%s: %s\n", event.kind().name(), child);
	                
	                handleFileCreated(kind, filename, dir);
		        }
		        else if(kind == StandardWatchEventKinds.ENTRY_DELETE){
			        WatchEvent<Path> ev = (WatchEvent<Path>)event;
			        Path filename = ev.context();

	                // Context for directory entry event is the file name of entry
	                Path child = dir.resolve(filename);
	
	                // print out event
	                System.out.format("Removed! %s: %s\n", event.kind().name(), child);
	                handleFileDeleted(kind, filename, dir);
		        }
		        else{
		        	System.out.println("Other event: " + kind.name());
		        }
		        
                // reset key and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);

                    // all directories are inaccessible
                    if (keys.isEmpty()) {
                    	System.out.println("Good bye!");
                        break;
                    }
                }
		    }
		}
	}
	
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
    
    private void handleFileCreated(WatchEvent.Kind<?> kind, Path fileName, Path dir){
    	for(DirectoryWatcherListener listener: listeners){
//    		WebserviceDummy dummy = new WebserviceDummy();
//    		dummy.handleNewFile(fileName, dir);
    		System.out.println("Notify listener: " + listener);
    		listener.onDirectoryChanged(kind, fileName, dir);
    	}
    }
    
    private void handleFileDeleted(WatchEvent.Kind<?> kind, Path fileName, Path dir){
    	for(DirectoryWatcherListener listener: listeners){
//    		WebserviceDummy dummy = new WebserviceDummy();
//    		dummy.handleNewFile(fileName, dir);
//    		System.out.println("Notify listener: " + listener);
    		listener.onDirectoryChanged(kind, fileName, dir);
    	}
    }
}
