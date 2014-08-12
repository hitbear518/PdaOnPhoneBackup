package com.zsxj.pda.wdt;

public class Position {

	public final int positionId;
	public final String positionName;
	
	public Position(int positionId, String positionName) {
		this.positionId = positionId;
		this.positionName = positionName;
	}
	
	public static String[] getNames(Position[] positions) {
		String[] names = new String[positions.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = positions[i].positionName;
		}
		return names;
	}
}
