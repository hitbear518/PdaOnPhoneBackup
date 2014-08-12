package com.zsxj.pda.wdt;


public class Spec {
	
	public final int specId;
	public final String goodsNum;
	public final String goodsName;
	public final String barcode;
	public final String specCode;
	public final String specName;
	public final String specBarcode;
	public final int positionId;
	public final String positionName;
	public final String stock;
	
	public final int recId;
	public final String stockOld;
	public final String stockPd;
	public final String remark;
	
	public Spec(int specId, String goodsNum, String goodsName,
			String specCode, String specName, String specBarcode,
			int positionId, String positionName, String stock) {
		super();
		this.specId = specId;
		this.goodsNum = goodsNum;
		this.goodsName = goodsName;
		this.specCode = specCode;
		this.specName = specName;
		this.specBarcode = specBarcode;
		this.positionId = positionId;
		this.positionName = positionName;
		this.stock = stock;
		
		recId = -1;
		barcode = null;
		stockOld = null;
		stockPd = null;
		remark = null;
	}
	
	public Spec(int recId, int specId, String specBarcode, String goodsNum, String goodsName,
			String barcode, String specCode, String specName, int positionId, String positionName,
			String stockOld, String stockPd, String remark) {
		this.recId = recId;
		this.specId = specId;
		this.specBarcode = specBarcode;
		this.goodsNum = goodsNum;
		this.goodsName = goodsName;
		this.barcode = barcode;
		this.specCode = specCode;
		this.specName = specName;
		this.positionId = positionId;
		this.positionName = positionName;
		this.stockOld = stockOld;
		this.stockPd = stockPd;
		this.remark = remark;
		
		stock = null;
	}
}
