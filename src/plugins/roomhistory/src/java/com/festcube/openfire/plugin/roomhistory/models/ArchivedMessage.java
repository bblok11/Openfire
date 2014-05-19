package com.festcube.openfire.plugin.roomhistory.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerInfo;
import org.xmpp.packet.JID;

public class ArchivedMessage 
{
    private Long id;
	private JID fromJID;
    private JID roomJID;
    private Date sentDate;
    private String body;

    /**
     * Creates a new archived message.
     *
     * @param fromJID the JID of the user that sent the message.
     * @param toJID the JID of the user/room that the message was sent to.
     * @param sentDate the date the message was sent.
     * @param body the body of the message
     */
    public ArchivedMessage(JID fromJID, JID roomJID, Date sentDate, String body) {

        this.fromJID = fromJID;
        this.roomJID = roomJID;
        this.sentDate = sentDate;
        this.body = body;
    }
    
    public ArchivedMessage(ResultSet rs) throws SQLException{
    	
    	XMPPServerInfo serverInfo = XMPPServer.getInstance().getServerInfo();
    	
    	this.id = rs.getLong("id");
    	this.fromJID = new JID(rs.getString("nick"), serverInfo.getXMPPDomain(), null);
    	this.roomJID = new JID(rs.getString("roomJID"));
    	this.sentDate = new Date(rs.getLong("sentDate"));
    	this.body = rs.getString("body");
    }


    /**
     * The JID of the user that sent the message.
     *
     * @return the sender JID.
     */
    public JID getFromJID() {
        return fromJID;
    }

    /**
     * The JID of the room that received the message.
     *
     * @return the recipient JID.
     */
    public JID getRoomJID(){
        return roomJID;
    }

    /**
     * The date the message was sent.
     *
     * @return the date the message was sent.
     */
    public Date getSentDate() {
        return sentDate;
    }

    /**
     * The body of the message.
     *
     * @return the body of the message.
     */
    public String getBody() {
        return body;
    }
    
    public Long getId(){
    	return id;
    }
}
