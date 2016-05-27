package com.reconinstruments.jetbleantswitchertester;

/**
 * 
 * This is an interface for any class to implement that listens to radio changes
 * 
 * @author patrickcho
 * @since 2014.03.04
 * @version 1
 * 
 */
public interface OnRadioSwitchListener {
	
	/**
	 * callback for the change of radio 
	 * @param isBLE
	 */
	void onChange(boolean isBLE);
}
