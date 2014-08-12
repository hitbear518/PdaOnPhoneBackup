package com.zsxj.pda.ui.client;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTUpdate;
import com.zsxj.pda.wdt.WDTUpdate.UpdateCallBack;
import com.zsxj.pdaonphone.R;

public class StockTransferActivity extends Activity implements UpdateCallBack{
		
	public static final int REQUEST_NEW_POSITION = 0;
	
	private int mWhichOut;
	private int mWhichIn;
	
	private int mSpecId;
	private int[] mPositionIds;
	private String[] mPositionNames;
	private String[] mStocks;
	
	private EditText mInOutStockEdit;
	private Spinner mOutSpinner;
	private Spinner mInSpinner;
	private ArrayAdapter<String> mAdapter;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.stock_tranfer_fail);
				break;
			case HandlerCases.UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.stock_tranfer_fail);
				break;
			case HandlerCases.UPDATE_SUCCESS:
				Util.toast(getApplicationContext(), R.string.stock_tranfer_success);
				finish();
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stock_transfer_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.stock_transfer);
		actionBar.setDisplayHomeAsUpEnabled(true);
	
		mSpecId = getIntent().getIntExtra(Extras.SPEC_ID, -1);
		mPositionIds = getIntent().getIntArrayExtra(Extras.POSITION_IDS);
		mPositionNames = 
				getIntent().getStringArrayExtra(Extras.POSITION_NAMES);
		mStocks = getIntent().getStringArrayExtra(Extras.STOCKS);
		
		mInOutStockEdit = (EditText) findViewById(R.id.in_out_stock_edit);
	 	mOutSpinner = (Spinner) findViewById(R.id.out_position_names_spinner);
	 	mInSpinner = (Spinner) findViewById(R.id.in_position_names_spinner);
	 	
 		initSpinners();
	 	
	 	mInOutStockEdit.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (EditorInfo.IME_ACTION_DONE == actionId ||
						EditorInfo.IME_NULL == actionId && 
						KeyEvent.ACTION_DOWN == event.getAction()) {
					tranfer();
					return true;
				}
				return false;
			}
		});
	}
	
	private void tranfer() {
		if(mWhichOut == mWhichIn) {
			Util.toast(getApplicationContext(), R.string.same_position_error);
			return;
		}
		
		String diffStr = mInOutStockEdit.getText().toString();
		if (!Util.checkValidAndToast(getApplicationContext(), diffStr))
			return;
		
		double diff = Double.parseDouble(diffStr);
		double outStock = Double.parseDouble(mStocks[mWhichOut]);
		double inStock = Double.parseDouble(mStocks[mWhichIn]);
		
		mStocks[mWhichOut] = Util.trimDouble(String.valueOf(outStock - diff));
		mStocks[mWhichIn] = Util.trimDouble(String.valueOf(inStock + diff));
		
		new WDTUpdate().stockTransfer(
				Globals.getWarehouseId(getApplicationContext()), mSpecId, 
				mPositionIds[mWhichOut], mPositionIds[mWhichIn], mStocks[mWhichOut], 
				mStocks[mWhichIn], StockTransferActivity.this);
	}
	
	private void newPosition() {
		Intent intent = new Intent(StockTransferActivity.this, 
				NewPositonActivity.class);
		intent.putExtra(Extras.SPEC_ID, 
				getIntent().getIntExtra(Extras.SPEC_ID, -1));
		intent.putExtra(Extras.WAREHOUSE_ID, 
				getIntent().getIntExtra(Extras.WAREHOUSE_ID, -1));
		startActivityForResult(intent, REQUEST_NEW_POSITION);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.stock_transfer_acitons, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.new_position_action:
			newPosition();
			break;
		case R.id.ok_action:
			tranfer();
			break;
		default:
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void initSpinners() {
		mAdapter = new ArrayAdapter<String>(
	 			this, R.layout.simple_spinner_dropdown_item, mPositionNames);
	 	mOutSpinner.setAdapter(mAdapter);
	 	mOutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichOut = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	 	mInSpinner.setAdapter(mAdapter);
	 	mInSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				mWhichIn = position;				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (REQUEST_NEW_POSITION == requestCode) {
			if (RESULT_OK == resultCode) {
				int newPosId = data.getIntExtra(Extras.POSITION_ID, -1);
				String newPosName = 
						data.getStringExtra(Extras.POSITION_NAME);
				
				for (int i = 0; i < mPositionIds.length; i++)
					if (newPosId == mPositionIds[i]) {
						int pos = mAdapter.getPosition(newPosName);
						mInSpinner.setSelection(pos);
						return;
					}
						
				int[] posIds = new int[mPositionIds.length + 1];
				for (int i = 0; i < mPositionIds.length; i++) {
					posIds[i] = mPositionIds[i];
				}
				posIds[mPositionIds.length] = newPosId;
				String[] posNames = new String[mPositionNames.length + 1];
				for (int i = 0; i < mPositionNames.length; i ++) {
					posNames[i] = mPositionNames[i];
				}
				posNames[mPositionNames.length] = newPosName;
				
				String[] stocks = new String[mStocks.length + 1];
				for (int i = 0; i < mStocks.length; i++) {
					stocks[i] = mStocks[i];
				}
				stocks[mStocks.length] = "0.0000";
				
				mPositionIds = posIds;
				mPositionNames = posNames;
				mStocks = stocks;
				
				mAdapter = new ArrayAdapter<String>(this, 
						android.R.layout.simple_spinner_dropdown_item, mPositionNames);
				mOutSpinner.setAdapter(mAdapter);
				mInSpinner.setAdapter(mAdapter);
				mInSpinner.setSelection(mPositionNames.length - 1);
			}
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
