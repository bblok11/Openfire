package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

abstract public class ArchivedCubeNotification extends ArchivedMessage
{
	protected int type;
	protected String content;
	
	public ArchivedCubeNotification(Date sentDate, int type, String content) {
		
		this.sentDate = sentDate;
		this.type = type;
		this.content = content;
    }
    
    public ArchivedCubeNotification(ResultSet rs) throws SQLException {
    	
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
}
