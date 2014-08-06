# $Revision$
# $Date$

INSERT INTO ofVersion (name, version) VALUES ('roomhistory', 0);

CREATE TABLE ofRoomHistory (
   id    			 		BIGINT unsigned  NOT NULL AUTO_INCREMENT,
   roomJID			 		VARCHAR(255)     NOT NULL,
   nick		         		VARCHAR(255)     NOT NULL,
   sentDate          		BIGINT           NOT NULL,
   body              		TEXT,
   cubeNotificationType 	INT,
   cubeNotificationContent	TEXT,
   
   PRIMARY KEY (`id`),
   INDEX ofRoomHistory_room_indx (roomJID)
);

CREATE TABLE ofRoomStatus (
   roomJID			 		VARCHAR(255)     NOT NULL,
   lastMessageDate     		BIGINT           NOT NULL,
   
   PRIMARY KEY (`roomJID`)
);