package com.festcube.openfire.plugin.subplugins.roomhistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.models.CubeNotificationRecipient;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedChatMessage;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedCubeNotification;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedGlobalCubeNotification;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedMessage;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.ArchivedRecipientCubeNotification;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.IRoomChatMessage;
import com.festcube.openfire.plugin.subplugins.roomhistory.models.RoomData;
import com.festcube.openfire.plugin.xep0059.XmppResultSet;


public class ArchiveManager 
{
	private static final String INSERT_CHAT_MESSAGE = "INSERT INTO ofRoomChatHistory(roomJID, nick, sentDate, `order`, body) VALUES (?,?,?,?,?)";
	private static final String INSERT_NOTIFICATION_MESSAGE = "INSERT INTO ofRoomNotificationHistory(sentDate, type, content) VALUES (?,?,?)";
	private static final String INSERT_NOTIFICATION_RECIPIENT = "INSERT INTO ofRoomNotificationHistoryRecipients(roomNotificationHistoryId, roomJID, `order`) VALUES (?, ?, ?)";
	
	private static final String SELECT_MESSAGES = ""
			+ "("
			+ "  SELECT id, sentDate, `order`, nick, body, NULL notificationType, NULL notificationContent "
			+ "  FROM ofRoomChatHistory"
			+ "  WHERE roomJID = ?"
			+ "  %s"
			+ ")"
			+ "UNION"
			+ "("
			+ "  SELECT id, sentDate, `order`, NULL nick, NULL body, type notificationType, content notificationContent "
			+ "  FROM ofRoomNotificationHistory"
			+ "  JOIN ofRoomNotificationHistoryRecipients ON ofRoomNotificationHistory.id = ofRoomNotificationHistoryRecipients.roomNotificationHistoryId"
			+ "  WHERE ofRoomNotificationHistoryRecipients.roomJID = ?"
			+ "  %s"
			+ ")"
			+ "%s %s";
	
