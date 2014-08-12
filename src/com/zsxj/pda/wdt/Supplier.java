package com.zsxj.pda.wdt;

public class Supplier {

	public final int supplierId;
	public final String supplierName;
	
	public Supplier(int providerId, String providerName) {
		this.supplierId = providerId;
		this.supplierName = providerName;
	}
	
	public static String[] getNames(Supplier[] suppliers) {
		String[] names = new String[suppliers.length];
		for (int i = 0; i < suppliers.length; i++) {
			names[i] = suppliers[i].supplierName;	
		}
		return names;
	}
	
	public static Supplier[] mergeSuppliers(Supplier[] array1, Supplier[] array2) {
		if (null == array1) {
			return array2;
		}

		Supplier[] merged = new Supplier[array1.length + array2.length];
		for (int i = 0; i < array1.length; i++) {
			merged[i] = array1[i];
		}
		for (int i = 0; i < array2.length; i++) {
			merged[array1.length + i] = array2[i];
		}
		return merged;
	}
}
