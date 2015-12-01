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

public class IQDeleteDeviceHandler extends IQHandler {

	private final IQHandlerInfo info;
	private ArchiveManager archiveManager;
	
	private static final Log Log = CommonsLogFactory.getLog(IQDeleteDeviceHandler.class);
	
	
	public IQDeleteDeviceHandler(ArchiveManager archiveManager) {
		
		super("Push notifications delete");
				
		this.info = new IQHandlerInfo("deletedevice", MUCHelper.NS_IQ_MOBILE_DEVICE_DELETE);
		this.archiveManager = archiveManager;
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		if (!packet.getType().equals(IQ.Type.set)) {
			return null;
		}
		
		Element deleteEl = packet.getChildElement();
		
		String identifier = deleteEl.elementTextTrim("identifier");
		String platformIdRaw = deleteEl.elementTextTrim("platformId");
		
		Connection dbConnection = archiveManager.getConnection();
		if(dbConnection == null){
			throw new UnauthorizedException();
		}
		
		Integer platformId = Integer.valueOf(platformIdRaw);
		if(platformId.intValue() != 1){
			throw new UnauthorizedException("Unsupported platform");
		}
		
		// Save room last message date
		String username = packet.getFrom().getNode();
		
		Log.info("Deleting device " + identifier + " for user " + username);
		archiveManager.deleteDevice(username, identifier, platformId);
		
		DbConnectionManager.closeConnection(dbConnection);
		
		return IQ.createResultIQ(packet);
	}

	@Override
	public IQHandlerInfo getInfo() {
		
		return this.info;
	}
}
