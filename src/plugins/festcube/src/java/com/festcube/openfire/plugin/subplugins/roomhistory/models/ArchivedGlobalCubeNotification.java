package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.util.ArrayList;
import java.util.Date;

import com.festcube.openfire.plugin.models.CubeNotificationRecipient;

public class ArchivedGlobalCubeNotification extends ArchivedCubeNotification
{
	protected ArrayList<CubeNotificationRecipient> recipients;
	
	public ArchivedGlobalCubeNotification(Date sentDate, int type, int notificationId, String data, String descriptions, ArrayList<CubeNotificationRecipient> recipients) {
		
		super(sentDate, type, notificationId, data, descriptions);
		
		if(recipients == null){
			recipients = new ArrayList<CubeNotificationRecipient>();
		}
		
		this.recipients = recipients;
    }
	
	public ArchivedRecipientCubeNotification getRecipientNotification(CubeNotificationRecipient recipient){
		
		return new ArchivedRecipientCubeNotification(recipient.getJid(), this.sentDate, this.type, this.notificationId, this.data, this.descriptions, recipient.getOrder());
	}
	
	public ArrayList<CubeNotificationRecipient> getRecipients(){
		return recipients;
	}
}
