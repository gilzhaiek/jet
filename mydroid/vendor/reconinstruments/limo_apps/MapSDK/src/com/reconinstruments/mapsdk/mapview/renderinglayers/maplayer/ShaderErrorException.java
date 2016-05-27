package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

public class ShaderErrorException extends Exception {
	
	public ShaderErrorException(String message){
		super(message);
	}
	
	public ShaderErrorException(Throwable thrown){
		super(thrown);
	}
	
	public ShaderErrorException(String message, Throwable thrown){
		super(message, thrown);
	}
	

}
