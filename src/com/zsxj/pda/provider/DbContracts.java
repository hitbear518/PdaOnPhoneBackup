package com.zsxj.pda.provider;

import com.zsxj.pda.provider.ProviderContract.CashSaleGoods;
import com.zsxj.pda.provider.ProviderContract.FastInExamineGoodsInfo;
import com.zsxj.pda.provider.ProviderContract.PdInfo;
import com.zsxj.pda.provider.ProviderContract.Positions;
import com.zsxj.pda.provider.ProviderContract.TradeGoods;

public class DbContracts {
	static final int DATABASE_VERSION = 5;
	
	private static final String PRIMARY_KEY = " primary key";
	private static final String NOT_NULL = " not null";
	private static final String TEXT_TYPE = " TEXT";
	private static final String INT_TYPE = " INTEGER";
	private static final String REAL_TYPE = " REAL";
	private static final String COMMA_SEP = ", ";

	public static final class PdInfoTable {
		
		public static final String TABLE_NAME = "pd_info_table";
		
		static final String SQL_CREATE_TABLE = 
				"CREATE TABLE " + TABLE_NAME + " ("
				+ PdInfo._ID + INT_TYPE + PRIMARY_KEY + COMMA_SEP
				+ PdInfo.COLUMN_NAME_PD_ID + INT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_REC_ID + INT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_SPEC_ID + INT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_SPEC_BARCODE + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_GOODS_NUM + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_GOODS_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_BARCODE + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_SPEC_CODE + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_SPEC_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_POSITION_ID + INT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_POSITION_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_STOCK_OLD + REAL_TYPE + NOT_NULL + COMMA_SEP
				+ PdInfo.COLUMN_NAME_STOCK_PD + REAL_TYPE + COMMA_SEP
				+ PdInfo.COLUMN_NAME_REMARK + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ "UNIQUE (" + PdInfo.COLUMN_NAME_PD_ID + COMMA_SEP 
				+ PdInfo.COLUMN_NAME_REC_ID + ")"+ ");";
				
		static final String SQL_DELETE_TABLE = 
				"DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static final class PositionTable {
		
		public static final String TABLE_NAME = "position_table";
		
		public static final String SQL_CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " ("
				+ Positions._ID + INT_TYPE + PRIMARY_KEY + COMMA_SEP
				+ Positions.COLUMN_NAME_POSITION_ID + INT_TYPE + NOT_NULL + COMMA_SEP 
				+ Positions.COLUMN_NAME_POSITION_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ Positions.COLUMN_NAME_WAREHOUSE_ID + INT_TYPE + NOT_NULL + COMMA_SEP 
				+ "UNIQUE (" + Positions.COLUMN_NAME_POSITION_ID + COMMA_SEP 
				+ Positions.COLUMN_NAME_WAREHOUSE_ID + "));";
		
		static final String SQL_DELETE_TABLE = 
				"DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static final class TempPositionTable {
		
		public static final String TABLE_NAME = "temp_position_table";
		
		public static final String SQL_CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " ("
				+ Positions._ID + INT_TYPE + PRIMARY_KEY + COMMA_SEP
				+ Positions.COLUMN_NAME_POSITION_ID + INT_TYPE + NOT_NULL + COMMA_SEP 
				+ Positions.COLUMN_NAME_POSITION_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP
				+ Positions.COLUMN_NAME_WAREHOUSE_ID + INT_TYPE + NOT_NULL + COMMA_SEP 
				+ "UNIQUE (" + Positions.COLUMN_NAME_POSITION_ID + COMMA_SEP 
				+ Positions.COLUMN_NAME_WAREHOUSE_ID + "));";
		
		static final String SQL_DELETE_TABLE = 
				"DROP TABLE IF EXISTS " + TABLE_NAME;
		
