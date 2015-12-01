package com.festcube.openfire.plugin.subplugins.pushnotifications.handlers;

import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.IQ;

import com.festcube.openfire.plugin.MUCHelper;
import com.festcube.openfire.plugin.subplugins.pushnotifications.ArchiveManager;

public class IQUpdateDeviceHandler extends IQHandler {
	
	private final IQHandlerInfo info;
	private ArchiveManager archiveManager;
	
	private static final Log Log = CommonsLogFactory.getLog(IQUpdateDeviceHandler.class);
	
	
	public IQUpdateDeviceHandler(ArchiveManager archiveManager) {
		
		super("Push notifications update");
		
		Log.info("INIT Update");
		
		this.info = new IQHandlerInfo("updatedevice", MUCHelper.NS_IQ_MOBILE_DEVICE_UPDATE);
		this.archiveManager = archiveManager;
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		if (!packet.getType().equals(IQ.Type.set)) {
			return null;
		}
		
		Element updateEl = packet.getChildElement();
		
		String identifier = updateEl.elementTextTrim("identifier");
		String platformIdRaw = updateEl.elementTextTrim("platformId");
		String model = updateEl.elementTextTrim("model");
		String pushToken = updateEl.elementTextTrim("pushToken");
		
		if(identifier == null || identifier.equals("") || platformIdRaw == null || platformIdRaw.equals("")){
			throw new UnauthorizedException("No identifier or platform specified");
		}
		
		Integer platformId = Integer.valueOf(platformIdRaw);
		if(platformId.intValue() != 1){
			throw new UnauthorizedException("Unsupported platform");
		}
		
		Connection dbConnection = archiveManager.getConnection();
		if(dbConnection == null){
			throw new UnauthorizedException();
		}
		
		String username = packet.getFrom().getNode();
		
		Log.info("Updating device " + identifier + " form user " + username);
		archiveManager.updateDevice(username, identifier, platformId, model, pushToken);
		
		DbConnectionManager.closeConnection(dbConnection);
		
		return IQ.createResultIQ(packet);
	}

	@Override
	public IQHandlerInfo getInfo() {
		
		return this.info;
	}
}