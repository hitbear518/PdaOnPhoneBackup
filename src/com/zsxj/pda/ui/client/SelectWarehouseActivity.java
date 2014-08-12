package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pda.wdt.Warehouse;
import com.zsxj.pdaonphone.R;

public class SelectWarehouseActivity extends Activity implements QueryCallBack {
	
	private RelativeLayout mProgressLayout;
	private ListView mWarehouseLv;
	private Warehouse[] mWarehouses = {};
	private MyListAdapter mAdapter;
	private WDTQuery mQuery = WDTQuery.getinstance();
	
	private int mWarehouseId;
	private boolean mResetWarehouse;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mProgressLayout.setVisibility(View.GONE);
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Log.e("SelectWarehouse", getString(R.string.prepare_query_error));
				Util.toast(getApplicationContext(), R.string.query_warehouse_fail);
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), R.string.query_warehouse_fail);
				break;
			case HandlerCases.EMPTY_RESULT:
				Log.e("SelectWarehouse", getString(R.string.empty_result));
				Util.toast(getApplicationContext(), R.string.no_warehouse);
				break;
			case HandlerCases.QUERY_SUCCESS:
				mAdapter.notifyDataSetChanged();
				break;
			default:
				break;
			}
		}
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_warehouse_activity);
		
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		
		mResetWarehouse = getIntent().getBooleanExtra(Extras.RESET_WAREHOUSE, false);
		mWarehouseId = readWarehouseId();
		if (!mResetWarehouse) {
			if (mWarehouseId != -1) {
				Intent startMain = new Intent(SelectWarehouseActivity.this, MainActivity.class);
				startMain.putExtra(Extras.WAREHOUSE_ID, mWarehouseId);
				startActivity(startMain);
				finish();
			}
		}
		
		mWarehouseLv = (ListView) findViewById(R.id.warehouses_lv);
		mAdapter = new MyListAdapter(this);
		mWarehouseLv.setAdapter(mAdapter);
		mWarehouseLv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mWarehouseId = mWarehouses[position].warehouseId;
				mAdapter.notifyDataSetChanged();
				saveWarehouseInfo(position);
				if (!mResetWarehouse) {
					Intent startMain = new Intent(SelectWarehouseActivity.this, MainActivity.class);
					startMain.putExtra(Extras.WAREHOUSE_ID, mWarehouses[position].warehouseId);
					startActivity(startMain);
				}
				finish();
			}
		});
		
		
		mQuery.queryWarehouses(this, this);
	}
	
	private int readWarehouseId() {
		SharedPreferences prefs = 
				getSharedPreferences(Globals.getUserPrefsName(), MODE_PRIVATE);
		return prefs.getInt(PrefKeys.WAREHOUSE_ID, -1);
	}
	
	private void saveWarehouseInfo(int which) {
		SharedPreferences.Editor editor = getSharedPreferences(
				Globals.getUserPrefsName(), MODE_PRIVATE).edit();
		editor.putInt(PrefKeys.WAREHOUSE_ID, mWarehouses[which].warehouseId);
		editor.putString(PrefKeys.WAREHOUSE_NAME, mWarehouses[which].warehouseName);
		editor.commit();
	}
	
	private class MyListAdapter extends BaseAdapter {
		
		private final Context mContext;

		public MyListAdapter(Context context) {
			mContext = context;
		}
		
		@Override
		public int getCount() {
			return null == mWarehouses?0:mWarehouses.length;  
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {			
			convertView = LayoutInflater.from(mContext).inflate(R.layout.checked_warehouse_item, parent, false);
//			setBackground(convertView, position);
			CheckedTextView warehouseTv = (CheckedTextView) convertView.findViewById(R.id.warehouse_ctv);
			warehouseTv.setText(mWarehouses[position].warehouseName);
			if (mWarehouses[position].warehouseId == mWarehouseId) {
				warehouseTv.setChecked(true);
			} else {
				warehouseTv.setChecked(false);
			}
			return convertView;
		}

//		private void setBackground(View view, int position) {
//			if (0 == position) {
//				view.setBackgroundResource(R.drawable.section_bg_1);
//			} else if (position < getCount() - 1) {
//				view.setBackgroundResource(R.drawable.section_bg_2);
//			} else {
//				view.setBackgroundResource(R.drawable.section_bg_3);
//			}
//		}
	}

	@Override
	public void onQuerySuccess(Object qr) {
				
		if (null != qr) {
			if (!Warehouse[].class.isInstance(qr)) {
				throw new ClassCastException("Wrong query result type");
			}
			
			mWarehouses = (Warehouse[])qr;
			mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);
		} else {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
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
}
