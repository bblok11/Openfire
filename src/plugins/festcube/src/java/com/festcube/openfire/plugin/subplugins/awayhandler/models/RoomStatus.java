package com.festcube.openfire.plugin.subplugins.awayhandler.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.xmpp.packet.JID;

public class RoomStatus 
{
	private JID roomJID;
	private Date lastMessageDate;
	
	public RoomStatus(ResultSet rs) throws SQLException{
    	
    	this.roomJID = new JID(rs.getString("roomJID"));
    	
    	long lastMessageLong = rs.getLong("lastMessageDate");
    	this.lastMessageDate = lastMessageLong != 0 ? new Date(lastMessageLong) : null;
    }
	
	public JID getRoomJID() {
		return roomJID;
	}

	public Date getLastMessageDate() {
		return lastMessageDate;
	}
}
