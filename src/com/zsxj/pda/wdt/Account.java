package com.zsxj.pda.wdt;

public class Account {

	public final int accountId;
	public final String accountName;
	
	public Account(int accountId, String accountName) {
	
		this.accountId = accountId;
		this.accountName = accountName;
	}
	
	public static String[] getAccountNames(Account[] accounts) {
		String[] names = new String[accounts.length];
		for (int i = 0; i < accounts.length; i++) {
			names[i] = accounts[i].accountName;
		}
		return names;
	}
	
	public static String[] getNames(Account[] accounts) {
		String[] names = new String[accounts.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = accounts[i].accountName;
		}
		return names;
	}
}
