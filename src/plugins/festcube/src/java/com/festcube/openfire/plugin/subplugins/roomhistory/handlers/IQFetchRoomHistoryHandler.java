package com.festcube.openfire.plugin.subplugins.roomhistory.handlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import com.festcube.openfire.plugin.MUCHelper;
import com.festcube.openfire.plugin.subplugins.roomhistory.ArchiveManager;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedChatMediaMessage;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedChatMessage;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedCubeNotification;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedMessage;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedRecipientCubeNotification;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.IRoomChatMessage;
import com.festcube.openfire.plugin.xep0059.XmppResultSet;

public class IQFetchRoomHistoryHandler extends IQHandler {
	
	private static final Log Log = CommonsLogFactory.getLog(IQFetchRoomHistoryHandler.class);
	private final IQHandlerInfo info;
	
	private ArchiveManager archiveManager;

	
	public IQFetchRoomHistoryHandler(ArchiveManager manager){
		
		super("Room history");
		this.info = new IQHandlerInfo("roomhistory", MUCHelper.NS_IQ_ROOM_HISTORY);
		
		this.archiveManager = manager;
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Element roomHistoryEl = packet.getChildElement();
		IQ reply = IQ.createResultIQ(packet);
		
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
        
        String modeAttributeValue = roomHistoryEl.attributeValue("mode");
        boolean latestMode = modeAttributeValue != null ? modeAttributeValue.equals("latest") : false;
        
        // Create response
		Element responseEl = reply.setChildElement("roomhistory", MUCHelper.NS_IQ_ROOM_HISTORY);
		responseEl.addAttribute("room", roomJid.toBareJID());
		
		ArrayList<IRoomChatMessage> messages = archiveManager.getArchivedMessages(roomJid, resultSet, latestMode);
		
		for(IRoomChatMessage message : messages){
			
			Element messageEl = responseEl.addElement("message");
			
			if(message instanceof ArchivedChatMessage){
			
				ArchivedChatMessage archivedMessage = (ArchivedChatMessage)message;
				
				JID fromJID = new JID(archivedMessage.getRoomJID().getNode(), archivedMessage.getRoomJID().getDomain(), archivedMessage.getFromJID().getNode());
				
				messageEl.addAttribute("from", fromJID.toFullJID());
				messageEl.addAttribute("to", roomJid.toBareJID());
				messageEl.addAttribute("type", "groupchat");
				
				if(archivedMessage instanceof ArchivedChatMediaMessage){
					
					ArchivedChatMediaMessage mediaMessage = (ArchivedChatMediaMessage)archivedMessage;
					
					Element mediaEl = messageEl.addElement("media", MUCHelper.NS_MESSAGE_MEDIA);
					mediaEl.addAttribute("type", mediaMessage.getType().getStringValue());
					mediaEl.addAttribute("base", mediaMessage.getUrlBase());
					mediaEl.addAttribute("filename", mediaMessage.getUrlFilename());
					mediaEl.addAttribute("id", mediaMessage.getMediaId().toString());
				}
				else {
					
					Element bodyEl = messageEl.addElement("body");
					bodyEl.addText(archivedMessage.getBody());
				}
			}
			else if(message instanceof ArchivedRecipientCubeNotification){
				
				ArchivedRecipientCubeNotification notification = (ArchivedRecipientCubeNotification)message;
				
				messageEl.addAttribute("from", roomJid.toBareJID());
				messageEl.addAttribute("to", roomJid.toBareJID());
				messageEl.addAttribute("type", "headline");
				
				Element notificationEl = messageEl.addElement("cubenotification", MUCHelper.NS_MESSAGE_NOTIFICATION);
				notificationEl.addAttribute("type", String.valueOf(notification.getType()));
				
				notificationEl.addElement("data").addText(notification.getData());
				
				// Descriptions
				String descriptionsString = notification.getDescriptions();
				if(descriptionsString != null){
					
					Element descriptionsEl = notificationEl.addElement("descriptions");
					
					try {
						
						JSONObject descriptions = new JSONObject(descriptionsString);
						
						@SuppressWarnings("unchecked")
						Iterator<String> locales = descriptions.keys();

						while(locales.hasNext()) {
							
						    String locale = (String)locales.next();
						    String description = descriptions.getString(locale);
						    
						    Element currentDescriptionEl = descriptionsEl.addElement("description");
						    currentDescriptionEl.addAttribute("locale", locale);
						    currentDescriptionEl.setText(description);
						}
						
					} catch (JSONException e) {
						
						Log.error("Error while parsing cube notification descriptions JSON", e);
					}
				}
			}
			
			Element delayEl = messageEl.addElement("delay", "urn:xmpp:delay");
			delayEl.addAttribute("stamp", XMPPDateTimeFormat.format(message.getSentDate()));
			
			messageEl.addElement("order", MUCHelper.NS_MESSAGE_ORDER).setText(message.getOrder().toString());
		}
		
		
		if (resultSet != null && messages.size() > 0) {
			
			IRoomChatMessage firstMessage = messages.get(0);
			IRoomChatMessage lastMessage = messages.get(messages.size() - 1);
			
			resultSet.setFirst(firstMessage.getOrder().toString());
			resultSet.setLast(lastMessage.getOrder().toString());
			resultSet.setCount(archiveManager.getArchivedMessagesCount(roomJid));
			
			if(firstMessage.getOrder().longValue() == 0){
				resultSet.setFirstIndex((long)0);
			}
			
			responseEl.add(resultSet.createResultElement());
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
