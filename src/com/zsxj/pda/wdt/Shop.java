package com.zsxj.pda.wdt;

public class Shop {

	public final int shopId;
	public final String shopName;

	public Shop(int shopId, String shopName) {
		this.shopId = shopId;
		this.shopName = shopName;
	}
	
	@Override
	public String toString() {
		return shopName;
	}
}
