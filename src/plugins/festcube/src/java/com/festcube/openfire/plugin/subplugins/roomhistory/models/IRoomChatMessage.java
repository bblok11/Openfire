package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.util.Date;

import org.xmpp.packet.JID;

public interface IRoomChatMessage 
{
	public Date getSentDate();
    public Long getId();
    public JID getRoomJID();
    public Long getOrder();
}