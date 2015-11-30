package com.festcube.openfire.plugin.subplugins.pushnotifications.handlers;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;

import com.festcube.openfire.plugin.MUCHelper;
import com.festcube.openfire.plugin.subplugins.pushnotifications.ArchiveManager;

public class IQUpdateDeviceInfoHandler extends IQHandler {
	
	private final IQHandlerInfo info;
	private ArchiveManager archiveManager;
	
	public IQUpdateDeviceInfoHandler(ArchiveManager archiveManager) {
		
		super("Push notifications");
		
		this.info = new IQHandlerInfo("update", MUCHelper.NS_IQ_MOBILE_DEVICE);
		this.archiveManager = archiveManager;
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		if (!packet.getType().equals(IQ.Type.set)) {
			throw new UnauthorizedException();
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
		
		// Save room last message date
		String username = packet.getFrom().getNode();
		archiveManager.updateDeviceInfo(username, identifier, platformId, model, pushToken);
		
		DbConnectionManager.closeConnection(dbConnection);
		
		return IQ.createResultIQ(packet);
	}

	@Override
	public IQHandlerInfo getInfo() {
		
		return this.info;
	}
}