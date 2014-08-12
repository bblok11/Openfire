package com.festcube.openfire.plugin.awayhandler;

import java.io.File;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.util.log.util.CommonsLogFactory;

import com.festcube.openfire.plugin.awayhandler.handlers.IQFetchHandler;


public class AwayHandlerPlugin implements Plugin {

	private static final Log Log = CommonsLogFactory.getLog(AwayHandlerPlugin.class);
	
	private IQFetchHandler fetchHandler;
	private MUCInterceptor mucInterceptor;
	
	private ArchiveManager archiveManager;
	
	
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
        
		Log.info("AwayHandler plugin initialized");
		
		archiveManager = new ArchiveManager();
		
		// MUCInterceptor intercepts messages, so potential missed message counts can be increased
		mucInterceptor = new MUCInterceptor(archiveManager);
		MUCEventDispatcher.addListener(mucInterceptor);
		
		// IQ Handler for returning the away data
		fetchHandler = new IQFetchHandler(archiveManager);
		XMPPServer.getInstance().getIQRouter().addHandler(fetchHandler);
    }

    public void destroyPlugin() {
        
    	MUCEventDispatcher.removeListener(mucInterceptor);
    	XMPPServer.getInstance().getIQRouter().removeHandler(fetchHandler);
    }
}