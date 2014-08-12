package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.PrefKeys;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Globals;
import com.zsxj.pda.util.Util;
import com.zsxj.pda.wdt.Account;
import com.zsxj.pda.wdt.WDTException;
import com.zsxj.pda.wdt.WDTUpdate;
import com.zsxj.pda.wdt.WDTUpdate.UpdateCallBack;
import com.zsxj.pdaonphone.R;

public class FastPdActivity extends Activity implements UpdateCallBack {
	
	private TextView mSpecBarcodeTv;
	private TextView mGoodsNumTv;
	private TextView mGoodsNameTv;
	private TextView mBarcodeTv;
	private TextView mSpecCodeTv;
	private TextView mSpecNameTv;
	private Spinner mPositionNamesSp;
	private TextView mStockTv;
	private Spinner mAccountsSp;
	private EditText mStockPdEdit;
	
	private int mWhichPosition;
	private int mWhichAccount;
	
	private String mBarcode;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.NO_CONN:
				Util.toast(getApplicationContext(), R.string.no_conn);
				break;
			case HandlerCases.PREPARE_UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.fast_pd_fail);
				break;
			case HandlerCases.UPDATE_FAIL:
				Util.toast(getApplicationContext(), R.string.fast_pd_fail);
				break;
			case HandlerCases.UPDATE_SUCCESS:
				Util.toast(getApplicationContext(), R.string.fast_pd_success);
				finish();
				break;
			case HandlerCases.SCAN_RESULT:
				Intent startScan = 
					new Intent(getApplicationContext(), ScanAndListActivity.class);
				startScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startScan.putExtra(Extras.BARCODE, mBarcode);
				startScan.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_FAST_PD);
				startActivity(startScan);
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fast_pd_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.fast_pd);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		final View rootView = findViewById(R.id.root);
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
				if (heightDiff > 300) {
					getActionBar().hide();
				} else {
					getActionBar().show();
				}
			}
		});
		
		mSpecBarcodeTv = (TextView) findViewById(R.id.spec_barcode_tv);
		String specBarcode = getIntent().getStringExtra(Extras.SPEC_BARCODE);
		mSpecBarcodeTv.setText(specBarcode);
		
		mGoodsNumTv = (TextView) findViewById(R.id.goods_num_tv);
		String goodsNum = getIntent().getStringExtra(Extras.GOODS_NUM);
		mGoodsNumTv.setText(goodsNum);
		
		mGoodsNameTv = (TextView) findViewById(R.id.goods_name_tv);
		String goodsName = getIntent().getStringExtra(Extras.GOODS_NAME);
		mGoodsNameTv.setText(goodsName);
		
		mBarcodeTv = (TextView) findViewById(R.id.barcode_tv);
		String barcode = getIntent().getStringExtra(Extras.BARCODE);
		mBarcodeTv.setText(barcode);
		
		mSpecCodeTv = (TextView) findViewById(R.id.spec_code_tv);
		String specCode = getIntent().getStringExtra(Extras.SPEC_CODE);
		mSpecCodeTv.setText(specCode);
		
		mSpecNameTv = (TextView) findViewById(R.id.spec_name_tv);
		String specName = getIntent().getStringExtra(Extras.SPEC_NAME);
		mSpecNameTv.setText(specName);
		
		mPositionNamesSp = (Spinner) findViewById(R.id.position_names_sp);
		String[] positionNames = getIntent().getStringArrayExtra(Extras.POSITION_NAMES);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				R.layout.simple_spinner_item, 
				positionNames);
		mPositionNamesSp.setAdapter(adapter);
		mStockTv = (TextView) findViewById(R.id.stock_can_order_btn);
		final String[] stocks = getIntent().getStringArrayExtra(Extras.STOCKS);
		mPositionNamesSp.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						mWhichPosition = position;
						mStockTv.setText(stocks[mWhichPosition]);				
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
		});
		
		mAccountsSp = (Spinner) findViewById(R.id.accounts_sp);
		Account[] accounts = Globals.getAccounts();
		String[] accountNames = Account.getNames(accounts);
		adapter = new ArrayAdapter<String>(this, 
				R.layout.simple_spinner_item, accountNames);
		mAccountsSp.setAdapter(adapter);
		mAccountsSp.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						mWhichAccount = position;						
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
		});
		
		mStockPdEdit = (EditText) findViewById(R.id.stock_pd_edit);
		mStockPdEdit.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE ||
						EditorInfo.IME_NULL == actionId &&
						KeyEvent.ACTION_DOWN == event.getAction()) {
					update();
					return true;
				} else {
					return false;
				}
			}
		});
		mStockPdEdit.requestFocus();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.ok_action, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.ok_action:
			update();
			break;
		default:
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	

	
	private void update() {
		int specId = getIntent().getIntExtra(Extras.SPEC_ID, -1);
		int[] positionIds = getIntent().getIntArrayExtra(Extras.POSITION_IDS);
		String stockPd = mStockPdEdit.getText().toString();
		
		if (!Util.checkValidAndToast(getApplicationContext(), stockPd))
			return;
		
		String infoStr = specId + "," + positionIds[mWhichPosition] + "," + stockPd;
		
		Account[] accounts = Globals.getAccounts();
		int accountId = accounts[mWhichAccount].accountId;
		SharedPreferences prefs = getSharedPreferences(Globals.getUserPrefsName(), 
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PrefKeys.WHICH_ACCOUNT, mWhichAccount);
		editor.commit();
		
		new WDTUpdate().fastPd(
			Globals.getUserId(), 
			Globals.getWarehouseId(this), 
			infoStr, accountId, this);
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
