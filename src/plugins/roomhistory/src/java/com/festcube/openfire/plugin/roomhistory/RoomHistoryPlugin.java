package com.festcube.openfire.plugin.roomhistory;

import java.io.File;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.log.util.CommonsLogFactory;

import com.festcube.openfire.plugin.roomhistory.handlers.IQFetchHandler;

public class RoomHistoryPlugin implements Plugin {

	private static final Log Log = CommonsLogFactory.getLog(RoomHistoryPlugin.class);
	
	private IQFetchHandler fetchHandler;
	private MUCInterceptor mucInterceptor;
	private ArchiveManager archiveManager;
	
	
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
        
		Log.info("Roomhistory plugin initialized");
		
		archiveManager = new ArchiveManager(TaskEngine.getInstance());
		
		fetchHandler = new IQFetchHandler(archiveManager);
		XMPPServer.getInstance().getIQRouter().addHandler(fetchHandler);
		
		mucInterceptor = new MUCInterceptor(archiveManager);
		MUCEventDispatcher.addListener(mucInterceptor);
		
		archiveManager.start();
    }

    public void destroyPlugin() {
        
    	XMPPServer.getInstance().getIQRouter().removeHandler(fetchHandler);
    	MUCEventDispatcher.removeListener(mucInterceptor);
    	
    	archiveManager.stop();
    }
}
