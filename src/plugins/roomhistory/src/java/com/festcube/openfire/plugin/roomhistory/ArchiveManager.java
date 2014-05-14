package com.festcube.openfire.plugin.roomhistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.roomhistory.xep0059.XmppResultSet;


public class ArchiveManager 
{
	private static final String INSERT_MESSAGE = "INSERT INTO ofRoomHistory(roomJID, nick, sentDate, body) VALUES (?,?,?,?)";
	private static final String SELECT_MESSAGES = "SELECT * FROM ofRoomHistory WHERE roomJID = ?";
	private static final String COUNT_MESSAGES = "SELECT COUNT(id) as messageCount FROM ofRoomHistory WHERE roomJID = ?";
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);

	private Queue<ArchivedMessage> messageQueue;
	
	private boolean archivingRunning = false;
	private TimerTask archiveTask;
	
	private TaskEngine taskEngine;
	
	
	public ArchiveManager(TaskEngine engine){
		
		messageQueue = new ConcurrentLinkedQueue<ArchivedMessage>();
		taskEngine = engine;
	}
	
	public void processMessage(JID sender, JID receiver, Date date, String body){
		
		ArchivedMessage message = new ArchivedMessage(sender, receiver, date, body);
		messageQueue.add(message);
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
		
		messageQueue.clear();
		messageQueue = null;
	}
	
	public ArrayList<ArchivedMessage> getArchivedMessages(JID roomJID, XmppResultSet resultSet){
		
//		ArrayList<ArchivedMessage> results = new ArrayList<ArchivedMessage>();
//		
//		return results;
		
		return fetchMessagesFromDatabase(roomJID, null, null, 0);
	}
	
	public Integer getArchivedMessagesCount(JID roomJID){
		
		return messageCountFromDatabase(roomJID);
	}
	
	
	private ArrayList<ArchivedMessage> fetchMessagesFromDatabase(JID roomJID, Date beforeDate, Date afterDate, int limit){
		
		ArrayList<ArchivedMessage> results = new ArrayList<ArchivedMessage>();
		
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			pstmt = con.prepareStatement(SELECT_MESSAGES);
			pstmt.setString(1, roomJID.toBareJID());

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
	
	private Integer messageCountFromDatabase(JID roomJID){
		
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Integer messageCount = 0;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			pstmt = con.prepareStatement(COUNT_MESSAGES);
			pstmt.setString(1, roomJID.toBareJID());

			rs = pstmt.executeQuery();
			
			while(rs.next()){
				messageCount = rs.getInt("messageCount");
			}
		} 
		catch (SQLException sqle) {
			Log.error("Error couting messages", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return messageCount;
	}
	
	
	private class ArchivingTask implements Runnable {

		public void run() {
			
			synchronized (this) {
				
				if (archivingRunning) {
					return;
				}
				archivingRunning = true;
			}
			
			if (!messageQueue.isEmpty()) {
				
				Connection con = null;
				PreparedStatement pstmt = null;
				
				try {
					
					con = DbConnectionManager.getConnection();

					pstmt = con.prepareStatement(INSERT_MESSAGE);
					ArchivedMessage message;
					int count = 0;
					
					while ((message = messageQueue.poll()) != null) {
						
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
