package com.festcube.openfire.plugin.subplugins.awayhandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.subplugins.awayhandler.models.AwayData;
import com.festcube.openfire.plugin.subplugins.awayhandler.models.RoomStatus;


public class ArchiveManager 
{
	private static final String SELECT_SYNC = "SELECT * FROM ofAwayData WHERE nick = ? AND (missedMessages > 0 OR roomJID IN %s)";
	private static final String SELECT_BY_NICK = "SELECT * FROM ofAwayData WHERE nick = ?";
	
	private static final String INSERT_UPDATE_LAST_SEEN_1 = "INSERT INTO ofAwayData(roomJID, nick, missedMessages, lastSeenDate) VALUES ";
	private static final String INSERT_UPDATE_LAST_SEEN_VALUES = "(?,?,0,?)";
	private static final String INSERT_UPDATE_LAST_SEEN_2 = " ON DUPLICATE KEY UPDATE lastSeenDate = VALUES(lastSeenDate)";
	
	private static final String INSERT_UPDATE_MISSED_MESSAGES_1 = "INSERT INTO ofAwayData(roomJID, nick, missedMessages, lastSeenDate) VALUES ";
	private static final String INSERT_UPDATE_MISSED_MESSAGES_VALUES = "(?, ?, 1, NULL)";
	private static final String INSERT_UPDATE_MISSED_MESSAGES_2 = " ON DUPLICATE KEY UPDATE missedMessages = missedMessages+1";
	
	private static final String UPDATE_RESET_MISSED_MESSAGED = "UPDATE ofAwayData SET missedMessages = 0, lastSeenDate = ? WHERE roomJID = ? AND nick = ?";
	
	private static final String INSERT_UPDATE_LAST_MESSAGE_DATE_ORDER = "INSERT INTO ofRoomStatus(roomJID, lastMessageDate, lastMessageOrder) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE lastMessageDate = VALUES(lastMessageDate), lastMessageOrder = VALUES(lastMessageOrder)";
	private static final String SELECT_ROOM_STATUS = "SELECT * FROM ofRoomStatus WHERE roomJID IN ";
	
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);
	
	
	public HashMap<JID, AwayData> getAwayDataByNick(String nick)
	{
		HashMap<JID, AwayData> results = new HashMap<JID, AwayData>();
			
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(SELECT_BY_NICK);
			pstmt.setString(1, nick);

			rs = pstmt.executeQuery();

			while (rs.next()) {
				
				AwayData data = new AwayData(rs);
				results.put(data.getRoomJID(), data);
			}
			
		} 
		catch (SQLException sqle) {
			Log.error("Error selecting awaydata", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return results;
	}
	
	public HashMap<JID, AwayData> getAwayDataSync(String nick, ArrayList<JID> roomJids)
	{
		HashMap<JID, AwayData> results = new HashMap<JID, AwayData>();
			
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			String roomsQuery = "";
			if(roomJids.size() > 0){
				
				int jidCounter = 0;
				roomsQuery = "(";
				
				for(JID jid : roomJids){
					
					if(jidCounter > 0){
						roomsQuery += ",";
					}
					
					roomsQuery += "?";
					
					jidCounter++;
				}
				
				roomsQuery += ")";
			}
			else {
				roomsQuery = "(NULL)";
			}

			pstmt = con.prepareStatement(String.format(SELECT_SYNC, roomsQuery));
			pstmt.setString(1, nick);
			
			int argCounter = 2;
			for(JID jid : roomJids){
				
				pstmt.setString(argCounter, jid.toBareJID());
				argCounter++;
			}

			rs = pstmt.executeQuery();

			while (rs.next()) {
				
				AwayData data = new AwayData(rs);
				results.put(data.getRoomJID(), data);
			}
			
		} 
		catch (SQLException sqle) {
			Log.error("Error selecting awaydata", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return results;
	}
	
	
	public HashMap<JID, RoomStatus> getRoomStatusForRooms(Collection<JID> roomJIDs)
	{
		HashMap<JID, RoomStatus> results = new HashMap<JID, RoomStatus>();
			
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();
			
			String query = SELECT_ROOM_STATUS + "(";
			
			int roomCounter = 0;
			for(JID room : roomJIDs){
				
				if(roomCounter > 0){
					query += ",";
				}
				
				query += "?";
				roomCounter++;
			}
			
			query += ")";
			
			pstmt = con.prepareStatement(query);
			
			int argCounter = 1;
			for(JID room : roomJIDs){
				
				pstmt.setString(argCounter, room.toBareJID());
				argCounter++;
			}

			rs = pstmt.executeQuery();

			while (rs.next()) {
				
				RoomStatus status = new RoomStatus(rs);
				results.put(status.getRoomJID(), status);
			}
			
		} 
		catch (SQLException sqle) {
			Log.error("Error selecting roomstatus", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return results;
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
			
			String roomBareJid = roomJid.toBareJID();
			
			int argCounter = 1;
			
			for(String nick : nicknames){
				
				pstmt.setString(argCounter, roomBareJid);
				pstmt.setString(argCounter+1, nick);
				
				argCounter += 2;
			}
			
			success = pstmt.execute();
		}
		catch (SQLException sqle) {
			
			Log.error("Error increasing missed messages", sqle);
		}
		
		return success;
	}
	
	public boolean updateRoomLastMessageDateAndOrder(Connection con, JID roomJID, Date date, Long order)
	{
		PreparedStatement pstmt = null;
		
		try {
			
			pstmt = con.prepareStatement(INSERT_UPDATE_LAST_MESSAGE_DATE_ORDER);
			pstmt.setString(1, roomJID.toBareJID());
			pstmt.setLong(2, date.getTime());
			pstmt.setLong(3, order);
			
			return pstmt.execute();
		} 
		catch (SQLException sqle) {
			Log.error("Error update last message date", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
		
		return false;
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
			pstmt.setLong(1, new Date().getTime());
			pstmt.setString(2, roomJid.toBareJID());
			pstmt.setString(3, nickname);
			
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
