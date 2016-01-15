package com.festcube.openfire.plugin.subplugins.pushnotifications;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;

import com.festcube.openfire.plugin.subplugins.awayhandler.models.AwayData;
import com.festcube.openfire.plugin.subplugins.pushnotifications.models.UserMobileDevice;

public class ArchiveManager
{
	private static final String INSERT_UPDATE_MOBILE_DEVICE = "INSERT INTO ofUserMobileDevices(username, deviceIdentifier, devicePlatformId, deviceModel, pushToken, locale, creationDate, modificationDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE deviceModel = VALUES(deviceModel), pushToken = VALUES(pushToken), locale = VALUES(locale), modificationDate = VALUES(modificationDate)";
	
	private static final String DELETE_MOBILE_DEVICE = "DELETE FROM ofUserMobileDevices WHERE username=? AND deviceIdentifier=? AND devicePlatformId=?";

	private static final String SELECT_MOBILE_DEVICES_FROM_USER = "SELECT * FROM ofUserMobileDevices WHERE username = ?";
	private static final String SELECT_MOBILE_DEVICE_FROM_TOKEN = "SELECT * FROM ofUserMobileDevices WHERE devicePlatformId = ? AND pushToken = ? LIMIT 1";
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);
	
	Cache<String, ArrayList<UserMobileDevice>> userDevicesCache;
	
	
	public ArchiveManager(){
		
		this.userDevicesCache = CacheFactory.createCache("User Devices");
		this.userDevicesCache.setMaxLifetime(JiveConstants.HOUR);
	}
	
	public boolean updateDevice(String username, String deviceIdentifier, Integer devicePlatformId, String deviceModel, String pushToken, String locale)
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
			pstmt.setString(6, locale);
			pstmt.setString(7, dateString);
			pstmt.setString(8, dateString);
			
			pstmt.execute();
			
			invalidateDevicesFromUsername(username);
			
			return true;
		} 
		catch (SQLException sqle) {
			Log.error("Error create/update device info", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
		
		return false;
	}
	
	public boolean deleteDevice(String username, String deviceIdentifier, Integer devicePlatformId)
	{
		PreparedStatement pstmt = null;
		Connection con = getConnection();
		
		try {
			
			pstmt = con.prepareStatement(DELETE_MOBILE_DEVICE);
			pstmt.setString(1, username);
			pstmt.setString(2, deviceIdentifier);
			pstmt.setInt(3, devicePlatformId.intValue());
			
			pstmt.execute();
			
			invalidateDevicesFromUsername(username);
			
			return true;
		} 
		catch (SQLException sqle) {
			Log.error("Error deleting device info", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
		
		return false;
	}
	
	public ArrayList<UserMobileDevice> getDevicesByUsername(String username)
	{
		if(this.userDevicesCache.containsKey(username)){
			return this.userDevicesCache.get(username);
		}
		
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
			
			this.userDevicesCache.put(username, results);
		} 
		catch (SQLException sqle) {
			Log.error("Error selecting devices", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return results;
	}
	
	public UserMobileDevice getDeviceByToken(Integer devicePlatformId, String pushToken)
	{
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			con = DbConnectionManager.getConnection();

			pstmt = con.prepareStatement(SELECT_MOBILE_DEVICE_FROM_TOKEN);
			pstmt.setInt(1, devicePlatformId);
			pstmt.setString(2, pushToken);

			rs = pstmt.executeQuery();
			rs.first();
			
			if(rs.getRow() == 0){
				return null;
			}

			return new UserMobileDevice(rs);
		} 
		catch (SQLException sqle) {
			Log.error("Error selecting device", sqle);
		} 
		finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		
		return null;
	}
	
	public void invalidateDevicesFromUsername(String username)
	{
		userDevicesCache.remove(username);
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
