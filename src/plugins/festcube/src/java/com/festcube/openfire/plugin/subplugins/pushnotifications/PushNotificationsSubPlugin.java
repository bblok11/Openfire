package com.festcube.openfire.plugin.subplugins.pushnotifications;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;

import com.festcube.openfire.plugin.ISubPlugin;
import com.festcube.openfire.plugin.subplugins.pushnotifications.handlers.IQUpdateDeviceInfoHandler;

public class PushNotificationsSubPlugin implements ISubPlugin {

	private static final Log Log = CommonsLogFactory.getLog(PushNotificationsSubPlugin.class);
	
	private IQUpdateDeviceInfoHandler updateHandler;
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
			pushManager = new PushNotificationManager(archiveManager, certificatePath, certificatePassword, debugMode);
		} catch (Exception e) {
			
			Log.error("Error while initializing PushNotificationManager: ", e);
		}

		// IQ Handler for broadcasting the notifications
		updateHandler = new IQUpdateDeviceInfoHandler(archiveManager);
		XMPPServer.getInstance().getIQRouter().addHandler(updateHandler);
	}

	@Override
	public void destroy() {

    	XMPPServer.getInstance().getIQRouter().removeHandler(updateHandler);
    	
    	if(pushManager != null){
    		pushManager.destroy();
    	}
	}
	
	
	public void sendNotifications(MUCRoom room, Message message, ArrayList<JID> recipients){
		
		if(pushManager != null){
			pushManager.send(room, message, recipients);
		}
	}
}