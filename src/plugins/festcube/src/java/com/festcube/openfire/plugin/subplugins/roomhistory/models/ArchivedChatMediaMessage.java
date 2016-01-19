package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.xmpp.packet.JID;

public class ArchivedChatMediaMessage extends ArchivedChatMessage 
{
	public enum Type {
		IMAGE(0, "image");
		
		private int _intValue;
		private String _stringValue;
		
		Type(int intValue, String stringValue) {
	        this._intValue = intValue;
	        this._stringValue = stringValue;
	    }
		
		public int getIntValue() {
            return _intValue;
		}
		
		public String getStringValue() {
            return _stringValue;
		}
		
		public static Type fromInteger(int value) {
	        switch(value) {
	        case 0:
	            return IMAGE;
	        }
	        return null;
	    }
		
		public static Type fromString(String value) {
	        
			if(value.equals("image")){
				return IMAGE;
			}
	        return null;
	    }
	}
	
	protected Type type;
	protected String urlBase;
	protected String urlFilename;
	protected Integer mediaId;
	
	public ArchivedChatMediaMessage(JID fromJID, JID roomJID, Date sentDate, Long order, Type type, String urlBase, String urlFilename, Integer mediaId) 
	{
		super(fromJID, roomJID, sentDate, order, null);
		
		this.type = type;
		this.urlBase = urlBase;
		this.urlFilename = urlFilename;
		this.mediaId = mediaId;
	}
	
	public ArchivedChatMediaMessage(ResultSet rs, JID roomJID) throws SQLException{
    	
    	super(rs, roomJID);
    	
    	this.type = Type.fromInteger(rs.getInt("mediaTypeId"));
    	this.urlBase = rs.getString("mediaUrlBase");
    	this.urlFilename = rs.getString("mediaUrlFilename");
    	this.mediaId = rs.getInt("mediaMediaId");
    }

	public Type getType() {
		return type;
	}

	public String getUrlBase() {
		return urlBase;
	}

	public String getUrlFilename() {
		return urlFilename;
	}
	
	public Integer getMediaId() {
		return mediaId;
	}
}