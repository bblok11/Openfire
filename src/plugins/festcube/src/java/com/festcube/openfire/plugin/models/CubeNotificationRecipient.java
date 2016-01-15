package com.festcube.openfire.plugin.models;

import org.xmpp.packet.JID;

public class CubeNotificationRecipient 
{
	private JID jid;
	private Long order;
	
	public CubeNotificationRecipient(JID jid, Long order){
		
		this.jid = jid;
		this.order = order;
	}
	
	public JID getJid(){
		return this.jid;
	}
	
	public Long getOrder(){
		return this.order;
	}
}