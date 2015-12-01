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
		boolean messageBodyIsEmpty = message.getBody() == null || message.getBody().equals("");
		boolean isNotification = message.getType() == Type.headline && cubeNotificationEl != null;
		
		if(messageBodyIsEmpty && !isNotification){
			
			// Ignore, this is an empty message and no notification
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
		Long messageOrder = Long.valueOf(message.getChildElement("order", MUCHelper.NS_MESSAGE_ORDER).getText());
		new Thread(new ArchivingTask(roomJID, presentJIDs, awayJIDs, messageOrder, !isNotification)).start();
		
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
				
				messageRouter.route(awayMessage);
			}
		}
		
		// Send push notifications
		ArrayList<JID> pushReceiverJIDs = new ArrayList<JID>();
		for(JID jid : roomMembers){
			
			if(awayJIDs.contains(jid)){
				
				// User is not in room, add to receivers
				pushReceiverJIDs.add(jid);
			}
			else {
				
				// User is in room, check presence status
				MUCRole role = jidRoles.get(jid);
				Presence presence = role.getPresence();
				Presence.Show presenceShow = presence != null ? presence.getShow() : Presence.Show.chat;
				
				if(presenceShow != null && !presence.getShow().equals(Presence.Show.chat)){
					pushReceiverJIDs.add(jid);
				}
			}
		}
		
		pushNotifications.sendNotifications(room, user, message, pushReceiverJIDs);
	}

	@Override
	public void privateMessageRecieved(JID toJID, JID fromJID, Message message) {

		//
	}

	@Override
	public void roomSubjectChanged(JID roomJID, JID user, String newSubject) {

		//
	}
	
	
	private class ArchivingTask implements Runnable {

		private JID roomJID;
		private ArrayList<JID> presentJIDs;
		private ArrayList<JID> awayJIDs;
		private boolean increaseMissedMessages;
		private Long messageOrder;
		
		public ArchivingTask(JID roomJID, ArrayList<JID> presentJIDs, ArrayList<JID> awayJIDs, Long messageOrder, boolean increaseMissedMessages)
		{
			super();
			
			this.roomJID = roomJID;
			this.presentJIDs = presentJIDs;
			this.awayJIDs = awayJIDs;
			this.increaseMissedMessages = increaseMissedMessages;
			this.messageOrder = messageOrder;
		}
		
		public void run() {
			
			Connection dbConnection = archiveManager.getConnection();
			
			// Update last seen date for participants
			ArrayList<String> presentNicknames = new ArrayList<String>();
			
			for(JID jid : presentJIDs){
				presentNicknames.add(jid.getNode());
			}
			
			if(presentNicknames.size() > 0){
				archiveManager.updateLastSeenDate(dbConnection, roomJID, presentNicknames);
			}
			
			
			if(!increaseMissedMessages){
				
				// Increase missed messages for away users
				ArrayList<String> awayNicknames = new ArrayList<String>();
				
				for(JID jid : awayJIDs){
					awayNicknames.add(jid.getNode());
				}
				
				if(awayNicknames.size() > 0){
					archiveManager.increaseMissedMessages(dbConnection, roomJID, awayNicknames);
				}
			}
			
			// Save room last message date
			archiveManager.updateRoomLastMessageDateAndOrder(dbConnection, roomJID, new Date(), messageOrder);
			
			DbConnectionManager.closeConnection(dbConnection);
		}
	}
}
