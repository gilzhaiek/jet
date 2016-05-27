package com.reconinstruments.applauncher.transcend;

/* Simple declaration of JumpEnd interface. Syper anal Java requires this to be
 * in separate file; otherwise it should be on top of MODJumpManager who implements it
 * It is just a fancy way of declaring a callback */
public interface JumpEndEvent {
	public void stopTimer();
	public void landed(ReconJump jump); 
}