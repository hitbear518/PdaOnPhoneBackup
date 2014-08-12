package com.zsxj.pda.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.zsxj.pda.provider.DbContracts.CashSaleGoodsTable;
import com.zsxj.pda.provider.DbContracts.FastInExamineGoodsInfoTable;
import com.zsxj.pda.provider.DbContracts.PdInfoTable;
import com.zsxj.pda.provider.DbContracts.PositionTable;
import com.zsxj.pda.provider.DbContracts.TradeGoodsTable;
import com.zsxj.pda.provider.ProviderContract.CashSaleGoods;
import com.zsxj.pda.provider.ProviderContract.FastInExamineGoodsInfo;
import com.zsxj.pda.provider.ProviderContract.PdInfo;
import com.zsxj.pda.provider.ProviderContract.Positions;
import com.zsxj.pda.provider.ProviderContract.TradeGoods;
import com.zsxj.pda.util.Globals;

public class Provider extends ContentProvider {
	
	private DbHelper mDbHelper;
	
	private static final int CODE_PD_INFO = 0;
	private static final int CODE_PD_INFO_ID = 1;
	private static final int CODE_POSITIONS = 10;
	private static final int CODE_POSITION_ID = 11;
	private static final int CODE_FAST_EXAMINE_GOODS_INFO = 20;
	private static final int CODE_FAST_EXAMINE_GOODS_INFO_ID = 21;
	private static final int CODE_TRADE_GOODS = 30;
	private static final int CODE_TRADE_GOODS_ID = 31;
	private static final int CODE_CASH_GOODS = 40;
	private static final int CODE_CASH_GOODS_ID = 41;
	private static final UriMatcher sUriMatcher = 
			new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
				PdInfo.PATH, CODE_PD_INFO);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
				PdInfo.PATH + "/#", CODE_PD_INFO_ID);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
				Positions.PATH, CODE_POSITIONS);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
				Positions.PATH + "/#", CODE_POSITION_ID);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
			FastInExamineGoodsInfo.PATH, CODE_FAST_EXAMINE_GOODS_INFO);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
			FastInExamineGoodsInfo.PATH + "/#", CODE_FAST_EXAMINE_GOODS_INFO_ID);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
			TradeGoods.PATH, CODE_TRADE_GOODS);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
			TradeGoods.PATH + "/#", CODE_TRADE_GOODS_ID);
		sUriMatcher.addURI(ProviderContract.AUTHORITY, 
			CashSaleGoods.PATH, CODE_CASH_GOODS);
		sUriMatcher.addURI(ProviderContract.AUTHORITY,
			CashSaleGoods.PATH + "/#", CODE_CASH_GOODS_ID);
	}

	@Override
	public boolean onCreate() {
		mDbHelper = new DbHelper(getContext(), Globals.getDbName());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case CODE_PD_INFO:
			queryBuilder.setTables(PdInfoTable.TABLE_NAME);
			break;
		case CODE_PD_INFO_ID:
			queryBuilder.setTables(PdInfoTable.TABLE_NAME);
			queryBuilder.appendWhere(PdInfo._ID
					+ "=" + uri.getLastPathSegment());
			break;
		case CODE_POSITIONS:
			queryBuilder.setTables(PositionTable.TABLE_NAME);
			break;
		case CODE_POSITION_ID:
			queryBuilder.setTables(PositionTable.TABLE_NAME);
			queryBuilder.appendWhere(Positions._ID
					+ "=" + uri.getLastPathSegment());
			break;
		case CODE_FAST_EXAMINE_GOODS_INFO:
			queryBuilder.setTables(FastInExamineGoodsInfoTable.TABLE_NAME);
			break;
		case CODE_TRADE_GOODS:
			queryBuilder.setTables(TradeGoodsTable.TABLE_NAME);
			break;
		case CODE_CASH_GOODS:
			queryBuilder.setTables(CashSaleGoodsTable.TABLE_NAME);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = queryBuilder.query(db, projection, selection, 
				selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case CODE_PD_INFO:
			return PdInfo.CONTENT_TYPE;
		case CODE_PD_INFO_ID:
			return PdInfo.CONTENT_ITEM_TYPE;
		case CODE_POSITIONS:
			return Positions.CONTENT_TYPE;
		case CODE_POSITION_ID:
			return Positions.CONTENT_ITEM_TYPE;
		case CODE_FAST_EXAMINE_GOODS_INFO:
			return FastInExamineGoodsInfo.CONTENT_TYPE;
		case CODE_TRADE_GOODS:
			return TradeGoods.CONTENT_TYPE;
		case CODE_CASH_GOODS:
			return CashSaleGoods.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final int match = sUriMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		long id = 0;
		switch (match) {
		case CODE_PD_INFO:
			 id = db.insertWithOnConflict(PdInfoTable.TABLE_NAME, 
					null, values, SQLiteDatabase.CONFLICT_ROLLBACK);
			 if (-1 == id) {
				 Log.e("provider", "insert fail recId = " 
						 + values.getAsInteger(PdInfo.COLUMN_NAME_REC_ID));
			 } else {
				 Log.d("provider", "insert success recId = " 
						 + values.getAsInteger(PdInfo.COLUMN_NAME_REC_ID));
			 }
			break;
		case CODE_POSITIONS:
			id = db.insertWithOnConflict(PositionTable.TABLE_NAME, 
					null, values, SQLiteDatabase.CONFLICT_ROLLBACK);
			if (-1 == id) {
				 Log.e("provider", "insert fail positionId = " 
						 + values.getAsInteger(Positions.COLUMN_NAME_POSITION_ID)
						 + " and warehouseId = "
						 + values.getAsInteger(Positions.COLUMN_NAME_WAREHOUSE_ID));
			 } else {
				 Log.d("provider", "insert success positionId = " 
						 + values.getAsInteger(Positions.COLUMN_NAME_POSITION_ID)
						 + " and warehouseId = "
						 + values.getAsInteger(Positions.COLUMN_NAME_WAREHOUSE_ID));
			 }
			break;
		case CODE_FAST_EXAMINE_GOODS_INFO:
			id = db.insertWithOnConflict(FastInExamineGoodsInfoTable.TABLE_NAME, 
				null, values, SQLiteDatabase.CONFLICT_ROLLBACK);
			if (-1 == id) {
				Log.e("Provider", "insert fail specId = " +
					values.getAsInteger(FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID));
			} else {
				Log.i("Provider", "insert success specId = " +
					values.getAsInteger(FastInExamineGoodsInfo.COLUMN_NAME_SPEC_ID));
			}
			break;
		case CODE_TRADE_GOODS:
			id = db.insertWithOnConflict(TradeGoodsTable.TABLE_NAME, 
				null, values, SQLiteDatabase.CONFLICT_ROLLBACK);
			if (-1 == id) {
				Log.e("Provider", "insert fail recId = " + 
					values.getAsInteger(TradeGoods.COLUMN_NAME_REC_ID));
			} else {
				Log.i("Provider", "insert success recId = " + 
					values.getAsInteger(TradeGoods.COLUMN_NAME_REC_ID));
			}
			break;
		case CODE_CASH_GOODS:
			id = db.insertWithOnConflict(CashSaleGoodsTable.TABLE_NAME, 
				null, values, SQLiteDatabase.CONFLICT_ABORT);
			if (-1 == id) {
				Log.e("Provider", "insert fail specId = " +
					values.getAsInteger(CashSaleGoods.COLUMN_NAME_SPEC_ID));
			} else {
				Log.i("Provider", "insert success specId = " +
					values.getAsInteger(CashSaleGoods.COLUMN_NAME_SPEC_ID));
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.withAppendedPath(uri, id + "");
	}
	
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		final int match = sUriMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int rowsInserted = 0;
		switch (match) {
		case CODE_PD_INFO:
			db.beginTransactionNonExclusive();
			try {
				rowsInserted = values.length;
				long id = -1;
				for (int i = 0; i < rowsInserted; i++) {
					id = db.insert(PdInfoTable.TABLE_NAME, null, values[i]);
					if (-1 == id) {
						Log.e("provider", "insert fail recId = " 
								 + values[i].getAsInteger(PdInfo.COLUMN_NAME_REC_ID));
						break;
					 } else {
						 Log.d("provider", "insert success recId = " 
								 + values[i].getAsInteger(PdInfo.COLUMN_NAME_REC_ID));
					 }
				}
				if (id != -1) {
					db.setTransactionSuccessful();
					Log.d("provider", "transaction successful");
				} else {
					Log.d("provider", "transaction fail");
				}
			}  finally {
				db.endTransaction();
			}
			break;
		case CODE_POSITIONS:
			db.beginTransaction();
			try {
				rowsInserted = values.length;
				long id = -1;
				for (int i = 0; i < rowsInserted; i++) {
					id = db.insert(PositionTable.TABLE_NAME, null, values[i]);
					if (-1 == id) {
						Log.e("provider", "insert fail positionId = "
								+ values[i].getAsInteger(Positions.COLUMN_NAME_POSITION_ID)
								+ " and warehouseId = "
								+ values[i].getAsInteger(Positions.COLUMN_NAME_WAREHOUSE_ID));
						break;
					} else {
						Log.d("provider", "insert success positionId = "
								+ values[i].getAsInteger(Positions.COLUMN_NAME_POSITION_ID)
								+ " and warehouseId = "
								+ values[i].getAsInteger(Positions.COLUMN_NAME_WAREHOUSE_ID));
					}
				}
				if (id != -1) {
					db.setTransactionSuccessful();
					Log.d("provider", "transaction successful");
				} else {
					Log.d("provider", "transaction fail");
				}
			} finally {
				db.endTransaction();
			}
			break;
		case CODE_TRADE_GOODS:
			db.beginTransaction();
			try {
				rowsInserted = values.length;
				long id = -1;
				for (int i = 0; i < rowsInserted; i++) {
					id = db.insert(TradeGoodsTable.TABLE_NAME, null, values[i]);
					if (-1 == id) {
						Log.e("provider", "insert fail recId = " +
							values[i].getAsInteger(TradeGoods.COLUMN_NAME_REC_ID));
						break;
					} else {
						Log.i("Provider", "insert success recId " +
							values[i].getAsInteger(TradeGoods.COLUMN_NAME_REC_ID));
					}
				}
				if (id != -1) {
					db.setTransactionSuccessful();
					Log.i("Provider", "transaction success");
				} else {
					Log.i("Provider", "transaction fail");
				}
			} finally {
				db.endTransaction();
			}
		default:
			break;
		}
		getContext().getContentResolver().notifyChange(PdInfo.CONTENT_URI, null);
		return rowsInserted;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final int match = sUriMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int rowsDeleted = 0;
		String id;
		switch (match) {
		case CODE_PD_INFO:
			rowsDeleted = db.delete(PdInfoTable.TABLE_NAME, selection, selectionArgs);
			break;
		case CODE_PD_INFO_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = db.delete(PdInfoTable.TABLE_NAME, 
						PdInfo._ID + " = " + id, null);
			} else {
				rowsDeleted = db.delete(PdInfoTable.TABLE_NAME, 
						PdInfo._ID + " = " + id + " and " + selection, 
						selectionArgs);
			}
			break;
		case CODE_FAST_EXAMINE_GOODS_INFO:
			rowsDeleted = db.delete(FastInExamineGoodsInfoTable.TABLE_NAME, 
					selection, selectionArgs);
			break;
		case CODE_FAST_EXAMINE_GOODS_INFO_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = db.delete(FastInExamineGoodsInfoTable.TABLE_NAME, 
					FastInExamineGoodsInfo._ID + "=" + id, null);
			} else {
				rowsDeleted = db.delete(FastInExamineGoodsInfoTable.TABLE_NAME, 
					FastInExamineGoodsInfo._ID + "=" + id + " and " + selection, 
					selectionArgs);
			}
			break;
		case CODE_TRADE_GOODS:
			rowsDeleted = db.delete(TradeGoodsTable.TABLE_NAME, 
				selection, selectionArgs);
			break;
		case CODE_CASH_GOODS:
			rowsDeleted = db.delete(CashSaleGoodsTable.TABLE_NAME, 
					selection, selectionArgs);
			break;
		case CODE_CASH_GOODS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = db.delete(CashSaleGoodsTable.TABLE_NAME, 
					FastInExamineGoodsInfo._ID + "=" + id, null);
			} else {
				rowsDeleted = db.delete(CashSaleGoodsTable.TABLE_NAME, 
					FastInExamineGoodsInfo._ID + "=" + id + " and " + selection, 
					selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final int match = sUriMatcher.match(uri);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int rowsUpdated = 0;
		String id;
		switch (match) {
		case CODE_PD_INFO:
			rowsUpdated = 
				db.update(PdInfoTable.TABLE_NAME, values, selection, selectionArgs);
			break;
		case CODE_PD_INFO_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = db.update(PdInfoTable.TABLE_NAME, values, 
						PdInfo._ID + " = " + id, null);
			} else {
				rowsUpdated = db.update(PdInfoTable.TABLE_NAME, values, 
						PdInfo._ID + " = " + id + " AND " + selection, 
						selectionArgs);
			}
			break;
		case CODE_FAST_EXAMINE_GOODS_INFO:
			rowsUpdated = db.update(FastInExamineGoodsInfoTable.TABLE_NAME, 
				values, selection, selectionArgs);
			break;
		case CODE_TRADE_GOODS:
			rowsUpdated = db.update(TradeGoodsTable.TABLE_NAME, 
				values, selection, selectionArgs);
			break;
		case CODE_TRADE_GOODS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = db.update(TradeGoodsTable.TABLE_NAME, values, 
					TradeGoods._ID + "=" + id, null);
			} else {
				rowsUpdated = db.update(TradeGoodsTable.TABLE_NAME, values, 
					TradeGoods._ID + "=" + id + " AND " + selection, selectionArgs);
			}
			break;
		case CODE_CASH_GOODS:
			rowsUpdated = db.update(CashSaleGoodsTable.TABLE_NAME, 
				values, selection, selectionArgs);
			Log.i("Provider, update code cash goods", "rowsUpdated = " + rowsUpdated);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

}
