package com.festcube.openfire.plugin.roomhistory;

import java.util.Date;

import org.jivesoftware.openfire.muc.MUCEventListener;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MUCInterceptor implements MUCEventListener {

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
		
		//
	}

	@Override
	public void occupantLeft(JID roomJID, JID user) {

		//
	}

	@Override
	public void nicknameChanged(JID roomJID, JID user, String oldNickname,
			String newNickname) {

		//
	}

	@Override
	public void messageReceived(JID roomJID, JID user, String nickname,
			Message message) {
		
		String messageBody = message.getBody();
		
		if(messageBody != null){
		
			archiveManager.processMessage(user, roomJID, new Date(), messageBody);
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
