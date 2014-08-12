package com.zsxj.pda.wdt;

public class PdEntry {

	public final int pdId;
	public final String pdNum;
	public final String createUser;
	public final String createdTime;
	
	public PdEntry(int id, String number, String creater,
			String createdTime) {
		this.pdId = id;
		this.pdNum = number;
		this.createUser = creater;
		this.createdTime = createdTime;
	}
}
