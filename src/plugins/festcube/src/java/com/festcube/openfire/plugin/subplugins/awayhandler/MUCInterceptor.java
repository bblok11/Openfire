package com.festcube.openfire.plugin.subplugins.awayhandler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCEventListener;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Presence.Show;

import com.festcube.openfire.plugin.MUCHelper;
import com.festcube.openfire.plugin.subplugins.pushnotifications.PushNotificationsSubPlugin;

public class MUCInterceptor implements MUCEventListener 
{
	private static final Log Log = CommonsLogFactory.getLog(MUCInterceptor.class);
	
	private ArchiveManager archiveManager;
	private PushNotificationsSubPlugin pushNotifications;
	
	public MUCInterceptor(ArchiveManager manager, PushNotificationsSubPlugin pushNotifications){
		
		this.archiveManager = manager;
		this.pushNotifications = pushNotifications;
	}
	
	@Override
	public void roomCreated(JID roomJID) {
		
		//
	}

	@Override
	public void roomDestroyed(JID roomJID) {
		
		//
	}

	@Override
	public void occupantJoined(JID roomJID, JID user, String nickname) {
		
		// Reset missed messages for this user
		
		Connection dbConnection = archiveManager.getConnection();
		if(dbConnection != null){
			
			archiveManager.resetMissedMessages(dbConnection, roomJID, nickname);
			
			DbConnectionManager.closeConnection(dbConnection);
		}
	}

	@Override
	public void occupantLeft(JID roomJID, JID user) {

	}

	@Override
	public void nicknameChanged(JID roomJID, JID user, String oldNickname,
			String newNickname) {

		//
	}

	@Override
	public void messageReceived(JID roomJID, JID user, String nickname,
			Message message) {
		
		MUCRoom room = MUCHelper.getRoom(roomJID);
		
		if(room == null){
			return;
		}
		
		Element cubeNotificationEl = message.getChildElement("cubenotification", MUCHelper.NS_MESSAGE_NOTIFICATION);
		boolean isNotification = message.getType() == Type.headline && cubeNotificationEl != null;
		Element orderElement = message.getChildElement("order", MUCHelper.NS_MESSAGE_ORDER);
		
		if(orderElement == null){
			
			// No order assigned, so this message can be ignored
			return;
		}
		
		// Get room members
		Collection<JID> roomMembers = room.getMembers();
		ArrayList<JID> awayJIDs = new ArrayList<JID>(roomMembers);
		ArrayList<JID> presentJIDs = new ArrayList<JID>();
		
		// Remove online + present participants
		Collection<MUCRole> participants = room.getParticipants();
		HashMap<JID, MUCRole> jidRoles = new HashMap<JID, MUCRole>();
		
		for(MUCRole participant : participants){
			
			JID jid = participant.getUserAddress().asBareJID();
			
			if(jid != null){
				
				awayJIDs.remove(jid);
				presentJIDs.add(jid);
			}
			
			jidRoles.put(jid, participant);
		}
		
		// Archive in thread
		Long messageOrder = Long.valueOf(orderElement.getText());
		String messageStamp = message.getChildElement("stamp", MUCHelper.NS_MESSAGE_STAMP).getText();
		
		new Thread(new ArchivingAndNotifyTask(room, presentJIDs, awayJIDs, user, message, messageOrder)).start();
		
		if(!isNotification){
			
			// Let away users know there was a message
			String xmppDomain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
			MessageRouter messageRouter = XMPPServer.getInstance().getMessageRouter();
			
			for(JID jid : awayJIDs){
				
				if(jid == null){
					continue;
				}
				
				Message awayMessage = new Message();
				awayMessage.setType(Type.headline);
				awayMessage.setFrom(xmppDomain);
				awayMessage.setTo(jid);
				
				Element missedEl = awayMessage.addChildElement("missedroommessage", MUCHelper.NS_IQ_AWAYDATA);
				missedEl.addAttribute("roomJid", roomJID.toBareJID());
				missedEl.addAttribute("order", messageOrder.toString());
				missedEl.addAttribute("stamp", messageStamp);
				
				messageRouter.route(awayMessage);
			}
		}
	}

	@Override
	public void privateMessageRecieved(JID toJID, JID fromJID, Message message) {

		//
	}

	@Override
	public void roomSubjectChanged(JID roomJID, JID user, String newSubject) {

		//
	}
	
	
	private class ArchivingAndNotifyTask implements Runnable {

		private MUCRoom room;
		private ArrayList<JID> presentJIDs;
		private ArrayList<JID> awayJIDs;
		private JID senderJID;
		private Message message;
		private Long messageOrder;
		
		public ArchivingAndNotifyTask(MUCRoom room, ArrayList<JID> presentJIDs, ArrayList<JID> awayJIDs, JID senderJID, Message message, Long messageOrder)
		{
			super();
			
			this.room = room;
			this.presentJIDs = presentJIDs;
			this.awayJIDs = awayJIDs;
			this.senderJID = senderJID;
			this.message = message;
			this.messageOrder = messageOrder;
		}
		
		public void run() {
			
			Connection dbConnection = archiveManager.getConnection();
			
			JID roomJID = room.getJID();
			
			// Update last seen date for participants
			ArrayList<String> presentNicknames = new ArrayList<String>();
			
			for(JID jid : presentJIDs){
				presentNicknames.add(jid.getNode());
			}
			
			if(presentNicknames.size() > 0){
				archiveManager.updateLastSeenDate(dbConnection, roomJID, presentNicknames);
			}
			
			
			// Increase missed messages for away users
			ArrayList<String> awayNicknames = new ArrayList<String>();
			
			for(JID jid : awayJIDs){
				awayNicknames.add(jid.getNode());
			}
			
			if(awayNicknames.size() > 0){
				archiveManager.increaseMissedMessages(dbConnection, roomJID, awayNicknames);
			}
			
			
			// Save room last message date & order
			archiveManager.updateRoomLastMessageDateAndOrder(dbConnection, roomJID, new Date(), messageOrder);
			
			DbConnectionManager.closeConnection(dbConnection);
			
			
			// Send push notifications
			HashMap<String, Integer> awayJIDsMissedMessages = archiveManager.getMissedMessagesByNicks(awayNicknames);
			pushNotifications.sendNotifications(room, senderJID, message, awayJIDs, awayJIDsMissedMessages);
		}
	}
}
