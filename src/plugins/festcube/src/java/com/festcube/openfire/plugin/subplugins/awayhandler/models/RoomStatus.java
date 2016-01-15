package com.festcube.openfire.plugin.subplugins.awayhandler.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.xmpp.packet.JID;

public class RoomStatus 
{
	private JID roomJID;
	private Date lastMessageDate;
	private Long lastMessageOrder;
	
	public RoomStatus(ResultSet rs) throws SQLException{
    	
    	this.roomJID = new JID(rs.getString("roomJID"));
    	
    	long lastMessageDateLong = rs.getLong("lastMessageDate");
    	this.lastMessageDate = lastMessageDateLong != 0 ? new Date(lastMessageDateLong) : null;
    	
    	long lastMessageOrderLong = rs.getLong("lastMessageOrder");
    	this.lastMessageOrder = new Long(lastMessageOrderLong);
    }
	
	public JID getRoomJID() {
		return roomJID;
	}

	public Date getLastMessageDate() {
		return lastMessageDate;
	}
	
	public Long getLastMessageOrder(){
		return lastMessageOrder;
	}
}
