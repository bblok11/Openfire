package com.festcube.openfire.plugin.subplugins.pushnotifications;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.ArrayList;

import org.apache.commons.logging.Log;

import com.festcube.openfire.plugin.ISubPlugin;
import com.festcube.openfire.plugin.subplugins.pushnotifications.handlers.IQDeleteDeviceHandler;
import com.festcube.openfire.plugin.subplugins.pushnotifications.handlers.IQUpdateDeviceHandler;

public class PushNotificationsSubPlugin implements ISubPlugin {

	private static final Log Log = CommonsLogFactory.getLog(PushNotificationsSubPlugin.class);
	
	private IQUpdateDeviceHandler updateHandler;
	private IQDeleteDeviceHandler deleteHandler;
	
	private PushNotificationManager pushManager;
	private ArchiveManager archiveManager;
	
	
	@Override
	public void initialize() {
		
		Log.info("PushNotificationsSubPlugin subplugin initialized");
		
		archiveManager = new ArchiveManager();
		
		String certificatePath = JiveGlobals.getProperty("plugin.festcube.pushnotifications.certificatePath", null);
		String certificatePassword = JiveGlobals.getProperty("plugin.festcube.pushnotifications.certificatePassword", null);
		boolean debugMode = JiveGlobals.getBooleanProperty("plugin.festcube.pushnotifications.debug", false);
		
		try {
			pushManager = new PushNotificationManager(TaskEngine.getInstance(), archiveManager, certificatePath, certificatePassword, debugMode);
		} catch (Exception e) {
			
			Log.error("Error while initializing PushNotificationManager: ", e);
		}

		// IQ Handlers
		updateHandler = new IQUpdateDeviceHandler(archiveManager);
		XMPPServer.getInstance().getIQRouter().addHandler(updateHandler);
		
		deleteHandler = new IQDeleteDeviceHandler(archiveManager);
		XMPPServer.getInstance().getIQRouter().addHandler(deleteHandler);
		
		pushManager.start();
	}

	@Override
	public void destroy() {

    	XMPPServer.getInstance().getIQRouter().removeHandler(updateHandler);
    	XMPPServer.getInstance().getIQRouter().removeHandler(deleteHandler);
    	
    	if(pushManager != null){
    		pushManager.destroy();
    	}
	}
	
	
	public void sendNotifications(MUCRoom room, JID senderJID, Message message, ArrayList<JID> recipients){
		
		if(pushManager != null && recipients.size() > 0){
			pushManager.send(room, senderJID, message, recipients);
		}
	}
}