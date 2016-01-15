package com.festcube.openfire.plugin.subplugins.notificationbroadcaster;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.log.util.CommonsLogFactory;

import com.festcube.openfire.plugin.ISubPlugin;
import com.festcube.openfire.plugin.subplugins.notificationbroadcaster.handlers.IQSendNotificationHandler;
import com.festcube.openfire.plugin.subplugins.roomhistory.RoomHistorySubPlugin;

public class NotificationBroadcasterSubPlugin implements ISubPlugin
{
	private static final Log Log = CommonsLogFactory.getLog(NotificationBroadcasterSubPlugin.class);
	
	private RoomHistorySubPlugin roomHistoryPlugin;
	
	private IQSendNotificationHandler sendHandler;
	
	public NotificationBroadcasterSubPlugin(RoomHistorySubPlugin roomHistory)
	{
		roomHistoryPlugin = roomHistory;
	}
	
	public void initialize() {
        
		Log.info("NotificationBroadcaster subplugin initialized");

		// IQ Handler for broadcasting the notifications
		sendHandler = new IQSendNotificationHandler(roomHistoryPlugin);
		XMPPServer.getInstance().getIQRouter().addHandler(sendHandler);
    }

    public void destroy() {
        
    	XMPPServer.getInstance().getIQRouter().removeHandler(sendHandler);
    }
}
