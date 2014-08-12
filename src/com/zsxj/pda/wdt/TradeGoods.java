package com.zsxj.pda.wdt;

public class TradeGoods {
	
	public final int recId;
	public final String barcode;
	public final String goodNo;
	public final String goodName;
	public final String specCode;
	public final String specName;
	public final String sellCount;
	public final String countCheck;
	public final String positionName;
	
	public TradeGoods(int recId, String barcode, String goodNo, String goodName,
			String specCode, String specName, String positionName, String sellCount) {
		this.recId = recId;
		this.barcode = barcode;
		this.goodNo = goodNo;
		this.goodName = goodName;
		this.specCode = specCode;
		this.specName = specName;
		this.positionName = positionName;
		this.sellCount = sellCount;
		countCheck = "";
	}
}
