package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

abstract public class ArchivedCubeNotification extends ArchivedMessage
{
	protected int type;
	protected String data;
	protected String descriptions;
	
	public ArchivedCubeNotification(Date sentDate, int type, String data, String descriptions) {
		
		this.sentDate = sentDate;
		this.type = type;
		this.data = data;
		this.descriptions = descriptions;
    }
    
    public ArchivedCubeNotification(ResultSet rs) throws SQLException {
    	
    	this.id = rs.getLong("id");
    	this.sentDate = new Date(rs.getLong("sentDate"));
    	this.type = rs.getInt("notificationType");
    	this.data = rs.getString("notificationData");
    	this.descriptions = rs.getString("notificationDescriptions");
    }
    
    public void setId(long id){
    	this.id = id;
    }

	public int getType() {
		return type;
	}

	public String getData() {
		return data;
	}
	
	public String getDescriptions() {
		return descriptions;
	}
}
