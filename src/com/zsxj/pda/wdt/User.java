package com.zsxj.pda.wdt;

public class User {

	public final int userId;
	public final String userNo;
	public final String userName;
	
	public User(int userId, String userNo, String userName) {
		super();
		this.userId = userId;
		this.userNo = userNo;
		this.userName = userName;
	}
	
	@Override
	public String toString() {
		
		return userName;
	}
}
