package com.reconinstruments.symptomchecker;

public enum TestTypes {
	NineAxis, Bluetooth, Wifi, Pressure, Temperature, GPS, Camera;

	public String GetName() {
		return this.name();
	}
}
