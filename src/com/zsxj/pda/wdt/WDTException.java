package com.zsxj.pda.wdt;

public class WDTException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int status;
	
	public WDTException(int status, String message) {
		super(message);
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
	
}
