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
			+ "SELECT SUM(messageCountSub) as messageCount, MIN(firstMessageDateSub) as firstMessageDate FROM"
			+ "("
			+ "  SELECT COUNT(id) messageCountSub, MIN(sentDate) as firstMessageDateSub"
			+ "  FROM ofRoomChatHistory "
			+ "  WHERE roomJID = ?"
			
			+ "  UNION ALL"
			
			+ "  SELECT COUNT(id) messageCountSub, MIN(sentDate) as firstMessageDateSub"
			+ "  FROM ofRoomNotificationHistory"
			+ "  JOIN ofRoomNotificationHistoryRecipients ON ofRoomNotificationHistory.id = ofRoomNotificationHistoryRecipients.roomNotificationHistoryId"
			+ "  WHERE ofRoomNotificationHistoryRecipients.roomJID = ?"
			+ ") a";
	
	private JID roomJID;
	private Long messageCount;
	private Long firstMessageDate;
	private Queue<ArchivedMessage> messageBuffer;
	
	private boolean messageDataLoaded = false;
	private Long lastRequest;
	
	
	public RoomData(JID roomJID){
		
		this.roomJID = roomJID;
		this.messageBuffer = new ConcurrentLinkedQueue<ArchivedMessage>();
	}
	
	public void addMessage(ArchivedMessage message){
		
		messageBuffer.add(message);
		
		if(messageDataLoaded){
			
			messageCount++;
			
			if(firstMessageDate == null){
				firstMessageDate = message.getSentDate().getTime();
			}
		}
	}
	
	public ArchivedMessage pollMessage(){
		
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
	
	public boolean isFirstMessage(ArchivedMessage message){
		
		if(!messageDataLoaded){
			
			if(loadMessageData()){
				messageDataLoaded = true;
			}
		}
		
		if(firstMessageDate == null){
			return true;
		}
		
		return message.getSentDate().getTime() == firstMessageDate.longValue();
	}
	
	public ArchivedMessage getOldestMessageInBuffer(){
		
		return messageBuffer.peek();
	}
	
	public Queue<ArchivedMessage> getMessageBuffer(){
		
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
	
	
	private boolean loadMessageData(){
		
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Long msgCount = null;
		Long firstMsgDate = null;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			pstmt = con.prepareStatement(COUNT_MESSAGES);
			pstmt.setString(1, roomJID.toBareJID());
			pstmt.setString(2, roomJID.toBareJID());

			rs = pstmt.executeQuery();
			
			while(rs.next()){
				
				msgCount = rs.getLong("messageCount");
				firstMsgDate = rs.getLong("firstMessageDate");
			}
		} 
		catch (SQLException sqle) {
			Log.error("Error loading message data", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		if(msgCount == null || firstMsgDate == null){
			return false;
		}
		
		
		messageCount = msgCount + messageBuffer.size();
		
		if(firstMsgDate > 0){
			firstMessageDate = firstMsgDate;
		}
		else if(messageBuffer.size() > 0){
			firstMessageDate = messageBuffer.peek().getSentDate().getTime();
		}
		
		return true;
	}
}
