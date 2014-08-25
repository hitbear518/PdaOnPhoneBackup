package com.zsxj.pda.wdt;

public class Logistics {

	public final int logisticId;
	public final String logisticName;
	public final String logisticCode;
	
	public Logistics(int logisticId, String logisticName, String logisticCode) {
		this.logisticId = logisticId;
		this.logisticName = logisticName;
		this.logisticCode = logisticCode;
	}
	
	public static String[] getNames(Logistics[] logistics) {
		String[] names = new String[logistics.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = logistics[i].logisticName;
		}
		return names;
	}
	
	@Override
	public String toString() {
		return logisticName;
	}
}
