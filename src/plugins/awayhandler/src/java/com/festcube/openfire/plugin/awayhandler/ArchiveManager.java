package com.festcube.openfire.plugin.awayhandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;


public class ArchiveManager 
{
	private static final String INSERT_UPDATE_LAST_SEEN_1 = "INSERT INTO ofAwayData(roomJID, nick, missedMessages, lastSeenDate) VALUES ";
	private static final String INSERT_UPDATE_LAST_SEEN_VALUES = "(?,?,0,?)";
	private static final String INSERT_UPDATE_LAST_SEEN_2 = " ON DUPLICATE KEY UPDATE lastSeenDate = VALUES(lastSeenDate)";
	
	private static final String INSERT_UPDATE_MISSED_MESSAGES_1 = "INSERT INTO ofAwayData(roomJID, nick, missedMessages, lastSeenDate, lastMissedMessageDate) VALUES ";
	private static final String INSERT_UPDATE_MISSED_MESSAGES_VALUES = "(?, ?, 1, NULL, ?)";
	private static final String INSERT_UPDATE_MISSED_MESSAGES_2 = " ON DUPLICATE KEY UPDATE missedMessages = missedMessages+1, lastMissedMessageDate = VALUES(lastMissedMessageDate)";
	
	private static final String UPDATE_RESET_MISSED_MESSAGED = "UPDATE ofAwayData SET missedMessages = 0, lastMissedMessageDate = NULL WHERE roomJID = ? AND nick = ?";
	
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);
	
	
	public ArchiveManager(TaskEngine engine){

	}
	
	public boolean increaseMissedMessages(Connection con, JID roomJid, ArrayList<String> nicknames){
		
		PreparedStatement pstmt = null;
		boolean success = false;
		
		try {
			
			String query = INSERT_UPDATE_MISSED_MESSAGES_1;

			int nickCounter = 0;
			for(String nick : nicknames){
				
				if(nickCounter > 0){
					query += ",";
				}
				
				query += INSERT_UPDATE_MISSED_MESSAGES_VALUES;
				
				nickCounter++;
			}
			
			query += INSERT_UPDATE_MISSED_MESSAGES_2;
			
			pstmt = con.prepareStatement(query);
			
			long date = new Date().getTime();
			String roomBareJid = roomJid.toBareJID();
			
			int argCounter = 1;
			
			for(String nick : nicknames){
				
				pstmt.setString(argCounter, roomBareJid);
				pstmt.setString(argCounter+1, nick);
				pstmt.setLong(argCounter+2, date);
				
				argCounter += 3;
			}
			
			success = pstmt.execute();
		}
		catch (SQLException sqle) {
			
			Log.error("Error increasing missed messages", sqle);
		}
		
		return success;
	}
	
	public boolean updateLastSeenDate(Connection con, JID roomJid, ArrayList<String> nicknames){
		
		PreparedStatement pstmt = null;
		boolean success = false;
		
		try {
			
			String query = INSERT_UPDATE_LAST_SEEN_1;

			int nickCounter = 0;
			for(String nick : nicknames){
				
				if(nickCounter > 0){
					query += ",";
				}
				
				query += INSERT_UPDATE_LAST_SEEN_VALUES;
				
				nickCounter++;
			}
			
			query += INSERT_UPDATE_LAST_SEEN_2;
			
			pstmt = con.prepareStatement(query);
			
			long date = new Date().getTime();
			String roomBareJid = roomJid.toBareJID();
			
			int argCounter = 1;
			
			for(String nick : nicknames){
				
				pstmt.setString(argCounter, roomBareJid);
				pstmt.setString(argCounter+1, nick);
				pstmt.setLong(argCounter+2, date);
				
				argCounter += 3;
			}
			
			success = pstmt.execute();
		}
		catch (SQLException sqle) {
			
			Log.error("Error updating last seen date", sqle);
		} 
		
		return success;
	}
	
	public boolean resetMissedMessages(Connection con, JID roomJid, String nickname)
	{
		PreparedStatement pstmt = null;
		boolean success = false;
		
		try {
			
			pstmt = con.prepareStatement(UPDATE_RESET_MISSED_MESSAGED);
			pstmt.setString(1, roomJid.toBareJID());
			pstmt.setString(2, nickname);
			
			success = pstmt.execute();
		}
		catch (SQLException sqle) {
			
			Log.error("Error resetting missed messages", sqle);
		}
		
		return success;
	}
	
	
	public Connection getConnection(){
		
		Connection con = null;
		
		try {
			con = DbConnectionManager.getConnection();
		}
		catch(SQLException e){
			
			Log.error("Unable to get DB connection", e);
		}
		
		return con;
	}
}
