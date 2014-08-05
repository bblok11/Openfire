# $Revision$
# $Date$

INSERT INTO ofVersion (name, version) VALUES ('awaydata', 0);

CREATE TABLE ofAwayData (
   roomJID			 		VARCHAR(255)     NOT NULL,
   nick		         		VARCHAR(255)     NOT NULL,
   missedMessages          	INT unsigned     NOT NULL,
   lastSeenDate        		BIGINT,
   lastMissedMessageDate	BIGINT,
   
   PRIMARY KEY (`roomJID`,`nick`),
   INDEX ofAwayData_nick_indx (nick)
);