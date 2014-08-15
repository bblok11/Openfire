package com.festcube.openfire.plugin;

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
	public static final String NS_IQ_SEND_NOTIFICATIONS = "fc:sendnotifications";
	public static final String NS_IQ_AWAYDATA = "fc:awaydata";
	public static final String NS_IQ_ROOM_HISTORY = "fc:room:history";
	
	public static final String NS_MESSAGE_NOTIFICATION = "fc:notifications";
	
	private static final Log Log = CommonsLogFactory.getLog(MUCHelper.class);
	
	public static MUCRoom getRoom(JID roomJID){
		
		MultiUserChatService service = MUCHelper.getMUCService(roomJID);
		if(service == null){
			return null;
		}
		
		return service.getChatRoom(roomJID.getNode());
	}
	
	public static boolean userIsOccupantOfRoom(MUCRoom room, JID userJID)
	{
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
	
	public static boolean userIsOccupantOfRoom(JID userJID, JID roomJID){
		
		MultiUserChatService service = MUCHelper.getMUCService(roomJID);
		MUCRoom room = service.getChatRoom(roomJID.getNode());
		
		if(room == null){
			return false;
		}
		
		return userIsOccupantOfRoom(room, userJID);
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
