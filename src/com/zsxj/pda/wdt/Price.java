package com.zsxj.pda.wdt;

public class Price {

	public final String suppliyPrice;
	public final String retailPrice;
	public final String wholesalePrice;
	public final String memberPrice;
	public final String purchasePrice;
	
	public Price(String suppliyPrice, String retailPrice,
			String wholesalePrice, String memberPrice, String purchasePrice) {
		this.suppliyPrice = suppliyPrice;
		this.retailPrice = retailPrice;
		this.wholesalePrice = wholesalePrice;
		this.memberPrice = memberPrice;
		this.purchasePrice = purchasePrice;
	}
}
