package com.festcube.openfire.plugin.awayhandler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCEventListener;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;


public class MUCInterceptor implements MUCEventListener {

	protected static final String NAMESPACE = "fc:awaydata";
	
	private ArchiveManager archiveManager;
	
	public MUCInterceptor(ArchiveManager manager){
		
		archiveManager = manager;
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
		Connection dbConnection = archiveManager.getConnection();
		
		if(room == null || message.getBody() == null || dbConnection == null){
			return;
		}
		
		// Get room members
		Collection<JID> roomMembers = room.getMembers();
		ArrayList<JID> awayJIDs = new ArrayList<JID>(roomMembers);
		ArrayList<JID> presentJIDs = new ArrayList<JID>();
		
		// Remove online + present participants
		Collection<MUCRole> participants = room.getParticipants();
		
		for(MUCRole participant : participants){
			
			JID jid = participant.getUserAddress().asBareJID();
			
			if(jid != null){
				
				awayJIDs.remove(jid);
				presentJIDs.add(jid);
			}
		}
		
		// Increase missed messages for away users
		ArrayList<String> awayNicknames = new ArrayList<String>();
		
		for(JID jid : awayJIDs){
			awayNicknames.add(jid.getNode());
		}
		
		archiveManager.increaseMissedMessages(dbConnection, roomJID, awayNicknames);
		
		
		// Update last seen date for participants
		ArrayList<String> presentNicknames = new ArrayList<String>();
		
		for(JID jid : presentJIDs){
			presentNicknames.add(jid.getNode());
		}
		
		archiveManager.updateLastSeenDate(dbConnection, roomJID, presentNicknames);
		
		// Save room last message date
		archiveManager.updateRoomLastMessageDate(dbConnection, roomJID, new Date());
		
		DbConnectionManager.closeConnection(dbConnection);
		
		
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
			
			Element missedEl = awayMessage.addChildElement("missedroommessage", NAMESPACE);
			missedEl.addAttribute("roomJid", roomJID.toBareJID());
			
			messageRouter.route(awayMessage);
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

}
