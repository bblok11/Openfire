# $Revision$
# $Date$

INSERT INTO ofVersion (name, version) VALUES ('festcube', 1);

CREATE TABLE ofRoomChatHistory (
   id    			 		BIGINT unsigned  NOT NULL AUTO_INCREMENT,
   roomJID			 		VARCHAR(255)     NOT NULL,
   nick		         		VARCHAR(255)     NOT NULL,
   sentDate          		BIGINT           NOT NULL,
   body              		TEXT,
   
   PRIMARY KEY (`id`),
   INDEX ofRoomChatHistory_room_indx (roomJID)
);

CREATE TABLE ofRoomNotificationHistory (
   id    			 		BIGINT unsigned  NOT NULL AUTO_INCREMENT,
   sentDate          		BIGINT           NOT NULL,
   type 					INT,
   content					TEXT,
   
   PRIMARY KEY (`id`)
);

CREATE TABLE ofRoomNotificationHistoryRecipients (
   roomNotificationHistoryId 	BIGINT unsigned  NOT NULL,
   roomJID			 			VARCHAR(255)     NOT NULL,
   
   PRIMARY KEY (`roomNotificationHistoryId`,`roomJID`),
   INDEX ofRoomNotificationHistoryRecipients_room_indx (roomJID)
);

CREATE TABLE ofAwayData (
   roomJID			 		VARCHAR(255)     NOT NULL,
   nick		         		VARCHAR(255)     NOT NULL,
   missedMessages          	INT unsigned     NOT NULL,
   lastSeenDate        		BIGINT,
   
   PRIMARY KEY (`roomJID`,`nick`),
   INDEX ofAwayData_nick_indx (nick)
);

CREATE TABLE ofRoomStatus (
   roomJID			 		VARCHAR(255)     NOT NULL,
   lastMessageDate     		BIGINT           NOT NULL,
   
   PRIMARY KEY (`roomJID`)
);