		public static final String SQL_COPY_DATA = 
				"INSERT INTO " + PositionTable.TABLE_NAME + " ("
				+ Positions.COLUMN_NAME_POSITION_ID + COMMA_SEP 
				+ Positions.COLUMN_NAME_POSITION_NAME + COMMA_SEP
				+ Positions.COLUMN_NAME_WAREHOUSE_ID + ") SELECT "
				+ Positions.COLUMN_NAME_POSITION_ID + COMMA_SEP
				+ Positions.COLUMN_NAME_POSITION_NAME + COMMA_SEP
				+ Positions.COLUMN_NAME_WAREHOUSE_ID
				+ " FROM " + TABLE_NAME + " WHERE "
				+ Positions.COLUMN_NAME_WAREHOUSE_ID + "=";
	}
	
	public static final class FastInExamineGoodsInfoTable {
		
		public static final String TABLE_NAME = "fast_examine_goods_info_table";
		
		public static final String SQL_CREATE_TABLE = 
			"CREATE TABLE " + TABLE_NAME + " (" +
			FastInExamineGoodsInfo._ID + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID + INT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_BARCODE + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NUM + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_GOODS_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_CODE + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_SPEC_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_COUNT + INT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_UNIT_PRICE + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			FastInExamineGoodsInfo.COLUMN_NAME_DISCOUNT + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			"UNIQUE (" + FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID + "));";
		
		static final String SQL_DELETE_TABLE = 
			"DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static final class TradeGoodsTable {
		
		public static final String TABLE_NAME = "trade_goods_table";
		
		public static final String SQL_CREATE_TABLE =
			"CREATE TABLE " + TABLE_NAME + " (" +
			TradeGoods._ID + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
			TradeGoods.COLUMN_NAME_REC_ID + INT_TYPE + " UNIQUE " + COMMA_SEP +
			TradeGoods.COLUMN_NAME_BARCODE + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			TradeGoods.COLUMN_NAME_GOODS_NO + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			TradeGoods.COLUMN_NAME_GOODS_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			TradeGoods.COLUMN_NAME_SPEC_CODE + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			TradeGoods.COLUMN_NAME_SPEC_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			TradeGoods.COLUMN_NAME_POSITION_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			TradeGoods.COLUMN_NAME_SELL_COUNT + REAL_TYPE + NOT_NULL + COMMA_SEP +
			TradeGoods.COLUMN_NAME_COUNT_CHECK + REAL_TYPE + NOT_NULL + COMMA_SEP +
			TradeGoods.COLUMN_NAME_EDITABLE + INT_TYPE + NOT_NULL + ");";
		
		static final String SQL_DELETE_TABLE = 
			"DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static final class CashSaleGoodsTable {
		
		public static final String TABLE_NAME = "cash_sale_goods_table";
		
		public static final String SQL_CREATE_TABLE = 
			"CREATE TABLE " + TABLE_NAME + " (" +
			CashSaleGoods._ID + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_SPEC_ID + INT_TYPE + NOT_NULL + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_SPEC_BARCODE + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_GOODS_NUM + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_GOODS_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_SPEC_CODE + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_SPEC_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_CASH_SALE_STOCK + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_COUNT + INT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_RETAILE_PRICE + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_WHOLESALE_PRICE + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_MEMBER_PRICE + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_PURCHASE_PRICE + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_PRICE_1 + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_PRICE_2 + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_PRICE_3 + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_DISCOUNT + TEXT_TYPE + NOT_NULL + COMMA_SEP + 
			CashSaleGoods.COLUMN_NAME_WAREHOUSE_ID + INT_TYPE + NOT_NULL + COMMA_SEP +
			CashSaleGoods.COLUMN_NAME_BARCODE + TEXT_TYPE + NOT_NULL + COMMA_SEP +
			"UNIQUE (" + CashSaleGoods.COLUMN_NAME_SPEC_ID + COMMA_SEP + CashSaleGoods.COLUMN_NAME_WAREHOUSE_ID + "));";
		
		static final String SQL_DELETE_TABLE = 
			"DROP TABLE IF EXISTS " + TABLE_NAME;
	}
}
