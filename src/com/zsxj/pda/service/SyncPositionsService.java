package com.zsxj.pda.service;

import java.text.NumberFormat;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

import com.zsxj.pda.provider.DbContracts.PositionTable;
import com.zsxj.pda.provider.DbContracts.TempPositionTable;
import com.zsxj.pda.provider.DbHelper;
import com.zsxj.pda.provider.ProviderContract.Positions;
import com.zsxj.pda.util.ConstParams.Actions;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.wdt.Position;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pdaonphone.R;

public class SyncPositionsService extends IntentService implements
		QueryCallBack {

	public static final int PAGE_SIZE = 50;

	private DbHelper mDbHelper;
	private int mWarehouseId;
	private int mOffset;
	private int mPositionCount;
	
	public SyncPositionsService() {
		super("SyncPositionsService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mDbHelper = new DbHelper(this, Globals.getDbName());
		SharedPreferences prefs = 
				getSharedPreferences(Globals.getUserPrefsName(), MODE_PRIVATE);
		mWarehouseId = prefs.getInt(PrefKeys.WAREHOUSE_ID, -1);
		clearTempTable();
		mOffset = 0;
		WDTQuery.getinstance().queryPositionCount(this, this, mWarehouseId);
	}
	
	private void clearTempTable() {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		String whereClause = 
				Positions.COLUMN_NAME_WAREHOUSE_ID + "=?";
		String[] whereArgs = {
				mWarehouseId + ""
		};
		int n = db.delete(TempPositionTable.TABLE_NAME, whereClause, whereArgs);
		Log.d("clear", "deleted " + n + " records");
	}

	@Override
	public void onQuerySuccess(Object qr) {
		if (null == qr) {
			Log.e("sync", "没有数据");
			reportError("没有数据");
			return;
		}
		
		if (Integer.class.isInstance(qr)) {
			mPositionCount = (Integer) qr;
			if (-1 == mPositionCount) {
				Log.e("sync", "查询货位数量错误");
				reportError("查询货位数量错误");
				return;
			} else {
				WDTQuery.getinstance().queryPositions(this, this, mWarehouseId, mOffset, PAGE_SIZE);
			}
		} else if (Position[].class.isInstance(qr)) {
			Position[] positions = (Position[]) qr;
			insertTemp(positions);
			mOffset += positions.length;
			reportPercent();
			if (mOffset < mPositionCount) {
				WDTQuery.getinstance().queryPositions(this, this, mWarehouseId, mOffset, PAGE_SIZE);
			} else {
				copyData();
				reportCompletion();
			}
		} else {
			throw new ClassCastException("Wrong query result type");
		}
	}
	
	private void insertTemp(Position[] positions) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			for(int i = 0;  i < positions.length; i++) {
				ContentValues values = new ContentValues();
				values.put(Positions.COLUMN_NAME_POSITION_ID, positions[i].positionId);
				values.put(Positions.COLUMN_NAME_POSITION_NAME, positions[i].positionName);
				values.put(Positions.COLUMN_NAME_WAREHOUSE_ID, mWarehouseId);
				db.insertOrThrow(TempPositionTable.TABLE_NAME, null, values);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	private void reportPercent() {
		Double percentDouble = mOffset / (double) mPositionCount;
		NumberFormat nf = NumberFormat.getPercentInstance();
		nf.setMinimumFractionDigits(2);
		String percent = "下载：" + nf.format(percentDouble);
		
		Intent response = new Intent(Actions.POSITION_SYNC_RESPONSE_PERCENT_ACTION);
		response.putExtra(Extras.POSITION_SYNC_RESPONSE_PERCENT, percent);
		LocalBroadcastManager.getInstance(this).sendBroadcast(response);
	}
	
	private void reportCompletion() {
		Intent response = new Intent(Actions.POSITION_SYNC_RESPONSE_COMPLETION_ACTION);
		Time now = new Time();
		now.setToNow();
		String fTime = now.format("%Y-%m-%d %H:%M:%S");
		SharedPreferences.Editor editor = 
				getSharedPreferences(Globals.getWarehousePrefsName(mWarehouseId), MODE_PRIVATE).edit();
		editor.putString(PrefKeys.SYNC_POSITION_TIME, fTime);
		editor.commit();
		response.putExtra(Extras.POSITION_SYNC_RESPONSE_TIME, fTime);
		LocalBroadcastManager.getInstance(this).sendBroadcast(response);
	}
	
	private void reportError(String errorMsg) {
		Intent interrupt = new Intent(Actions.POSITION_SYNC_RESPONSE_INTERRUPT_ACTION);
		interrupt.putExtra(Extras.SYNC_ERROR_MSG, errorMsg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(interrupt);
	}
	
	private void copyData() {
		Intent reportIntent = new Intent(Actions.POSITION_SYNC_RESPONSE_PERCENT_ACTION);
		reportIntent.putExtra(Extras.POSITION_SYNC_RESPONSE_PERCENT, "存储临时数据");
		LocalBroadcastManager.getInstance(this).sendBroadcast(reportIntent);
		
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		String whereClause = 
				Positions.COLUMN_NAME_WAREHOUSE_ID + "=?";
		String[] whereArgs = {
				mWarehouseId + ""
		};
		
		db.delete(PositionTable.TABLE_NAME, whereClause, whereArgs);
		db.execSQL(TempPositionTable.SQL_COPY_DATA + mWarehouseId);
		mDbHelper.close();
	}

	@Override
	public void onQueryFail(int type, WDTException wdtEx) {
		if (null == wdtEx) {
			switch (type) {
			case HandlerCases.NO_CONN:
				Log.e("sync", "无连接");
				reportError(getString(R.string.no_conn));
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Log.e("sync", "查询准备失败");
				reportError(getString(R.string.query_positions_fail));
				break;
			default:
				break;
			}
			return;
		}
		switch (wdtEx.getStatus()) {
		case 1064:
			reportError(getString(R.string.query_positions_fail));
			break;
		default:
			break;
		}
	}
}
