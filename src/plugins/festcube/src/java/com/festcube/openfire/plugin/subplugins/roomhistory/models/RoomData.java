package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;


public class RoomData 
{
	private static final Log Log = CommonsLogFactory.getLog(RoomData.class);
	private static final String COUNT_MESSAGES = ""
			+ "SELECT SUM(messageCountSub) as messageCount, MAX(lastMessageOrderSub) as lastMessageOrder FROM"
			+ "("
			+ "  SELECT COUNT(id) messageCountSub, MAX(`order`) as lastMessageOrderSub"
			+ "  FROM ofRoomChatHistory "
			+ "  WHERE roomJID = ?"
			
			+ "  UNION ALL"
			
			+ "  SELECT COUNT(id) messageCountSub, MAX(`order`) as lastMessageOrderSub"
			+ "  FROM ofRoomChatMediaHistory "
			+ "  WHERE roomJID = ?"
			
			+ "  UNION ALL"
			
			+ "  SELECT COUNT(id) messageCountSub, MAX(`order`) as lastMessageOrderSub"
			+ "  FROM ofRoomNotificationHistory"
			+ "  JOIN ofRoomNotificationHistoryRecipients ON ofRoomNotificationHistory.id = ofRoomNotificationHistoryRecipients.roomNotificationHistoryId"
			+ "  WHERE ofRoomNotificationHistoryRecipients.roomJID = ?"
			+ ") a";
	
	private JID roomJID;
	private Long messageCount;
	private Queue<IRoomChatMessage> messageBuffer;
	
	private boolean messageDataLoaded = false;
	private Long lastRequest;
	private Long lastMessageOrder;
	
	
	public RoomData(JID roomJID){
		
		this.roomJID = roomJID;
		this.messageBuffer = new ConcurrentLinkedQueue<IRoomChatMessage>();
	}
	
	public void addMessage(IRoomChatMessage message){
		
		messageBuffer.add(message);
		
		if(messageDataLoaded){
			messageCount++;
		}
	}
	
	public IRoomChatMessage pollMessage(){
		
		return messageBuffer.poll();
	}
	
	public Long getMessageCount(){
		
		if(!messageDataLoaded){
			
			if(loadMessageData()){
				messageDataLoaded = true;
			}
		}
		
		return messageCount;
	}
	
	public int getMessageBufferSize(){
		
		return messageBuffer.size();
	}
	
	public boolean messageBufferIsEmpty(){
		
		return messageBuffer.isEmpty();
	}
	
	public boolean isFirstMessage(IRoomChatMessage message){
		
		return message.getOrder().longValue() == 0;
	}
	
	public IRoomChatMessage getOldestMessageInBuffer(){
		
		return messageBuffer.peek();
	}
	
	public Queue<IRoomChatMessage> getMessageBuffer(){
		
		return messageBuffer;
	}
	
	public JID getRoomJID(){
		
		return roomJID;
	}
	
	public Date getLastRequestDate(){
		
		if(lastRequest == null){
			return null;
		}
		
		return new Date(lastRequest);
	}
	
	public void updateLastRequest(){
		
		lastRequest = new Date().getTime();
	}
	
	public Long getLastMessageOrder(){
		
		if(!messageDataLoaded){
			
			if(loadMessageData()){
				messageDataLoaded = true;
			}
		}
		
		return lastMessageOrder;
	}
	
	public Long consumeNextMessageOrder(){
		
		Long lastOrder = getLastMessageOrder();
		Long newOrder = lastOrder != null ? new Long(lastOrder.longValue()+1) : new Long(0);
		
		lastMessageOrder = newOrder;
		return newOrder;
	}
	
	
	private boolean loadMessageData(){
		
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Long msgCount = null;
		Long lastMsgOrder = null;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			pstmt = con.prepareStatement(COUNT_MESSAGES);
			pstmt.setString(1, roomJID.toBareJID());
			pstmt.setString(2, roomJID.toBareJID());
			pstmt.setString(3, roomJID.toBareJID());

			rs = pstmt.executeQuery();
			
			while(rs.next()){
				
				msgCount = rs.getLong("messageCount");
				lastMsgOrder = rs.getLong("lastMessageOrder");
			}
		} 
		catch (SQLException sqle) {
			Log.error("Error loading message data", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		if(msgCount == null || lastMsgOrder == null){
			return false;
		}
		
		lastMessageOrder = msgCount > 0 ? lastMsgOrder : null;
		messageCount = msgCount + messageBuffer.size();
		
		return true;
	}
}
