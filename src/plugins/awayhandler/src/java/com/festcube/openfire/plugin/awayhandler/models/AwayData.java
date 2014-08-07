package com.festcube.openfire.plugin.awayhandler.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.xmpp.packet.JID;

public class AwayData 
{
	private JID roomJID;
	private String nick;
	private int missedMessages;
	private Date lastSeenDate;
	
	public AwayData(ResultSet rs) throws SQLException{
    	
    	this.roomJID = new JID(rs.getString("roomJID"));
    	this.nick = rs.getString("nick");
    	this.missedMessages = rs.getInt("missedMessages");
    	
    	long lastSeenDateLong = rs.getLong("lastSeenDate");
    	this.lastSeenDate = lastSeenDateLong != 0 ? new Date(lastSeenDateLong) : null;
    }
	
	public JID getRoomJID() {
		return roomJID;
	}

	public String getNick() {
		return nick;
	}

	public int getMissedMessages() {
		return missedMessages;
	}

	public Date getLastSeenDate() {
		return lastSeenDate;
	}
}
