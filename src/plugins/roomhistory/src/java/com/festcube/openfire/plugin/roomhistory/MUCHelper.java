package com.festcube.openfire.plugin.roomhistory;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

public class MUCHelper 
{
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);
	
	public static boolean userIsOccupantOfRoom(JID userJID, JID roomJID){
		
		MultiUserChatService service = MUCHelper.getMUCService(roomJID);
		MUCRoom room = service.getChatRoom(roomJID.getNode());
		
		if(room == null){
			return false;
		}
		
		boolean result = false;
		
		try {
			
			List<MUCRole> rolesList = room.getOccupantsByBareJID(userJID.asBareJID());
			result = !rolesList.isEmpty();
		}
		catch(UserNotFoundException e){
			
			result = false;
		}
		
		return result;
	}
	
	public static boolean userIsMemberOfRoom(JID userJID, JID roomJID){
		
		MultiUserChatService service = MUCHelper.getMUCService(roomJID);
		MUCRoom room = service.getChatRoom(roomJID.getNode());
		
		if(room == null){
			return false;
		}
		
		ArrayList<JID> roomMembers = new ArrayList<JID>();
		roomMembers.addAll(room.getOwners());
		roomMembers.addAll(room.getAdmins());
		roomMembers.addAll(room.getMembers());
		
		boolean foundUser = false;
		
		for(JID memberJID : roomMembers){
			
			if(memberJID.toBareJID().equals(userJID.toBareJID())){
				foundUser = true;
			}
		}
		
		return foundUser;
	}
	
	private static MultiUserChatService getMUCService(JID jid){
		
		MultiUserChatManager manager = XMPPServer.getInstance().getMultiUserChatManager();
		MultiUserChatService service = manager.getMultiUserChatService(jid);
		
		return service;
	}
}
