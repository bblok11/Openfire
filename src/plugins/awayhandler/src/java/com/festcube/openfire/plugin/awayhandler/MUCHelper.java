package com.festcube.openfire.plugin.awayhandler;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Presence.Show;

public class MUCHelper 
{
	private static final Log Log = CommonsLogFactory.getLog(MUCHelper.class);

	
	public static MUCRoom getRoom(JID roomJID){
		
		MultiUserChatService service = MUCHelper.getMUCService(roomJID);
		return service.getChatRoom(roomJID.getNode());
	}

	
	private static MultiUserChatService getMUCService(JID jid){
		
		MultiUserChatManager manager = XMPPServer.getInstance().getMultiUserChatManager();
		MultiUserChatService service = manager.getMultiUserChatService(jid);
		
		return service;
	}
}