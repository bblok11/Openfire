package com.festcube.openfire.plugin.roomhistory.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.xmpp.packet.JID;

public class ArchivedCubeNotification extends ArchivedMessage 
{
	private int notificationType;
	private String notificationContent;
	
	public ArchivedCubeNotification(JID fromJID, JID roomJID, Date sentDate, String body, int notificationType, String notificationContent) {

        super(fromJID, roomJID, sentDate, body);
		
		this.notificationType = notificationType;
		this.notificationContent = notificationContent;
    }
    
    public ArchivedCubeNotification(ResultSet rs) throws SQLException{
    	
    	super(rs);
    	
    	this.notificationType = rs.getInt("cubeNotificationType");
    	this.notificationContent = rs.getString("cubeNotificationContent");
    }
    
    public int getNoficitationType(){
    	
    	return notificationType;
    }
    
    public String getNotificationContent(){
    	
    	return notificationContent;
    }

}
