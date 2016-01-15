package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.xmpp.packet.JID;

public class ArchivedRecipientCubeNotification extends ArchivedCubeNotification implements IRoomChatMessage
{
	protected JID roomJID;
	protected Long order;
	
	public ArchivedRecipientCubeNotification(JID roomJID, Date sentDate, int type, String data, String descriptions, Long order) {
		
		super(sentDate, type, data, descriptions);
		
		this.roomJID = roomJID;
		this.order = order;
    }
	
	public ArchivedRecipientCubeNotification(ResultSet rs, JID roomJID) throws SQLException {
    	
    	super(rs);
		
    	this.roomJID = roomJID;
		this.order = rs.getLong("order");
    }
	
	public Long getOrder(){
		return this.order;
	}
	
	public JID getRoomJID(){
        return roomJID;
    }
}
