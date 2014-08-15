package com.festcube.openfire.plugin.subplugins.awayhandler;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.util.log.util.CommonsLogFactory;

import com.festcube.openfire.plugin.ISubPlugin;
import com.festcube.openfire.plugin.subplugins.awayhandler.handlers.IQFetchAwayDataHandler;


public class AwayHandlerSubPlugin implements ISubPlugin {

	private static final Log Log = CommonsLogFactory.getLog(AwayHandlerSubPlugin.class);
	
	private IQFetchAwayDataHandler fetchHandler;
	private MUCInterceptor mucInterceptor;
	
	private ArchiveManager archiveManager;
	
	
	public void initialize() {
        
		Log.info("AwayHandler subplugin initialized");
		
		archiveManager = new ArchiveManager();
		
		// MUCInterceptor intercepts messages, so potential missed message counts can be increased
		mucInterceptor = new MUCInterceptor(archiveManager);
		MUCEventDispatcher.addListener(mucInterceptor);
		
		// IQ Handler for returning the away data
		fetchHandler = new IQFetchAwayDataHandler(archiveManager);
		XMPPServer.getInstance().getIQRouter().addHandler(fetchHandler);
    }

    public void destroy() {
        
    	MUCEventDispatcher.removeListener(mucInterceptor);
    	XMPPServer.getInstance().getIQRouter().removeHandler(fetchHandler);
    }
}