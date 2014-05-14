package com.festcube.openfire.plugin.roomhistory.handler;

import java.util.ArrayList;

import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.roomhistory.ArchiveManager;
import com.festcube.openfire.plugin.roomhistory.ArchivedMessage;
import com.festcube.openfire.plugin.roomhistory.MUCHelper;
import com.festcube.openfire.plugin.roomhistory.xep0059.XmppResultSet;

public class IQFetchHandler extends IQHandler {
	
	protected static final String NAMESPACE = "fc:room:history";
	private final IQHandlerInfo info;
	
	private ArchiveManager archiveManager;

	
	public IQFetchHandler(ArchiveManager manager){
		
		super("Room history");
		this.info = new IQHandlerInfo("roomhistory", NAMESPACE);
		
		this.archiveManager = manager;
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		IQ reply = IQ.createResultIQ(packet);
		Element roomHistoryEl = packet.getChildElement();
		
		JID roomJid = new JID(roomHistoryEl.attributeValue("room"));
		JID userJid = packet.getFrom();
		
		if(!MUCHelper.userIsOccupantOfRoom(userJid, roomJid)){
			throw new org.jivesoftware.openfire.auth.UnauthorizedException("You are not an occupant of this room");
		}
		
		XmppResultSet resultSet = null;
		Element setElement = roomHistoryEl.element(QName.get("set", XmppResultSet.NAMESPACE));
        if (setElement != null){
            resultSet = new XmppResultSet(setElement);
        }
        
        
		
		Element responseEl = reply.setChildElement("roomhistory", NAMESPACE);
		responseEl.addAttribute("room", roomJid.toBareJID());
		
		ArrayList<ArchivedMessage> messages = archiveManager.getArchivedMessages(roomJid, resultSet);
		
		for(ArchivedMessage message : messages){
			
			JID fromJID = new JID(message.getRoomJID().getNode(), message.getRoomJID().getDomain(), message.getFromJID().getNode());
			
			Element messageEl = responseEl.addElement("message");
			messageEl.addAttribute("from", fromJID.toFullJID());
			messageEl.addAttribute("to", message.getRoomJID().toBareJID());
			messageEl.addAttribute("type", "groupchat");
			
			Element bodyEl = messageEl.addElement("body");
			bodyEl.addText(message.getBody());
			
			Element delayEl = messageEl.addElement("delay", "urn:xmpp:delay");
			delayEl.addAttribute("stamp", XMPPDateTimeFormat.format(message.getSentDate()));
		}
		
		
		if (resultSet != null && messages.size() > 0) {
			
			ArchivedMessage firstMessage = messages.get(0);
			ArchivedMessage lastMessage = messages.get(messages.size() - 1);
			
			resultSet.setFirst(XMPPDateTimeFormat.format(firstMessage.getSentDate()));
			resultSet.setFirstIndex(0);
			resultSet.setLast(XMPPDateTimeFormat.format(lastMessage.getSentDate()));
			resultSet.setCount(archiveManager.getArchivedMessagesCount(roomJid));
			
			responseEl.add(resultSet.createResultElement());
		}
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		
		return this.info;
	}

}
