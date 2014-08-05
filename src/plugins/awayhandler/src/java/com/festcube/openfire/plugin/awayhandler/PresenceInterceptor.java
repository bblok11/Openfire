package com.festcube.openfire.plugin.awayhandler;

import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

public class PresenceInterceptor implements PacketInterceptor {

	private static final Log Log = CommonsLogFactory.getLog(PresenceInterceptor.class);
	
	private ArchiveManager archiveManager;
	
	public PresenceInterceptor(ArchiveManager manager){
		
		archiveManager = manager;
	}
	
	@Override
	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {
		
		if(incoming && processed && packet instanceof Presence){
			
			// Is presence stanza
			Presence presence = (Presence)packet;
			Element mucElement = presence.getChildElement("x", "http://jabber.org/protocol/muc");
			Element showElement = presence.getChildElement("show", "");
			
			JID toJID = presence.getTo();
			JID fromJID = presence.getFrom();

			
			if(mucElement != null && showElement != null && presence.isAvailable()){
				
				if(showElement.getTextTrim().equals("chat")){
				
					// Got chat presence to a MUC room, first check if room exists
					MUCRoom mucRoom = MUCHelper.getRoom(toJID);
					if(mucRoom != null){
						
						// Room exists, check if the user has access to it
						if(MUCHelper.userIsOccupantOfRoom(mucRoom, fromJID)){
							
							// User has access, check if he has the reserved nickname
							String reservedNick = mucRoom.getReservedNickname(fromJID);
							if(reservedNick == null || reservedNick.equals(toJID.getResource())){
							
								Log.info("Got chat presence: " + presence.toString());
								
								String nickname = toJID.getResource();
								
								Connection dbConnection = archiveManager.getConnection();
								if(dbConnection != null){
									
									archiveManager.resetMissedMessages(dbConnection, toJID, nickname);
									
									Log.info("Reset missed messages for nickname " + nickname + " in room " + toJID.toBareJID());
									DbConnectionManager.closeConnection(dbConnection);
								}
							}
							else {
								
								Log.info("Got chat presence in room, but user " + fromJID.toString() + " didnt use the reserved nickname");
							}
						}
						else {
							
							Log.info("Got chat presence in room, but user " + fromJID.toString() + " is not an occupant of room " + toJID.getNode());
						}
					}
					else {
						
						Log.info("Got chat presence in room, but room " + toJID.getNode() + " does not exist");
					}
				}
			}
		}
	}
}
