package com.festcube.openfire.plugin.subplugins.pushnotifications;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.user.UserNameManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.festcube.openfire.plugin.subplugins.pushnotifications.models.UserMobileDevice;
import com.relayrides.pushy.apns.ApnsEnvironment;
import com.relayrides.pushy.apns.PushManager;
import com.relayrides.pushy.apns.PushManagerConfiguration;
import com.relayrides.pushy.apns.util.ApnsPayloadBuilder;
import com.relayrides.pushy.apns.util.SSLContextUtil;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import com.relayrides.pushy.apns.util.TokenUtil;

public class PushNotificationManager 
{
	private ArchiveManager archiveManager;
	final PushManager<SimpleApnsPushNotification> pushManager;
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);
	
	public PushNotificationManager(ArchiveManager archiveManager, String certificatePath, String certificatePassword, boolean debug) throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		this.archiveManager = archiveManager;
		
		pushManager = new PushManager<SimpleApnsPushNotification>(
						debug ? ApnsEnvironment.getSandboxEnvironment() : ApnsEnvironment.getProductionEnvironment(),
						SSLContextUtil.createDefaultSSLContext(certificatePath, certificatePassword),
						null, // Optional: custom event loop group
						null, // Optional: custom ExecutorService for calling listeners
						null, // Optional: custom BlockingQueue implementation
						new PushManagerConfiguration(),
			        	"FestcubePushManager");

		pushManager.start();
	}
	
	public void send(MUCRoom room, JID senderJID, Message message, ArrayList<JID> recipients) {
		
		// Build payload
		String userName = "";
		try {
			userName = UserNameManager.getUserName(senderJID, "");
		} catch (UserNotFoundException e1) {
			// Ignore
		}
		
		String body = userName + " in " + room.getDescription() + ":\n" + message.getBody();
		
		final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
		payloadBuilder.setAlertBody(body);
		payloadBuilder.setBadgeNumber(1);
		payloadBuilder.setSoundFileName(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME);
		
		final String payload = payloadBuilder.buildWithDefaultMaximumLength();
		
		for(JID recipient : recipients){
			
			ArrayList<UserMobileDevice> devices = archiveManager.getDevicesByUsername(recipient.getNode());
			
			for(UserMobileDevice device : devices){
			
				if(device.getPushToken() == null){
					continue;
				}
				
				try {
					
					byte[] token = TokenUtil.tokenStringToByteArray(device.getPushToken());
					pushManager.getQueue().put(new SimpleApnsPushNotification(token, payload));
				}
				catch(Exception e){
					
					Log.error("Unable to send notification to " + device.getPushToken(), e);
				}
			}
		}
	}
	
	public void destroy(){
		
		try {
			pushManager.shutdown();
		} catch (InterruptedException e) {
			// Ignore
		}
	}
}
