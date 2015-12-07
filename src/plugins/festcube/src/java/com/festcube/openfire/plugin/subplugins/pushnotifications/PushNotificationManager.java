package com.festcube.openfire.plugin.subplugins.pushnotifications;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.user.UserNameManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.log.util.CommonsLogFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.festcube.openfire.plugin.subplugins.pushnotifications.models.UserMobileDevice;
import com.relayrides.pushy.apns.ApnsEnvironment;
import com.relayrides.pushy.apns.ExpiredToken;
import com.relayrides.pushy.apns.ExpiredTokenListener;
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
	private TaskEngine taskEngine;
	
	private TimerTask expiredTokensTask;
	
	private static final Log Log = CommonsLogFactory.getLog(ArchiveManager.class);
	private static final long EXPIRED_TOKENS_CLEANUP_INTERVAL = JiveConstants.HOUR;
	
	
	public PushNotificationManager(TaskEngine taskEngine, ArchiveManager archiveManager, String certificatePath, String certificatePassword, boolean debug) throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		this.taskEngine = taskEngine;
		this.archiveManager = archiveManager;
		
		pushManager = new PushManager<SimpleApnsPushNotification>(
						debug ? ApnsEnvironment.getSandboxEnvironment() : ApnsEnvironment.getProductionEnvironment(),
						SSLContextUtil.createDefaultSSLContext(certificatePath, certificatePassword),
						null, // Optional: custom event loop group
						null, // Optional: custom ExecutorService for calling listeners
						null, // Optional: custom BlockingQueue implementation
						new PushManagerConfiguration(),
			        	"FestcubePushManager");
		
		pushManager.registerExpiredTokenListener(new FestcubeExpiredTokenListener());
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
			
			ArrayList<String> pushTokens = archiveManager.getPushTokensByUsername(recipient.getNode());
			
			for(String pushToken : pushTokens){
			
				try {
					
					byte[] token = TokenUtil.tokenStringToByteArray(pushToken);
					pushManager.getQueue().put(new SimpleApnsPushNotification(token, payload));
				}
				catch(Exception e){
					
					Log.error("Unable to send notification to " + pushToken, e);
				}
			}
		}
	}
	
	public void start(){
		
		pushManager.start();
		
		pushManager.requestExpiredTokens();
		
		// Schedule expired tokens task
		expiredTokensTask = new TimerTask() {
			@Override
			public void run() {
				pushManager.requestExpiredTokens();
			}
		};
		
		taskEngine.scheduleAtFixedRate(expiredTokensTask, EXPIRED_TOKENS_CLEANUP_INTERVAL, EXPIRED_TOKENS_CLEANUP_INTERVAL);
	}
	
	public void destroy(){
		
		if(expiredTokensTask != null){
			
			expiredTokensTask.cancel();
			expiredTokensTask = null;
		}
		
		try {
			pushManager.shutdown();
		} catch (InterruptedException e) {
			// Ignore
		}
	}
	
	
	private class FestcubeExpiredTokenListener implements ExpiredTokenListener<SimpleApnsPushNotification>
	{
		@Override
	    public void handleExpiredTokens(
	            final PushManager<? extends SimpleApnsPushNotification> pushManager,
	            final Collection<ExpiredToken> expiredTokens) {
			
			Log.info("Push expired tokens: " + expiredTokens.size());

	        for (final ExpiredToken expiredToken : expiredTokens) {
	            
	        	String tokenString = TokenUtil.tokenBytesToString(expiredToken.getToken());
	        	UserMobileDevice device = archiveManager.getDeviceByToken(UserMobileDevice.PLATFORM_IOS, tokenString);
	        	
	        	Log.info("Expired token " + tokenString + " got device " + (device != null ? device.getModificationDate().toString() : "no"));
	        	
	        	if(device == null){
	        		continue;
	        	}
	        	
	        	if(expiredToken.getExpiration().after(device.getModificationDate())){
	        		
	        		Log.info("Deleting expired token " + tokenString);
	        		
	        		// Remove
	        		archiveManager.deleteDevice(device.getUsername(), device.getDeviceIdentifier(), device.getDevicePlatformId());
	        	}
	        }
	    }
	}
}
