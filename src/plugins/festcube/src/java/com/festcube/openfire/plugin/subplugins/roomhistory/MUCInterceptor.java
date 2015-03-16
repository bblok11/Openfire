package com.festcube.openfire.plugin.subplugins.roomhistory;

import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.openfire.muc.MUCEventListener;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.dom4j.Element;

import com.festcube.openfire.plugin.MUCHelper;

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
		Type messageType = message.getType();
		
		if(messageBody != null && messageType == Type.groupchat){
			
			Date date = new Date();
			
			Element stampElement = message.getChildElement("stamp", MUCHelper.NS_MESSAGE_STAMP);
			if(stampElement != null){
				
				try {
				
					String dateString = stampElement.getText();
					if(dateString != null && dateString != ""){
						
						XMPPDateTimeFormat dateFormat = new XMPPDateTimeFormat();
						date = dateFormat.parseString(dateString);
					}
				}
				catch(ParseException e){}
			}
			
			Element stampEl = message.addChildElement("stamp", MUCHelper.NS_MESSAGE_STAMP);
			stampEl.addText(XMPPDateTimeFormat.format(date));
		
			archiveManager.processMessage(user, roomJID, date, messageBody);
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
