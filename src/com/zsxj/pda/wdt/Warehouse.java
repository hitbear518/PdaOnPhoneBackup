package com.zsxj.pda.wdt;

public class Warehouse {

	public final int warehouseId;
	public final String warehouseNO;
	public final String warehouseName;

	public Warehouse(int warehouseId, String warehouseNO, String warehouseName) {
		this.warehouseId = warehouseId;
		this.warehouseNO = warehouseNO;
		this.warehouseName = warehouseName;
	}
	
	public static String[] getNames(Warehouse[] warehouses) {
		String[] names = new String[warehouses.length];
		for (int i = 0; i < warehouses.length; i++) {
			names[i] = warehouses[i].warehouseName;
		}
		return names;
	}
}
