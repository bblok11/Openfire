package com.festcube.openfire.plugin;

import java.io.File;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.log.util.CommonsLogFactory;

import com.festcube.openfire.plugin.subplugins.awayhandler.AwayHandlerSubPlugin;
import com.festcube.openfire.plugin.subplugins.notificationbroadcaster.NotificationBroadcasterSubPlugin;
import com.festcube.openfire.plugin.subplugins.roomhistory.RoomHistorySubPlugin;

public class FestcubePlugin implements Plugin
{
	private static final Log Log = CommonsLogFactory.getLog(FestcubePlugin.class);
	
	private AwayHandlerSubPlugin awayHandler;
	private NotificationBroadcasterSubPlugin notificationBroadcaster;
	private RoomHistorySubPlugin roomHistory;
	
	
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
        
		Log.info("Festcube plugin initialized");

		awayHandler = new AwayHandlerSubPlugin();
		roomHistory = new RoomHistorySubPlugin();
		notificationBroadcaster = new NotificationBroadcasterSubPlugin(roomHistory);
		
		awayHandler.initialize();
		notificationBroadcaster.initialize();
		roomHistory.initialize();
    }

    public void destroyPlugin() {
        
    	awayHandler.destroy();
		notificationBroadcaster.destroy();
		roomHistory.destroy();
    }
}
