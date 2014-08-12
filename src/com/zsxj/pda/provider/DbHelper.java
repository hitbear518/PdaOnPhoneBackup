package com.zsxj.pda.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.zsxj.pda.provider.DbContracts.CashSaleGoodsTable;
import com.zsxj.pda.provider.DbContracts.FastInExamineGoodsInfoTable;
import com.zsxj.pda.provider.DbContracts.PdInfoTable;
import com.zsxj.pda.provider.DbContracts.PositionTable;
import com.zsxj.pda.provider.DbContracts.TempPositionTable;
import com.zsxj.pda.provider.DbContracts.TradeGoodsTable;

/**
 * Verion 1------------------------------
 * Added:
 * 		PdInfoTable
 * 		PositionTable
 * 		TempPositionTable
 * Version 2------------------------------
 * Added:
 * 		FastInExamineGoodsInfoTable
 * 		TradeGoodsTable
 * Version 3------------------------------
 * Added:
 * 		CashSaleGoodsTable
 * Version 4-----------------------------
 * Changed:
 * 		CashSaleGoodsTable
 * Version 5-------------------------------
 * Changed:
 * 		TradeGoodsTable
 */
public class DbHelper extends SQLiteOpenHelper {

	public DbHelper(Context context, String name) {
		super(context, name, null, DbContracts.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(PdInfoTable.SQL_CREATE_TABLE);
		db.execSQL(PositionTable.SQL_CREATE_TABLE);
		db.execSQL(TempPositionTable.SQL_CREATE_TABLE);
		
		db.execSQL(FastInExamineGoodsInfoTable.SQL_CREATE_TABLE);
		db.execSQL(TradeGoodsTable.SQL_CREATE_TABLE);
		
		db.execSQL(CashSaleGoodsTable.SQL_CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		
		switch (oldVersion) {
		case 1:
			db.execSQL(FastInExamineGoodsInfoTable.SQL_CREATE_TABLE);
			db.execSQL(TradeGoodsTable.SQL_CREATE_TABLE);
		case 2:
			db.execSQL(CashSaleGoodsTable.SQL_CREATE_TABLE);
			break;
		case 3:
			// version 3 to 4 added a new column
			db.execSQL(CashSaleGoodsTable.SQL_DELETE_TABLE);
			db.execSQL(CashSaleGoodsTable.SQL_CREATE_TABLE);
		case 4:
			// new column
			db.execSQL(TradeGoodsTable.SQL_DELETE_TABLE);
			db.execSQL(TradeGoodsTable.SQL_CREATE_TABLE);
		default:
			break;
		}
	}
}
