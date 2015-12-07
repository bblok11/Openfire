package com.festcube.openfire.plugin.subplugins.pushnotifications.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class UserMobileDevice 
{
	public static final Integer PLATFORM_IOS = 1;
	
	private String username;
	private String deviceIdentifier;
	private Integer devicePlatformId;
	private String deviceModel;
	private String pushToken;
	
	private Date creationDate;
	private Date modificationDate;
	
	public UserMobileDevice(ResultSet rs) throws SQLException
	{
		this.username = rs.getString("username");
		this.deviceIdentifier = rs.getString("deviceIdentifier");
		this.devicePlatformId = rs.getInt("devicePlatformId");
		this.deviceModel = rs.getString("deviceModel");
		this.pushToken = rs.getString("pushToken");
		
		this.creationDate = new Date(Long.valueOf(rs.getString("creationDate")));
		this.modificationDate = new Date(Long.valueOf(rs.getString("modificationDate")));
	}
	
	public String getUsername() {
		return username;
	}

	public String getDeviceIdentifier() {
		return deviceIdentifier;
	}

	public Integer getDevicePlatformId() {
		return devicePlatformId;
	}

	public String getDeviceModel() {
		return deviceModel;
	}

	public String getPushToken() {
		return pushToken;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public Date getModificationDate() {
		return modificationDate;
	}
}