	private static final long CLEANUP_LIMIT = JiveConstants.MINUTE * 2;
	
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);

	private HashMap<String,RoomData> roomDataMap;
	private Queue<ArchivedGlobalCubeNotification> notificationQueue;
	
	private boolean archivingRunning = false;
	private boolean cleanRunning = false;
	
	private TimerTask archiveTask;
	private TimerTask cleanTask;
	
	private TaskEngine taskEngine;
	
	
	public ArchiveManager(TaskEngine engine){
		
		roomDataMap = new HashMap<String,RoomData>();
		notificationQueue = new ConcurrentLinkedQueue<ArchivedGlobalCubeNotification>();
		
		taskEngine = engine;
	}
	
	public void processMessage(JID sender, JID receiver, Date date, Long order, String body){
		
		ArchivedChatMessage message = new ArchivedChatMessage(sender, receiver, date, order, body);
		
		RoomData roomData = getOrCreateRoomData(receiver);
		roomData.addMessage(message);
		
		roomData.updateLastRequest();
	}
	
	public void processNotification(Date date, int type, String content, ArrayList<CubeNotificationRecipient> recipients){
		
		ArchivedGlobalCubeNotification notification = new ArchivedGlobalCubeNotification(date, type, content, recipients);
		notificationQueue.add(notification);
		
		// Add the notification to each room
		for(CubeNotificationRecipient recipient : recipients){
			
			ArchivedRecipientCubeNotification recipientNotification = notification.getRecipientNotification(recipient);
			
			RoomData roomData = getOrCreateRoomData(recipient.getJid());
			roomData.addMessage(recipientNotification);
			
			roomData.updateLastRequest();
		}
	}
	
	public void start(){
		
		archiveTask = new TimerTask() {
			@Override
			public void run() {
				new ArchivingTask().run();
			}
		};
		
		taskEngine.scheduleAtFixedRate(archiveTask, JiveConstants.MINUTE, JiveConstants.MINUTE);
		
		
		cleanTask = new TimerTask() {
			@Override
			public void run() {
				new CleanupTask().run();
			}
		};
		
		taskEngine.scheduleAtFixedRate(cleanTask, JiveConstants.SECOND * 30, JiveConstants.MINUTE * 2);
	}
	
	public void stop(){
		
		archiveTask.cancel();
		archiveTask = null;
		
		cleanTask.cancel();
		cleanTask = null;
		
		roomDataMap.clear();
		roomDataMap = null;
	}
	
	public ArrayList<IRoomChatMessage> getArchivedMessages(JID roomJID, XmppResultSet resultSet, boolean latestMode){
		
		Long beforeOrder = null;
		Long afterOrder = null;
		Long max = null;
		
		ArrayList<IRoomChatMessage> results = new ArrayList<IRoomChatMessage>();
		
		try {
			
			if(resultSet != null){
				
				beforeOrder = resultSet.getBefore() != null ? Long.valueOf(resultSet.getBefore()) : null;
				afterOrder = resultSet.getAfter() != null ? Long.valueOf(resultSet.getAfter()) : null;
				max = resultSet.getMax() != null ? resultSet.getMax() : null;
			}
		}
		catch(Exception e){
			
			Log.error("Error while parsing set: " + e.toString() + " " + ExceptionUtils.getStackTrace(e));
			// Swallow
		}
	
		RoomData roomData = getOrCreateRoomData(roomJID);
		IRoomChatMessage oldestInBuffer = roomData.getOldestMessageInBuffer();
		
		if(afterOrder != null && beforeOrder == null && latestMode == false){
			
			// Only afterOrder, no latest mode
			
			// Database
			ArrayList<IRoomChatMessage> messagesFromDb = fetchMessagesFromDatabase(roomJID, "ASC", afterOrder, null, max != null ? max : 0);
			results.addAll(messagesFromDb);
			
			// Buffer
			if(max == null || (long)results.size() < max){
				
				ArrayList<IRoomChatMessage> buffer = new ArrayList<IRoomChatMessage>(roomData.getMessageBuffer());
				for(IRoomChatMessage message : buffer){
					
					results.add(message);
					
					if(max != null && (long)results.size() >= max){
						break;
					}
				}
			}
		}
		else {
			
			// beforeOrder and afterOrder, only beforeOrder or none
			
			if(oldestInBuffer != null && ((beforeOrder == null || (beforeOrder != null && beforeOrder.compareTo(oldestInBuffer.getOrder()) > 0)))){
				
				ArrayList<IRoomChatMessage> buffer = new ArrayList<IRoomChatMessage>(roomData.getMessageBuffer());
				Collections.reverse(buffer);
				
				for(IRoomChatMessage message : buffer){
					
					if(beforeOrder != null && (message.getOrder().compareTo(beforeOrder) > 0 || message.getOrder().equals(beforeOrder))){
						continue;
					}
					if(afterOrder != null && (message.getOrder().compareTo(afterOrder) < 0 || message.getOrder().equals(afterOrder))){
						break;
					}
					
					results.add(message);
					
					if(max != null && (long)results.size() >= max){
						break;
					}
				}
			}
				
			if(max == null || (long)results.size() < max){
				
				// Get the remaining items from the database
				Long dbBefore = results.size() > 0 ? results.get(0).getOrder() : beforeOrder;
				ArrayList<IRoomChatMessage> messagesFromDb = fetchMessagesFromDatabase(roomJID, "DESC", afterOrder, dbBefore, max != null ? max - results.size() : 0);
				
				results.addAll(messagesFromDb);
			}
			
			Collections.reverse(results);
		}
		
		roomData.updateLastRequest();
		
		return results;
	}
	
	public Long getArchivedMessagesCount(JID roomJID){
		
		RoomData roomData = getOrCreateRoomData(roomJID);
		return roomData.getMessageCount();
	}
	
	
	private ArrayList<IRoomChatMessage> fetchMessagesFromDatabase(JID roomJID, String sortDirection, Long afterOrder, Long beforeOrder, long limit){
		
		ArrayList<IRoomChatMessage> results = new ArrayList<IRoomChatMessage>();
		
		if(sortDirection == null){
			sortDirection = "ASC";
		}
		
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			String sendDateCondition = "";
			
			if(afterOrder != null){
				sendDateCondition += " AND `order` > ?";
			}
			if(beforeOrder != null){
				sendDateCondition += " AND `order` < ?";
			}
			
			String orderCondition = " ORDER BY `order` " + sortDirection;
			
			String limitCondition = "";
			if(limit > 0){
				limitCondition = " LIMIT ?";
			}
			
			String query = String.format(SELECT_MESSAGES, sendDateCondition, sendDateCondition, orderCondition, limitCondition);
			
			pstmt = con.prepareStatement(query);
			
			int paramCursor = 1;
			
			// Assign params twice, for both the subqueries
			for(int i=0; i<2; i++){
			
				pstmt.setString(paramCursor, roomJID.toBareJID());
				paramCursor++;
				
				if(afterOrder != null){
					
					pstmt.setLong(paramCursor, afterOrder);
					paramCursor++;
				}
				if(beforeOrder != null){
					
					pstmt.setLong(paramCursor, beforeOrder);
					paramCursor++;
				}
			}

			if(limit > 0){
				
				pstmt.setLong(paramCursor, limit);
				paramCursor++;
			}

			rs = pstmt.executeQuery();

			while (rs.next()) {
				
				IRoomChatMessage message = null;
				
				if(rs.getInt("notificationType") > 0){
					message = new ArchivedRecipientCubeNotification(rs, roomJID);
				}
				else {
					message = new ArchivedChatMessage(rs, roomJID);
				}
				
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
	
	public RoomData getOrCreateRoomData(JID roomJID){
		
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
			
			if (!roomDataMap.isEmpty() || !notificationQueue.isEmpty()) {
				
				Connection con = null;
				
				try {
					
					con = DbConnectionManager.getConnection();
					
					// Notifications
					
					con.setAutoCommit(false);
					
					PreparedStatement pstmtNotificationMessage = con.prepareStatement(INSERT_NOTIFICATION_MESSAGE, Statement.RETURN_GENERATED_KEYS);
					PreparedStatement pstmtNotificationRecipient = con.prepareStatement(INSERT_NOTIFICATION_RECIPIENT);
					ResultSet generatedKeys;
					
					ArchivedGlobalCubeNotification notification;
					
					while ((notification = notificationQueue.poll()) != null) {
						
						try {
						
							// Notification
							pstmtNotificationMessage.setLong(1, notification.getSentDate().getTime());
							pstmtNotificationMessage.setInt(2, notification.getType());
							DbConnectionManager.setLargeTextField(pstmtNotificationMessage, 3, notification.getContent());
							
							int affectedRows = pstmtNotificationMessage.executeUpdate();
					        if (affectedRows == 0) {
					            throw new SQLException("Creating notification failed, no rows affected.");
					        }

					        generatedKeys = pstmtNotificationMessage.getGeneratedKeys();
					        if (generatedKeys.next()) {
					        	notification.setId(generatedKeys.getLong(1));
					        } else {
					            throw new SQLException("Creating notification failed, no generated key obtained.");
					        }
					        
							
							// Recipients
					        int recipientCounter = 0;
					        
							for(CubeNotificationRecipient recipient : notification.getRecipients()){
								
								pstmtNotificationRecipient.setLong(1, notification.getId());
								pstmtNotificationRecipient.setString(2, recipient.getJid().toBareJID());
								pstmtNotificationRecipient.setLong(3, recipient.getOrder());
								
								if (DbConnectionManager.isBatchUpdatesSupported()) {
									pstmtNotificationRecipient.addBatch();
								} 
								else {
									pstmtNotificationRecipient.execute();
								}
								
								// Only batch up to 500 items at a time.
								if (recipientCounter % 500 == 0 && DbConnectionManager.isBatchUpdatesSupported()) {
									pstmtNotificationRecipient.executeBatch();
								}
								
								recipientCounter++;
							}
							
							if (DbConnectionManager.isBatchUpdatesSupported()) {
								pstmtNotificationRecipient.executeBatch();
							}
						
							con.commit();
							generatedKeys.close();
						}
						catch (SQLException e ) {

				            try {
				            	
				            	Log.error("Transaction cube-notification is being rolled back", e);
				                con.rollback();
				                
				            } catch(SQLException excep) {
				            	
				            	Log.error(e.getMessage(), e);
				            }
				            
					    }
					}
					
					pstmtNotificationMessage.close();
					pstmtNotificationRecipient.close();
			        con.setAutoCommit(true);
					
					
			        // Messages
			        
					PreparedStatement pstmtMessage = null;

					try {
						
						pstmtMessage = con.prepareStatement(INSERT_CHAT_MESSAGE);
						IRoomChatMessage message;
						int messageCounter = 0;
						
						for (RoomData roomData : roomDataMap.values()) {
							
							if(roomData.messageBufferIsEmpty()){
								continue;
							}
							
							while ((message = roomData.pollMessage()) != null) {
								
								if(message instanceof ArchivedChatMessage){
									
									ArchivedChatMessage chatMessage = (ArchivedChatMessage)message;
								
									pstmtMessage.setString(1, chatMessage.getRoomJID().toBareJID());
									pstmtMessage.setString(2, chatMessage.getFromJID().getNode());
									pstmtMessage.setLong(3, chatMessage.getSentDate().getTime());
									pstmtMessage.setLong(4, chatMessage.getOrder());
									DbConnectionManager.setLargeTextField(pstmtMessage, 5, chatMessage.getBody());
									
									if (DbConnectionManager.isBatchUpdatesSupported()) {
										pstmtMessage.addBatch();
									} 
									else {
										pstmtMessage.execute();
									}
									
									// Only batch up to 500 items at a time.
									if (messageCounter % 500 == 0 && DbConnectionManager.isBatchUpdatesSupported()) {
										pstmtMessage.executeBatch();
									}
									
									messageCounter++;
								}
							}
						}
						
						if (DbConnectionManager.isBatchUpdatesSupported()) {
							pstmtMessage.executeBatch();
						}
					}
					catch (Exception e) {
						Log.error(e.getMessage(), e);
					}
					finally {
						
						if(pstmtMessage != null){
							pstmtMessage.close();
						}
					}
				} 
				catch (Exception e) {
					Log.error(e.getMessage(), e);
				} 
				finally {
					DbConnectionManager.closeConnection(con);
				}
			}
			
			archivingRunning = false;
		}
	}
	
	
	private class CleanupTask implements Runnable {

		public void run() {
			
			synchronized (this) {
				
				if (cleanRunning) {
					return;
				}
				cleanRunning = true;
			}
			
			Log.debug("Starting cleanup, room count: " + roomDataMap.size());
			
			if (!roomDataMap.isEmpty()) {
				
				Date now = new Date();
				Iterator<Entry<String,RoomData>> iter = roomDataMap.entrySet().iterator();
				
				while (iter.hasNext()) {
					
				    Entry<String,RoomData> entry = iter.next();
				    
				    String key = entry.getKey();
				    RoomData roomData = entry.getValue();
				    
				    if(now.getTime() - roomData.getLastRequestDate().getTime() >= CLEANUP_LIMIT){
						
						if(roomData.getMessageBufferSize() == 0){
							
							Log.debug("Removed roomData for " + key);
							iter.remove();
						}
					}
				}
			}
			
			Log.debug("Cleanup done, count: " + roomDataMap.size());
			
			cleanRunning = false;
		}
	}
}
