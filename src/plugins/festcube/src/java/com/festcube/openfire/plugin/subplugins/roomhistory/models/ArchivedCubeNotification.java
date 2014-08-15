package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.xmpp.packet.JID;

public class ArchivedCubeNotification extends ArchivedMessage
{
	protected int type;
	protected String content;
	protected ArrayList<JID> recipients;
	
	public ArchivedCubeNotification(Date sentDate, int type, String content, ArrayList<JID> recipients) {
		
		this.sentDate = sentDate;
		this.type = type;
		this.content = content;
		
		if(recipients == null){
			recipients = new ArrayList<JID>();
		}
		
		this.recipients = recipients;
    }
    
    public ArchivedCubeNotification(ResultSet rs) throws SQLException{
    	
    	this.id = rs.getLong("id");
    	this.sentDate = new Date(rs.getLong("sentDate"));
    	this.type = rs.getInt("notificationType");
    	this.content = rs.getString("notificationContent");
    }
    
    public void setId(long id){
    	this.id = id;
    }

	public int getType() {
		return type;
	}

	public String getContent() {
		return content;
	}
	
	public ArrayList<JID> getRecipients()
	{
		return recipients;
	}
}
