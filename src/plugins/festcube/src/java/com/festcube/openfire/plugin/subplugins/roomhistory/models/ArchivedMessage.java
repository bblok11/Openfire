package com.festcube.openfire.plugin.subplugins.roomhistory.models;

import java.util.Date;

public class ArchivedMessage 
{
    protected Long id;
    protected Date sentDate;
    
    /**
     * The date the message was sent.
     *
     * @return the date the message was sent.
     */
    public Date getSentDate() {
        return sentDate;
    }
    
    public Long getId(){
    	return id;
    }
}
