package com.reconinstruments.chrono;

class Lap {
	
	private int lapNumber;
	private String time;
	private boolean selected;
	
	public Lap(int lapNum, String lapTime) {
		lapNumber = lapNum;
		time = lapTime;
		selected = false;
	}
	
	public int getLapNumber() {
		return lapNumber;
	}
	
	public String getLapTime() {
		return time;
	}
	
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}