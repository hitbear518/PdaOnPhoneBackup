package com.zsxj.pda.ui.client;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.zsxj.pda.provider.ProviderContract.TradeGoods;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.User;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pda.wdt.WDTUpdate;
import com.zsxj.pda.wdt.WDTUpdate.UpdateCallBack;
import com.zsxj.pdaonphone.R;

public class OutExamineGoodsActivity extends ListActivity implements QueryCallBack, 
	LoaderManager.LoaderCallbacks<Cursor>, UpdateCallBack {
	
	private int mUpdateCase;
	private static final int CASE_STOCK_OUT = 0;
	private static final int CASE_PICK_ERROR = 1;
	
	private Spinner mPickerSp;
	private TextView mWorkNumTv;
	
	private User[] mPickers;
	private int mWhichPicker;
	
	private int mTradeId;
	private int mPickerId;
	private String mPostId;
	private int mWarehouseId;
	
	private boolean ready = false;
	private String mSelection = null;
	
	private SimpleCursorAdapter mAdapter;
	
	private boolean mIsScanning = false;
	private String mBarcode;
	
	private com.zsxj.pda.wdt.TradeGoods[] mTradeGoods;

	@SuppressLint("HandlerLeak")	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Util.toast(getApplicationContext(), "查询拣货员失败");
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), "查询拣货员失败");
				break;
			case HandlerCases.QUERY_2_FAIL:
				Util.toast(getApplicationContext(), "查询订单详情失败");
				break;
			case HandlerCases.EMPTY_RESULT:
				Util.toast(getApplicationContext(), "空数据");
				break;
			case HandlerCases.QUERY_SUCCESS:
				dealPickerData();
				break;
			case HandlerCases.QUERY_2_SUCCESS:
				dealTradeGoodsData();
				break;
			case HandlerCases.SCAN_RESULT:
				countCheckPlus();
				break;
			case HandlerCases.UPDATE_SUCCESS:
				updateSuccess();
				finish();
				break;
			case HandlerCases.UPDATE_FAIL:
				updateFail();
				break;
			default:
				break;
			}
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.out_examine_goods_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.out_examine_goods);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mPickerSp = (Spinner) findViewById(R.id.picker_sp);
		mWorkNumTv = (TextView) findViewById(R.id.work_num_tv);
		
		mPickerSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichPicker = position;
				mWorkNumTv.setText(mPickers[position].userNo);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		mTradeId = getIntent().getIntExtra(Extras.TRADE_ID, -1);
		mPickerId = getIntent().getIntExtra(Extras.PICKER_ID, -1);
		mPostId = getIntent().getStringExtra(Extras.POST_ID);
		mWarehouseId = getIntent().getIntExtra(Extras.WAREHOUSE_ID, -1);
		
		WDTQuery.getinstance().queryPickers(this, this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.out_examine_goods_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem stockOutAction = menu.findItem(R.id.action_stock_out);
		stockOutAction.setEnabled(ready);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.action_filter:
			new AlertDialog.Builder(this)
			.setTitle(R.string.filter)
			.setItems(R.array.filters_out_exam, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					filterList(which);
				}
			})
			.setNegativeButton(android.R.string.cancel, null).show();
			break;
		case R.id.action_stock_out:
			stockOut();
			break;
		case R.id.action_picking_error:
			new AlertDialog.Builder(OutExamineGoodsActivity.this)
				.setMessage("确定该订单拣货错？")
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mUpdateCase = CASE_PICK_ERROR;
						new WDTUpdate().pickError(OutExamineGoodsActivity.this, mTradeId);
					}
				}).show();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Cursor c = mAdapter.getCursor();
		if (c.moveToPosition(position)) {
			int editable = c.getInt(c.getColumnIndex(TradeGoods.COLUMN_NAME_EDITABLE));
			if (0 == editable) {
				return;
			}
				
			String barcode = c.getString(c.getColumnIndex(
				TradeGoods.COLUMN_NAME_BARCODE));
			String goodsNo = c.getString(c.getColumnIndex(
				TradeGoods.COLUMN_NAME_GOODS_NO));
			String goodsName = c.getString(c.getColumnIndex(
				TradeGoods.COLUMN_NAME_GOODS_NAME));
			String specCode = c.getString(c.getColumnIndex(
				TradeGoods.COLUMN_NAME_SPEC_CODE));
			String specName = c.getString(c.getColumnIndex(
				TradeGoods.COLUMN_NAME_SPEC_NAME));
			double sellCount = c.getDouble(c.getColumnIndex(
				TradeGoods.COLUMN_NAME_SELL_COUNT));
			double countCheck = c.getDouble(c.getColumnIndex(
				TradeGoods.COLUMN_NAME_COUNT_CHECK));
			Intent intent = new Intent(this, OutExamineGoodsCheckActivity.class);
			intent.putExtra(Extras._ID, id);
			intent.putExtra(Extras.BARCODE, barcode);
			intent.putExtra(Extras.GOODS_NUM, goodsNo);
			intent.putExtra(Extras.GOODS_NAME, goodsName);
			intent.putExtra(Extras.SPEC_CODE, specCode);
			intent.putExtra(Extras.SPEC_NAME, specName);
			intent.putExtra(Extras.SELL_COUNT, sellCount);
			intent.putExtra(Extras.COUNT_CHECK, countCheck);
			startActivity(intent);
		}
	}
	
	private void updateSuccess() {
		switch (mUpdateCase) {
		case CASE_STOCK_OUT:
			Util.toast(getApplicationContext(), "出库成功");
			break;
		case CASE_PICK_ERROR:
			Util.toast(getApplicationContext(), "拣货出错");
		default:
			break;
		}
	}
	
	private void updateFail() {
		switch (mUpdateCase) {
		case CASE_STOCK_OUT:
			Util.toast(getApplicationContext(), "出库失败");
			break;
		case CASE_PICK_ERROR:
			Util.toast(getApplicationContext(), "提交拣货出错信息失败");
		default:
			break;
		}
	}
	
	private void dealPickerData() {
		ArrayAdapter<User> adapter = new ArrayAdapter<User>(this, 
			R.layout.simple_spinner_item, 
			mPickers);
		mPickerSp.setAdapter(adapter);
		
		for (int i = 0; i < mPickers.length; i++) {
			if (mPickerId == mPickers[i].userId) {
				mWhichPicker = i;
				mPickerSp.setSelection(mWhichPicker);
				break;
			}
		}
		
		getContentResolver().delete(TradeGoods.CONTENT_URI, null, null);
		WDTQuery.getinstance().queryTradeGoods(this, this, mTradeId, mWarehouseId);
	}
	
	private void dealTradeGoodsData() {
		ContentValues[] valuesArr = new ContentValues[mTradeGoods.length];
		for (int i = 0; i < mTradeGoods.length; i++) {
			valuesArr[i] = new ContentValues();
			valuesArr[i].put(TradeGoods.COLUMN_NAME_REC_ID, mTradeGoods[i].recId);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_BARCODE, mTradeGoods[i].barcode);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_GOODS_NO, mTradeGoods[i].goodNo);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_GOODS_NAME, mTradeGoods[i].goodName);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_SPEC_CODE, mTradeGoods[i].specCode);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_SPEC_NAME, mTradeGoods[i].specName);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_POSITION_NAME, mTradeGoods[i].positionName);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_SELL_COUNT, 
				Double.parseDouble(mTradeGoods[i].sellCount));
			valuesArr[i].put(TradeGoods.COLUMN_NAME_COUNT_CHECK, 0);
			valuesArr[i].put(TradeGoods.COLUMN_NAME_EDITABLE, 0);
		}
		getContentResolver().bulkInsert(TradeGoods.CONTENT_URI, valuesArr);
		
		String[] from = {
			TradeGoods.COLUMN_NAME_BARCODE,
			TradeGoods.COLUMN_NAME_GOODS_NO,
			TradeGoods.COLUMN_NAME_GOODS_NAME,
			TradeGoods.COLUMN_NAME_SPEC_CODE,
			TradeGoods.COLUMN_NAME_SPEC_NAME,
			TradeGoods.COLUMN_NAME_SELL_COUNT,
			TradeGoods.COLUMN_NAME_COUNT_CHECK
		};
		int[] to = {
			R.id.barcode_tv,
			R.id.goods_num_tv,
			R.id.goods_name_tv,
			R.id.spec_code_tv,
			R.id.spec_name_tv,
			R.id.sell_count_tv,
			R.id.count_check_tv
		};
		
		mAdapter = new SimpleCursorAdapter(this, 
			R.layout.out_examine_goods_list_item, null, from, to, 0);
		setListAdapter(mAdapter);
		getListView().setVisibility(View.VISIBLE);

		getLoaderManager().initLoader(0, null, this);
	}
	
	private void countCheckPlus() {
		String selection = TradeGoods.COLUMN_NAME_BARCODE + "=?";
		String[] selectionArgs = {
			mBarcode
		};
		
		Cursor c = getContentResolver().query(TradeGoods.CONTENT_URI, null, 
			selection, selectionArgs, null);
		
		if (c.getCount() == 0) {
			c.close();
			return;
		}
			
		
		if (c.getCount() == 1) {
			if (c.moveToNext()) {
				long id =c.getLong(c.getColumnIndex(TradeGoods._ID));
				double countCheck = c.getDouble(c.getColumnIndex(
					TradeGoods.COLUMN_NAME_COUNT_CHECK));
				ContentValues values = new ContentValues();
				values.put(TradeGoods.COLUMN_NAME_COUNT_CHECK, countCheck + 1);
				values.put(TradeGoods.COLUMN_NAME_EDITABLE, 1);
				getContentResolver().update(ContentUris.withAppendedId(
					TradeGoods.CONTENT_URI, id), values, null, null);
			}	
		} else {
			final List<Long> idList = new ArrayList<Long>();
			final List<Double> countCheckList = new ArrayList<Double>();
			List<String> strList = new ArrayList<String>();
			
			while (c.moveToNext()) {
				long id = c.getLong(c.getColumnIndex(TradeGoods._ID));
				idList.add(id);
				String goodsNo = c.getString(c.getColumnIndex(
					TradeGoods.COLUMN_NAME_GOODS_NO));
				String goodsName = c.getString(c.getColumnIndex(
					TradeGoods.COLUMN_NAME_GOODS_NAME));
				String specCode = c.getString(c.getColumnIndex(
					TradeGoods.COLUMN_NAME_SPEC_CODE));
				String specName = c.getString(c.getColumnIndex(
					TradeGoods.COLUMN_NAME_SPEC_NAME));
				double sellCount = c.getDouble(c.getColumnIndex(
					TradeGoods.COLUMN_NAME_SELL_COUNT));
				double countCheck = c.getDouble(c.getColumnIndex(
					TradeGoods.COLUMN_NAME_COUNT_CHECK));
				countCheckList.add(countCheck);
				
				String str = goodsNo + "\n" + goodsName + "\n" + 
					specCode + getString(R.string.slash) + specName + "\n" +
					getString(R.string.quantity) + sellCount + "\t" +
					getString(R.string.count_check) + countCheck;
				strList.add(str);
			}
			final ContentValues values = new ContentValues();
			new AlertDialog.Builder(this).setTitle("选择货品")
				.setItems(strList.toArray(new String[strList.size()]), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						values.put(TradeGoods.COLUMN_NAME_COUNT_CHECK, countCheckList.get(which) + 1);
						values.put(TradeGoods.COLUMN_NAME_EDITABLE, 1);
						getContentResolver().update(ContentUris.withAppendedId(
							TradeGoods.CONTENT_URI, idList.get(which)), values, null, null);
					}
				}).show();
		}
		c.close();
	}
	
	private void stockOut() {
		if (!TextUtils.isEmpty(mPostId)) {
			mUpdateCase = CASE_STOCK_OUT;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("确认出库")
				.setPositiveButton(android.R.string.ok, 
					new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new WDTUpdate().stockOut(OutExamineGoodsActivity.this, 
							mTradeId, mPickers[mWhichPicker].userId, 
							Globals.getUserId());
					}
				})
				.setNegativeButton(android.R.string.cancel, null).show();
			return;
		}
		
		new AlertDialog.Builder(this)
			.setMessage("物流单号为空，确认出库？")
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mUpdateCase = CASE_STOCK_OUT;
					new WDTUpdate().stockOut(OutExamineGoodsActivity.this, 
						mTradeId, mPickers[mWhichPicker].userId, Globals.getUserId());
				}
			}).show();
		
	}
	
	private void filterList(int which) {
		switch (which) {
		case 0:
			mSelection = null;
			break;
		case 1:
			mSelection = TradeGoods.COLUMN_NAME_SELL_COUNT + "=" +
				TradeGoods.COLUMN_NAME_COUNT_CHECK;
			break;
		case 2:
			mSelection = TradeGoods.COLUMN_NAME_COUNT_CHECK + ">0 AND " +
				TradeGoods.COLUMN_NAME_COUNT_CHECK + "<>" +
				TradeGoods.COLUMN_NAME_SELL_COUNT;
			break;
		case 3:
			mSelection = TradeGoods.COLUMN_NAME_COUNT_CHECK + "=0";
		default:
			break;
		}
		getLoaderManager().restartLoader(0, null, this);
	}
	
	@Override
	public void onQuerySuccess(Object qr) {
		if (qr != null) {
			if (User[].class.isInstance(qr)) {
				castQueryResult(qr);
				mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);	
			} else if (com.zsxj.pda.wdt.TradeGoods[].class.isInstance(qr)) {
				mTradeGoods = (com.zsxj.pda.wdt.TradeGoods[]) qr;
				mHandler.sendEmptyMessage(HandlerCases.QUERY_2_SUCCESS);
			} else {
				throw new ClassCastException("Wrong query result type");
			}
		} else {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
		}
	}

	private void castQueryResult(Object qr) {
		User[] pickers = (User[]) qr;
		mPickers = new User[pickers.length + 1];
		mPickers[0] = new User(0, "", "");
		for (int i = 0; i < pickers.length; i++) {
			mPickers[i + 1] = pickers[i]; 
		}
	}
	
	@Override
	public void onQueryFail(int type, WDTException wdtEx) {
		if (null == wdtEx) {
			mHandler.sendEmptyMessage(type);
			return;
		}
		switch (wdtEx.getStatus()) {
		case 1064:
			mHandler.sendEmptyMessage(HandlerCases.QUERY_FAIL);
			break;
		default:
			break;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, TradeGoods.CONTENT_URI, null, mSelection, null,
			TradeGoods.COLUMN_NAME_REC_ID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		checkReady();
	}
	
	private void checkReady() {
		Cursor c0 = getContentResolver().query(TradeGoods.CONTENT_URI, null, 
			null, null, TradeGoods.COLUMN_NAME_REC_ID);
		String selection = TradeGoods.COLUMN_NAME_SELL_COUNT + "=" +
			TradeGoods.COLUMN_NAME_COUNT_CHECK;
		Cursor c1 = getContentResolver().query(TradeGoods.CONTENT_URI, null, 
			selection, null, null);
		if (c0.getCount() == c1.getCount()) {
			ready = true;
			stockOut();
			invalidateOptionsMenu();
		} else if (ready) {
			ready = false;
			invalidateOptionsMenu();
		}
		c0.close();
		c1.close();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onUpdateSuccess() {
		mHandler.sendEmptyMessage(HandlerCases.UPDATE_SUCCESS);
	}

	@Override
	public void onUpdateFail(int type, WDTException wdtEx) {
		if (null == wdtEx) {
			mHandler.sendEmptyMessage(type);
			return;
		}
		switch (wdtEx.getStatus()) {
		case 1064:
			mHandler.sendEmptyMessage(HandlerCases.UPDATE_FAIL);
			break;
		default:
			break;
		}
	}
}
