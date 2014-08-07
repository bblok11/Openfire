package com.festcube.openfire.plugin.awayhandler;

import java.util.ArrayList;
import java.util.Collection;
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
		try {
			
			List<MUCRole> roles = room.getOccupantsByBareJID(userJID.asBareJID());
			
			if(roles.size() > 0){
				
				return true;
			}
		}
		catch(UserNotFoundException e){
			
			return false;
		}
		
		return false;
	}

	
	private static MultiUserChatService getMUCService(JID jid){
		
		MultiUserChatManager manager = XMPPServer.getInstance().getMultiUserChatManager();
		MultiUserChatService service = manager.getMultiUserChatService(jid);
		
		return service;
	}
	
	private static MultiUserChatService getMUCService(String subdomain){
		
		MultiUserChatManager manager = XMPPServer.getInstance().getMultiUserChatManager();
		MultiUserChatService service = manager.getMultiUserChatService(subdomain);
		
		return service;
	}
}