package com.zsxj.pda.ui.client;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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

import com.zsxj.pda.provider.ProviderContract.PdInfo;
import com.zsxj.pda.util.ConstParams.Extras;
import com.zsxj.pda.util.ConstParams.HandlerCases;
import com.zsxj.pda.util.ConstParams.ScanType;
import com.zsxj.pda.util.Util;
import com.zsxj.pdaonphone.R;

public class PdActivity extends Activity{
	
	private TextView mSpecBarcodeTv;
	private TextView mGoodsNumTv;
	private TextView mGoodsNameTv;
	private TextView mBarcodeTv;
	private TextView mSpecCodeTv;
	private TextView mSpecNameTv;
	private Spinner mPositionNamesSp;
	private TextView mStockOldTv;
	private EditText mStockPdEdit;
	
	private String mBarcode;
	
	private int mPdId;
	private int mWhichPosition;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerCases.SCAN_RESULT:
				Intent startScan = 
					new Intent(getApplicationContext(), ScanAndListActivity.class);
				startScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startScan.putExtra(Extras.SCAN_TYPE, ScanType.TYPE_PD);
				startScan.putExtra(Extras.PD_ID, mPdId);
				startScan.putExtra(Extras.BARCODE, mBarcode);
				startActivity(startScan);
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
		setContentView(R.layout.pd_activity);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.pd);
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
		mStockOldTv = (TextView) findViewById(R.id.stock_old_tv);
		final String[] stocksOld = getIntent().getStringArrayExtra(Extras.STOCKS_OLD);
		final String[] stocksPd = getIntent().getStringArrayExtra(Extras.STOCKS_PD);
		mPositionNamesSp.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						mWhichPosition = position;
						mStockOldTv.setText(stocksOld[mWhichPosition]);	
						mStockPdEdit.setText(stocksPd[mWhichPosition]);
						mStockPdEdit.setSelection(mStockPdEdit.getText().length());
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
		
		mPdId = getIntent().getIntExtra(Extras.PD_ID, -1);
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
		int[] recIds = getIntent().getIntArrayExtra(Extras.RECORD_IDS);
		String stockPd = mStockPdEdit.getText().toString();
	
		ContentValues values = new ContentValues();
		if (!TextUtils.isEmpty(stockPd)) {
			values.put(PdInfo.COLUMN_NAME_STOCK_PD, Double.parseDouble(stockPd));
		} else {
			values.putNull(PdInfo.COLUMN_NAME_STOCK_PD);
		}
		String selection = PdInfo.COLUMN_NAME_PD_ID + "=? AND "
				+ PdInfo.COLUMN_NAME_REC_ID + "=?";
		String[] selectionArgs = {
				mPdId + "",
				recIds[mWhichPosition] + "",
		};
		int rows = getContentResolver().update(PdInfo.CONTENT_URI, values, 
			selection, selectionArgs);
		if (rows > 0) {
			Util.toast(getApplicationContext(), "盘点成功");
		} else {
			Util.toast(getApplicationContext(), "盘点失败");
		}
		finish();
	}

}
