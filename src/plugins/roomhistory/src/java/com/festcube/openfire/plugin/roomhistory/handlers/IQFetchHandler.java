package com.festcube.openfire.plugin.roomhistory.handlers;

import java.util.ArrayList;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import com.festcube.openfire.plugin.roomhistory.ArchiveManager;
import com.festcube.openfire.plugin.roomhistory.MUCHelper;
import com.festcube.openfire.plugin.roomhistory.models.ArchivedMessage;
import com.festcube.openfire.plugin.roomhistory.xep0059.XmppResultSet;

public class IQFetchHandler extends IQHandler {
	
	private static final Log Log = CommonsLogFactory.getLog(IQFetchHandler.class);
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
		
		try {
		
		JID roomJid = new JID(roomHistoryEl.attributeValue("room"));
		JID userJid = packet.getFrom();
		
		if(!MUCHelper.userIsOccupantOfRoom(userJid, roomJid)){
			
			return error(packet, PacketError.Condition.not_authorized);
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
			resultSet.setLast(XMPPDateTimeFormat.format(lastMessage.getSentDate()));
			resultSet.setCount(archiveManager.getArchivedMessagesCount(roomJid));
			
			if(archiveManager.isFirstMessageInRoom(firstMessage, roomJid)){
				resultSet.setFirstIndex((long)0);
			}
			
			responseEl.add(resultSet.createResultElement());
		}
		
		}
		catch(Exception e){
			
			Log.error("Roomhistory exception: " + e.toString() + " " + ExceptionUtils.getStackTrace(e));
		}
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		
		return this.info;
	}
	
	
	protected IQ error(Packet packet, PacketError.Condition condition) {
		IQ reply;

		reply = new IQ(IQ.Type.error, packet.getID());
		reply.setFrom(packet.getTo());
		reply.setTo(packet.getFrom());
		reply.setError(condition);
		return reply;
	}

}
