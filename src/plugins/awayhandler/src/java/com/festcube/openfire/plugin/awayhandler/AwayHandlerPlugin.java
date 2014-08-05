package com.festcube.openfire.plugin.awayhandler;

import java.io.File;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.log.util.CommonsLogFactory;


public class AwayHandlerPlugin implements Plugin {

	private static final Log Log = CommonsLogFactory.getLog(AwayHandlerPlugin.class);
	
	private MUCInterceptor mucInterceptor;
	private PresenceInterceptor presenceInterceptor;
	
	private ArchiveManager archiveManager;
	
	
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
        
		Log.info("AwayHandler plugin initialized");
		
		archiveManager = new ArchiveManager(TaskEngine.getInstance());
		
		// MUCInterceptor intercepts messages, so potential missed message counts can be increased
		mucInterceptor = new MUCInterceptor(archiveManager);
		MUCEventDispatcher.addListener(mucInterceptor);
		
		// PresenceInterceptor intercepts presence:show stanza's to the room, so the missed message count can be reset
		presenceInterceptor = new PresenceInterceptor(archiveManager);
		InterceptorManager.getInstance().addInterceptor(presenceInterceptor);
    }

    public void destroyPlugin() {
        
    	MUCEventDispatcher.removeListener(mucInterceptor);
    	InterceptorManager.getInstance().removeInterceptor(presenceInterceptor);
    }
}