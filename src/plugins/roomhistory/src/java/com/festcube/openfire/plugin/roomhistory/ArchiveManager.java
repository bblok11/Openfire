package com.festcube.openfire.plugin.roomhistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Queue;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.roomhistory.models.ArchivedMessage;
import com.festcube.openfire.plugin.roomhistory.models.RoomData;
import com.festcube.openfire.plugin.roomhistory.xep0059.XmppResultSet;


public class ArchiveManager 
{
	private static final String INSERT_MESSAGE = "INSERT INTO ofRoomHistory(roomJID, nick, sentDate, body) VALUES (?,?,?,?)";
	private static final String SELECT_MESSAGES = "SELECT * FROM ofRoomHistory WHERE roomJID = ?";
	
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);

	private HashMap<String,RoomData> roomDataMap;
	
	private boolean archivingRunning = false;
	private TimerTask archiveTask;
	
	private TaskEngine taskEngine;
	
	
	public ArchiveManager(TaskEngine engine){
		
		roomDataMap = new HashMap<String,RoomData>();
		taskEngine = engine;
	}
	
	public void processMessage(JID sender, JID receiver, Date date, String body){
		
		ArchivedMessage message = new ArchivedMessage(sender, receiver, date, body);
		
		RoomData roomData = getOrCreateRoomData(receiver);
		roomData.addMessage(message);
		
		roomData.updateLastRequest();
	}
	
	public void start(){
		
		archiveTask = new TimerTask() {
			@Override
			public void run() {
				new ArchivingTask().run();
			}
		};
		
		taskEngine.scheduleAtFixedRate(archiveTask, JiveConstants.MINUTE, JiveConstants.MINUTE);
	}
	
	public void stop(){
		
		archiveTask.cancel();
		archiveTask = null;
		
		roomDataMap.clear();
		roomDataMap = null;
	}
	
	public ArrayList<ArchivedMessage> getArchivedMessages(JID roomJID, XmppResultSet resultSet){
		
		XMPPDateTimeFormat dtFormat = new XMPPDateTimeFormat();
		
		Date beforeDate = null;
		Date afterDate = null;
		Long max = resultSet.getMax();
		
		ArrayList<ArchivedMessage> results = new ArrayList<ArchivedMessage>();
		
		try {
			beforeDate = resultSet.getBefore() != null ? dtFormat.parseString(resultSet.getBefore()) : null;
			afterDate = resultSet.getAfter() != null ? dtFormat.parseString(resultSet.getAfter()) : null;
		}
		catch(Exception e){
			// swallow
		}
		
		RoomData roomData = getOrCreateRoomData(roomJID);
		ArchivedMessage oldestInBuffer = roomData.getOldestMessageInBuffer();
		
		if(beforeDate != null && afterDate != null){
			
			
		}
		else if(beforeDate != null){
			
			ArrayList<ArchivedMessage> messagesFromBuffer = new ArrayList<ArchivedMessage>();
			
			if(beforeDate.after(oldestInBuffer.getSentDate())){
				
				Queue<ArchivedMessage> buffer = roomData.getMessageBuffer();
				for(ArchivedMessage message : buffer){
					
					if(message.getSentDate().after(beforeDate) || message.getSentDate().equals(beforeDate)){
						break;
					}
					
					messagesFromBuffer.add(message);
					
					if(max != null){
						if((long)messagesFromBuffer.size() >= max){
							break;
						}
					}
				}
			}
				
			if((long)messagesFromBuffer.size() < max){
				
				// Get the remaining items from the database
				Date dbBefore = messagesFromBuffer.size() > 0 ? messagesFromBuffer.get(0).getSentDate() : beforeDate;
				ArrayList<ArchivedMessage> messagesFromDb = fetchMessagesFromDatabase(roomJID, "DESC", null, dbBefore, max - messagesFromBuffer.size());
				Collections.reverse(messagesFromDb);
				
				results.addAll(messagesFromDb);
			}
			
			results.addAll(messagesFromBuffer);
		}
		else if(afterDate != null){
			
			
		}
		
		roomData.updateLastRequest();
		
		return results;
	}
	
	public Long getArchivedMessagesCount(JID roomJID){
		
		RoomData roomData = getOrCreateRoomData(roomJID);
		return roomData.getMessageCount();
	}
	
	public boolean isFirstMessageInRoom(ArchivedMessage message, JID roomJID){
		
		RoomData roomData = getOrCreateRoomData(roomJID);
		return roomData.isFirstMessage(message);
	}
	
	
	private ArrayList<ArchivedMessage> fetchMessagesFromDatabase(JID roomJID, String sortDirection, Date afterDate, Date beforeDate, long limit){
		
		ArrayList<ArchivedMessage> results = new ArrayList<ArchivedMessage>();
		
		if(sortDirection == null){
			sortDirection = "ASC";
		}
		
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			String query = SELECT_MESSAGES;
			
			if(afterDate != null){
				query += " AND sentDate > ?";
			}
			if(beforeDate != null){
				query += " AND sentDate < ?";
			}
			if(limit > 0){
				query += " LIMIT ?";
			}
			
			query += " ORDER BY sentDate " + sortDirection;
			
			
			int paramCursor = 1;
			
			pstmt = con.prepareStatement(query);
			pstmt.setString(1, roomJID.toBareJID());
			paramCursor++;
			
			if(afterDate != null){
				pstmt.setLong(paramCursor, afterDate.getTime());
				paramCursor++;
			}
			if(beforeDate != null){
				pstmt.setLong(paramCursor, beforeDate.getTime());
				paramCursor++;
			}
			if(limit > 0){
				pstmt.setLong(paramCursor, limit);
				paramCursor++;
			}

			rs = pstmt.executeQuery();

			while (rs.next()) {
				
				ArchivedMessage message = new ArchivedMessage(rs);
				results.add(message);
			}
			
		} 
		catch (SQLException sqle) {
			Log.error("Error selecting messages", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return results;
	}
	
	private RoomData getOrCreateRoomData(JID roomJID){
		
		String roomJidString = roomJID.toString();
		
		if(!roomDataMap.containsKey(roomJidString)){
			roomDataMap.put(roomJidString, new RoomData(roomJID));
		}
		
		return roomDataMap.get(roomJidString);
	}
	
	
	private class ArchivingTask implements Runnable {

		public void run() {
			
			synchronized (this) {
				
				if (archivingRunning) {
					return;
				}
				archivingRunning = true;
			}
			
			if (!roomDataMap.isEmpty()) {
				
				Connection con = null;
				PreparedStatement pstmt = null;
				
				try {
					
					con = DbConnectionManager.getConnection();

					pstmt = con.prepareStatement(INSERT_MESSAGE);
					ArchivedMessage message;
					int count = 0;
					
					for (RoomData roomData : roomDataMap.values()) {
						
						if(roomData.messageBufferIsEmpty()){
							continue;
						}
						
						while ((message = roomData.pollMessage()) != null) {
							
							pstmt.setString(1, message.getRoomJID().toBareJID());
							pstmt.setString(2, message.getFromJID().getNode());
							pstmt.setLong(3, message.getSentDate().getTime());
							DbConnectionManager.setLargeTextField(pstmt, 4, message.getBody());
							
							if (DbConnectionManager.isBatchUpdatesSupported()) {
								pstmt.addBatch();
							} 
							else {
								pstmt.execute();
							}
							
							// Only batch up to 500 items at a time.
							if (count % 500 == 0 && DbConnectionManager.isBatchUpdatesSupported()) {
								pstmt.executeBatch();
							}
							
							count++;
						}
					}
					
					if (DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.executeBatch();
					}

				} 
				catch (Exception e) {
					Log.error(e.getMessage(), e);
				} 
				finally {
					DbConnectionManager.closeConnection(pstmt, con);
				}
			}
			
			archivingRunning = false;
		}
	}
}
