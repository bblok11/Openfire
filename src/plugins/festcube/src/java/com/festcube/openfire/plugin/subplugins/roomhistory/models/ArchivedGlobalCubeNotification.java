package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.util.ArrayList;
import java.util.Date;

import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.models.CubeNotificationRecipient;

public class ArchivedGlobalCubeNotification extends ArchivedCubeNotification
{
	protected ArrayList<CubeNotificationRecipient> recipients;
	
	public ArchivedGlobalCubeNotification(Date sentDate, int type, String content, ArrayList<CubeNotificationRecipient> recipients) {
		
		super(sentDate, type, content);
		
		if(recipients == null){
			recipients = new ArrayList<CubeNotificationRecipient>();
		}
		
		this.recipients = recipients;
    }
	
	public ArchivedRecipientCubeNotification getRecipientNotification(CubeNotificationRecipient recipient){
		
		return new ArchivedRecipientCubeNotification(recipient.getJid(), this.sentDate, this.type, this.content, recipient.getOrder());
	}
	
	public ArrayList<CubeNotificationRecipient> getRecipients(){
		return recipients;
	}
}
