package com.zsxj.pda.wdt;

public class Logistics {

	public final int logisticId;
	public final String logisticName;
	
	public Logistics(int logisticId, String logisticName) {
		this.logisticId = logisticId;
		this.logisticName = logisticName;
	}
	
	public static String[] getNames(Logistics[] logistics) {
		String[] names = new String[logistics.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = logistics[i].logisticName;
		}
		return names;
	}
}
