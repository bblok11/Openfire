package com.festcube.openfire.plugin.subplugins.roomhistory;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.ISubPlugin;
import com.festcube.openfire.plugin.subplugins.roomhistory.handlers.IQFetchRoomHistoryHandler;

public class RoomHistorySubPlugin implements ISubPlugin {

	private static final Log Log = CommonsLogFactory.getLog(RoomHistorySubPlugin.class);
	
	private IQFetchRoomHistoryHandler fetchHandler;
	private MUCInterceptor mucInterceptor;
	private ArchiveManager archiveManager;
	
	
	public void initialize() {
        
		Log.info("Roomhistory subplugin initialized");
		
		archiveManager = new ArchiveManager(TaskEngine.getInstance());
		
		fetchHandler = new IQFetchRoomHistoryHandler(archiveManager);
		XMPPServer.getInstance().getIQRouter().addHandler(fetchHandler);
		
		mucInterceptor = new MUCInterceptor(archiveManager);
		MUCEventDispatcher.addListener(mucInterceptor);
		
		archiveManager.start();
    }

    public void destroy() {
        
    	XMPPServer.getInstance().getIQRouter().removeHandler(fetchHandler);
    	MUCEventDispatcher.removeListener(mucInterceptor);
    	
    	archiveManager.stop();
    }
    
    public void reportRoomNotification(int type, String content, ArrayList<JID> recipients)
    {
    	archiveManager.processNotification(new Date(), type, content, recipients);
    }
}