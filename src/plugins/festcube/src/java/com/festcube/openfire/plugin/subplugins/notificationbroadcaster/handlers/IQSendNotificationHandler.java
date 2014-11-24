package com.festcube.openfire.plugin.subplugins.notificationbroadcaster.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;

import com.festcube.openfire.plugin.MUCHelper;
import com.festcube.openfire.plugin.subplugins.roomhistory.RoomHistorySubPlugin;


public class IQSendNotificationHandler extends IQHandler {

	private static final Log Log = CommonsLogFactory.getLog(IQSendNotificationHandler.class);
	private final IQHandlerInfo info;
	private final JID fromJid;
	
	private RoomHistorySubPlugin roomHistoryPlugin;
	
	private final ArrayList<JID> allowedUsers;
	
	
	public IQSendNotificationHandler(RoomHistorySubPlugin roomHistory) {
		
		super("Notification broadcaster");
		
		this.roomHistoryPlugin = roomHistory;
		this.info = new IQHandlerInfo("send", MUCHelper.NS_IQ_SEND_NOTIFICATIONS);
		this.fromJid = new JID(XMPPServer.getInstance().getServerInfo().getXMPPDomain());
		
		// Load allowed users
		String allowedStr = JiveGlobals.getProperty("plugin.notificationBroadcaster.allowedJids.sendnotifications", "");
		String[] usernames = allowedStr.split(",");
		
		allowedUsers = new ArrayList<JID>();
		
		for(String username : usernames){
			
			try {
				
				JID jid = new JID(username);
				if(jid != null){
					
					allowedUsers.add(jid);
					
					Log.info("Allowed notification send user: " + jid.toString());
				}
			}
			catch(Exception e){
				// Ignore
			}
		}
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		if(!allowedUsers.contains(packet.getFrom().asBareJID())){
			
			Log.info("Not-allowed user tried to send notifications: " + packet.getFrom().toString());
			throw new UnauthorizedException();
		}
		
		Element sendEl = packet.getChildElement();
		
		// User notifications
		
		@SuppressWarnings("unchecked")
		List<Element> userNotificationEls = sendEl.elements("usernotification");
		
		for(Element el : userNotificationEls){
			sendUserNotification(el);
		}
		
		// Room notifications
		
		@SuppressWarnings("unchecked")
		List<Element> cubeNotificationEls = sendEl.elements("cubenotification");
		
		for(Element el : cubeNotificationEls){
			sendCubeNotification(el);
		}
		
		IQ reply = IQ.createResultIQ(packet);
		
		return reply;
	}
	
	private void sendUserNotification(Element notificationEl)
	{
		Element idEl = notificationEl.element("id");
		Element typeEl = notificationEl.element("type");
		Element recipientsEl = notificationEl.element("recipients");
		
		if(idEl == null || typeEl == null || recipientsEl == null){
			return;
		}
		
		String idValue = idEl.getTextTrim();
		String typeValue = typeEl.getTextTrim();
		
		@SuppressWarnings("unchecked")
		List<Element> recipients = recipientsEl.elements("jid");
		
		
		MessageRouter messageRouter = XMPPServer.getInstance().getMessageRouter();
		
		Message generatedMessage = new Message();
		generatedMessage.setType(Type.headline);
		generatedMessage.setFrom(fromJid);
		
		Element generatedNotification = generatedMessage.addChildElement("usernotification", MUCHelper.NS_MESSAGE_NOTIFICATION);
		generatedNotification.addAttribute("id", idValue);
		generatedNotification.addAttribute("type", typeValue);
		
		for(Element recipientEl : recipients){
			
			try {
				
				String recipientValue = recipientEl.getTextTrim();
				if(recipientValue == null || recipientValue.equals("")){
					continue;
				}
				
				JID jid = new JID(recipientValue);
				if(jid != null){
					
					Message newMessage = generatedMessage.createCopy();
					newMessage.setTo(jid);
					
					messageRouter.route(newMessage);
				}
			}
			catch(Exception e){
				// Ignore
			}
		}
	}
	
	private void sendCubeNotification(Element notificationEl)
	{
		Element idEl = notificationEl.element("id");
		Element typeEl = notificationEl.element("type");
		Element silentEl = notificationEl.element("silent");
		Element dataEl = notificationEl.element("data");
		Element recipientsEl = notificationEl.element("recipients");
		
		if(typeEl == null || dataEl == null || recipientsEl == null){
			return;
		}
		
		String typeValue = typeEl.getTextTrim();
		String idValue = idEl.getTextTrim();
		String dataValue = dataEl.getTextTrim();
		boolean isSilent = silentEl != null;
		
		@SuppressWarnings("unchecked")
		List<Element> recipients = recipientsEl.elements("jid");
		
		
		MultiUserChatManager manager = XMPPServer.getInstance().getMultiUserChatManager();
		MessageRouter messageRouter = XMPPServer.getInstance().getMessageRouter();
		
		Message generatedMessage = new Message();
		generatedMessage.setType(Type.headline);
		
		Element generatedNotification = generatedMessage.addChildElement("cubenotification", MUCHelper.NS_MESSAGE_NOTIFICATION);
		generatedNotification.addAttribute("id", idValue);
		generatedNotification.addAttribute("type", typeValue);
		
		if(isSilent){
			generatedNotification.addAttribute("silent", "silent");
		}
		
		generatedNotification.setText(dataValue);
		
		ArrayList<JID> recipientJids = new ArrayList<JID>();
		
		for(Element recipientEl : recipients){
			
			try {
				
				String recipientValue = recipientEl.getTextTrim();
				if(recipientValue == null || recipientValue.equals("")){
					continue;
				}
				
				JID jid = new JID(recipientValue);
				if(jid != null){
					
					jid = jid.asBareJID();
					
					MultiUserChatService service = manager.getMultiUserChatService(jid);
					if(service == null){
						continue;
					}
					
					MUCRoom room = service.getChatRoom(jid.getNode());
					if(room == null){
						continue;
					}
					
					// Send message in cube
					Message newMessage = generatedMessage.createCopy();
					newMessage.setTo(jid);
					newMessage.setFrom(jid);
					
					room.send(newMessage);
					
					// Let the listeners know
					MUCEventDispatcher.messageReceived(jid, jid, "", newMessage);
					
					
					// Send notification to users
					if(!isSilent){
						
						Collection<JID> roomMembers = room.getMembers();
						for(JID member : roomMembers){
							
							Message userMessage = generatedMessage.createCopy();
							userMessage.setFrom(fromJid);
							userMessage.setTo(member);
							
							messageRouter.route(userMessage);
						}
					}
					
					recipientJids.add(jid);
				}
			}
			catch(Exception e){
				// Ignore
			}
		}
		
		// Report to room history
		if(!isSilent){
			
			roomHistoryPlugin.reportRoomNotification(Integer.valueOf(typeValue), dataValue, recipientJids);
		}
	}

	@Override
	public IQHandlerInfo getInfo() {
		
		return this.info;
	}

}
