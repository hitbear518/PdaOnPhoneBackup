package com.zsxj.pda.wdt;

public class TradeInfo {

	public final int tradeId;
	public final int pickerId;
	public final int tradeStatus;
	public final int bStockOut;
	public final int bFreezed;
	public final int refundStatus;
	public final String postId;
	public final int warehouseId;
	
	public TradeInfo(int tradeId, int pickerId, int tradeStatus, int bStockOut,
			int bFreezed, int refundStatus, String postId, int warehouseId) {
		super();
		this.tradeId = tradeId;
		this.pickerId = pickerId;
		this.tradeStatus = tradeStatus;
		this.bStockOut = bStockOut;
		this.bFreezed = bFreezed;
		this.refundStatus = refundStatus;
		this.postId = postId;
		this.warehouseId = warehouseId;
	}
}