package com.zsxj.pda.ui.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;

import com.zsxj.pda.provider.ProviderContract.PdInfo;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.Account;
import com.zsxj.pda.wdt.Spec;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pda.wdt.WDTUpdate;
import com.zsxj.pda.wdt.WDTUpdate.UpdateCallBack;
import com.zsxj.pdaonphone.R;

public class PdDetailsActivity extends ListActivity implements QueryCallBack, 
	UpdateCallBack {

	private Spec[] mSpecs;
	private SimpleCursorAdapter mAdapter;
	private RelativeLayout mProgressLayout;
	private ProgressBar mLoadingPb;
	
	private int mPdId;
	private int mAllItems;
	private boolean mKeepLoading;
	
	private ExecutorService mSingleThreadExecutor;
	
	private static final int PAGE_SIZE = 15;
	
	private static final int FILTER_ALL_ITEMS = 0;
	private static final int FILTER_COMPLETED = 1;
	private static final int FILTER_PART = 2;
	private static final int FILTER_EMPTY = 3;
	
	private String mBarcode;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				mProgressLayout.setVisibility(View.GONE);
				mLoadingPb.setVisibility(View.GONE);
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				mProgressLayout.setVisibility(View.GONE);
				mLoadingPb.setVisibility(View.GONE);
				Util.toast(getApplicationContext(), R.string.get_pdd_fail);
				break;
			case HandlerCases.QUERY_FAIL:
				mProgressLayout.setVisibility(View.GONE);
				mLoadingPb.setVisibility(View.GONE);
				Util.toast(getApplicationContext(), R.string.get_pdd_fail);
			case HandlerCases.EMPTY_RESULT:
				mProgressLayout.setVisibility(View.GONE);
				mLoadingPb.setVisibility(View.GONE);
				Util.toast(getApplicationContext(), "查询结果为空");
				break;
			case HandlerCases.GET_COUNT_ERROR:
				mProgressLayout.setVisibility(View.GONE);
				mLoadingPb.setVisibility(View.GONE);
				Util.toast(getApplicationContext(), "查询盘点单下条目数错误");
				break;
			case HandlerCases.GET_COUNT:
				mProgressLayout.setVisibility(View.GONE);
				mLoadingPb.setMax(mAllItems);
				if (0 == mAllItems) {
					Util.toast(getApplicationContext(), R.string.empty_result);
					mLoadingPb.setVisibility(View.GONE);
					break;
				}
				
				fillList();
				mLoadingPb.setVisibility(View.VISIBLE);
				mLoadingPb.setProgress(mAdapter.getCount());
				if (mAdapter.getCount() < mAllItems) {
					mKeepLoading = true;
					Util.toast(getApplicationContext(), R.string.updating_pdd);
					WDTQuery.getinstance().queryPdDetails(PdDetailsActivity.this, 
							PdDetailsActivity.this, mPdId, mAdapter.getCount(), PAGE_SIZE);
				} else {
					mKeepLoading = false;
					mLoadingPb.setVisibility(View.GONE);
				}
				break;
			case HandlerCases.QUERY_SUCCESS:
				dealData();
				break;
			case HandlerCases.INSERT_OVER:
				fillList();
				mLoadingPb.setProgress(mAdapter.getCount());
				
				if (mAdapter.getCount() >= mAllItems) {
					mLoadingPb.setVisibility(View.GONE);
					mKeepLoading = false;
				}
				
				if (mKeepLoading) {
					WDTQuery.getinstance().queryPdDetails(PdDetailsActivity.this, 
							PdDetailsActivity.this, mPdId, mAdapter.getCount(), PAGE_SIZE);
				}
				break;
			case HandlerCases.PREPARE_UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.pd_submit_fail);
				break;
			case HandlerCases.UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.pd_submit_fail);
				break;
			case HandlerCases.UPDATE_SUCCESS:
				Util.toast(getApplicationContext(), R.string.pd_submit_success);
				setResult(RESULT_OK);
				finish();
				break;
			case HandlerCases.SCAN_RESULT:
				Intent startScanAndList = 
					new Intent(getApplicationContext(), ScanAndListActivity.class);
				startScanAndList.putExtra(Extras.PD_ID, mPdId);
				startScanAndList.putExtra(Extras.BARCODE, mBarcode);
				startScanAndList.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_PD);
				startActivity(startScanAndList);
				break;
			default:
				break;
			}
		}
	};
	
	private void dealData() {
		Spec[] specs = mSpecs;
		mSpecs = null;
		mSingleThreadExecutor.execute(new InsertTask(specs));
	}
	
	private void fillList() {
		String[] projection = {
				PdInfo._ID,
				PdInfo.COLUMN_NAME_SPEC_BARCODE,
				PdInfo.COLUMN_NAME_GOODS_NAME,
				PdInfo.COLUMN_NAME_SPEC_CODE,
				PdInfo.COLUMN_NAME_SPEC_NAME,
				PdInfo.COLUMN_NAME_POSITION_NAME,
				PdInfo.COLUMN_NAME_STOCK_OLD,
				PdInfo.COLUMN_NAME_STOCK_PD
			};
		String selection = PdInfo.COLUMN_NAME_PD_ID + "=?";
		String[] selectionArgs = {
			mPdId + ""
		};
		String orderBy = PdInfo.COLUMN_NAME_POSITION_ID;
		Cursor cursor = getContentResolver().query(PdInfo.CONTENT_URI, 
				projection, selection, selectionArgs, orderBy);
		mAdapter.changeCursor(cursor);
	}
	
	private void filterList(int which) {
		String[] projection = {
				PdInfo._ID,
				PdInfo.COLUMN_NAME_SPEC_BARCODE,
				PdInfo.COLUMN_NAME_GOODS_NAME,
				PdInfo.COLUMN_NAME_SPEC_CODE,
				PdInfo.COLUMN_NAME_SPEC_NAME,
				PdInfo.COLUMN_NAME_POSITION_NAME,
				PdInfo.COLUMN_NAME_STOCK_OLD,
				PdInfo.COLUMN_NAME_STOCK_PD
			};
		String selection = null;
		String[] selectionArgs = null;
		switch (which) {
		case FILTER_ALL_ITEMS:
			selection = PdInfo.COLUMN_NAME_PD_ID + "=?";
			selectionArgs = new String[] {
				mPdId + ""
			};
			break;
		case FILTER_COMPLETED:
			selection = PdInfo.COLUMN_NAME_PD_ID + "=? AND "
				+ PdInfo.COLUMN_NAME_STOCK_PD + "=" + PdInfo.COLUMN_NAME_STOCK_OLD;
			selectionArgs = new String[] {
				mPdId + ""
			};
			break;
		case FILTER_PART:
			selection = PdInfo.COLUMN_NAME_PD_ID + "=? AND "
				+ PdInfo.COLUMN_NAME_STOCK_PD + " IS NOT NULL AND "
				+ PdInfo.COLUMN_NAME_STOCK_PD + "<>" + PdInfo.COLUMN_NAME_STOCK_OLD;
			selectionArgs = new String[] {
				mPdId + ""
			};
			break;
		case FILTER_EMPTY:
			selection = PdInfo.COLUMN_NAME_PD_ID + "=? AND "
				+ PdInfo.COLUMN_NAME_STOCK_PD + " IS NULL";
			selectionArgs = new String[] {
				mPdId + "",
			};
			break;
		default:
			break;
		}
		String orderBy = PdInfo.COLUMN_NAME_POSITION_ID;
		Cursor cursor = getContentResolver().query(PdInfo.CONTENT_URI, 
				projection, selection, selectionArgs, orderBy);
		mAdapter.changeCursor(cursor);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pd_details_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.pd_entering);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mPdId = getIntent().getIntExtra(Extras.PD_ID, -1);
		
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		mLoadingPb = (ProgressBar) findViewById(R.id.loading_pb);
		mLoadingPb.setIndeterminate(false);

		getListView().setFastScrollEnabled(true);
		String[] from = {
			PdInfo.COLUMN_NAME_SPEC_BARCODE,
			PdInfo.COLUMN_NAME_GOODS_NAME,
			PdInfo.COLUMN_NAME_SPEC_CODE,
			PdInfo.COLUMN_NAME_SPEC_NAME,
			PdInfo.COLUMN_NAME_POSITION_NAME,
			PdInfo.COLUMN_NAME_STOCK_OLD,
			PdInfo.COLUMN_NAME_STOCK_PD
		};
		int[] to = {
			R.id.spec_barcode_tv,
			R.id.goods_name_tv,
			R.id.spec_code_tv,
			R.id.spec_name_tv,
			R.id.position_name_tv,
			R.id.stock_old_tv,
			R.id.stock_pd_tv
		};
		mAdapter = new SimpleCursorAdapter(this, R.layout.pd_details_list_item, 
				null, from, to, 0);
		setListAdapter(mAdapter);
		
		mSingleThreadExecutor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.pd_details_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_filter:
			if (mAllItems < 0) {
				Util.toast(getApplicationContext(), "查询盘点单下条目数错误");
				break;
			}
			if (mKeepLoading) {
				Util.toast(getApplicationContext(), "请等待数据载入完成");
				break;
			}
			new AlertDialog.Builder(this)
				.setTitle(R.string.filter)
				.setItems(R.array.filters, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						filterList(which);
					}
				})
				.setNegativeButton(android.R.string.cancel, null).show();
			break;
		case R.id.action_submit:
			if (mAllItems < 0) {
				Util.toast(getApplicationContext(), "查询盘点单下条目数错误");
				break;
			}
			if (mKeepLoading) {
				Util.toast(getApplicationContext(), "请等待数据载入完成");
				break;
			}
			new AlertDialog.Builder(this)
				.setTitle(R.string.select_account)
				.setItems(Account.getAccountNames(Globals.getAccounts()), 
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								SharedPreferences.Editor editor = 
										getSharedPreferences(
												Globals.getUserPrefsName(), 
												Context.MODE_PRIVATE).edit();
								editor.putInt(PrefKeys.WHICH_ACCOUNT, 
										which);
								editor.commit();
								submit();
							}
						}).show();
			break;
		case R.id.action_update_pdd:
			updatePdd();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void updatePdd() {
		String where = PdInfo.COLUMN_NAME_PD_ID + "=?";
		String[] selectionArgs = {
				mPdId + ""
		};
		getContentResolver().delete(PdInfo.CONTENT_URI, where, selectionArgs);
		WDTQuery.getinstance().queryPdDetailCount(this, this, mPdId);
	}
	
	private void submit() {
		int userId = Globals.getUserId();
		SharedPreferences prefs = getSharedPreferences(
				Globals.getUserPrefsName(), Context.MODE_PRIVATE);
		int whichAccount = prefs.getInt(PrefKeys.WHICH_ACCOUNT, -1);
		int accountId = Globals.getAccounts()[whichAccount].accountId;
		
		List<String> infoStrList = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		
		String selection = PdInfo.COLUMN_NAME_PD_ID + "=?";
		String[] selectionArgs = {
				mPdId + ""
		};
		Cursor c = getContentResolver().query(PdInfo.CONTENT_URI, null, 
				selection, selectionArgs, null);
		while (c.moveToNext()) {
			sb.append(c.getInt(c.getColumnIndex(PdInfo.COLUMN_NAME_REC_ID)));
			sb.append(",");
			sb.append(c.getInt(c.getColumnIndex(PdInfo.COLUMN_NAME_POSITION_ID)));
			sb.append(",");
			String stockPd = c.getString(c.getColumnIndex(
					PdInfo.COLUMN_NAME_STOCK_PD));
			if (TextUtils.isEmpty(stockPd)) {
				stockPd = "-1";
			}
			sb.append(stockPd).append(",");
			sb.append(c.getString(c.getColumnIndex(
					PdInfo.COLUMN_NAME_REMARK)));
			if (sb.length() > 3900) {
				infoStrList.add(sb.toString());
				sb = new StringBuilder();
			} else {
				sb.append(",");
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
			infoStrList.add(sb.toString());
		}
		new WDTUpdate().pdSubmit(userId, mPdId, accountId, infoStrList, this);
	}

	@Override
	public void onQuerySuccess(Object qr) {
		if (null == qr) {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
			return;
		}
		
		if (Integer.class.isInstance(qr)) {
			Integer count = (Integer) qr;
			mAllItems = count;
			if (mAllItems < -1) {
				mHandler.sendEmptyMessage(HandlerCases.GET_COUNT_ERROR);
			} else {
				mHandler.sendEmptyMessage(HandlerCases.GET_COUNT);
			}
		} else if (Spec[].class.isInstance(qr)) {
			mSpecs = (Spec[]) qr;
			mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);
		} else {
			throw new ClassCastException("Wrong query result type");
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
	protected void onDestroy() {          
	    super.onDestroy();
	    if (mAdapter != null && mAdapter.getCursor() != null) {    
	        mAdapter.getCursor().close();    
	    }
	}  
	
	private class InsertTask implements Runnable {
		
		private Spec[] mInnerSpecs;
		
		public InsertTask(Spec[] specs) {
			mInnerSpecs = specs;
		}
		
		@Override
		public void run() {
			ContentValues[] values = new ContentValues[mInnerSpecs.length];
			for (int i = 0; i < mInnerSpecs.length; i++) {
				values[i] = new ContentValues();
				values[i].put(PdInfo.COLUMN_NAME_PD_ID, mPdId);
				values[i].put(PdInfo.COLUMN_NAME_REC_ID, mInnerSpecs[i].recId);
				values[i].put(PdInfo.COLUMN_NAME_SPEC_ID, mInnerSpecs[i].specId);
				values[i].put(PdInfo.COLUMN_NAME_SPEC_BARCODE, mInnerSpecs[i].specBarcode);
				values[i].put(PdInfo.COLUMN_NAME_GOODS_NUM, mInnerSpecs[i].goodsNum);
				values[i].put(PdInfo.COLUMN_NAME_GOODS_NAME, mInnerSpecs[i].goodsName);
				values[i].put(PdInfo.COLUMN_NAME_BARCODE, mInnerSpecs[i].barcode);
				values[i].put(PdInfo.COLUMN_NAME_SPEC_CODE, mInnerSpecs[i].specCode);
				values[i].put(PdInfo.COLUMN_NAME_SPEC_NAME, mInnerSpecs[i].specName);
				values[i].put(PdInfo.COLUMN_NAME_POSITION_ID, mInnerSpecs[i].positionId);
				values[i].put(PdInfo.COLUMN_NAME_POSITION_NAME, mInnerSpecs[i].positionName);
				values[i].put(PdInfo.COLUMN_NAME_STOCK_OLD, 
						Double.parseDouble(mInnerSpecs[i].stockOld));
				values[i].put(PdInfo.COLUMN_NAME_REMARK, mInnerSpecs[i].remark);
			}
			getContentResolver().bulkInsert(PdInfo.CONTENT_URI, values);
			mHandler.sendEmptyMessage(HandlerCases.INSERT_OVER);
		}
		
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