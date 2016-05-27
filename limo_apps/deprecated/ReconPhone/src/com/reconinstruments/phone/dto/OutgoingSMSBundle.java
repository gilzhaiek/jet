package com.reconinstruments.phone.dto;

public class OutgoingSMSBundle {

    String num;
    String body;
    
    public OutgoingSMSBundle(String num, String body) {
	super();
	this.num = num;
	this.body = body;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
}
