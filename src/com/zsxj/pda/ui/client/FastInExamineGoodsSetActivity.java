package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.zsxj.pda.provider.ProviderContract.FastInExamineGoodsInfo;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.Supplier;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTQuery;
import com.zsxj.pda.wdt.WDTQuery.QueryCallBack;
import com.zsxj.pda.wdt.Warehouse;
import com.zsxj.pdaonphone.R;

public class FastInExamineGoodsSetActivity extends Activity implements QueryCallBack {
	
	private static final int PAGE_SIZE = 75;
	
	private RelativeLayout mProgressLayout;
	private Spinner mSupplierSp;
	private Spinner mInWarehouseSp;
	private Spinner mDoPriceSp;
	private Button mOkBtn;
	
	private Supplier[] mSuppliers;
	private Supplier[] mTempSuppliers;
	private Warehouse[] mWarehouses;
	private int mWhichSupplier;
	private int mWhichWarehouse;
	private int mWhichPrice;
	
	private int mSupplierCount;
	private int mSupplierPos;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_QUERY_ERROR:
				Log.e("FastInExamineGoodsActivity", 
						getString(R.string.prepare_query_error));
				Util.toast(getApplicationContext(), R.string.prepare_query_error);
				break;
			case HandlerCases.GET_COUNT:
				WDTQuery.getinstance().querySuppliers(FastInExamineGoodsSetActivity.this, 
						FastInExamineGoodsSetActivity.this, mSupplierPos, PAGE_SIZE);
				break;
			case HandlerCases.GET_COUNT_ERROR:
				Util.toast(getApplicationContext(), "get count error");
				break;
			case HandlerCases.QUERY_FAIL:
				Util.toast(getApplicationContext(), R.string.query_suppliers_fail);
				break;
			case HandlerCases.EMPTY_RESULT:
				Util.toast(getApplicationContext(), "empty result ");
				break;
			case HandlerCases.QUERY_SUCCESS:
				onQuerySuppliersSuccess(); 
				break;
			case HandlerCases.QUERY_2_FAIL:
				Util.toast(getApplicationContext(), R.string.query_warehouse_fail);
				break;
			case HandlerCases.QUERY_2_SUCCESS:
				onQueryWarehousesSuccess();
				break;
			default:
				break;
			}
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fast_in_examine_goods_set_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.fast_in_examine_goods);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
		mSupplierSp = (Spinner) findViewById(R.id.supplier_sp);
		mInWarehouseSp = (Spinner) findViewById(R.id.in_warehouse_sp);
		mDoPriceSp = (Spinner) findViewById(R.id.do_price_sp);
		mOkBtn = (Button) findViewById(R.id.ok_btn);
		
		Globals.setWhichSupplier(-1);
		Globals.setWhichWarehouse(-1);
		Globals.setWhichPrice(-1);
		
		mTempSuppliers = null;
		mWarehouses = null;
		
		mSupplierSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichSupplier = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		mWhichSupplier = -1 != Globals.getWhichPrice() ? 
			Globals.getWhichSupplier() : 0;
			
		mInWarehouseSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int positioin, long id) {
				mWhichWarehouse = positioin;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		mWhichWarehouse = -1 != Globals.getWhichWarehouse() ?
			Globals.getWhichWarehouse() : 0;
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
			R.layout.simple_spinner_dropdown_item, 
			getResources().getStringArray(R.array.prices));
		mDoPriceSp.setAdapter(adapter);
		mWhichPrice = -1 != Globals.getWhichPrice() ? Globals.getWhichPrice() : 0;
		mDoPriceSp.setSelection(mWhichPrice);
		
		mDoPriceSp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichPrice = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		mSupplierPos = 0;
		mSuppliers = null;
		WDTQuery.getinstance().querySupplierCount(this, this);
		
		mOkBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Globals.setWhichSupplier(mWhichSupplier);
				Globals.setWhichWarehouse(mWhichWarehouse);
				Globals.setModuleUseWarehouseId(mWarehouses[mWhichWarehouse].warehouseId);
				Globals.setWhichPrice(mWhichPrice);
				Globals.setSupplierId(mTempSuppliers[mWhichSupplier].supplierId);
				Intent intent = new Intent(getBaseContext(), 
					FastInExamineGoodsListActivity.class);
				intent.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_FAST_IN_EXAMINE_GOODS);
				startActivity(intent);
			}
		});
	}
	
	private void onQuerySuppliersSuccess() {
		mSuppliers = Supplier.mergeSuppliers(mSuppliers, mTempSuppliers);
		mSupplierPos += mTempSuppliers.length;
		if (mSupplierPos < mSupplierCount) {
			WDTQuery.getinstance().querySuppliers(this, this, mSupplierPos, PAGE_SIZE);
		} else {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				R.layout.simple_spinner_dropdown_item, Supplier.getNames(mSuppliers));
			mSupplierSp.setAdapter(adapter);
			mSupplierSp.setSelection(mWhichSupplier);
			
			WDTQuery.getinstance().queryWarehouses(this, this);
		}
	}
	
	private void onQueryWarehousesSuccess() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
			R.layout.simple_spinner_dropdown_item, Warehouse.getNames(mWarehouses));
		mInWarehouseSp.setAdapter(adapter);
		mInWarehouseSp.setSelection(mWhichWarehouse);
		
		mProgressLayout.setVisibility(View.GONE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onQuerySuccess(Object qr) {
		if (null == qr) {
			mHandler.sendEmptyMessage(HandlerCases.EMPTY_RESULT);
			return;
		}
		
		if (Integer.class.isInstance(qr)) {
			Integer count = (Integer) qr;
			mSupplierCount = count;
			mHandler.sendEmptyMessage(HandlerCases.GET_COUNT);
		}else if (Supplier[].class.isInstance(qr)) {
			mTempSuppliers = (Supplier[]) qr;
			mHandler.sendEmptyMessage(HandlerCases.QUERY_SUCCESS);
		} else if (Warehouse[].class.isInstance(qr)) {
			mWarehouses = (Warehouse[]) qr;
			mHandler.sendEmptyMessage(HandlerCases.QUERY_2_SUCCESS);
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
		getContentResolver().delete(FastInExamineGoodsInfo.CONTENT_URI, null, null);
	}
}
