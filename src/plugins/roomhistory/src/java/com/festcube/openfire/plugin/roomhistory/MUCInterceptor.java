package com.festcube.openfire.plugin.roomhistory;

import java.util.Date;

import org.dom4j.Element;
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
		Element notificationElement = message.getChildElement("cubenotification", "fc:cube:notification");
		
		Date date = new Date();
		
		if(notificationElement != null){
			
			int typeId = Integer.parseInt(notificationElement.attributeValue("type"));
			String content = notificationElement.getTextTrim();
			String silent = notificationElement.attributeValue("silent");
			
			boolean isSilent = false;
			if(silent != null){
				if(silent.equals("silent")){
					isSilent = true;
				}
			}
			
			archiveManager.processNotification(user, roomJID, date, messageBody, typeId, isSilent, content);
		}
		else if(messageBody != null){
		
			archiveManager.processMessage(user, roomJID, date, messageBody);
		}
		
		// Save last message date
		if(messageBody != null){
			archiveManager.saveLastMessageDate(roomJID, date);
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
