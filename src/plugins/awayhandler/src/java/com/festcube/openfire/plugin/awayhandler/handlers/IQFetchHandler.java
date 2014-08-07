package com.festcube.openfire.plugin.awayhandler.handlers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import com.festcube.openfire.plugin.awayhandler.ArchiveManager;
import com.festcube.openfire.plugin.awayhandler.models.AwayData;
import com.festcube.openfire.plugin.awayhandler.models.RoomStatus;

public class IQFetchHandler extends IQHandler
{
	private static final Log Log = CommonsLogFactory.getLog(IQFetchHandler.class);
	protected static final String NAMESPACE = "fc:awaydata";
	private final IQHandlerInfo info;
	
	private ArchiveManager archiveManager;

	
	public IQFetchHandler(ArchiveManager manager){
		
		super("Away Data");
		this.info = new IQHandlerInfo("awaydata", NAMESPACE);
		
		this.archiveManager = manager;
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		IQ reply = IQ.createResultIQ(packet);
		
		JID from = packet.getFrom();
		String nick = from.getNode();
		
		Element responseEl = reply.setChildElement("awaydata", NAMESPACE);
		
		HashMap<JID, AwayData> awayData = archiveManager.getAwayDataByNick(nick);
		if(awayData.size() > 0){
			
			HashMap<JID, RoomStatus> roomStatuses = archiveManager.getRoomStatusForRooms(awayData.keySet());
			
			Iterator it = awayData.entrySet().iterator();
		    while (it.hasNext()) {
		    	
		        @SuppressWarnings("unchecked")
				Map.Entry<JID, AwayData> pairs = (Map.Entry<JID, AwayData>)it.next();
		        
		        JID roomJid = pairs.getKey();
		        
		        Element room = responseEl.addElement("room");
				room.addAttribute("jid", roomJid.toBareJID());
				
				// Away data
				AwayData roomAwayData = pairs.getValue();
				
				if(roomAwayData != null){
					
					room.addElement("missedmessages").addText(String.valueOf(roomAwayData.getMissedMessages()));
					
					Element lastSeenDateEl = room.addElement("lastseendate");
					if(roomAwayData.getLastSeenDate() != null){
						lastSeenDateEl.addText(XMPPDateTimeFormat.format(roomAwayData.getLastSeenDate()));
					}
				}
				
				// Room status
				Element lastMessageDateEl = room.addElement("lastmessagedate");
				RoomStatus roomStatus = roomStatuses.get(roomJid);
				
				if(roomStatus != null){
					
					lastMessageDateEl.addText(XMPPDateTimeFormat.format(roomStatus.getLastMessageDate()));
				}
		        
		        it.remove();
		    }
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