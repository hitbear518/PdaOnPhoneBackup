package com.zsxj.pda.wdt;

public class CashSaleSpec {

	public final int specId;
	public final String specBarcode;
	public final String goodsNum;
	public final String goodsName;
	public final String specCode;
	public final String specName;
	public final String retailPrice;
	public final String wholesalePrice;
	public final String memberPrice;
	public final String purchasePrice;
	public final String price1;
	public final String price2;
	public final String price3;
	public final String stock;
	public final int warehouseId;
	
	public CashSaleSpec(int specId, String specBarcode, String goodsNum,
			String goodsName, String specCode, String specName,
			String retailPrice, String wholePrice,
			String memberPrice, String purchasePrice,
			String price1, String price2, String price3,
			String stock, int warehouseId) {
		super();
		this.specId = specId;
		this.specBarcode = specBarcode;
		this.goodsNum = goodsNum;
		this.goodsName = goodsName;
		this.specCode = specCode;
		this.specName = specName;
		this.retailPrice = retailPrice;
		this.wholesalePrice = wholePrice;
		this.memberPrice = memberPrice;
		this.purchasePrice = purchasePrice;
		this.price1 = price1;
		this.price2 = price2;
		this.price3 = price3;
		this.stock = stock;
		this.warehouseId = warehouseId;
	}
	
	public static String[] getPrices() {
		return new String[] {
			"零售价",
			"批发价",
			"会员价",
			"标准采购价",
			"价格1",
			"价格2",
			"价格3"
		};
	}
}
