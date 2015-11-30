package com.festcube.openfire.plugin.subplugins.pushnotifications.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMobileDevice 
{
	private String username;
	private String deviceIdentifier;
	private Integer devicePlatformId;
	private String deviceModel;
	private String pushToken;
	
	public UserMobileDevice(ResultSet rs) throws SQLException
	{
		this.username = rs.getString("username");
		this.deviceIdentifier = rs.getString("deviceIdentifier");
		this.devicePlatformId = rs.getInt("devicePlatformId");
		this.deviceModel = rs.getString("deviceModel");
		this.pushToken = rs.getString("pushToken");
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
}
