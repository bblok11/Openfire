package com.festcube.openfire.plugin.subplugins.pushnotifications;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.subplugins.awayhandler.models.AwayData;
import com.festcube.openfire.plugin.subplugins.pushnotifications.models.UserMobileDevice;

public class ArchiveManager
{
	private static final String INSERT_UPDATE_MOBILE_DEVICE = "INSERT INTO ofUserMobileDevices(username, deviceIdentifier, devicePlatformId, deviceModel, pushToken, creationDate, modificationDate) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE deviceModel = VALUES(deviceModel), pushToken = VALUES(pushToken), modificationDate = VALUES(modificationDate)";
	private static final String SELECT_MOBILE_DEVICES_FROM_USER = "SELECT * FROM ofUserMobileDevices WHERE username = ?";
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);
	
	public boolean updateDeviceInfo(String username, String deviceIdentifier, Integer devicePlatformId, String deviceModel, String pushToken)
	{
		PreparedStatement pstmt = null;
		Connection con = getConnection();
		
		try {
			
			String dateString = StringUtils.dateToMillis(new Date());
			
			pstmt = con.prepareStatement(INSERT_UPDATE_MOBILE_DEVICE);
			pstmt.setString(1, username);
			pstmt.setString(2, deviceIdentifier);
			pstmt.setInt(3, devicePlatformId.intValue());
			pstmt.setString(4, deviceModel);
			pstmt.setString(5, pushToken);
			pstmt.setString(6, dateString);
			pstmt.setString(7, dateString);
			
			return pstmt.execute();
		} 
		catch (SQLException sqle) {
			Log.error("Error create/update device info", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
		
		return false;
	}
	
	public ArrayList<UserMobileDevice> getDevicesByUsername(String username)
	{
		ArrayList<UserMobileDevice> results = new ArrayList<UserMobileDevice>();
			
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(SELECT_MOBILE_DEVICES_FROM_USER);
			pstmt.setString(1, username);

			rs = pstmt.executeQuery();

			while (rs.next()) {
				
				UserMobileDevice device = new UserMobileDevice(rs);
				results.add(device);
			}
			
		} 
		catch (SQLException sqle) {
			Log.error("Error selecting devices", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return results;
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
